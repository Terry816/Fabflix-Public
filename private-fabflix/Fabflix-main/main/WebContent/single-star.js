function handleResult(resultData) {
    const star = resultData[0];
    if (!star) {
        jQuery("#star_info").html("<p>Star not found.</p>");
        return;
    }

    const starInfoElement = jQuery("#star_info");
    starInfoElement.append(`
        <div class="star-name">${star["star_name"]}</div>
        <p><strong>Date of Birth:</strong> ${star["birthYear"] ? star["birthYear"] : "N/A"}</p>
    `);

    const movieTableBodyElement = jQuery("#movie_table_body");

    for (const item of resultData) {
        let rowHTML = "<tr>";
        rowHTML += "<td><a href='single-movie.html?id=" + item["movie_id"] + "'>" + item["movie_title"] + "</a></td>";
        rowHTML += "<td>" + item["movie_year"] + "</td>";
        rowHTML += "<td>" + item["director"] + "</td>";
        rowHTML += `<td>
            <button class="btn btn-sm btn-success add-to-cart-btn" data-movie-id="${item["movie_id"]}">
                Add to Cart
            </button>
        </td>`;
        rowHTML += "</tr>";
        movieTableBodyElement.append(rowHTML);
    }
}


const starId = getUrlParam("id");

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: `api/single-star?id=${starId}`,
    success: (data) => handleResult(data),
    error: () => jQuery("#star_info").html("<p>Failed to load star details.</p>")
});

$(document).on("click", ".add-to-cart-btn", function () {
    const movieId = $(this).data("movie-id");
    addMovieToCart(movieId)
        .done((response) => alert(response.message))
        .fail(() => alert("Failed to add movie to cart."));
});
