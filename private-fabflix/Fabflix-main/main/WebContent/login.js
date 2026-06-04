let login_form = $("#login_form");

/**
 * Handle the data returned by LoginServlet
 * @param resultDataJson parsed JSON object
 */
function handleLoginResult(resultDataJson) {
    console.log("handle login response");
    console.log(resultDataJson);
    console.log(resultDataJson["status"]);

    if (resultDataJson["status"] === "success") {
        window.location.replace("index.html");
    } else {
        console.log("show error message");
        console.log(resultDataJson["message"]);
        $("#login_error_message").text(resultDataJson["message"]);
    }
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitLoginForm(formSubmitEvent) {
    console.log("submit login form");
    formSubmitEvent.preventDefault();

    $.ajax("api/login", {
        method: "POST",
        data: login_form.serialize(),
        dataType: "json", // ✅ tells jQuery to parse automatically
        success: handleLoginResult,
        error: (xhr, status, error) => {
            console.log("AJAX error:", status, error);
            $("#login_error_message").text("An unexpected error occurred.");
        }
    });
}

login_form.submit(submitLoginForm);
