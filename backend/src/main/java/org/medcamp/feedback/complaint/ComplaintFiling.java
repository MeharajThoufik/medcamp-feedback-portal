package org.medcamp.feedback.complaint;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.medcamp.feedback.common.ApiException;
import org.medcamp.feedback.complaint.ComplaintDtos.ComplaintBrief;
import org.medcamp.feedback.complaint.ComplaintDtos.ComplaintCategoryResponse;
import org.medcamp.feedback.complaint.ComplaintDtos.ComplaintRequest;
import org.medcamp.feedback.feedback.Feedback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a reported problem into a complaint. Called inside the feedback submission's
 * transaction, so the feedback and its complaint are saved together or not at all.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ComplaintFiling {

	private static final int MAX_REFERENCE_ATTEMPTS = 10;

	private final ComplaintRepository complaints;
	private final ComplaintCategoryRepository categories;
	private final ReferenceNumbers referenceNumbers;

	public ComplaintFiling(ComplaintRepository complaints, ComplaintCategoryRepository categories,
			ReferenceNumbers referenceNumbers) {
		this.complaints = complaints;
		this.categories = categories;
		this.referenceNumbers = referenceNumbers;
	}

	/** Checks the category before anything is saved. */
	public ComplaintCategory requireCategory(String code) {
		return categories.findByCodeAndActiveTrue(code)
			.orElseThrow(() -> ApiException.badRequest("UNKNOWN_COMPLAINT_CATEGORY", "Unknown problem type: " + code));
	}

	/** Files the complaint for already saved feedback and returns its reference number. */
	public String file(Feedback feedback, ComplaintCategory category, ComplaintRequest request) {
		Complaint complaint = new Complaint(feedback, category, request.description().trim(), newReference());
		return complaints.save(complaint).getReferenceNo();
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public List<ComplaintCategoryResponse> activeCategories() {
		return categories.findByActiveTrueOrderBySortOrderAsc()
			.stream()
			.map(ComplaintCategoryResponse::from)
			.toList();
	}

	/** Feedback id to its complaint, for the feedback entries that have one. */
	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public Map<Long, ComplaintBrief> briefsForFeedback(Collection<Long> feedbackIds) {
		if (feedbackIds.isEmpty()) {
			return Map.of();
		}
		return complaints.findByFeedbackIdIn(feedbackIds)
			.stream()
			.collect(Collectors.toMap(c -> c.getFeedback().getId(), ComplaintBrief::from, (a, b) -> a));
	}

	private String newReference() {
		for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
			String reference = referenceNumbers.generate();
			if (!complaints.existsByReferenceNo(reference)) {
				return reference;
			}
		}
		throw new IllegalStateException("Could not create a unique complaint reference");
	}

}
