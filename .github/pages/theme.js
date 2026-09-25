// Blocking in <head> so a stored override applies before first paint.
// No override: no data-theme, CSS follows prefers-color-scheme live.
(() => {
    const KEY = 'theme';
    const root = document.documentElement;
    const media = window.matchMedia('(prefers-color-scheme: dark)');
    const system = () => (media.matches ? 'dark' : 'light');
    const effective = () => root.dataset.theme ?? system();

    const store = theme => {
        try {
            if (theme) localStorage.setItem(KEY, theme);
            else localStorage.removeItem(KEY);
        } catch {
            // Storage blocked: choice not persisted.
        }
    };

    try {
        const stored = localStorage.getItem(KEY);
        if (stored === 'light' || stored === 'dark') root.dataset.theme = stored;
    } catch {
        // Storage blocked: follow the OS.
    }

    document.addEventListener('DOMContentLoaded', () => {
        const button = document.getElementById('theme-toggle');
        const sync = () => {
            const dark = effective() === 'dark';
            // SVGElement has no `hidden` property; set the attribute directly.
            button.querySelector('.icon-light').toggleAttribute('hidden', dark);
            button.querySelector('.icon-dark').toggleAttribute('hidden', !dark);
            button.setAttribute('aria-label', dark ? 'Switch to light theme' : 'Switch to dark theme');
        };

        button.addEventListener('click', () => {
            const next = effective() === 'dark' ? 'light' : 'dark';
            // Matching the OS drops the override so OS changes are followed again.
            if (next === system()) {
                delete root.dataset.theme;
                store(null);
            } else {
                root.dataset.theme = next;
                store(next);
            }
            sync();
        });
        media.addEventListener('change', sync);

        sync();
        button.hidden = false;
    });
})();
