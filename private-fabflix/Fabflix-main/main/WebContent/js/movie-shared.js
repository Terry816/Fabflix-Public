function splitCsv(value) {
    return value ? value.split(/\s*,\s*/) : [];
}

function buildGenreLinks(genres, limit) {
    return splitCsv(genres)
        .slice(0, limit)
        .map((genre) => `<a href="movie-list.html?genre=${encodeURIComponent(genre)}">${genre}</a>`)
        .join(", ");
}

function buildStarLinks(stars, starIds, limit) {
    const names = splitCsv(stars);
    const ids = splitCsv(starIds);
    return names
        .slice(0, limit)
        .map((name, index) => `<a href="single-star.html?id=${ids[index]}">${name}</a>`)
        .join(", ");
}

function getMovieListQueryParams() {
    const url = new URL(window.location.href);
    return {
        title: url.searchParams.get("title") || "",
        year: url.searchParams.get("year") || "",
        director: url.searchParams.get("director") || "",
        star: url.searchParams.get("star") || "",
        genre: url.searchParams.get("genre") || "",
        startsWith: url.searchParams.get("startsWith") || "",
        top20: url.searchParams.get("top20") || "",
        q: url.searchParams.get("q") || ""
    };
}

function getMovieSortParams() {
    const value = sessionStorage.getItem("sort") || "title-asc";
    let sort1 = "title";
    let dir1 = "asc";
    let sort2 = "rating";
    let dir2 = "asc";

    switch (value) {
        case "title-desc":
            dir1 = "desc";
            dir2 = "desc";
            break;
        case "title-asc-rating-desc":
            dir2 = "desc";
            break;
        case "title-desc-rating-asc":
            dir1 = "desc";
            break;
        case "rating-asc":
            sort1 = "rating";
            sort2 = "title";
            break;
        case "rating-desc":
            sort1 = "rating";
            dir1 = "desc";
            sort2 = "title";
            dir2 = "desc";
            break;
        case "rating-asc-title-desc":
            sort1 = "rating";
            sort2 = "title";
            dir2 = "desc";
            break;
        case "rating-desc-title-asc":
            sort1 = "rating";
            dir1 = "desc";
            sort2 = "title";
            break;
    }

    return {sort1, dir1, sort2, dir2};
}
