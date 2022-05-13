package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import it.polimi.tiw.projects.dao.ConferenceDAO;
import org.apache.commons.lang.StringEscapeUtils;

import it.polimi.tiw.projects.beans.UserBean;
import it.polimi.tiw.projects.utils.ConnectionHandler;

@WebServlet("/CreateConference")
public class CreateConference extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private Connection connection = null;

	public CreateConference() {
		super();
	}

	public void init() throws ServletException {
		connection = ConnectionHandler.getConnection(getServletContext());
	}

	private Date getMeYesterday() {
		return new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// If the user is not logged in (not present in session) redirect to the login
		HttpSession session = request.getSession();
		if (session.isNew() || session.getAttribute("user") == null) {
			String loginpath = getServletContext().getContextPath() + "/index.html";
			response.sendRedirect(loginpath);
			return;
		}

		// Get and parse all parameters from request
		boolean isBadRequest = false;
		String title = null;
		Timestamp date = new Timestamp(0);
		Time duration = null;
		int guests = 0;

		try {
			title = StringEscapeUtils.escapeJava(request.getParameter("title"));
			date = Timestamp.valueOf(request.getParameter("date") + " " + request.getParameter("time") + ":00.000000000");
			duration = Time.valueOf(request.getParameter("duration") + ":00");
			guests = Integer.parseInt(request.getParameter("guests"));

			Timestamp now = new Timestamp(System.currentTimeMillis());

			isBadRequest = title.isEmpty() || date.before(now) || guests <= 0;
		} catch (NumberFormatException | NullPointerException e) {
			isBadRequest = true;
			e.printStackTrace();
		}
		if (isBadRequest) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect or missing param values");
			return;
		}

		// Create conference in DB
		UserBean user = (UserBean) session.getAttribute("user");
		ConferenceDAO conferenceDAO = new ConferenceDAO(connection);
		try {
			conferenceDAO.createConference(title, date, duration, guests, user.getId());
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to create mission");
			return;
		}

		// return the user to the right view
		String ctxpath = getServletContext().getContextPath();
		String path = ctxpath + "/Anagrafica";
		response.sendRedirect(path);
	}

	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
