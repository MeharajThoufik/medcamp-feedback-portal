package org.medcamp.feedback.camp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.medcamp.feedback.department.Department;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Who can see and change which camps, checked on the server for every role. */
class CampAccessTests extends CampTestSupport {

	private User admin;
	private User organizer;
	private User otherOrganizer;
	private Department eyeCare;

	@BeforeEach
	void setUp() {
		admin = user("admin@test.org", Role.SUPER_ADMIN);
		organizer = user("asha@test.org", Role.ORGANIZER);
		otherOrganizer = user("ravi@test.org", Role.ORGANIZER);
		eyeCare = department("Eye Care");
	}

	@ParameterizedTest
	@EnumSource(Role.class)
	void onlySuperAdminsAndOrganizersCanCreateCamps(Role role) throws Exception {
		User caller = user("caller@test.org", role);

		createCamp(caller, campBody(organizer.getId(), service(null, eyeCare, "Eye check-up")))
			.andExpect(role == Role.SUPER_ADMIN || role == Role.ORGANIZER ? status().isCreated()
					: status().isForbidden());
	}

	@Test
	void anOrganizerAlwaysOrganizesTheirOwnNewCamp() throws Exception {
		createCamp(organizer, campBody(otherOrganizer.getId())).andExpect(status().isCreated())
			.andExpect(jsonPath("$.organizerId").value(organizer.getId()))
			.andExpect(jsonPath("$.canEdit").value(true));
	}

	@Test
	void anOrganizerCannotSeeOrChangeAnotherOrganizersCamp() throws Exception {
		Camp camp = camp("Ravi's camp", otherOrganizer, eyeCare);

		mvc.perform(get("/api/camps/{id}", camp.getId()).with(as(organizer)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CAMP_ACCESS_DENIED"));
		updateCamp(organizer, camp.getId(), campBody(null, service(null, eyeCare, "Eye check-up")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("NOT_CAMP_ORGANIZER"));
		changeStatus(organizer, camp.getId(), "ARCHIVED").andExpect(status().isForbidden());

		updateCamp(otherOrganizer, camp.getId(), campBody(null, service(null, eyeCare, "Eye check-up")))
			.andExpect(status().isOk());
	}

	@Test
	void superAdminCanEditAnyCampAndReassignItsOrganizer() throws Exception {
		Camp camp = camp("Asha's camp", organizer, eyeCare);

		updateCamp(admin, camp.getId(), campBody(otherOrganizer.getId())).andExpect(status().isOk())
			.andExpect(jsonPath("$.organizerName").value(otherOrganizer.getFullName()));
	}

	@Test
	void anOrganizerCannotHandTheirCampToSomeoneElse() throws Exception {
		Camp camp = camp("Asha's camp", organizer, eyeCare);

		updateCamp(organizer, camp.getId(), campBody(otherOrganizer.getId())).andExpect(status().isOk())
			.andExpect(jsonPath("$.organizerId").value(organizer.getId()));
	}

	@Test
	void eachRoleSeesTheRightCampsInTheList() throws Exception {
		Department dental = department("Dental");
		camp("Eye camp", organizer, eyeCare);
		camp("Dental camp", otherOrganizer, dental);
		User eyeDoctor = users.save(new User("eye@test.org", "Eye Doctor", "x", Role.MEDICAL_STAFF, eyeCare));

		expectListedCamps(admin, "Eye camp", "Dental camp");
		expectListedCamps(user("analyst@test.org", Role.ANALYST), "Eye camp", "Dental camp");
		expectListedCamps(organizer, "Eye camp");
		expectListedCamps(eyeDoctor, "Eye camp");
		expectListedCamps(user("nurse@test.org", Role.MEDICAL_STAFF));
	}

	@Test
	void medicalStaffAndAnalystsCanReadButNotChangeCamps() throws Exception {
		Camp camp = camp("Eye camp", organizer, eyeCare);
		User eyeDoctor = users.save(new User("eye@test.org", "Eye Doctor", "x", Role.MEDICAL_STAFF, eyeCare));
		User analyst = user("analyst@test.org", Role.ANALYST);

		for (User reader : new User[] { eyeDoctor, analyst }) {
			mvc.perform(get("/api/camps/{id}", camp.getId()).with(as(reader)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.canEdit").value(false))
				.andExpect(jsonPath("$.nextStatuses", empty()));
			updateCamp(reader, camp.getId(), campBody(null)).andExpect(status().isForbidden());
			changeStatus(reader, camp.getId(), "ARCHIVED").andExpect(status().isForbidden());
		}
		mvc.perform(get("/api/camps/{id}", camp.getId()).with(as(user("nurse@test.org", Role.MEDICAL_STAFF))))
			.andExpect(status().isForbidden());
	}

	private void expectListedCamps(User caller, String... names) throws Exception {
		mvc.perform(get("/api/camps").with(as(caller)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[*].name", containsInAnyOrder((Object[]) names)));
	}

}
