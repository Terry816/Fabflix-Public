function handleMovieData(data, pageSize, isTop20) {
    const tbody = $("#movie_table_body").empty();
    $("#loading").hide();
    $("#movie_table_container").show();

    data.forEach(movie => {
        const starLinks = buildStarLinks(movie.stars, movie.star_ids, 3);
        const genreLinks = buildGenreLinks(movie.genres, 3);

        tbody.append(`
            <tr>
                <td><a href="single-movie.html?id=${movie.movie_id}">${movie.title}</a></td>
                <td>${movie.year}</td>
                <td>${movie.director}</td>
                <td>${genreLinks}</td>
                <td>${starLinks}</td>
                <td>${movie.rating}</td>
                <td><button class="btn btn-sm btn-success add-to-cart-btn" data-movie-id="${movie.movie_id}">Add to Cart</button></td>
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
    const query = getMovieListQueryParams();
    const sort = getMovieSortParams();
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
    const queryNow = JSON.stringify(getMovieListQueryParams());
    const previousQuery = sessionStorage.getItem("lastQuery");

    if (queryNow !== previousQuery) {
        sessionStorage.clear();
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

    $("#movie_table_body").on("click", ".add-to-cart-btn", function () {
        const movieId = $(this).data("movie-id");
        addMovieToCart(movieId)
            .done((response) => alert(response.message))
            .fail(() => alert("Failed to add movie to cart."));
    });
});
