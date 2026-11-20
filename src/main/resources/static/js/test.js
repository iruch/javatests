(() => {
    const data = window.JAVA_TEST;
    const wizard = document.querySelector('[data-wizard]');
    if (!data || !wizard || !data.questions.length) return;

    const $ = (selector, root = document) => root.querySelector(selector);
    const questions = data.questions;
    const limitMs = data.secondsPerQuestion * 1000;
    const saveUrl = wizard.dataset.saveUrl;
    const state = {
        index: 0,
        answers: questions.map(() => ''),
        viewed: questions.map(() => false),
        remainingMs: questions.map(() => limitMs),
        spentMs: questions.map(() => 0),
        startedAt: new Date(),
        lastTick: performance.now(),
        timerId: null,
        running: true,
    };

    const el = {
        current: $('[data-current]', wizard),
        total: $('[data-total]', wizard),
        timer: $('[data-timer]', wizard),
        progress: $('[data-progress]', wizard),
        lesson: $('[data-lesson]', wizard),
        question: $('[data-question]', wizard),
        toggleAnswer: $('[data-toggle-answer]', wizard),
        viewed: $('[data-viewed]', wizard),
        reference: $('[data-reference]', wizard),
        referenceText: $('[data-reference-text]', wizard),
        answer: $('[data-answer]', wizard),
        prev: $('[data-prev]', wizard),
        next: $('[data-next]', wizard),
        submit: $('[data-submit]', wizard),
        failed: $('[data-failed]'),
        failedReason: $('[data-failed-reason]'),
        failedSaved: $('[data-failed-saved]'),
        finished: $('[data-finished]'),
        saveStatus: $('[data-save-status]'),
        stats: $('[data-stats]'),
        saved: $('[data-saved]'),
        saveError: $('[data-save-error]'),
        retry: $('[data-retry-save]'),
    };

    document.querySelectorAll('[data-restart]').forEach((link) => { link.href = window.location.href; });

    const formatClock = (ms) => {
        const total = Math.max(0, Math.ceil(ms / 1000));
        return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
    };

    const tick = () => {
        if (!state.running) return;
        const now = performance.now();
        const elapsed = now - state.lastTick;
        state.lastTick = now;
        state.remainingMs[state.index] -= elapsed;
        state.spentMs[state.index] += elapsed;
        if (state.remainingMs[state.index] <= 0) {
            fail(state.index);
            return;
        }
        renderTimer();
    };

    const renderTimer = () => {
        const remaining = state.remainingMs[state.index];
        el.timer.textContent = formatClock(remaining);
        el.timer.classList.toggle('warning', remaining <= 30000);
    };

    const render = () => {
        const i = state.index;
        const q = questions[i];
        const last = i === questions.length - 1;
        el.current.textContent = String(i + 1);
        el.total.textContent = String(questions.length);
        el.progress.style.width = `${((i + 1) / questions.length) * 100}%`;
        el.lesson.textContent = `${q.topic} · ${q.lesson}`;
        el.question.textContent = q.question;
        el.referenceText.textContent = q.answer;
        el.reference.hidden = true;
        el.toggleAnswer.textContent = 'Показать ответ';
        el.viewed.hidden = !state.viewed[i];
        el.answer.value = state.answers[i];
        el.prev.disabled = i === 0;
        el.next.hidden = last;
        el.submit.hidden = !last;
        renderTimer();
        el.answer.focus({ preventScroll: true });
    };

    const go = (index) => {
        tick();
        if (!state.running) return;
        state.answers[state.index] = el.answer.value;
        state.index = index;
        render();
        wizard.scrollIntoView({ block: 'start', behavior: 'smooth' });
    };

    function confirmLeave(event) {
        event.preventDefault();
        event.returnValue = '';
    }

    const stop = () => {
        state.running = false;
        clearInterval(state.timerId);
        window.removeEventListener('beforeunload', confirmLeave);
    };

    const payload = (status, failedIndex) => ({
        scope: data.scope,
        scopeId: data.scopeId,
        status,
        startedAt: state.startedAt.toISOString(),
        finishedAt: new Date().toISOString(),
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        failedQuestionNumber: failedIndex === undefined ? null : failedIndex + 1,
        answers: questions.map((q, i) => ({
            questionId: q.id,
            userAnswer: state.answers[i],
            answerViewed: state.viewed[i],
            timeSpentSec: Math.round(state.spentMs[i] / 1000),
        })),
    });

    const post = (body) => fetch(saveUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
        keepalive: true,
    }).then((response) => (response.ok ? response.json() : Promise.reject(new Error(`HTTP ${response.status}`))));

    const fail = (index) => {
        stop();
        const body = payload('FAILED', index);
        state.answers = questions.map(() => '');
        wizard.hidden = true;
        el.failedReason.textContent = `Время на вопрос ${index + 1} из ${questions.length} истекло (2 минуты). Тест сброшен, ответы не сохранены.`;
        el.failed.hidden = false;
        el.failed.scrollIntoView({ block: 'start' });
        post(body).then(() => { el.failedSaved.hidden = false; }).catch(() => {});
    };

    const showStats = (attempt) => {
        const stats = [
            ['Вопросов', attempt.questionCount],
            ['С ответом', attempt.answered],
            ['Без ответа', attempt.skipped],
            ['Ответ просмотрен', attempt.viewed],
            ['Время', attempt.totalTime],
        ];
        el.stats.replaceChildren(...stats.map(([label, value]) => {
            const item = document.createElement('div');
            const strong = document.createElement('strong');
            strong.textContent = String(value);
            const caption = document.createElement('span');
            caption.textContent = label;
            item.append(strong, caption);
            return item;
        }));
    };

    const save = (body) => {
        el.saveStatus.textContent = 'Сохраняю результат в историю…';
        el.saveStatus.hidden = false;
        el.saveError.hidden = true;
        post(body)
            .then((result) => {
                showStats(result.attempt);
                document.querySelectorAll('[data-download-variant]').forEach((button) => {
                    button.dataset.downloadFiles = result.downloads[button.dataset.downloadVariant];
                });
                document.querySelectorAll('[data-history-link]').forEach((link) => { link.href = result.historyUrl; });
                el.saveStatus.hidden = true;
                el.saved.hidden = false;
            })
            .catch(() => {
                el.saveStatus.hidden = true;
                el.saveError.hidden = false;
                el.retry.onclick = () => save(body);
            });
    };

    const submit = () => {
        tick();
        if (!state.running) return;
        state.answers[state.index] = el.answer.value;
        const empty = state.answers.filter((a) => !a.trim()).length;
        if (empty && !window.confirm(`Без ответа: ${empty} из ${questions.length}. Всё равно сдать тест?`)) return;
        stop();
        wizard.hidden = true;
        el.finished.hidden = false;
        el.finished.scrollIntoView({ block: 'start' });
        save(payload('SUBMITTED'));
    };

    el.toggleAnswer.addEventListener('click', () => {
        const show = el.reference.hidden;
        el.reference.hidden = !show;
        el.toggleAnswer.textContent = show ? 'Скрыть ответ' : 'Показать ответ';
        if (show) {
            state.viewed[state.index] = true;
            el.viewed.hidden = false;
        }
    });
    el.answer.addEventListener('input', () => { state.answers[state.index] = el.answer.value; });
    el.prev.addEventListener('click', () => go(state.index - 1));
    el.next.addEventListener('click', () => go(state.index + 1));
    el.submit.addEventListener('click', submit);
    window.addEventListener('beforeunload', confirmLeave);

    wizard.hidden = false;
    render();
    state.lastTick = performance.now();
    state.timerId = setInterval(tick, 250);
})();
