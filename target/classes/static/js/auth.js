function Auth() {
    fetch("/auth?username=" + document.getElementById("username").value + "&password=" + document.getElementById("password").value)
        .then(function (response) {
            response.text().then(function (text) {
                if(text.localeCompare("auth_error") == 0) {
                    document.getElementById("toast").className = "spectrum-Toast spectrum-Toast--warning";
                    document.getElementsByClassName("wrapper")[0].style.borderColor = "rgb(203, 111, 16)";
                    document.getElementById("login-message").innerHTML = "Greška pri autentikaciji.";
                } else {
                    window.location.replace("/panel?token=" + text);
                    document.cookie = text;
                }
            });
        })
}