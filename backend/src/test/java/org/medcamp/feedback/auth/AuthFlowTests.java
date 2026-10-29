package org.medcamp.feedback.auth;

import org.junit.jupiter.api.Test;
import org.medcamp.feedback.ApiTestSupport;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowTests extends ApiTestSupport {

	@Test
	void signInReturnsCurrentUserAndStartsASession() throws Exception {
		user("organizer@test.org", Role.ORGANIZER);

		MockHttpSession session = (MockHttpSession) login("Organizer@Test.org", PASSWORD).andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("organizer@test.org"))
			.andExpect(jsonPath("$.role").value("ORGANIZER"))
			.andReturn()
			.getRequest()
			.getSession();

		mvc.perform(get("/api/auth/me").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("ORGANIZER"));
	}

	@Test
	void wrongPasswordAndUnknownEmailGiveTheSameAnswer() throws Exception {
		user("staff@test.org", Role.ANALYST);

		login("staff@test.org", "wrong-password").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
		login("nobody@test.org", PASSWORD).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void accountLocksAfterFiveWrongPasswords() throws Exception {
		user("locked@test.org", Role.ORGANIZER);

		for (int i = 0; i < 5; i++) {
			login("locked@test.org", "wrong-password").andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
		}

		login("locked@test.org", PASSWORD).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
	}

	@Test
	void disabledAccountCannotSignIn() throws Exception {
		User user = user("disabled@test.org", Role.ORGANIZER);
		user.setEnabled(false);

		login("disabled@test.org", PASSWORD).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
	}

	@Test
	void apiRequiresSignInWithoutCreatingASession() throws Exception {
		// The XSRF-TOKEN cookie cannot be checked here: Spring Security Test swaps in its own
		// CSRF token store under MockMvc.
		mvc.perform(get("/api/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
			.andExpect(request().sessionAttributeDoesNotExist());
	}

	@Test
	void postWithoutCsrfTokenIsRejected() throws Exception {
		user("csrf@test.org", Role.ORGANIZER);

		mvc.perform(post("/api/auth/login").param("email", "csrf@test.org").param("password", PASSWORD))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CSRF_INVALID"));
	}

	@Test
	void signOutEndsTheSession() throws Exception {
		user("logout@test.org", Role.ORGANIZER);
		MockHttpSession session = (MockHttpSession) login("logout@test.org", PASSWORD).andReturn()
			.getRequest()
			.getSession();

		mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());

		mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
	}

	@Test
	void mustChangePasswordBlocksEverythingElseUntilChanged() throws Exception {
		User admin = user("newadmin@test.org", Role.SUPER_ADMIN);
		admin.requirePasswordChange();

		mvc.perform(get("/api/users").with(as(admin)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
		mvc.perform(get("/api/auth/me").with(as(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.mustChangePassword").value(true));

		changePassword(as(admin), PASSWORD, "NewPassw0rd").andExpect(status().isNoContent());

		mvc.perform(get("/api/users").with(as(admin))).andExpect(status().isOk());
		login("newadmin@test.org", "NewPassw0rd").andExpect(status().isOk());
	}

	@Test
	void changePasswordChecksCurrentPasswordAndRules() throws Exception {
		User user = user("rules@test.org", Role.ORGANIZER);

		changePassword(as(user), "wrong-password", "NewPassw0rd").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("WRONG_CURRENT_PASSWORD"));
		changePassword(as(user), PASSWORD, "short1").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors.newPassword").exists());
		changePassword(as(user), PASSWORD, "lettersonly").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.newPassword").exists());
		changePassword(as(user), PASSWORD, PASSWORD).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PASSWORD_UNCHANGED"));
	}

	@Test
	void disablingAUserEndsTheirExistingSession() throws Exception {
		User user = user("active@test.org", Role.ORGANIZER);
		RequestPostProcessor existingSession = as(user);

		user.setEnabled(false);

		mvc.perform(get("/api/auth/me").with(existingSession))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("SESSION_ENDED"));
	}

	@Test
	void roleChangeAppliesWithoutSigningInAgain() throws Exception {
		User user = user("demoted@test.org", Role.SUPER_ADMIN);
		RequestPostProcessor existingSession = as(user);

		user.setRole(Role.ORGANIZER);

		mvc.perform(get("/api/users").with(existingSession)).andExpect(status().isForbidden());
	}

	private ResultActions login(String email, String password) throws Exception {
		return mvc.perform(post("/api/auth/login").with(csrf()).param("email", email).param("password", password));
	}

	private ResultActions changePassword(RequestPostProcessor asUser, String current, String next) throws Exception {
		return mvc.perform(post("/api/auth/change-password").with(asUser)
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"currentPassword": "%s", "newPassword": "%s"}
					""".formatted(current, next)));
	}

}
