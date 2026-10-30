package org.medcamp.feedback.complaint;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ComplaintDtos {

	private ComplaintDtos() {
	}

	/** The problem part of a feedback form. */
	public record ComplaintRequest(@NotBlank @Size(max = 30) String categoryCode,
			@NotBlank @Size(min = 10, max = 1000) String description) {
	}

	public record ComplaintCategoryResponse(String code, String labelEn, String labelHi) {

		static ComplaintCategoryResponse from(ComplaintCategory category) {
			return new ComplaintCategoryResponse(category.getCode(), category.getLabelEn(), category.getLabelHi());
		}

	}

	/**
	 * What the public status page shows. Nothing else: no names, contact details, camp details or
	 * internal notes, because anyone holding the reference can see it.
	 */
	public record PublicComplaintStatus(String referenceNo, ComplaintStatus status, Instant submittedAt,
			Instant lastUpdatedAt, String publicNote) {
	}

	/** A complaint as shown next to its feedback for staff. */
	public record ComplaintBrief(String referenceNo, ComplaintStatus status, ComplaintPriority priority,
			String categoryCode, String categoryLabelEn, String categoryLabelHi, String description) {

		public static ComplaintBrief from(Complaint complaint) {
			ComplaintCategory category = complaint.getCategory();
			return new ComplaintBrief(complaint.getReferenceNo(), complaint.getStatus(), complaint.getPriority(),
					category.getCode(), category.getLabelEn(), category.getLabelHi(), complaint.getDescription());
		}

	}

}
