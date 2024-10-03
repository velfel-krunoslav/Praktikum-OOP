function overview() {
	fetch("/overview?token=" + document.cookie)
		.then(function (response) {
			response.text().then(function (text) {
				document.getElementsByClassName("content")[0].innerHTML = text;
			});
		})
}

function passed() {

}

function attending() {

}

function eregister() {

}

function cheque() {

}

function changeActivity(b) {
    if(b.className.includes("is-open")) {
        b.className = "spectrum-Accordion-item";
    } else {
        b.className = "spectrum-Accordion-item is-open";
    }
}

overview();