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
 * Handles the returned star + movie list JSON and displays them
 */
function handleResult(resultData) {
    let star = resultData[0];

    let starInfoElement = jQuery("#star_info");
    starInfoElement.append(`
        <div class="star-name">${star["star_name"]}</div>
        <p><strong>Date of Birth:</strong> ${star["birthYear"] ? star["birthYear"] : "N/A"}</p>
    `);

    let movieTableBodyElement = jQuery("#movie_table_body");

    for (let i = 0; i < resultData.length; i++) {
        let rowHTML = "<tr>";
        rowHTML += "<td><a href='single-movie.html?id=" + resultData[i]["movie_id"] + "'>" + resultData[i]["movie_title"] + "</a></td>";
        rowHTML += "<td>" + resultData[i]["movie_year"] + "</td>";
        rowHTML += "<td>" + resultData[i]["director"] + "</td>";
        rowHTML += `<td>
            <button class="btn btn-sm btn-success" onclick="addToCart('${resultData[i]["movie_id"]}')">
                Add to Cart
            </button>
        </td>`;
        rowHTML += "</tr>";
        movieTableBodyElement.append(rowHTML);
    }
}

function addToCart(movieId) {
    jQuery.ajax({
        type: "POST",
        url: "api/add-to-cart",
        data: { movieId: movieId },
        success: function (response) {
            alert(response.message);
        },
        error: function () {
            alert("Failed to add movie to cart.");
        }
    });
}


// Trigger fetch
let starId = getParameterByName("id");

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: `api/single-star?id=${starId}`,
    success: (data) => handleResult(data)
});
