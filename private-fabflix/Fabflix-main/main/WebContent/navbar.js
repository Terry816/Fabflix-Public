jQuery(document).ready(function () {
    console.log("🧠 navbar.js loaded (hardcoded login state)");

    const navbar = jQuery("#navbar-buttons");

    const html = `
        <li class="nav-item">
            <a class="nav-link" href="index.html">Home</a>
        </li>
        <li class="nav-item">
            <a class="nav-link" href="movie-list.html?top20=true">Top 20</a>
        </li>
        <li class="nav-item">
            <a class="nav-link" href="shopping-cart.html">🛒 Cart</a>
        </li>
        <li class="nav-item">
            <a class="nav-link" href="#" id="logout-button">Logout</a>
        </li>
    `;

    navbar.html(html);

    jQuery("#logout-button").on("click", function () {
        jQuery.ajax({
            method: "GET",
            url: "api/logout",
            success: function () {
                console.log("👋 Logged out successfully.");
                window.location.replace("login.html");
            },
            error: function () {
                console.error("❌ Logout failed.");
            }
        });
    });
});
