function getQueryParams() {
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

function getSortParams() {
    const value = sessionStorage.getItem("sort") || "title-asc";
    let sort1 = "title", dir1 = "asc", sort2 = "rating", dir2 = "asc";

    switch (value) {
        case "title-desc": dir1 = "desc"; dir2 = "desc"; break;
        case "title-asc-rating-desc": dir1 = "asc"; dir2 = "desc"; break;
        case "title-desc-rating-asc": dir1 = "desc"; dir2 = "asc"; break;
        case "rating-asc": sort1 = "rating"; dir1 = "asc"; sort2 = "title"; dir2 = "asc"; break;
        case "rating-desc": sort1 = "rating"; dir1 = "desc"; sort2 = "title"; dir2 = "desc"; break;
        case "rating-asc-title-desc": sort1 = "rating"; dir1 = "asc"; sort2 = "title"; dir2 = "desc"; break;
        case "rating-desc-title-asc": sort1 = "rating"; dir1 = "desc"; sort2 = "title"; dir2 = "asc"; break;
    }

    return { sort1, dir1, sort2, dir2 };
}

function addToCart(movieId) {
    $.ajax({
        type: "POST",
        url: "api/add-to-cart",
        data: { movieId },
        success: (response) => alert(response.message),
        error: () => alert("Failed to add movie to cart.")
    });
}

function handleMovieData(data, pageSize, isTop20) {
    const tbody = $("#movie_table_body").empty();
    $("#loading").hide();
    $("#movie_table_container").show();

    data.forEach(movie => {
        const stars = movie.stars ? movie.stars.split(',') : [];
        const ids = movie.star_ids ? movie.star_ids.split(',') : [];
        const starLinks = stars.slice(0, 3).map((s, i) =>
            `<a href="single-star.html?id=${ids[i]}">${s.trim()}</a>`).join(", ");

        const genreLinks = (movie.genres || "")
            .split(/,\s*/)
            .slice(0, 3)
            .map(g => `<a href="movie-list.html?genre=${encodeURIComponent(g)}">${g}</a>`)
            .join(", ");

        tbody.append(`
            <tr>
                <td><a href="single-movie.html?id=${movie.movie_id}">${movie.title}</a></td>
                <td>${movie.year}</td>
                <td>${movie.director}</td>
                <td>${genreLinks}</td>
                <td>${starLinks}</td>
                <td>${movie.rating}</td>
                <td><button class="btn btn-sm btn-success" onclick="addToCart('${movie.movie_id}')">Add to Cart</button></td>
            </tr>
        `);
    });

    const currentPage = parseInt(sessionStorage.getItem("page") || "1");
    $("#prev-page").prop("disabled", currentPage === 1);
    $("#next-page").prop("disabled", data.length < pageSize);
    if (isTop20 && pageSize > 10) {
        $("#prev-page").prop("disabled", true);
        $("#next-page").prop("disabled", true);
    }
}

function fetchMovies() {
    const query = getQueryParams();
    const sort = getSortParams();
    const page = parseInt(sessionStorage.getItem("page") || "1");
    const pageSize = parseInt(sessionStorage.getItem("pageSize") || "10");

    const params = {
        page,
        pageSize,
        sort1: sort.sort1,
        dir1: sort.dir1,
        sort2: sort.sort2,
        dir2: sort.dir2
    };

    $("#current-page").text("Page " + page);

    const url = query.q
        ? `api/fulltext-search?q=${encodeURIComponent(query.q)}`
        : "api/search";

    $.ajax({
        method: "GET",
        url: url,
        data: query.q ? params : { ...query, ...params },
        success: (data) => handleMovieData(data, pageSize, query.top20 === "true"),
        error: () => $("#loading").text("Failed to load movie data.")
    });
}

$(document).ready(function () {
    const queryNow = JSON.stringify(getQueryParams());
    const previousQuery = sessionStorage.getItem("lastQuery");

    if (queryNow !== previousQuery) {
        sessionStorage.clear();  // Reset when search condition changes
        sessionStorage.setItem("lastQuery", queryNow);
        sessionStorage.setItem("page", "1");
    }

    $("#sort-select").val(sessionStorage.getItem("sort") || "title-asc");
    $("#page-size-select").val(sessionStorage.getItem("pageSize") || "10");

    fetchMovies();

    $("#sort-select").on("change", function () {
        sessionStorage.setItem("sort", $(this).val());
        sessionStorage.setItem("page", "1");
        fetchMovies();
    });

    $("#page-size-select").on("change", function () {
        sessionStorage.setItem("pageSize", $(this).val());
        sessionStorage.setItem("page", "1");
        fetchMovies();
    });

    $("#prev-page").on("click", function () {
        const page = Math.max(1, parseInt(sessionStorage.getItem("page") || "1") - 1);
        sessionStorage.setItem("page", page.toString());
        fetchMovies();
    });

    $("#next-page").on("click", function () {
        const page = parseInt(sessionStorage.getItem("page") || "1") + 1;
        sessionStorage.setItem("page", page.toString());
        fetchMovies();
    });
});
