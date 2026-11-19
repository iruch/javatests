(() => {
    const data = window.JAVA_TEST;
    const wizard = document.querySelector('[data-wizard]');
    if (!data || !wizard || !data.questions.length) return;

    const $ = (selector, root = document) => root.querySelector(selector);
    const questions = data.questions;
    const limitMs = data.secondsPerQuestion * 1000;
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
        finished: $('[data-finished]'),
        stats: $('[data-stats]'),
    };

    let promptTemplate = null;
    fetch(wizard.dataset.promptUrl)
        .then((response) => (response.ok ? response.text() : Promise.reject(response.status)))
        .then((text) => { promptTemplate = text; })
        .catch(() => { promptTemplate = null; });

    document.querySelectorAll('[data-restart]').forEach((link) => { link.href = window.location.href; });

    const formatClock = (ms) => {
        const total = Math.max(0, Math.ceil(ms / 1000));
        return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
    };

    const formatDuration = (ms) => {
        const total = Math.round(ms / 1000);
        const minutes = Math.floor(total / 60);
        return minutes ? `${minutes} мин ${total % 60} с` : `${total % 60} с`;
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
        el.lesson.textContent = q.topic === q.lesson ? q.lesson : `${q.topic} · ${q.lesson}`;
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

    const stop = () => {
        state.running = false;
        clearInterval(state.timerId);
        window.removeEventListener('beforeunload', confirmLeave);
    };

    const fail = (index) => {
        stop();
        state.answers = questions.map(() => '');
        wizard.hidden = true;
        el.failedReason.textContent = `Время на вопрос ${index + 1} из ${questions.length} истекло (2 минуты). Тест сброшен, ответы не сохранены.`;
        el.failed.hidden = false;
        el.failed.scrollIntoView({ block: 'start' });
    };

    const pad = (n) => String(n).padStart(2, '0');
    const fileDate = (date) => `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()}`;

    const buildResult = (finishedAt) => ({
        test: {
            title: data.title,
            date: fileDate(finishedAt),
            startedAt: state.startedAt.toISOString(),
            finishedAt: finishedAt.toISOString(),
            questionCount: questions.length,
            secondsPerQuestion: data.secondsPerQuestion,
        },
        summary: {
            answered: state.answers.filter((a) => a.trim()).length,
            unanswered: state.answers.filter((a) => !a.trim()).length,
            answerViewed: state.viewed.filter(Boolean).length,
            totalTimeSec: Math.round(state.spentMs.reduce((sum, ms) => sum + ms, 0) / 1000),
        },
        answers: questions.map((q, i) => ({
            number: i + 1,
            topic: q.topic,
            lesson: q.lesson,
            question: q.question,
            userAnswer: state.answers[i].trim(),
            answerViewed: state.viewed[i],
            referenceAnswer: q.answer,
            timeSpentSec: Math.round(state.spentMs[i] / 1000),
        })),
    });

    const buildPrompt = (result, resultFile) => {
        const values = {
            date: result.test.date,
            scope: result.test.title,
            questionCount: String(result.test.questionCount),
            answered: String(result.summary.answered),
            viewed: String(result.summary.answerViewed),
            resultFile,
        };
        const template = promptTemplate
            || '# JavaTestAgent\n\nПроверь ответы кандидата из файла `{{resultFile}}`: посчитай процент верных, частичных и неверных ответов, покажи ошибки, как надо было ответить, и подробно разбери каждый вопрос.\n';
        return template.replace(/\{\{(\w+)\}\}/g, (match, key) => (key in values ? values[key] : match));
    };

    const download = (name, content, type) => {
        const url = URL.createObjectURL(new Blob([content], { type }));
        const link = document.createElement('a');
        link.href = url;
        link.download = name;
        document.body.append(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 2000);
    };

    const submit = () => {
        tick();
        if (!state.running) return;
        state.answers[state.index] = el.answer.value;
        const empty = state.answers.filter((a) => !a.trim()).length;
        if (empty && !window.confirm(`Без ответа: ${empty} из ${questions.length}. Всё равно сдать тест?`)) return;
        stop();

        const finishedAt = new Date();
        const stamp = fileDate(finishedAt);
        const promptFile = `JavaTestAgent_${stamp}.md`;
        const resultFile = `JavaTestResult_${stamp}.json`;
        const result = buildResult(finishedAt);
        const prompt = buildPrompt(result, resultFile);
        const json = JSON.stringify(result, null, 2);

        const stats = [
            ['Вопросов', result.test.questionCount],
            ['С ответом', result.summary.answered],
            ['Без ответа', result.summary.unanswered],
            ['Ответ просмотрен', result.summary.answerViewed],
            ['Время', formatDuration(result.summary.totalTimeSec * 1000)],
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
        $('[data-download-prompt]').onclick = () => download(promptFile, prompt, 'text/markdown;charset=utf-8');
        $('[data-download-result]').onclick = () => download(resultFile, json, 'application/json;charset=utf-8');

        wizard.hidden = true;
        el.finished.hidden = false;
        el.finished.scrollIntoView({ block: 'start' });
        download(promptFile, prompt, 'text/markdown;charset=utf-8');
        setTimeout(() => download(resultFile, json, 'application/json;charset=utf-8'), 400);
    };

    function confirmLeave(event) {
        event.preventDefault();
        event.returnValue = '';
    }

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
