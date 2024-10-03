var registeredSubjects = new Array(50);
var rs = 0;

var eregisteredSubjects = new Array(50);
var ers = 0;

function eregisterSubject(chk) {
	if(chk.checked == true) {
		eregisteredSubjects[ers] = chk;
		ers++;
	} else {
		var i, j;
		for(i = 0; i < ers; i++) {
			if(chk === eregisteredSubjects[i]) {
				for(j = i; j < rs - 1; j++) {
					eregisteredSubjects[j] = eregisteredSubjects[j + 1];
				}
				ers--;
				return;
			}
		}
	}
}

function submitEregister() {
	var i;
	var result = new String();
	for(i = 0; i < ers -1; i++) {
		result += eregisteredSubjects[i].id + "__";
	}
	if (i > 0) {
		result += eregisteredSubjects[i].id;
		deliverContent("eregistersubmit", result);
	} else {
		document.getElementsByClassName("content")[0].innerHTML += "<br><br><p style=\"float: right;margin-right: 40px; margin-top: 30px;\"class=\"spectrum-Body spectrum-Body--M\">Prijava je prazna. Izaberite željene predmete.</p>"
	}
}

function submitRegistration() {
	var i;
	var result = new String();
	for(i = 0; i < rs -1; i++) {
		result += registeredSubjects[i].id + "__";
	}
	if (i > 0) {
		result += registeredSubjects[i].id;
		deliverContent("submit", result);
	} else {
		document.getElementsByClassName("content")[0].innerHTML += "<br><br><p style=\"float: right;margin-right: 40px; margin-top: 30px;\"class=\"spectrum-Body spectrum-Body--M\">Prijava je prazna. Izaberite željene predmete.</p>"
	}
}

function registerSubject(chk) {
	if(chk.checked == true) {
		registeredSubjects[rs] = chk;
		rs++;
	} else {
		var i, j;
		for(i = 0; i < rs; i++) {
			if(chk === registeredSubjects[i]) {
				for(j = i; j < rs - 1; j++) {
					registeredSubjects[j] = registeredSubjects[j + 1];
				}
				rs--;
				return;
			}
		}
	}
}

function deliverContent(api, arg) {
	if(arg == null) {
		fetch("/" + api + "?token=" + document.cookie)
		.then(function (response) {
			response.text().then(function (text) {
				document.getElementsByClassName("content")[0].innerHTML = text;
			});
		})
	} else {
		fetch("/" + api + "?token=" + document.cookie + "&arg=" + arg)
		.then(function (response) {
			response.text().then(function (text) {
				document.getElementsByClassName("content")[0].innerHTML = text;
			});
		})
	}
}

function overview() {
	deliverContent("overview", null);
}

function passed() {
	deliverContent("passed", null);
}

function enrolled() {
	deliverContent("enroll", null);
}

function eregister() {
	deliverContent("eregister", null);
}

function cheque() {
	deliverContent("cheque", null);
}

function waitlist() {
	deliverContent("waitlist", null);
}
function registerMarks() {
	inpt = document.getElementsByTagName("input");
	var args = "";
	var i = 0;
	for(i = 0; i < inpt.length-1; i++) {
		args += inpt[i].id + "_";
		args += inpt[i].value + "_";
	}
	args += inpt[i].id;
	args += inpt[i].value;

	deliverContent("registermarks", args);
}

function changeActivity(s) {
	var b = document.getElementById(s);
    if(b.className.includes("is-open")) {
        b.className = "spectrum-Accordion-item";
    } else {
        b.className = "spectrum-Accordion-item is-open";
    }
}

function logout() {
    document.cookie = null;
	window.location.replace("/");
}

overview();