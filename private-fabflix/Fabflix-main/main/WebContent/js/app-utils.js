function getUrlParam(name) {
    const url = new URL(window.location.href);
    return url.searchParams.get(name);
}

function goBackOrFallback(fallbackUrl) {
    if (document.referrer && window.history.length > 1) {
        window.history.back();
        return;
    }
    window.location.href = fallbackUrl;
}

function hideBackButtonWithoutHistory(buttonId) {
    const button = document.getElementById(buttonId);
    if (button && (!document.referrer || window.history.length <= 1)) {
        button.style.display = "none";
    }
}

function reloadOnHistoryNavigation() {
    window.addEventListener("pageshow", function (event) {
        const navigationEntries = window.performance.getEntriesByType("navigation");
        const isBackForward = navigationEntries.length > 0 && navigationEntries[0].type === "back_forward";
        if (event.persisted || isBackForward) {
            window.location.reload();
        }
    });
}

function addMovieToCart(movieId) {
    return $.ajax({
        method: "POST",
        url: "api/add-to-cart",
        data: {movieId}
    });
}
