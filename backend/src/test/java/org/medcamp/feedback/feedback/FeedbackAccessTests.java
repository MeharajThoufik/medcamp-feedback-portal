package org.medcamp.feedback.feedback;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.medcamp.feedback.camp.Camp;
import org.medcamp.feedback.department.Department;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Who may read feedback, and who sees attendees' contact details. */
class FeedbackAccessTests extends FeedbackTestSupport {

	private User organizer;
	private Department eyeCare;
	private Department dental;
	private Camp camp;

	@BeforeEach
	void setUp() throws Exception {
		organizer = user("asha@test.org", Role.ORGANIZER);
		eyeCare = department("Eye Care");
		dental = department("Dental");
		camp = openCamp("Rampur camp", organizer, eyeCare, dental);
		Long eyeService = camp.getServices().get(0).getId();
		Long dentalService = camp.getServices().get(1).getId();

		submitNew(camp, Map.of("serviceId", eyeService, "ratings", Map.of("OVERALL", 5, "WAITING_TIME", 2),
				"followUp", followUp(true, "Meena", "9876543210", null)))
			.andExpect(status().isCreated());
		submitNew(camp, Map.of("serviceId", dentalService, "ratings", Map.of("OVERALL", 2)))
			.andExpect(status().isCreated());
		submitNew(camp, Map.of("ratings", Map.of("OVERALL", 3))).andExpect(status().isCreated());
	}

	@Test
	void theCampsManagersSeeEverythingIncludingConsentedContactDetails() throws Exception {
		for (User manager : new User[] { organizer, user("admin@test.org", Role.SUPER_ADMIN) }) {
			mvc.perform(get("/api/camps/{id}/feedback", camp.getId()).with(as(manager)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(3))
				.andExpect(jsonPath("$.items[2].contact.phone").value("9876543210"));
		}
	}

	@Test
	void medicalStaffSeeOnlyTheirDepartmentAndNoContactDetails() throws Exception {
		User eyeDoctor = users.save(new User("eye@test.org", "Eye Doctor", "x", Role.MEDICAL_STAFF, eyeCare));

		mvc.perform(get("/api/camps/{id}/feedback", camp.getId()).with(as(eyeDoctor)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].departmentName").value("Eye Care"))
			.andExpect(jsonPath("$.items[0].anonymous").value(false))
			.andExpect(jsonPath("$.items[0].contact").doesNotExist());
		mvc.perform(get("/api/camps/{id}/feedback/summary", camp.getId()).with(as(eyeDoctor)))
			.andExpect(jsonPath("$.total").value(1));
	}

	@Test
	void analystsAndOutsidersGetNoIndividualFeedback() throws Exception {
		mvc.perform(get("/api/camps/{id}/feedback", camp.getId()).with(as(user("analyst@test.org", Role.ANALYST))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FEEDBACK_ACCESS_DENIED"));
		mvc.perform(get("/api/camps/{id}/feedback", camp.getId()).with(as(user("ravi@test.org", Role.ORGANIZER))))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/camps/{id}/feedback", camp.getId()).with(as(user("nurse@test.org", Role.MEDICAL_STAFF))))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/camps/{id}/feedback", camp.getId())).andExpect(status().isUnauthorized());
	}

	@Test
	void theSummaryAveragesEachCategory() throws Exception {
		mvc.perform(get("/api/camps/{id}/feedback/summary", camp.getId()).with(as(organizer)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(3))
			.andExpect(jsonPath("$.categories", hasSize(6)))
			.andExpect(jsonPath("$.categories[5].code").value("OVERALL"))
			.andExpect(jsonPath("$.categories[5].average", closeTo(10.0 / 3, 0.001)))
			.andExpect(jsonPath("$.categories[5].count").value(3))
			.andExpect(jsonPath("$.categories[1].average").value(2.0))
			.andExpect(jsonPath("$.categories[0].average").doesNotExist())
			.andExpect(jsonPath("$.categories[0].count").value(0));
	}

}
