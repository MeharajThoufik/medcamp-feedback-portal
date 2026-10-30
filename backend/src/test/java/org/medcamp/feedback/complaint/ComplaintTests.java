package org.medcamp.feedback.complaint;

import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.medcamp.feedback.camp.Camp;
import org.medcamp.feedback.department.Department;
import org.medcamp.feedback.feedback.FeedbackRepository;
import org.medcamp.feedback.feedback.FeedbackTestSupport;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Reporting a problem in the form, and the public status lookup by reference number. */
class ComplaintTests extends FeedbackTestSupport {

	private static final Map<String, Object> DIRTY_TOILETS = Map.of("categoryCode", "CLEANLINESS", "description",
			"The toilets near the eye ward were dirty");

	@Autowired
	private FeedbackRepository feedback;

	private User organizer;
	private Camp camp;

	@BeforeEach
	void setUp() {
		organizer = user("asha@test.org", Role.ORGANIZER);
		Department eyeCare = department("Eye Care");
		camp = openCamp("Rampur camp", organizer, eyeCare);
	}

	@Test
	void reportingAProblemCreatesAComplaintWithAReference() throws Exception {
		String reference = reportProblem();

		assertThat(reference).matches("MC-[0-9]{2}-[A-HJKMNP-Z2-9]{6}");
		mvc.perform(get("/api/camps/{id}/feedback", camp.getId()).with(as(organizer)))
			.andExpect(jsonPath("$.items[0].problemReported").value(true))
			.andExpect(jsonPath("$.items[0].complaint.referenceNo").value(reference))
			.andExpect(jsonPath("$.items[0].complaint.status").value("OPEN"))
			.andExpect(jsonPath("$.items[0].complaint.priority").value("MEDIUM"))
			.andExpect(jsonPath("$.items[0].complaint.categoryCode").value("CLEANLINESS"))
			.andExpect(jsonPath("$.items[0].complaint.description").value("The toilets near the eye ward were dirty"));
	}

	@Test
	void feedbackWithoutAProblemHasNoComplaint() throws Exception {
		submitNew(camp, Map.of()).andExpect(status().isCreated())
			.andExpect(jsonPath("$.complaintReference").doesNotExist());
		mvc.perform(get("/api/camps/{id}/feedback", camp.getId()).with(as(organizer)))
			.andExpect(jsonPath("$.items[0].problemReported").value(false))
			.andExpect(jsonPath("$.items[0].complaint").doesNotExist());
	}

	@Test
	void theProblemIsCheckedBeforeAnythingIsSaved() throws Exception {
		submitNew(camp, Map.of("complaint", Map.of("categoryCode", "DIAGNOSIS", "description", "Long enough text")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("UNKNOWN_COMPLAINT_CATEGORY"));
		submitNew(camp, Map.of("complaint", Map.of("categoryCode", "WAITING", "description", "Too short")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors['complaint.description']").exists());

		assertThat(feedback.count()).isZero();
	}

	@Test
	void theFormListsProblemTypesInBothLanguages() throws Exception {
		mvc.perform(get("/api/public/camps/{token}", camp.getPublicToken()))
			.andExpect(jsonPath("$.complaintCategories[0].code").value("WAITING"))
			.andExpect(jsonPath("$.complaintCategories[0].labelHi").value("लंबा इंतज़ार या भीड़"))
			.andExpect(jsonPath("$.complaintCategories.length()").value(8));
	}

	@Test
	void anyoneWithTheReferenceSeesOnlyTheStatus() throws Exception {
		String reference = reportProblem();

		mvc.perform(get("/api/public/complaints/{reference}", reference))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.referenceNo").value(reference))
			.andExpect(jsonPath("$.status").value("OPEN"))
			.andExpect(jsonPath("$.submittedAt").exists())
			.andExpect(jsonPath("$.lastUpdatedAt").exists())
			.andExpect(jsonPath("$.description").doesNotExist())
			.andExpect(jsonPath("$.categoryCode").doesNotExist())
			.andExpect(jsonPath("$.campName").doesNotExist())
			.andExpect(jsonPath("$.assignee").doesNotExist());

		// The way people type it from a screenshot also works.
		String typed = reference.toLowerCase().replace("-", " ");
		mvc.perform(get("/api/public/complaints/{reference}", typed)).andExpect(status().isOk())
			.andExpect(jsonPath("$.referenceNo").value(reference));
	}

	@Test
	void wrongReferencesAllLookTheSame() throws Exception {
		for (String wrong : new String[] { "MC-26-AAAAAA", "MC-26-K7Q2X", "hello", "MC-26-K7Q2X0" }) {
			mvc.perform(get("/api/public/complaints/{reference}", wrong).with(fromPhone(phoneIp)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COMPLAINT_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("No complaint has this reference."));
		}
	}

	@Test
	void lookupsFromOnePhoneAreLimited() throws Exception {
		for (int i = 0; i < PublicComplaintController.MAX_LOOKUPS; i++) {
			mvc.perform(get("/api/public/complaints/{reference}", "MC-26-AAAAAA").with(fromPhone(phoneIp)))
				.andExpect(status().isNotFound());
		}
		mvc.perform(get("/api/public/complaints/{reference}", "MC-26-AAAAAA").with(fromPhone(phoneIp)))
			.andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.code").value("TOO_MANY_LOOKUPS"));
	}

	private String reportProblem() throws Exception {
		String body = submitNew(camp, Map.of("complaint", DIRTY_TOILETS)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.complaintReference", matchesPattern("MC-.*")))
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.complaintReference");
	}

}
