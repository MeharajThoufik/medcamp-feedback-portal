package org.medcamp.feedback.camp;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.medcamp.feedback.department.Department;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CampManagementTests extends CampTestSupport {

	private User admin;
	private User organizer;
	private Department eyeCare;
	private Department dental;

	@BeforeEach
	void setUp() {
		admin = user("admin@test.org", Role.SUPER_ADMIN);
		organizer = user("asha@test.org", Role.ORGANIZER);
		eyeCare = department("Eye Care");
		dental = department("Dental");
	}

	@Test
	void newCampIsUpcomingWithARandomPublicTokenAndServicesInOrder() throws Exception {
		createCamp(organizer,
				campBody(null, service(null, dental, "Tooth cleaning"), service(null, eyeCare, " Eye check-up ")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("UPCOMING"))
			.andExpect(jsonPath("$.feedbackState").value("CAMP_NOT_ACTIVE"))
			.andExpect(jsonPath("$.publicToken", matchesPattern("[A-Za-z2-9]{8}")))
			.andExpect(jsonPath("$.services[*].name", contains("Tooth cleaning", "Eye check-up")))
			.andExpect(jsonPath("$.services[*].departmentName", contains("Dental", "Eye Care")))
			.andExpect(jsonPath("$.nextStatuses", contains("ACTIVE", "ARCHIVED")));
	}

	@Test
	void savingTheServiceListKeepsAddsRemovesAndReorders() throws Exception {
		Camp camp = camp("Rampur camp", organizer, eyeCare, dental);
		List<CampService> services = camp.getServices();
		Long eyeId = services.get(0).getId();

		updateCamp(organizer, camp.getId(),
				campBody(null, service(null, dental, "Blood pressure"), service(eyeId, eyeCare, "Vision test")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.services[*].name", contains("Blood pressure", "Vision test")))
			.andExpect(jsonPath("$.services[1].id").value(eyeId));
	}

	@Test
	void datesMustBeInOrder() throws Exception {
		Map<String, Object> body = campBody(null);

		createCamp(organizer, with(body, "endDate", TODAY.minusDays(1).toString()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_DATES"));
		createCamp(organizer, with(body, "feedbackClosesOn", TODAY.minusDays(1).toString()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_FEEDBACK_WINDOW"));
	}

	@Test
	void missingFieldsAreListed() throws Exception {
		createCamp(organizer, with(with(campBody(null), "name", " "), "startDate", null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors.name").exists())
			.andExpect(jsonPath("$.errors.startDate").exists());
	}

	@Test
	void serviceRulesAreChecked() throws Exception {
		createCamp(organizer, campBody(null, service(null, eyeCare, "Eye test"), service(null, dental, "EYE TEST")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("SERVICE_NAME_DUPLICATE"));

		Long otherCampsService = camp("Other camp", organizer, eyeCare).getServices().get(0).getId();
		Camp camp = camp("Rampur camp", organizer);
		updateCamp(organizer, camp.getId(), campBody(null, service(otherCampsService, eyeCare, "Eye test")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));

		dental.setActive(false);
		createCamp(organizer, campBody(null, service(null, dental, "Tooth cleaning")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("DEPARTMENT_INACTIVE"));
	}

	@Test
	void superAdminMustChooseAnActiveOrganizer() throws Exception {
		createCamp(admin, campBody(null)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ORGANIZER_REQUIRED"));
		createCamp(admin, campBody(user("analyst@test.org", Role.ANALYST).getId())).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ORGANIZER_INVALID"));

		User disabled = user("old@test.org", Role.ORGANIZER);
		disabled.setEnabled(false);
		createCamp(admin, campBody(disabled.getId())).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ORGANIZER_INVALID"));
	}

	@Test
	void campMovesThroughItsStatuses() throws Exception {
		Camp empty = camp("Empty camp", organizer);
		changeStatus(organizer, empty.getId(), "ACTIVE").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CAMP_HAS_NO_SERVICES"));

		Camp camp = camp("Rampur camp", organizer, eyeCare);
		changeStatus(organizer, camp.getId(), "CLOSED").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_STATUS_CHANGE"));
		changeStatus(organizer, camp.getId(), "ACTIVE").andExpect(status().isOk())
			.andExpect(jsonPath("$.feedbackState").value("OPEN"))
			.andExpect(jsonPath("$.nextStatuses", contains("CLOSED")));
		changeStatus(organizer, camp.getId(), "CLOSED").andExpect(jsonPath("$.feedbackState").value("CAMP_NOT_ACTIVE"));
		changeStatus(organizer, camp.getId(), "ARCHIVED").andExpect(status().isOk());

		updateCamp(organizer, camp.getId(), campBody(null)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CAMP_ARCHIVED"));
		changeStatus(organizer, camp.getId(), "CLOSED").andExpect(status().isOk());
		updateCamp(organizer, camp.getId(), campBody(null)).andExpect(status().isOk());
	}

	@Test
	void feedbackIsOpenOnlyInsideTheWindowOfAnActiveCamp() {
		Camp camp = camp("Rampur camp", organizer, eyeCare);
		camp.setFeedbackWindow(TODAY.minusDays(2), TODAY.plusDays(2));

		assertThat(camp.feedbackState(TODAY)).isEqualTo(FeedbackState.CAMP_NOT_ACTIVE);
		camp.setStatus(CampStatus.ACTIVE);
		assertThat(camp.feedbackState(TODAY.minusDays(2))).isEqualTo(FeedbackState.OPEN);
		assertThat(camp.feedbackState(TODAY.plusDays(2))).isEqualTo(FeedbackState.OPEN);
		assertThat(camp.feedbackState(TODAY.minusDays(3)))
			.isEqualTo(FeedbackState.NOT_STARTED);
		assertThat(camp.feedbackState(TODAY.plusDays(3))).isEqualTo(FeedbackState.ENDED);
	}

	@Test
	void listFiltersByStatusSearchAndDates() throws Exception {
		Camp rampur = camp("Rampur camp", organizer, eyeCare);
		rampur.setLocation("Rampur School");
		Camp old = camp("Old camp", organizer, eyeCare);
		old.setDates(TODAY.minusDays(40), TODAY.minusDays(39));
		Camp archived = camp("Archived camp", organizer, eyeCare);
		archived.setStatus(CampStatus.ARCHIVED);

		mvc.perform(get("/api/camps").with(as(admin)))
			.andExpect(jsonPath("$.items[*].name", contains("Rampur camp", "Old camp")));
		mvc.perform(get("/api/camps").param("status", "ARCHIVED").with(as(admin)))
			.andExpect(jsonPath("$.items[*].name", contains("Archived camp")));
		mvc.perform(get("/api/camps").param("search", "rampur school").with(as(admin)))
			.andExpect(jsonPath("$.items[*].name", contains("Rampur camp")));
		mvc.perform(get("/api/camps").param("from", TODAY.minusDays(45).toString())
			.param("to", TODAY.minusDays(39).toString())
			.with(as(admin))).andExpect(jsonPath("$.items[*].name", contains("Old camp")));
		mvc.perform(get("/api/camps").param("organizerId", String.valueOf(admin.getId())).with(as(admin)))
			.andExpect(jsonPath("$.totalItems").value(0));
	}

}
