package org.medcamp.feedback.department;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.medcamp.feedback.ApiTestSupport;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DepartmentTests extends ApiTestSupport {

	private User admin;

	@BeforeEach
	void setUp() {
		admin = user("admin@test.org", Role.SUPER_ADMIN);
	}

	@Test
	void namesAreUniqueIgnoringCase() throws Exception {
		department("Dental");

		mvc.perform(post("/api/departments").with(as(admin))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": " dental "}
					""")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DEPARTMENT_NAME_TAKEN"));
	}

	@Test
	void inactiveDepartmentsAreHiddenUnlessRequested() throws Exception {
		department("Dental");
		department("Archived").setActive(false);

		mvc.perform(get("/api/departments").with(as(admin))).andExpect(jsonPath("$[*].name", contains("Dental")));
		mvc.perform(get("/api/departments").param("includeInactive", "true").with(as(admin)))
			.andExpect(jsonPath("$[*].name", contains("Archived", "Dental")));
	}

}
