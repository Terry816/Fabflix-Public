function handleLookup(query, doneCallback) {
    console.log("autocomplete initiated");
    console.log("Sending AJAX to backend");

    const cached = sessionStorage.getItem(query);
    if (cached) {
        console.log("✅ Using cached result");
        const parsed = JSON.parse(cached);
        doneCallback({ suggestions: parsed });
        console.log("📦 Used suggestions:", parsed);
        return;
    }

    jQuery.ajax({
        method: "GET",
        url: `api/autocomplete?query=${escape(query)}`,
        success: (data) => {
            console.log("🚨 Raw suggestions from backend:", data);
            const safeData = data.filter(s => s && typeof s.value === 'string');
            sessionStorage.setItem(query, JSON.stringify(safeData));
            doneCallback({ suggestions: safeData });
            console.log("✅ Cleaned suggestions:", safeData);
        },
        error: (err) => {
            console.error("❌ Autocomplete AJAX error", err);
        }
    });
}

function handleSelectSuggestion(suggestion) {
    const movieId = suggestion.data.movieId;
    console.log("🎯 Selected:", suggestion.value, "→", movieId);
    window.location.href = `single-movie.html?id=${movieId}`;
}

$('#nav-autocomplete').autocomplete({
    lookup: (query, doneCallback) => {
        if (query.length >= 3) {
            handleLookup(query, doneCallback);
        }
    },
    onSelect: handleSelectSuggestion,
    minChars: 3,
    deferRequestBy: 300,
    triggerSelectOnValidInput: true,
    preserveInput: false,
    lookupLimit: 10,
    autoSelectFirst: false,
    appendTo: "body",
    showNoSuggestionNotice: false,

    // ✅ FIXED: Return raw highlighted title only, plugin wraps it automatically
    formatResult: function (suggestion, currentValue) {
        if (!suggestion || !suggestion.value) return "";
        const pattern = new RegExp("(" + currentValue.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&') + ")", "gi");
        return suggestion.value.replace(pattern, "<strong>$1</strong>");
    }
});

$('#nav-autocomplete').keypress(function (event) {
    if (event.keyCode === 13 && $('.autocomplete-selected').length === 0) {
        const query = $('#nav-autocomplete').val().trim();
        if (query.length > 0) {
            handleNormalSearch(query);  // ✅ Allow any query size for search
        }
    }
});

$('#nav-search-button').on('click', function () {
    const query = $('#nav-autocomplete').val();
    handleNormalSearch(query);
});

function handleNormalSearch(query) {
    console.log("🔍 Full-text search triggered by Enter:", query);
    window.location.href = `movie-list.html?q=${encodeURIComponent(query)}`;
}
