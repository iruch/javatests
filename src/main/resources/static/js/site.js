(() => {
    const themeKey = 'javatests-theme';
    const themeToggle = document.querySelector('[data-theme-toggle]');
    const themeColor = document.querySelector('meta[name="theme-color"]');
    const systemDark = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;
    const storedTheme = () => {
        try { return localStorage.getItem(themeKey); } catch (e) { return null; }
    };
    const applyTheme = (theme) => {
        const dark = theme === 'dark';
        document.documentElement.dataset.theme = dark ? 'dark' : 'light';
        if (themeColor) themeColor.content = dark ? '#151b23' : '#f6f8fa';
        if (themeToggle) {
            const label = dark ? 'Включить светлую тему' : 'Включить тёмную тему';
            themeToggle.setAttribute('aria-label', label);
            themeToggle.title = label;
        }
    };
    applyTheme(document.documentElement.dataset.theme);
    if (themeToggle) {
        themeToggle.addEventListener('click', () => {
            const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
            try { localStorage.setItem(themeKey, next); } catch (e) { /* private mode: theme lasts until reload */ }
            applyTheme(next);
        });
    }
    // Until the user picks a theme, follow the system setting live.
    systemDark?.addEventListener('change', (event) => {
        if (!storedTheme()) applyTheme(event.matches ? 'dark' : 'light');
    });

    const toggle = document.querySelector('[data-menu-toggle]');
    const backdrop = document.querySelector('[data-menu-close]');
    if (toggle && backdrop) {
        const setOpen = (open) => {
            document.body.classList.toggle('menu-open', open);
            toggle.setAttribute('aria-expanded', String(open));
        };

        toggle.addEventListener('click', () => setOpen(!document.body.classList.contains('menu-open')));
        backdrop.addEventListener('click', () => setOpen(false));
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') setOpen(false);
        });
        document.querySelectorAll('.topic-list a').forEach((link) => {
            link.addEventListener('click', () => setOpen(false));
        });
    }

    const rebuildDialog = document.querySelector('[data-rebuild-dialog]');
    if (rebuildDialog && typeof rebuildDialog.showModal === 'function') {
        const steps = rebuildDialog.querySelectorAll('[data-rebuild-step]');
        const showStep = (name) => steps.forEach((step) => { step.hidden = step.dataset.rebuildStep !== name; });
        const selectOs = (os) => {
            rebuildDialog.querySelectorAll('[data-os-tab]').forEach((tab) => {
                tab.setAttribute('aria-selected', String(tab.dataset.osTab === os));
            });
            rebuildDialog.querySelectorAll('[data-os-panel]').forEach((panel) => {
                panel.hidden = panel.dataset.osPanel !== os;
            });
        };
        const platform = (navigator.userAgentData?.platform || navigator.platform || navigator.userAgent).toLowerCase();
        const userOs = platform.includes('win') ? 'windows' : platform.includes('mac') ? 'mac' : 'linux';

        document.querySelector('[data-rebuild-open]')?.addEventListener('click', () => {
            showStep('confirm');
            rebuildDialog.showModal();
        });
        rebuildDialog.querySelectorAll('[data-rebuild-close]').forEach((button) => {
            button.addEventListener('click', () => rebuildDialog.close());
        });
        rebuildDialog.addEventListener('click', (event) => {
            if (event.target === rebuildDialog) rebuildDialog.close();
        });
        rebuildDialog.querySelector('[data-rebuild-yes]').addEventListener('click', () => {
            selectOs(userOs);
            showStep('run');
        });
        rebuildDialog.querySelectorAll('[data-os-tab]').forEach((tab) => {
            tab.addEventListener('click', () => selectOs(tab.dataset.osTab));
        });
        rebuildDialog.querySelectorAll('[data-copy]').forEach((button) => {
            button.addEventListener('click', async () => {
                try {
                    await navigator.clipboard.writeText(button.dataset.copy);
                    button.textContent = 'Скопировано';
                } catch (e) {
                    button.textContent = 'Не удалось';
                }
                setTimeout(() => { button.textContent = 'Скопировать'; }, 1500);
            });
        });
    }

    // A report download is a pair of files: the agent prompt (.md) and the answers (.json).
    const downloadFile = (url) => {
        const link = document.createElement('a');
        link.href = url;
        link.download = '';
        document.body.append(link);
        link.click();
        link.remove();
    };
    document.addEventListener('click', (event) => {
        const button = event.target.closest('[data-download-files]');
        if (!button || !button.dataset.downloadFiles) return;
        const base = button.dataset.downloadFiles;
        downloadFile(`${base}/md`);
        setTimeout(() => downloadFile(`${base}/json`), 500);
    });

    document.addEventListener('submit', (event) => {
        const form = event.target.closest('form[data-confirm]');
        if (form && !window.confirm(form.dataset.confirm)) event.preventDefault();
    });

    document.querySelectorAll('[data-history-toggle]').forEach((row) => {
        const details = document.getElementById(row.dataset.historyToggle);
        const setExpanded = (expanded) => {
            row.setAttribute('aria-expanded', String(expanded));
            row.classList.toggle('expanded', expanded);
            details.hidden = !expanded;
        };
        const isControl = (event) => event.target.closest('a, button, form, input, select, textarea');
        row.addEventListener('click', (event) => {
            if (!isControl(event)) setExpanded(details.hidden);
        });
        row.addEventListener('keydown', (event) => {
            if (isControl(event)) return;
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                setExpanded(details.hidden);
            }
        });
        if (window.location.hash === `#${row.id}`) {
            setExpanded(true);
            row.scrollIntoView({ block: 'start' });
        }
    });
})();
