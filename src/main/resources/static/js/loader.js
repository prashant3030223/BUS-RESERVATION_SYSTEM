document.addEventListener('DOMContentLoaded', () => {
    const loader = document.getElementById('global-loader');
    
    // Hide loader on initial load
    if (loader) {
        setTimeout(() => {
            loader.classList.add('loader-hidden');
        }, 500); // Small delay for smooth transition
    }

    // Handle link clicks
    document.addEventListener('click', (e) => {
        const link = e.target.closest('a');
        if (link && link.href && !link.href.startsWith('javascript:') && !link.href.includes('#') && link.target !== '_blank') {
            // Check if it's an internal link or same domain
            if (link.hostname === window.location.hostname) {
                // Don't show if it's just a modifier key click (new tab)
                if (!e.ctrlKey && !e.metaKey && !e.shiftKey && !e.altKey) {
                    showLoader();
                }
            }
        }
    });

    // Handle form submissions
    document.addEventListener('submit', (e) => {
        const form = e.target;
        if (!e.defaultPrevented) {
            showLoader();
        }
    });

    // Handle browser back/forward buttons
    window.addEventListener('pageshow', (event) => {
        if (event.persisted) {
            hideLoader();
        }
    });

    function showLoader() {
        if (loader) {
            loader.classList.remove('loader-hidden');
        }
    }

    function hideLoader() {
        if (loader) {
            loader.classList.add('loader-hidden');
        }
    }
});
