package org.medcamp.feedback.feedback;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.medcamp.feedback.camp.FeedbackState;
import org.medcamp.feedback.complaint.ComplaintDtos.ComplaintBrief;
import org.medcamp.feedback.complaint.ComplaintDtos.ComplaintCategoryResponse;
import org.medcamp.feedback.complaint.ComplaintDtos.ComplaintRequest;

public final class FeedbackDtos {

	private FeedbackDtos() {
	}

	// ---- Public (attendees, no sign-in) ----

	/** What the form needs. No organizer, no internal camp id. */
	public record PublicFormResponse(String name, String location, LocalDate startDate, LocalDate endDate,
			FeedbackState feedbackState, List<PublicService> services, List<CategoryResponse> categories,
			List<ComplaintCategoryResponse> complaintCategories) {
	}

	public record PublicService(Long id, String name, String departmentName) {
	}

	public record CategoryResponse(String code, String labelEn, String labelHi, boolean required) {

		static CategoryResponse from(RatingCategory category) {
			return new CategoryResponse(category.getCode(), category.getLabelEn(), category.getLabelHi(),
					category.isRequired());
		}

	}

	public record FormTokenResponse(String submissionToken) {
	}

	/**
	 * A submitted form. {@code ratings} maps a category code to a score from 1 to 5; categories
	 * left out were skipped. {@code serviceId} is null for "general visit / not sure".
	 * {@code complaint} is filled when the attendee reports a problem. {@code assisted} is accepted
	 * only from a signed-in staff member.
	 */
	public record FeedbackRequest(@NotBlank @Size(max = 120) String submissionToken,
			@NotNull @Pattern(regexp = "en|hi") String language, Long serviceId,
			@NotNull @Size(max = 20) Map<@NotBlank String, @NotNull @Min(1) @Max(5) Integer> ratings,
			@Size(max = 1000) String comment, @Valid ComplaintRequest complaint, @Valid FollowUpRequest followUp,
			boolean assisted) {
	}

	/** Contact details are kept only when {@code consent} is true. */
	public record FollowUpRequest(boolean consent, @Size(max = 100) String name, @Size(max = 20) String phone,
			@Email @Size(max = 255) String email) {
	}

	/** {@code complaintReference} is set when a problem was reported, for the attendee to keep. */
	public record SubmissionResponse(Instant submittedAt, String complaintReference) {
	}

	// ---- Staff ----

	/**
	 * One feedback row. {@code contact} is filled only for people who manage the camp and only
	 * when the attendee agreed to be contacted. {@code complaint} is set when a problem was reported.
	 */
	public record FeedbackItem(Long id, Instant submittedAt, Long serviceId, String serviceName,
			String departmentName, Map<String, Integer> ratings, String comment, boolean problemReported,
			ComplaintBrief complaint, String language, boolean anonymous, boolean assisted, String assistedByName,
			Contact contact) {
	}

	public record Contact(String name, String phone, String email) {
	}

	public record FeedbackSummary(long total, List<CategorySummary> categories) {
	}

	/** {@code average} is null when nobody rated this category yet. */
	public record CategorySummary(String code, String labelEn, String labelHi, Double average, long count) {
	}

}
