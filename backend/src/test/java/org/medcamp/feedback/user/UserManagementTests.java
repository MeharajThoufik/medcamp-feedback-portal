package org.medcamp.feedback.user;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.medcamp.feedback.ApiTestSupport;
import org.medcamp.feedback.department.Department;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserManagementTests extends ApiTestSupport {

	private User admin;

	@BeforeEach
	void setUp() {
		admin = user("admin@test.org", Role.SUPER_ADMIN);
	}

	@Test
	void newUserGetsATemporaryPasswordThatMustBeChanged() throws Exception {
		String body = createUser("""
				{"email": "Asha@Test.org", "fullName": "Asha Rao", "role": "ORGANIZER"}
				""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.user.email").value("asha@test.org"))
			.andExpect(jsonPath("$.user.mustChangePassword").value(true))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String temporaryPassword = JsonPath.read(body, "$.temporaryPassword");

		mvc.perform(post("/api/auth/login").with(csrf())
			.param("email", "asha@test.org")
			.param("password", temporaryPassword))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.mustChangePassword").value(true));
	}

	@Test
	void medicalStaffNeedAnActiveDepartment() throws Exception {
		createUser("""
				{"email": "doc@test.org", "fullName": "Dr Doc", "role": "MEDICAL_STAFF"}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DEPARTMENT_REQUIRED"));

		Department closed = department("Closed Ward");
		closed.setActive(false);
		createUser("""
				{"email": "doc@test.org", "fullName": "Dr Doc", "role": "MEDICAL_STAFF", "departmentId": %d}
				""".formatted(closed.getId())).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("DEPARTMENT_INACTIVE"));

		Department open = department("Eye Care");
		createUser("""
				{"email": "doc@test.org", "fullName": "Dr Doc", "role": "MEDICAL_STAFF", "departmentId": %d}
				""".formatted(open.getId())).andExpect(status().isCreated())
			.andExpect(jsonPath("$.user.departmentName").value("Eye Care"));
	}

	@Test
	void otherRolesNeverKeepADepartment() throws Exception {
		Department department = department("Eye Care");

		createUser("""
				{"email": "org@test.org", "fullName": "Organizer", "role": "ORGANIZER", "departmentId": %d}
				""".formatted(department.getId())).andExpect(status().isCreated())
			.andExpect(jsonPath("$.user.departmentId").doesNotExist());
	}

	@Test
	void emailsAreUniqueIgnoringCase() throws Exception {
		user("taken@test.org", Role.ANALYST);

		createUser("""
				{"email": "TAKEN@test.org", "fullName": "Someone", "role": "ANALYST"}
				""").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
	}

	@Test
	void invalidInputListsTheBadFields() throws Exception {
		createUser("""
				{"email": "not-an-email", "fullName": "", "role": null}
				""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors.email").exists())
			.andExpect(jsonPath("$.errors.fullName").exists())
			.andExpect(jsonPath("$.errors.role").exists());
	}

	@Test
	void adminCannotDisableThemselvesOrChangeTheirOwnRole() throws Exception {
		mvc.perform(put("/api/users/{id}/enabled", admin.getId()).with(as(admin))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"enabled": false}
					""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CANNOT_DISABLE_SELF"));

		mvc.perform(put("/api/users/{id}", admin.getId()).with(as(admin))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email": "admin@test.org", "fullName": "Admin", "role": "ANALYST"}
					""")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CANNOT_CHANGE_OWN_ROLE"));
	}

	@Test
	void resetPasswordUnlocksTheAccount() throws Exception {
		User locked = user("locked@test.org", Role.ORGANIZER);
		for (int i = 0; i < 5; i++) {
			locked.registerFailedLogin(java.time.Instant.now(), 5, java.time.Duration.ofMinutes(15));
		}

		String body = mvc.perform(post("/api/users/{id}/reset-password", locked.getId()).with(as(admin)).with(csrf()))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String temporaryPassword = JsonPath.read(body, "$.temporaryPassword");

		mvc.perform(post("/api/auth/login").with(csrf())
			.param("email", "locked@test.org")
			.param("password", temporaryPassword))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.mustChangePassword").value(true));
	}

	@Test
	void listFiltersBySearchRoleAndStatus() throws Exception {
		user("ravi@test.org", Role.ORGANIZER);
		User disabled = user("meena@test.org", Role.ORGANIZER);
		disabled.setEnabled(false);
		user("kiran@test.org", Role.ANALYST);

		mvc.perform(get("/api/users").param("search", "RAVI").with(as(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].email").value("ravi@test.org"));
		mvc.perform(get("/api/users").param("role", "ORGANIZER").param("enabled", "true").with(as(admin)))
			.andExpect(jsonPath("$.totalItems").value(1));
	}

	private ResultActions createUser(String json) throws Exception {
		return mvc.perform(
				post("/api/users").with(as(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
	}

}
