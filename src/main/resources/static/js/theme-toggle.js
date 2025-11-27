/**
 * Theme Toggle System
 * ===================
 * Professional Dark/Light Mode Toggle with LocalStorage Persistence
 */

(function () {
    'use strict';

    const THEME_KEY = 'bus-reservation-theme';
    const THEME_DARK = 'dark';
    const THEME_LIGHT = 'light';

    // Get saved theme or default to dark
    function getSavedTheme() {
        return localStorage.getItem(THEME_KEY) || THEME_DARK;
    }

    // Save theme to localStorage
    function saveTheme(theme) {
        localStorage.setItem(THEME_KEY, theme);
    }

    // Apply theme to document
    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);

        // Update all toggle buttons
        const toggleButtons = document.querySelectorAll('.theme-toggle-btn');
        toggleButtons.forEach(btn => {
            const moonIcon = btn.querySelector('.fa-moon');
            const sunIcon = btn.querySelector('.fa-sun');

            if (theme === THEME_LIGHT) {
                btn.setAttribute('aria-label', 'Switch to dark mode');
                btn.setAttribute('title', 'Switch to dark mode');
            } else {
                btn.setAttribute('aria-label', 'Switch to light mode');
                btn.setAttribute('title', 'Switch to light mode');
            }
        });

        // Dispatch custom event for other scripts
        window.dispatchEvent(new CustomEvent('themeChanged', { detail: { theme } }));
    }

    // Toggle between themes
    function toggleTheme() {
        const currentTheme = document.documentElement.getAttribute('data-theme') || THEME_DARK;
        const newTheme = currentTheme === THEME_DARK ? THEME_LIGHT : THEME_DARK;

        applyTheme(newTheme);
        saveTheme(newTheme);

        // Add animation class
        document.body.classList.add('theme-transitioning');
        setTimeout(() => {
            document.body.classList.remove('theme-transitioning');
        }, 400);
    }

    // Initialize theme on page load
    function initTheme() {
        const savedTheme = getSavedTheme();
        applyTheme(savedTheme);

        // Add event listeners to all theme toggle buttons
        document.addEventListener('click', function (e) {
            const toggleBtn = e.target.closest('.theme-toggle-btn');
            if (toggleBtn) {
                e.preventDefault();
                toggleTheme();
            }
        });

        // Keyboard accessibility
        document.addEventListener('keydown', function (e) {
            const toggleBtn = e.target.closest('.theme-toggle-btn');
            if (toggleBtn && (e.key === 'Enter' || e.key === ' ')) {
                e.preventDefault();
                toggleTheme();
            }
        });

        // Listen for system theme changes (optional)
        if (window.matchMedia) {
            const darkModeQuery = window.matchMedia('(prefers-color-scheme: dark)');

            // Only apply system preference if user hasn't set a preference
            if (!localStorage.getItem(THEME_KEY)) {
                applyTheme(darkModeQuery.matches ? THEME_DARK : THEME_LIGHT);
            }

            // Listen for changes
            darkModeQuery.addEventListener('change', (e) => {
                // Only auto-switch if user hasn't manually set a preference recently
                const lastManualChange = localStorage.getItem('theme-manual-change-time');
                const now = Date.now();

                if (!lastManualChange || (now - parseInt(lastManualChange)) > 3600000) { // 1 hour
                    applyTheme(e.matches ? THEME_DARK : THEME_LIGHT);
                    saveTheme(e.matches ? THEME_DARK : THEME_LIGHT);
                }
            });
        }
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initTheme);
    } else {
        initTheme();
    }

    // Expose toggle function globally for manual triggers
    window.toggleTheme = toggleTheme;

    // Track manual theme changes
    window.addEventListener('themeChanged', () => {
        localStorage.setItem('theme-manual-change-time', Date.now().toString());
    });

})();

/**
 * Theme Toggle Button Creator
 * Creates a theme toggle button dynamically
 */
function createThemeToggleButton() {
    const button = document.createElement('button');
    button.className = 'theme-toggle-btn';
    button.setAttribute('aria-label', 'Toggle theme');
    button.setAttribute('title', 'Toggle theme');
    button.setAttribute('type', 'button');

    button.innerHTML = `
        <div class="theme-toggle-icons">
            <i class="fas fa-moon"></i>
            <i class="fas fa-sun"></i>
        </div>
        <div class="theme-toggle-slider">
            <i class="fas fa-moon"></i>
        </div>
    `;

    return button;
}

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { createThemeToggleButton };
}
