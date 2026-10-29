package org.medcamp.feedback.auth;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.medcamp.feedback.ApiTestSupport;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Calls every Phase 1 endpoint directly as each role, so access rules are enforced by the server. */
class RoleAccessTests extends ApiTestSupport {

	@ParameterizedTest
	@EnumSource(Role.class)
	void onlySuperAdminCanManageUsers(Role role) throws Exception {
		User caller = user("caller@test.org", role);
		User target = user("target@test.org", Role.ANALYST);
		ResultMatcher allowed = role == Role.SUPER_ADMIN ? status().is2xxSuccessful() : status().isForbidden();

		mvc.perform(get("/api/users").with(as(caller))).andExpect(allowed);
		mvc.perform(get("/api/users/{id}", target.getId()).with(as(caller))).andExpect(allowed);
		mvc.perform(post("/api/users").with(as(caller))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email": "new@test.org", "fullName": "New User", "role": "ANALYST"}
					""")).andExpect(allowed);
		mvc.perform(put("/api/users/{id}", target.getId()).with(as(caller))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email": "target@test.org", "fullName": "Renamed", "role": "ANALYST"}
					""")).andExpect(allowed);
		mvc.perform(put("/api/users/{id}/enabled", target.getId()).with(as(caller))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"enabled": false}
					""")).andExpect(allowed);
		mvc.perform(post("/api/users/{id}/reset-password", target.getId()).with(as(caller)).with(csrf()))
			.andExpect(allowed);
	}

	@ParameterizedTest
	@EnumSource(Role.class)
	void everyoneCanReadDepartmentsButOnlySuperAdminCanChangeThem(Role role) throws Exception {
		User caller = user("caller@test.org", role);
		Long departmentId = department("Cardiology").getId();
		ResultMatcher allowed = role == Role.SUPER_ADMIN ? status().is2xxSuccessful() : status().isForbidden();

		mvc.perform(get("/api/departments").with(as(caller))).andExpect(status().isOk());
		mvc.perform(post("/api/departments").with(as(caller))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Dental"}
					""")).andExpect(allowed);
		mvc.perform(put("/api/departments/{id}", departmentId).with(as(caller))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Cardiology", "active": false}
					""")).andExpect(allowed);
	}

	@ParameterizedTest
	@EnumSource(Role.class)
	void everySignedInUserCanReadTheirOwnProfile(Role role) throws Exception {
		User caller = user("caller@test.org", role);

		mvc.perform(get("/api/auth/me").with(as(caller))).andExpect(status().isOk());
	}

}
