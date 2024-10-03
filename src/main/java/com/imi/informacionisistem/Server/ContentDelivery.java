package com.imi.informacionisistem.Server;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Calendar;

import java.sql.DriverManager;
import java.sql.*;

public class ContentDelivery {
	private static String[] terms = {
		"JANUAR",
		"FEBRUAR",
		"MART",
		"JUN",
		"JUL",
		"AVGUST",
		"SEPTEMBAR"
	};
	private static int[] mnth = {
		12,1,
		20,2,
		4,3,
		1,6,
		16,6,
		22,6,
		30,8
	};
	private static int MAX_No_SEMESTERS = 8;
	private String allTerms = null;
	private static int currentAcYear;
	private Connection connection = null;
	private Statement statement = null;
	private String sql = null;
	private ResultSet rs = null;
	private ResultSet rs2 = null;
	private ResultSet rs3 = null;

	@RequestMapping(value="/panel", method = RequestMethod.GET)
	public String Panel(@RequestParam(name="token") String t, Model model)
			throws SQLException {

		connection = DriverManager.getConnection("jdbc:mariadb://localhost/security", "root", "klaric314");
		statement = connection.createStatement();

		sql="SELECT * FROM tokens WHERE token=" + "\"" + t + "\"";
		rs = statement.executeQuery(sql);

		try {
			rs.first();
			rs.getInt("id");
		} catch (SQLDataException e) {
			return "login";
		} finally {
			statement.close();
			connection.close();
		}
		if(rs.getString("role").equals("profesor")) return "panelprof";
		return "panel";
	}

	@RequestMapping(value="/overview", method = RequestMethod.GET)
	@ResponseBody
	public String Overview(@RequestParam(name="token") String t)
			throws SQLException {

		connection = DriverManager.getConnection("jdbc:mariadb://localhost/security", "root", "klaric314");
		statement = connection.createStatement();
		int entityID;

		sql="SELECT * FROM tokens WHERE token=" + "\"" + t + "\"";
		rs = statement.executeQuery(sql);

		try {
			rs.first();
			entityID = rs.getInt("id");
		} catch (SQLDataException e) {
			return "login";
		} finally {
			statement.close();
			connection.close();
		}
		if(rs.getString("role").equals("profesor")) {
			String result = new String();
			String fullName;
			String degree;
			String title;
			connection = DriverManager.getConnection("jdbc:mariadb://localhost/studies", "root", "klaric314");
			statement = connection.createStatement();
			sql = "SELECT * FROM professors WHERE id=" + entityID;
			rs = statement.executeQuery(sql);
			rs.first();

			fullName = rs.getString("name") + " " + rs.getString("surname");
			degree = rs.getString("degree");
			title = rs.getString("title");

			result =
"<h2 id=\"full-name\">" + fullName + "</h2>" +
"<table class=\"spectrum-Table\" id=\"student-info\">" +
"	<tbody class=\"spectrum-Table-body\">" +
"		<tr class=\"spectrum-Table-row\">" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\"><strong>Studije</strong></td>" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\"><strong>Zvanje</strong></td>" +
"		</tr>" +
"		<tr class=\"spectrum-Table-row\">" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\">" + degree + "</td>" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\">" + title + "</td>" +
"		</tr>" +
"	</tbody>" +
"</table>";
			return result;
		} else {

		String result = new String();
		String fullName;
		String fileNumber;
		int UUID;
		String email;
		String phoneNumber;
		int markSum;
		int markCount;
		float avg;
		String status;
		int ECTS;
		int [] semesters = new int[MAX_No_SEMESTERS];
		int semno = 0;
		int tmpYr;

		connection = DriverManager.getConnection("jdbc:mariadb://localhost/studies", "root", "klaric314");
		statement = connection.createStatement();
		sql="SELECT * FROM students WHERE id=" + entityID;
		rs = statement.executeQuery(sql);
		rs.first();

		fullName = rs.getString("name") + " (" + rs.getString("middle_name") + ") " + rs.getString("surname");

		fileNumber = rs.getInt("file_number") + "/" + rs.getInt("enrollment_year");
		UUID = entityID;
		email = rs.getString("email");
		phoneNumber = rs.getString("phone_number");

		currentAcYear = Calendar.getInstance().get(Calendar.YEAR);
		if (Calendar.getInstance().get(Calendar.MONTH) >= 9) {
			currentAcYear++;
		}

	
			allTerms = new String();
			for (String s : terms) {
				allTerms += s;
			}
			sql =	"SELECT * " +
					"FROM exam_registrations " +
					"WHERE file_number=" + fileNumber.split("/")[0] + " " +
						"AND enrollment_year=" + fileNumber.split("/")[1] + " AND mark >= 6 " +
						"AND NOT EXISTS (" +
							"SELECT id " +
								"FROM exam_registrations AS a " +
								"WHERE a.file_number = file_number AND " +
								"a.enrollment_year = enrollment_year AND " +
								"a.subject_code = subject_code AND " +
								"a.id <> id AND " +
								"a.mark >= mark AND " +
								"(STRCMP(a.academic_year,academic_year) > 0 OR (STRCMP(a.academic_year,academic_year) = 0 AND LOCATE(a.term,\"" + allTerms + "\") < LOCATE(term,\"" + allTerms + "\")))" +
							")";
			rs2 = statement.executeQuery(sql);

			try {
				markCount = 0;
				markSum = 0;
				while (rs2.next()) {
					markCount++;
					markSum += rs2.getInt("mark");
				}
				avg = (float) markSum / markCount;
			} catch (SQLException e) {
				avg = 0;
			}

			ECTS = 0;

			rs2.first();

			while (rs2.next()) {
				sql="SELECT DISTINCT ects, semester FROM subjects WHERE code = \"" + rs2.getString("subject_code") + "\"";
				rs3 = statement.executeQuery(sql);
				rs3.first();
				try {
					ECTS += rs3.getInt(1);
					int sem = rs3.getInt(2);
					boolean flg = true;

					for(int i = 0; i <  semno; i++) {
						if(semesters[i] == sem) flg = false;
					}

					if(flg == true) {
						semesters[semno] = sem;
						semno++;
					}
				} catch (SQLException e) {
					ECTS += 0;
				}
			}

			try {
				tmpYr = semesters[0];
				for(int i = 1; i < semno; i++) {
					if(semesters[i] > tmpYr) {
						tmpYr = semesters[i];
					}
				}
			} catch (ArrayIndexOutOfBoundsException e){
				tmpYr = 0;
			}

			if(tmpYr % 2 == 1) {
				tmpYr++;
			}
			if ((tmpYr != 0) && (ECTS >= (tmpYr / 2))) {
				status="Budžet";
			} else {
				status="Samofinansiranje";
			}

		statement.close();
		connection.close();

		result =
"<h2 id=\"full-name\">" + fullName + "</h2>" +
"<table class=\"spectrum-Table\" id=\"student-info\">" +
"	<tbody class=\"spectrum-Table-body\">" +
"		<tr class=\"spectrum-Table-row\">" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\"><strong>Broj indeksa</strong></td>" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\"><strong>JIBS</strong></td>" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\"><strong>E-pošta</strong></td>" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\"><strong>Broj telefona</strong></td>" +
"		</tr>" +
"		<tr class=\"spectrum-Table-row\">" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\">" + fileNumber + "</td>" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\">" + UUID + "</td>" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\">" + email + "</td>" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\">" + phoneNumber + "</td>" +
"		</tr>" +
"	</tbody>" +
"</table>" +
"<table id=\"overview\" class=\"spectrum-Table\">" +
"	<tbody class=\"spectrum-Table-body\">" +
"		<tr class=\"spectrum-Table-row\">" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\"><strong>Studije</strong></td>" +
"			<td class=\"spectrum-Table-cell\">Osnovne akademske</td>" +
"		</tr>" +
"		<tr class=\"spectrum-Table-row\">" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\"><strong>Studijski program</strong></td>" +
"			<td class=\"spectrum-Table-cell\">Informatika</td>" +
"		</tr>" +
"		<tr class=\"spectrum-Table-row\">" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\"><strong>Prosek</strong></td>" +
"			<td class=\"spectrum-Table-cell\">" + (float)Math.round(avg * 100) / 100 + "</td>" +
"		</tr>" +
"		<tr class=\"spectrum-Table-row\">" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\"><strong>Status</strong></td>" +
"			<td class=\"spectrum-Table-cell\">" + status + "</td>" +
"		</tr>" +
"		<tr class=\"spectrum-Table-row\">" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\"><strong>Studijska godina</strong></td>" +
"			<td class=\"spectrum-Table-cell\">" + (currentAcYear - 1) + "/" + currentAcYear + "</td>" +
"		</tr>" +
"		<tr class=\"spectrum-Table-row\">" +
"			<td class=\"spectrum-Table-cell spectrum-Table-cell--divider\"><strong>ESPB</strong></td>" +
"			<td class=\"spectrum-Table-cell\">" + ECTS + "</td>" +
"		</tr>" +
"	</tbody>" +
"</table>";
		return result;
		}
	}
	@RequestMapping(value="/passed", method = RequestMethod.GET)
	@ResponseBody
	public String Passed(@RequestParam(name="token") String t)
			throws SQLException {

		connection = DriverManager.getConnection("jdbc:mariadb://localhost/security", "root", "klaric314");
		statement = connection.createStatement();
		int studentID;

		sql="SELECT * FROM tokens WHERE token=" + "\"" + t + "\"";
		rs = statement.executeQuery(sql);

		try {
			rs.first();
			studentID = rs.getInt("id");
		} catch (SQLDataException e) {
			return "login";
		} finally {
			statement.close();
			connection.close();
		}
		String result = new String();
		result =
"<table class=\"spectrum-Table\">" +
"	<thead class=\"spectrum-Table-head\">" +
"	<tr>" +
"		<th class=\"spectrum-Table-headCell\">Šifra predmeta</th>" +
"		<th class=\"spectrum-Table-headCell\">Naziv predmeta</th>" +
"		<th class=\"spectrum-Table-headCell\">Školska godina</th>" +
"		<th class=\"spectrum-Table-headCell\">Ispitni rok</th>" +
"		<th class=\"spectrum-Table-headCell\">Ocena</th>" +
"		<th class=\"spectrum-Table-headCell\">ESPB</th>" +
"		<th class=\"spectrum-Table-headCell\">Ispitivač</th>" +
"	</tr>" +
"	</thead>" +
"	<tbody class=\"spectrum-Table-body\">";
		String fileNumber;

		connection = DriverManager.getConnection("jdbc:mariadb://localhost/studies", "root", "klaric314");
		statement = connection.createStatement();
		sql="SELECT * FROM students WHERE id=" + studentID;
		rs = statement.executeQuery(sql);
		rs.first();

		fileNumber = rs.getInt("file_number") + "/" + rs.getInt("enrollment_year");
		sql =	"SELECT * " +
				"FROM exam_registrations " +
				"WHERE file_number=" + fileNumber.split("/")[0] + " " +
					"AND enrollment_year=" + fileNumber.split("/")[1];
		rs2 = statement.executeQuery(sql);
		rs2.first();

		while (rs2.next()) {
			sql = "SELECT * FROM subjects WHERE code = \"" + rs2.getString("subject_code") + "\"";
			rs3 = statement.executeQuery(sql);
			rs3.first();
			try {
				result +=
"	<tr class=\"spectrum-Table-row\">" +
"		<td class=\"spectrum-Table-cell\">" + rs2.getString("subject_code") + "</td>" +
"		<td class=\"spectrum-Table-cell\"><b>" + rs3.getString("name") + "</b></td>" +
"		<td class=\"spectrum-Table-cell\">" + rs2.getString("academic_year") + "</td>" +
"		<td class=\"spectrum-Table-cell\">" + rs2.getString("term") + "</td>" +
"		<td class=\"spectrum-Table-cell\">" + rs2.getInt("mark") + "</td>" +
"		<td class=\"spectrum-Table-cell\">" + rs3.getInt("ects") + "</td>";

			sql = "SELECT DISTINCT name, surname FROM professors WHERE id=" + rs2.getInt("professor_id");
			rs3 = statement.executeQuery(sql);
			rs3.first();
			
			result +=
"		<td class=\"spectrum-Table-cell\">" + rs3.getString("name") + " " + rs3.getString("surname") + "</td>" +
"	</tr>";
		} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		statement.close();
		connection.close();

		result +=
"	</tbody>" +
"</table>";
	statement.close();
	connection.close();
		return result;
	}
	@RequestMapping(value="/cheque", method = RequestMethod.GET)
	@ResponseBody
	public String Cheque(@RequestParam(name="token") String t)
			throws SQLException {
		connection = DriverManager.getConnection("jdbc:mariadb://localhost/security", "root", "klaric314");
		statement = connection.createStatement();
		int studentID;

		sql="SELECT * FROM tokens WHERE token=" + "\"" + t + "\"";
		rs = statement.executeQuery(sql);

		try {
			rs.first();
			studentID = rs.getInt("id");
		} catch (SQLDataException e) {
			return "login";
		} finally {
			statement.close();
			connection.close();
		}
		connection = DriverManager.getConnection("jdbc:mariadb://localhost/studies", "root", "klaric314");
		statement = connection.createStatement();

		sql = "SELECT DISTINCT name, surname FROM students WHERE id=" + studentID;
		rs = statement.executeQuery(sql);
		rs.first();

				String result = 
"<div class=\"cheque\">" +
"	<div class=\"left\">" +
"		<p>Uplatilac</p>" +
"		<p class=\"box-lg\">Ime i prezime: " + rs.getString("name") + " " + rs.getString("surname") + "<br>Adresa: (dopisati)</p>" +
"		<p>Svrha uplate</p>" +
"		<p class=\"box-lg\">Prijava ispita</p>" +
"		<p>Primalac</p>" +
"		<p class=\"box-lg\">Prirodno-matematčki fakultet, Kragujevac</p> <br>" +
"		<p style=\"display: inline;\">Pečat i potpis primaoca</p>" +
"		<p style=\"display: inline; padding-left: 40px;\">Mesto i datum prijema</p>" +
"		<br>" +
"		<br>" +
"		<p style=\"display: inline;\">___________________</p>" +
"		<p style=\"display: inline; padding-left: 40px;\">_____________________</p>" +
"	</div>" +
"	<div class=\"right\">" +
"		<p style=\"display: inline-block;\" >Šifra plaćanja</p>" +
"		<p style=\"display: inline-block; padding-left: 4px;\" >Valuta</p>" +
"		<p style=\"display: inline-block; padding-left: 20px;\" >Iznos</p> <br>" +
"		<p style=\"display: inline-block; padding-right:18px;\" class=\"box-xs\">189</p>" +
"		<p style=\"display: inline-block;\" class=\"box-xs\">RSD</p>" +
"		<p style=\"display: inline-block;\" class=\"box-s\">(dopisati)</p>" +
"		<p>Račun primaoca</p>" +
"		<p class=\"box-lg\">840-1017666-11</p>" +
"		<p>Model i poziv na broj (odobrenje)</p>" +
"		<p style=\"display: inline-block;\" class=\"box-xs\">97</p>" +
"		<p style=\"display: inline-block;width: 80% !important\" class=\"box-s\">87-509-" + studentID + "-2</p>" +
"		<p style=\"padding-top: 62px;\">Datum valute</p>" +
"		<p style=\"display: inline;\">___________________</p>" +
"		<p style=\"float:right;\">Uplatnice su jedinstvene za svakog studenta.</p>" +
"	</div>" +
"</div>";
	statement.close();
	connection.close();
	return result;
	}

	@RequestMapping(value="/enroll", method = RequestMethod.GET)
	@ResponseBody
	public String Enroll(@RequestParam(name="token") String t)
			throws SQLException {

		connection = DriverManager.getConnection("jdbc:mariadb://localhost/security", "root", "klaric314");
		statement = connection.createStatement();

		String result = new String();
		int studentID;
		int enrollment_year;

		sql="SELECT * FROM tokens WHERE token=" + "\"" + t + "\"";
		rs = statement.executeQuery(sql);

		try {
			rs.first();
			studentID = rs.getInt("id");
		} catch (SQLDataException e) {
			return "login";
		} finally {
			statement.close();
			connection.close();
		}

		connection = DriverManager.getConnection("jdbc:mariadb://localhost/studies", "root", "klaric314");
		statement = connection.createStatement();

		sql = "SELECT file_number, enrollment_year FROM students WHERE id=" + studentID;
		rs = statement.executeQuery(sql);
		rs.first();
		try {
			enrollment_year = rs.getInt("enrollment_year");
		} catch (SQLDataException e){
			e.printStackTrace();
			statement.close();
			connection.close();
			return null;
		}

		int nextAcYear;

		currentAcYear = Calendar.getInstance().get(Calendar.YEAR);
		
		/* Replace with Calendar.getInstance().get(Calendar.MONTH) >= 9 &&  Calendar.getInstance().get(Calendar.MONTH) <= 10*/
		if (true) {
			nextAcYear = currentAcYear - enrollment_year + 1;
			
			sql =	"SELECT * FROM subjects " +
					"WHERE semester=" + (nextAcYear * 2) + " " +
						"OR semester=" + (nextAcYear * 2 - 1);
			rs = statement.executeQuery(sql);
			rs.first();

			result =
"<table class=\"spectrum-Table\">" +
"	<thead class=\"spectrum-Table-head\">" +
"	<tr>" +
"		<th>" +
"		</th>" +
"		<th class=\"spectrum-Table-headCell\">Šifra predmeta</th>" +
"		<th class=\"spectrum-Table-headCell\">Tip</th>" +
"		<th class=\"spectrum-Table-headCell\">Naziv predmeta</th>" +
"		<th class=\"spectrum-Table-headCell\">ESPB</th>" +
"	</tr>" +
"	</thead>" +
"	<tbody class=\"spectrum-Table-body\">";
			while(rs.next()) {
result +=
"	<tr class=\"spectrum-Table-row\">" +
"		<th style=\"padding-top: 8px\">" +
"		<label class=\"spectrum-Checkbox\">" +
"		<input onclick=\"registerSubject(this)\" type=\"checkbox\" class=\"spectrum-Checkbox-input\" id=\"" + rs.getString("code") +"\">" +
"		<span class=\"spectrum-Checkbox-box\">" +
"			<svg class=\"spectrum-Icon spectrum-UIIcon-CheckmarkSmall spectrum-Checkbox-checkmark\" focusable=\"false\" aria-hidden=\"true\">" +
"				<use xlink:href=\"#spectrum-css-icon-CheckmarkSmall\" />" +
"			</svg>" +
"			<svg class=\"spectrum-Icon spectrum-UIIcon-DashSmall spectrum-Checkbox-partialCheckmark\" focusable=\"false\" aria-hidden=\"true\">" +
"				<use xlink:href=\"#spectrum-css-icon-DashSmall\" />" +
"			</svg>" +
"		</span>" +
"		</label>" +
"		</td>" +
"		<td class=\"spectrum-Table-cell\">" + rs.getString("code") + "</td>" +
"		<td class=\"spectrum-Table-cell\">" + rs.getString("type") + "</td>" +
"		<td class=\"spectrum-Table-cell\"><b>" + rs.getString("name") + "</b></td>" +
"		<td class=\"spectrum-Table-cell\">" + rs.getString("ects") + "</td>" +
"	</tr>";
			}
result +=
		"	</tbody>" +
		"</table>" +
		"<button style=\"display:block; float: right; margin-right: 40px;margin-bottom: 30px;\" onclick=\"submitRegistration()\" class=\"spectrum-Button spectrum-Button--cta\">" +
		"	<span class=\"spectrum-Button-label\">Prijava</span>" +
		"</button>";
			result +=
"	</tbody>" +
"</table>";
		} else {
			result += "<p style=\"margin-left: 40px; margin-right: auto;\"class=\"spectrum-Body spectrum-Body--M\">Prijava predmeta trenutno nije dostupna.</p>";

		}
		connection.close();
		statement.close();
		return result;
	}

	@RequestMapping(value = "/submit", method = RequestMethod.GET)
	@ResponseBody
	public String Submit(@RequestParam(name = "token") String t,
					 @RequestParam(name = "arg") String a ) throws SQLException {
		
		connection = DriverManager.getConnection("jdbc:mariadb://localhost/security", "root", "klaric314");
		statement = connection.createStatement();
		
		int studentID;

		sql="SELECT * FROM tokens WHERE token=" + "\"" + t + "\"";
		rs = statement.executeQuery(sql);

		try {
			rs.first();
			studentID = rs.getInt("id");
		} catch (SQLDataException e) {
			return "login";
		} finally {
			statement.close();
			connection.close();
		}

		connection = DriverManager.getConnection("jdbc:mariadb://localhost/studies", "root", "klaric314");
		statement = connection.createStatement();

		String [] vals = a.split("__");

		for(String u : vals) {
			sql = "INSERT INTO submitted_subjects (id, student_id, subject_code) VALUES (NULL, " + studentID + ", \"" + u + "\")";

			statement.executeQuery(sql);
		}

		statement.close();
		connection.close();


		return "<p style=\"margin-left: 40px;\"class=\"spectrum-Body spectrum-Body--M\">Prijava predmeta je uspešno obavljena.</p>";
	}

	@RequestMapping(value = "/eregister", method = RequestMethod.GET)
	@ResponseBody
	public String Eregister(@RequestParam(name = "token") String t) throws SQLException {
		
		connection = DriverManager.getConnection("jdbc:mariadb://localhost/security", "root", "klaric314");
		statement = connection.createStatement();
		
		int studentID;
		String result = new String();

		sql="SELECT * FROM tokens WHERE token=" + "\"" + t + "\"";
		rs = statement.executeQuery(sql);

		try {
			rs.first();
			studentID = rs.getInt("id");
		} catch (SQLDataException e) {
			return "login";
		} finally {
			statement.close();
			connection.close();
		}

		connection = DriverManager.getConnection("jdbc:mariadb://localhost/studies", "root", "klaric314");
		statement = connection.createStatement();

		int month = Calendar.getInstance().get(Calendar.MONTH);
		int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		
		int i;
		for(i = 1; i < mnth.length; i += 2) {
			if(mnth[i] >= month && mnth[i - 1] >= day) break;
		}

		String term = terms[i/2];

		result =
"<table class=\"spectrum-Table\">" +
"	<thead class=\"spectrum-Table-head\">" +
"	<tr>" +
"		<th>" +
"		</th>" +
"		<th class=\"spectrum-Table-headCell\">Šifra predmeta</th>" +
"		<th class=\"spectrum-Table-headCell\">Naziv predmeta</th>" +
"		<th class=\"spectrum-Table-headCell\">ESPB</th>" +
"		<th class=\"spectrum-Table-headCell\">Ispitni rok</th>" +
"	</tr>" +
"	</thead>" +
"	<tbody class=\"spectrum-Table-body\">";

		sql = "SELECT * FROM submitted_subjects WHERE student_id=" + studentID;
		rs = statement.executeQuery(sql);
		rs.first();

		while(rs.next()) {
			sql = "SELECT * FROM subjects WHERE code=\"" + rs.getString("subject_code") + "\"";
			rs2 = statement.executeQuery(sql);
			rs2.first();
			String subjectName = rs2.getString("name");
			int ects = rs2.getInt("ects");
			result +=
"	<tr class=\"spectrum-Table-row\">" +
"		<th style=\"padding-top: 8px\">" +
"		<label class=\"spectrum-Checkbox\">" +
"		<input onclick=\"eregisterSubject(this)\" type=\"checkbox\" class=\"spectrum-Checkbox-input\" id=\"" + rs.getString("subject_code") +"\">" +
"		<span class=\"spectrum-Checkbox-box\">" +
"			<svg class=\"spectrum-Icon spectrum-UIIcon-CheckmarkSmall spectrum-Checkbox-checkmark\" focusable=\"false\" aria-hidden=\"true\">" +
"				<use xlink:href=\"#spectrum-css-icon-CheckmarkSmall\" />" +
"			</svg>" +
"			<svg class=\"spectrum-Icon spectrum-UIIcon-DashSmall spectrum-Checkbox-partialCheckmark\" focusable=\"false\" aria-hidden=\"true\">" +
"				<use xlink:href=\"#spectrum-css-icon-DashSmall\" />" +
"			</svg>" +
"		</span>" +
"		</label>" +
"		</td>" +
"		<td class=\"spectrum-Table-cell\">" + rs.getString("subject_code") + "</td>" +
"		<td class=\"spectrum-Table-cell\"><b>" + subjectName + "</b></td>" +
"		<td class=\"spectrum-Table-cell\">" + ects + "</td>" +
"		<td class=\"spectrum-Table-cell\">" + term + "</td>" +
"	</tr>";
		}
		result +=
		"	</tbody>" +
		"</table>" +
		"<button style=\"display:block; float: right; margin-right: 40px;margin-bottom: 30px;\" onclick=\"submitEregister()\" class=\"spectrum-Button spectrum-Button--cta\">" +
		"	<span class=\"spectrum-Button-label\">Prijava</span>" +
		"</button>";

		statement.close();
		connection.close();
		return result;
	}

	@RequestMapping(value = "/eregistersubmit", method = RequestMethod.GET)
	@ResponseBody
	public String EregisterSubmit(@RequestParam(name = "token") String t,
					 @RequestParam(name = "arg") String a ) throws SQLException {
		
		connection = DriverManager.getConnection("jdbc:mariadb://localhost/security", "root", "klaric314");
		statement = connection.createStatement();
		
		int studentID;

		sql="SELECT * FROM tokens WHERE token=" + "\"" + t + "\"";
		rs = statement.executeQuery(sql);

		try {
			rs.first();
			studentID = rs.getInt("id");
		} catch (SQLDataException e) {
			return "login";
		} finally {
			statement.close();
			connection.close();
		}

		connection = DriverManager.getConnection("jdbc:mariadb://localhost/studies", "root", "klaric314");
		statement = connection.createStatement();

		int month = Calendar.getInstance().get(Calendar.MONTH);
		int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		
		int i;
		for(i = 1; i < mnth.length; i += 2) {
			if(mnth[i] >= month && mnth[i - 1] >= day) break;
		}

		String term = terms[i/2];
		String [] vals = a.split("__");

		for(String sc : vals) {
			sql = "INSERT INTO eregister (id, student_id, subject_code, term) VALUES (NULL, " + studentID + ", \"" + sc + "\", \"" + term + "\")";
			statement.executeQuery(sql);
		}


		statement.close();
		connection.close();
		return "<p style=\"margin-left: 40px;\"class=\"spectrum-Body spectrum-Body--M\">Prijava predmeta je uspešno obavljena.</p>";
	}
@RequestMapping(value = "/waitlist", method = RequestMethod.GET)
@ResponseBody
public String Waitlist(@RequestParam(name = "token") String t) throws SQLException {
	String result;
	connection = DriverManager.getConnection("jdbc:mariadb://localhost/security", "root", "klaric314");
	statement = connection.createStatement();
	
	int ID;

	sql="SELECT * FROM tokens WHERE token=" + "\"" + t + "\"";
	rs = statement.executeQuery(sql);

	try {
		rs.first();
		ID = rs.getInt("id");
	} catch (SQLDataException e) {
		return "login";
	} finally {
		statement.close();
		connection.close();
	}

	connection = DriverManager.getConnection("jdbc:mariadb://localhost/studies", "root", "klaric314");
	statement = connection.createStatement();

	sql = "SELECT * FROM submitted_subjects";
	rs = statement.executeQuery(sql);
	rs.first();

	sql = "SELECT * FROM designated_subjects WHERE professor_id=" + ID;
	rs2 = statement.executeQuery(sql);
	rs2.first();

	result =
"<table class=\"spectrum-Table\">" +
"	<thead class=\"spectrum-Table-head\">" +
"	<tr>" +
"		<th class=\"spectrum-Table-headCell\">Šifra predmeta</th>" +
"		<th class=\"spectrum-Table-headCell\">Naziv predmeta</th>" +
"		<th class=\"spectrum-Table-headCell\">Broj indeksa</th>" +
"		<th class=\"spectrum-Table-headCell\">Ime studenta</th>" +
"		<th class=\"spectrum-Table-headCell\">Ocena</th>" +
"	</tr>" +
"	</thead>" +
"	<tbody class=\"spectrum-Table-body\">";
	while(rs.next()) {
		boolean is = false;

		while(rs2.next()) {
			if(rs2.getString("subject_code").equals(rs.getString("subject_code"))) {
				is = true;
				break;
			}
		}

		if(is == true) {
			String subjectName;
			String fileNo;
			String studentName;
			

			sql = "SELECT * FROM subjects WHERE code=" + "\"" + rs2.getString("subject_code") + "\"";
			rs3 = statement.executeQuery(sql);
			rs3.first();

			subjectName = rs3.getString("name");

			sql = "SELECT * FROM students WHERE id=" + rs.getInt("student_id");
			rs3 = statement.executeQuery(sql);
			rs3.first();

			fileNo = rs3.getString("file_number") + "/" + rs3.getString("enrollment_year");
			studentName = rs3.getString("name") + " " + rs3.getString("surname");
			
			result +=
"	<tr class=\"spectrum-Table-row\">" +
"		<td class=\"spectrum-Table-cell\">" + rs2.getString("subject_code") + "</td>" +
"		<td class=\"spectrum-Table-cell\"><b>" + subjectName + "</b></td>" +
"		<td class=\"spectrum-Table-cell\">" + fileNo + "</td>" +
"		<td class=\"spectrum-Table-cell\">" + studentName + "</td>" +
"		<td class=\"spectrum-Table-cell\"><input style=\"width: 120px;\" type=\"text\" placeholder=\"Ocena\" name=\"field\" value=\"\" class=\"spectrum-Textfield\" id=\"" + rs2.getString("subject_code") + "_" + fileNo + "\">" + "</td>" +
"	</tr>";
		}
		rs2.first();
	}
	result +=
	"	</tbody>" +
	"</table>" +
	"<button style=\"display:block; float: right; margin-right: 40px;margin-bottom: 30px;\" onclick=\"registerMarks()\" class=\"spectrum-Button spectrum-Button--cta\">" +
	"	<span class=\"spectrum-Button-label\">Uvedi ocene</span>" +
	"</button>";
		result +=
"	</tbody>" +
"</table>";
	statement.close();
	connection.close();
	return result;
}
	@RequestMapping(value = "/registermarks", method = RequestMethod.GET)
	@ResponseBody
	public String RegisterMarks(@RequestParam(name = "token") String t,
					 @RequestParam(name = "arg") String a ) throws SQLException {
		connection = DriverManager.getConnection("jdbc:mariadb://localhost/security", "root", "klaric314");
		statement = connection.createStatement();
		
		int ID;

		sql="SELECT * FROM tokens WHERE token=" + "\"" + t + "\"";
		rs = statement.executeQuery(sql);

		try {
			rs.first();
			ID = rs.getInt("id");
		} catch (SQLDataException e) {
			return "login";
		} finally {
			statement.close();
			connection.close();
		}

		connection = DriverManager.getConnection("jdbc:mariadb://localhost/studies", "root", "klaric314");
		statement = connection.createStatement();

		String [] vals = a.split("_");

		int month = Calendar.getInstance().get(Calendar.MONTH);
		int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		
		int i;
		for(i = 1; i < mnth.length; i += 2) {
			if(mnth[i] >= month && mnth[i - 1] >= day) break;
		}

		String term = terms[i/2];

		String result = "<p style=\"margin-left: 40px;\"class=\"spectrum-Body spectrum-Body--M\">Upis ocena je uspešno obavljen.</p>";


		currentAcYear = Calendar.getInstance().get(Calendar.YEAR);
		if (Calendar.getInstance().get(Calendar.MONTH) > 10) {
			currentAcYear++;
		}

		for(i = 0; i < vals.length - 1; i += 3) {
			try {
				sql = "INSERT INTO exam_registrations (id, file_number, enrollment_year, subject_code, mark, professor_id, academic_year, term)" +
										 " VALUES (NULL, " + vals[i + 1].split("/")[0] + ", " + vals[i + 1].split("/")[1] + ", \""+ vals[i] + "\", " + vals[i + 2] + ", " + ID + ", \"" + currentAcYear + "/" + (currentAcYear + 1) + "\", \"" + term + "\")";

			} catch (ArrayIndexOutOfBoundsException e) {
				
			} finally {
				try {
					statement.executeQuery(sql);
				} catch (SQLSyntaxErrorException e) {
					
				}
			}

			sql = "DELETE FROM submitted_subjects WHERE subject_code=\"" + vals[i] + "\"";

			statement.executeQuery(sql);
		}

		statement.close();
		connection.close();
		
		return result;
	}

}