(() => {
    const toggle = document.querySelector('[data-menu-toggle]');
    const backdrop = document.querySelector('[data-menu-close]');
    if (!toggle || !backdrop) return;

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
})();
