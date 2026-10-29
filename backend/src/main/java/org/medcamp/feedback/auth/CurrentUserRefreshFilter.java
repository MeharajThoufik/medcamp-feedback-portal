package org.medcamp.feedback.auth;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.medcamp.feedback.common.ProblemResponseWriter;
import org.medcamp.feedback.user.User;
import org.medcamp.feedback.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Re-checks the signed-in user against the database on every API request, so that:
 * <ul>
 * <li>disabling or deleting a user ends their existing sessions immediately;</li>
 * <li>role and department changes apply without signing in again;</li>
 * <li>a user who must change their password can call nothing else until they do.</li>
 * </ul>
 * Not a Spring bean on purpose: it is added only to the security filter chain.
 */
public class CurrentUserRefreshFilter extends OncePerRequestFilter {

	private static final Set<String> ALLOWED_BEFORE_PASSWORD_CHANGE = Set.of("/api/auth/me",
			"/api/auth/change-password", "/api/auth/logout");

	private final UserRepository users;
	private final ProblemResponseWriter writer;
	private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
	private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

	public CurrentUserRefreshFilter(UserRepository users, ProblemResponseWriter writer) {
		this.users = users;
		this.writer = writer;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !path(request).startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication authentication = contextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails current)) {
			chain.doFilter(request, response);
			return;
		}

		Optional<User> found = users.findById(current.getId());
		if (found.isEmpty() || !found.get().isEnabled()) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
			contextHolder.clearContext();
			writer.writeProblem(response, HttpStatus.UNAUTHORIZED, "SESSION_ENDED",
					"Your account is no longer active. Please contact your administrator.");
			return;
		}

		User user = found.get();
		if (!current.matches(user)) {
			current = AppUserDetails.forSession(user);
			SecurityContext context = contextHolder.createEmptyContext();
			context.setAuthentication(
					UsernamePasswordAuthenticationToken.authenticated(current, null, current.getAuthorities()));
			contextHolder.setContext(context);
			contextRepository.saveContext(context, request, response);
		}

		if (current.isMustChangePassword() && !ALLOWED_BEFORE_PASSWORD_CHANGE.contains(path(request))) {
			writer.writeProblem(response, HttpStatus.FORBIDDEN, "PASSWORD_CHANGE_REQUIRED",
					"You must change your password before continuing.");
			return;
		}

		chain.doFilter(request, response);
	}

	private static String path(HttpServletRequest request) {
		return request.getRequestURI().substring(request.getContextPath().length());
	}

}
