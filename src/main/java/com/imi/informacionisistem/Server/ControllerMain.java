package com.imi.informacionisistem.Server;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.sql.*;

import com.imi.informacionisistem.Server.ContentDelivery;

@Controller

public class ControllerMain extends ContentDelivery {
	private final int COOKIE_LENGTH = 30;
	private Connection connection = null;
	private Statement statement = null;
	private String response = null;
	private String sql = null;
	private ResultSet rs = null;

	private String GenerateCookie() {
		String cookie = new String();
		char c;
		int t;
		for(int i = 0; i < COOKIE_LENGTH; i++) {
			t = (int)(Math.random() * 100) % 62; /* 26 lowercase + 26 uppercase + 10 digits; values 0 - 61. */

			if(t < 10) {
				c = (char)('0' + t);
			} else if(t >= 10 && t < 36) {
				c = (char)('a' + (t - 10));
			} else {
				c = (char)('A' + (t - 36));
			}

			cookie += c;
		}

		return cookie;
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String Login(HttpServletRequest request) {
		return "login";
	}


	@RequestMapping(value = "/auth", method = RequestMethod.GET)
	@ResponseBody
	public String Auth(@RequestParam(name = "username") String u,
					 @RequestParam(name = "password") String p ) throws SQLException {
		int id;
		String role;
		connection = DriverManager.getConnection("jdbc:mariadb://localhost/security", "root", "klaric314");
		statement = connection.createStatement();

		sql = "SELECT * FROM login WHERE username=\"" + u + "\" AND " + "password=\"" + p + "\"";

		rs = statement.executeQuery(sql);
		try {
			rs.first();
			id = rs.getInt("id");
			role = rs.getString("role");
			do {
				response = GenerateCookie();
				sql = "SELECT * FROM tokens WHERE token=\"" + response + "\"";
				rs = statement.executeQuery(sql);
			} while(rs.next() == true);

			sql = "INSERT INTO tokens(token, id, role) VALUES(\"" + response + "\", \"" + id + "\", \"" + role + "\")";
			statement.executeUpdate(sql);
		} catch (SQLDataException e) {
			response = "auth_error";
			return response;
		} finally {
			statement.close();
			connection.close();
		}
		return response;
	}
}