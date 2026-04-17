/**
 * Extracts a URL parameter by name
 */
function getParameterByName(target) {
    let url = window.location.href;
    target = target.replace(/[\[\]]/g, "\\$&");
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);

    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

/**
 * Handles the returned movie JSON object and displays it
 */
function handleMovieData(resultData) {
    let movie = resultData[0];

    // Display title and metadata
    let infoSection = jQuery("#movie_info");
    infoSection.append(`
        <div class="movie-title">${movie["title"]}</div>
        <div class="metadata mt-2">
            <p><strong>Year:</strong> ${movie["year"]}</p>
            <p><strong>Director:</strong> ${movie["director"]}</p>
            <p><strong>Rating:</strong> ${movie["rating"]}</p>
        </div>
    `);

    // ✅ GENRE HYPERLINK FIX:
    let genreLinks = movie["genres"]
        .split(/,\s*/)
        .map(g => `<a href="movie-list.html?genre=${encodeURIComponent(g)}">${g}</a>`)
        .join(", ");

    // Existing STAR logic (unchanged):
    let stars = movie["stars"].split(", ");
    let starIds = movie["star_ids"].split(",");
    let starLinks = stars.map((name, i) =>
        `<a href="single-star.html?id=${starIds[i]}">${name}</a>`
    );

    // ✅ Insert into table:
    let rowHTML = "<tr>";
    rowHTML += "<td>" + genreLinks + "</td>";
    rowHTML += "<td>" + starLinks.join(", ") + "</td>";
    rowHTML += "</tr>";

    jQuery("#movie_detail_table").append(rowHTML);

    // Add event listener for Add to Cart button
    jQuery("#add-to-cart-btn").on("click", function () {
        jQuery.ajax({
            type: "POST",
            url: "api/add-to-cart",
            data: { movieId: movie["movie_id"] },
            success: function (response) {
                alert(response.message);
            },
            error: function () {
                alert("Failed to add movie to cart.");
            }
        });
    });

}

let movieId = getParameterByName("id");

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: `api/single-movie?id=${movieId}`,
    success: (data) => handleMovieData(data)
});
