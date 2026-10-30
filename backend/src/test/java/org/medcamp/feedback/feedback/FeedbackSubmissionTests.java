package org.medcamp.feedback.feedback;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.medcamp.feedback.camp.Camp;
import org.medcamp.feedback.department.Department;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The attendee side: open the form, get a one-time token, submit. */
class FeedbackSubmissionTests extends FeedbackTestSupport {

	private User organizer;
	private Department eyeCare;
	private Camp camp;

	@BeforeEach
	void setUp() {
		organizer = user("asha@test.org", Role.ORGANIZER);
		eyeCare = department("Eye Care");
		camp = openCamp("Rampur camp", organizer, eyeCare);
	}

	@Test
	void theFormListsServicesAndRatingCategories() throws Exception {
		mvc.perform(get("/api/public/camps/{token}", camp.getPublicToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.feedbackState").value("OPEN"))
			.andExpect(jsonPath("$.services[0].name").value("Eye Care check-up"))
			.andExpect(jsonPath("$.services[0].departmentName").value("Eye Care"))
			.andExpect(jsonPath("$.categories[*].code", contains("REGISTRATION", "WAITING_TIME", "STAFF_BEHAVIOUR",
					"CLEANLINESS", "INFORMATION", "OVERALL")))
			.andExpect(jsonPath("$.categories[5].required").value(true))
			.andExpect(jsonPath("$.categories[5].labelHi").value("कुल मिलाकर अनुभव"))
			.andExpect(jsonPath("$.organizerName").doesNotExist());
	}

	@Test
	void anAttendeeSubmitsAnonymousFeedback() throws Exception {
		Long serviceId = camp.getServices().get(0).getId();
		submitNew(camp, Map.of("language", "hi", "serviceId", serviceId, "ratings",
				Map.of("OVERALL", 5, "WAITING_TIME", 2), "comment", "  Long queue at registration  "))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.submittedAt").exists());

		mvc.perform(get("/api/camps/{id}/feedback", camp.getId()).with(as(organizer)))
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].serviceName").value("Eye Care check-up"))
			.andExpect(jsonPath("$.items[0].ratings.OVERALL").value(5))
			.andExpect(jsonPath("$.items[0].ratings.WAITING_TIME").value(2))
			.andExpect(jsonPath("$.items[0].comment").value("Long queue at registration"))
			.andExpect(jsonPath("$.items[0].language").value("hi"))
			.andExpect(jsonPath("$.items[0].anonymous").value(true))
			.andExpect(jsonPath("$.items[0].contact").doesNotExist())
			.andExpect(jsonPath("$.items[0].assisted").value(false));
	}

	@Test
	void aFormCanBeSubmittedOnlyOnce() throws Exception {
		Map<String, Object> body = answers(formToken(camp));

		submit(camp, body).andExpect(status().isCreated());
		submit(camp, body).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_SUBMITTED"));
	}

	@Test
	void madeUpTokensAndTokensForAnotherCampAreRejected() throws Exception {
		submit(camp, answers("1700000000.abc.def")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FORM_TOKEN_INVALID"));

		Camp otherCamp = openCamp("Other camp", organizer, eyeCare);
		submit(camp, answers(formToken(otherCamp))).andExpect(jsonPath("$.code").value("FORM_TOKEN_INVALID"));
	}

	@Test
	void theFormIsClosedOutsideAnActiveCampsWindow() throws Exception {
		String token = formToken(camp);
		Camp upcoming = camp("Upcoming camp", organizer, eyeCare);

		mvc.perform(get("/api/public/camps/{token}/form-token", upcoming.getPublicToken()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("FORM_CLOSED"));

		camp.setFeedbackWindow(TODAY.minusDays(10), TODAY.minusDays(1));
		submit(camp, answers(token)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("FORM_CLOSED"));
	}

	@Test
	void answersAreChecked() throws Exception {
		submitNew(camp, Map.of("ratings", Map.of("WAITING_TIME", 3))).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("RATING_REQUIRED"));
		submitNew(camp, Map.of("ratings", Map.of("OVERALL", 6))).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		submitNew(camp, Map.of("ratings", Map.of("OVERALL", 3, "DIAGNOSIS", 3))).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("UNKNOWN_RATING_CATEGORY"));
		submitNew(camp, Map.of("language", "fr")).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		submitNew(camp, Map.of("comment", "x".repeat(1001))).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

		Long otherCampsService = camp("Other camp", organizer, eyeCare).getServices().get(0).getId();
		submitNew(camp, Map.of("serviceId", otherCampsService)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
	}

	@Test
	void contactDetailsAreKeptOnlyWithConsent() throws Exception {
		submitNew(camp, Map.of("followUp", followUp(false, "Ravi", "98765 43210", null)))
			.andExpect(status().isCreated());
		submitNew(camp, Map.of("followUp", followUp(true, "Meena", null, null))).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CONTACT_REQUIRED"));
		submitNew(camp, Map.of("followUp", followUp(true, "Meena", "12-34", null)))
			.andExpect(jsonPath("$.code").value("INVALID_PHONE"));
		submitNew(camp, Map.of("followUp", followUp(true, "Meena", null, "not-an-email")))
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
		submitNew(camp, Map.of("followUp", followUp(true, "Meena", "+91 98765-43210", null)))
			.andExpect(status().isCreated());

		mvc.perform(get("/api/camps/{id}/feedback", camp.getId()).with(as(organizer)))
			.andExpect(jsonPath("$.items", hasSize(2)))
			.andExpect(jsonPath("$.items[0].anonymous").value(false))
			.andExpect(jsonPath("$.items[0].contact.name").value("Meena"))
			.andExpect(jsonPath("$.items[0].contact.phone").value("+919876543210"))
			.andExpect(jsonPath("$.items[1].anonymous").value(true))
			.andExpect(jsonPath("$.items[1].contact").doesNotExist());
	}

	@Test
	void onePhoneCanSendOnlyAFewFormsPerCamp() throws Exception {
		Map<String, Object> first = answers(formToken(camp));
		submit(camp, first).andExpect(status().isCreated());
		// Retrying a form that already arrived does not use up the phone's allowance.
		submit(camp, first).andExpect(status().isConflict());
		for (int i = 1; i < PublicFeedbackService.MAX_SUBMISSIONS; i++) {
			submitNew(camp, Map.of()).andExpect(status().isCreated());
		}
		submitNew(camp, Map.of()).andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.code").value("TOO_MANY_SUBMISSIONS"));
	}

	@Test
	void signedInStaffCanFillInTheFormForAnAttendee() throws Exception {
		submitNew(camp, Map.of("assisted", true)).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ASSISTED_NOT_ALLOWED"));

		User analyst = user("analyst@test.org", Role.ANALYST);
		submit(camp, with(answers(formToken(camp)), "assisted", true), as(analyst)).andExpect(status().isForbidden());

		User volunteer = users.save(new User("vol@test.org", "Volunteer Kiran", "x", Role.MEDICAL_STAFF, eyeCare));
		// Assisted forms skip the per-phone limit: one staff laptop fills in many forms.
		for (int i = 0; i <= PublicFeedbackService.MAX_SUBMISSIONS; i++) {
			submit(camp, with(answers(formToken(camp)), "assisted", true), as(volunteer))
				.andExpect(status().isCreated());
		}
		mvc.perform(get("/api/camps/{id}/feedback", camp.getId()).with(as(organizer)))
			.andExpect(jsonPath("$.items[0].assisted").value(true))
			.andExpect(jsonPath("$.items[0].assistedByName").value("Volunteer Kiran"));
	}

	@Test
	void aServiceWithFeedbackCannotBeRemovedFromTheCamp() throws Exception {
		Long serviceId = camp.getServices().get(0).getId();
		submitNew(camp, Map.of("serviceId", serviceId)).andExpect(status().isCreated());

		updateCamp(organizer, camp.getId(), campBody(null)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SERVICE_HAS_FEEDBACK"));
		updateCamp(organizer, camp.getId(), campBody(null, service(serviceId, eyeCare, "Renamed check-up")))
			.andExpect(status().isOk());
	}

}
