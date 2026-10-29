package org.medcamp.feedback.auth;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.medcamp.feedback.common.ProblemResponseWriter;
import org.medcamp.feedback.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

/** JSON responses for sign-in, sign-in failure, 401 and 403, in place of redirects and HTML pages. */
@Component
public class AuthResponseHandlers {

	private final ProblemResponseWriter writer;
	private final AuthService authService;
	private final long lockoutMinutes;

	public AuthResponseHandlers(ProblemResponseWriter writer, AuthService authService, AppProperties properties) {
		this.writer = writer;
		this.authService = authService;
		this.lockoutMinutes = properties.security().lockoutDuration().toMinutes();
	}

	public void onLoginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
			throws IOException {
		// Sign-in rotates the CSRF token; reading it here sends the new XSRF-TOKEN cookie in this response.
		if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken csrfToken) {
			csrfToken.getToken();
		}
		AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
		writer.writeJson(response, HttpStatus.OK, authService.me(user.getId()));
	}

	public void onLoginFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		if (exception instanceof LockedException) {
			writer.writeProblem(response, HttpStatus.UNAUTHORIZED, "ACCOUNT_LOCKED",
					"Too many failed attempts. Try again in " + lockoutMinutes + " minutes.");
		}
		else if (exception instanceof DisabledException) {
			writer.writeProblem(response, HttpStatus.UNAUTHORIZED, "ACCOUNT_DISABLED",
					"This account has been disabled. Contact your administrator.");
		}
		else {
			writer.writeProblem(response, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
					"Incorrect email or password.");
		}
	}

	public void onUnauthenticated(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		writer.writeProblem(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Please sign in.");
	}

	public void onAccessDenied(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException exception) throws IOException {
		if (exception instanceof CsrfException) {
			writer.writeProblem(response, HttpStatus.FORBIDDEN, "CSRF_INVALID",
					"Your session token expired. Please try again.");
		}
		else {
			writer.writeProblem(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
					"You do not have permission to do this.");
		}
	}

}
