package org.medcamp.feedback.feedback;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.medcamp.feedback.auth.AppUserDetails;
import org.medcamp.feedback.camp.Camp;
import org.medcamp.feedback.camp.CampManager;
import org.medcamp.feedback.camp.CampService;
import org.medcamp.feedback.common.ApiException;
import org.medcamp.feedback.common.PageResponse;
import org.medcamp.feedback.complaint.ComplaintDtos.ComplaintBrief;
import org.medcamp.feedback.complaint.ComplaintFiling;
import org.medcamp.feedback.feedback.FeedbackDtos.CategorySummary;
import org.medcamp.feedback.feedback.FeedbackDtos.Contact;
import org.medcamp.feedback.feedback.FeedbackDtos.FeedbackItem;
import org.medcamp.feedback.feedback.FeedbackDtos.FeedbackSummary;
import org.medcamp.feedback.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feedback for staff, following the privacy rules in the project description:
 * <ul>
 * <li>Super Admins and the camp's organizer see every entry, with contact details when the
 * attendee agreed to be contacted.</li>
 * <li>Medical staff see entries about their department's services, without contact details.</li>
 * <li>Analysts get no individual entries; they see combined results on the dashboards.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class FeedbackQueryService {

	private static final int MAX_PAGE_SIZE = 100;

	private final CampManager campManager;
	private final FeedbackRepository feedback;
	private final RatingCategoryRepository categories;
	private final ComplaintFiling complaintFiling;

	public FeedbackQueryService(CampManager campManager, FeedbackRepository feedback,
			RatingCategoryRepository categories, ComplaintFiling complaintFiling) {
		this.campManager = campManager;
		this.feedback = feedback;
		this.categories = categories;
		this.complaintFiling = complaintFiling;
	}

	public PageResponse<FeedbackItem> list(long campId, int page, int size, AppUserDetails me) {
		Camp camp = requireAccess(campId, me);
		boolean showContact = CampManager.canManage(camp, me);
		PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
				Sort.by(Sort.Direction.DESC, "submittedAt").and(Sort.by(Sort.Direction.DESC, "id")));
		Page<Feedback> entries = feedback.findForCamp(camp.getId(), departmentFilter(me), pageRequest);
		Map<Long, ComplaintBrief> complaints = complaintFiling
			.briefsForFeedback(entries.getContent().stream().map(Feedback::getId).toList());
		return PageResponse.of(entries.map(entry -> toItem(entry, complaints.get(entry.getId()), showContact)));
	}

	public FeedbackSummary summary(long campId, AppUserDetails me) {
		Camp camp = requireAccess(campId, me);
		Long departmentId = departmentFilter(me);
		Map<String, CategoryAverage> averages = feedback.averagesForCamp(camp.getId(), departmentId)
			.stream()
			.collect(Collectors.toMap(CategoryAverage::code, Function.identity()));
		// Inactive categories are listed too while they still have answers.
		List<CategorySummary> rows = categories.findAllByOrderBySortOrderAsc()
			.stream()
			.filter(category -> category.isActive() || averages.containsKey(category.getCode()))
			.map(category -> {
				CategoryAverage average = averages.get(category.getCode());
				return new CategorySummary(category.getCode(), category.getLabelEn(), category.getLabelHi(),
						average == null ? null : average.average(), average == null ? 0 : average.count());
			})
			.toList();
		return new FeedbackSummary(feedback.countForCamp(camp.getId(), departmentId), rows);
	}

	private Camp requireAccess(long campId, AppUserDetails me) {
		if (me.getRole() == Role.ANALYST) {
			throw ApiException.forbidden("FEEDBACK_ACCESS_DENIED",
					"Analysts see combined results on the dashboards, not individual feedback.");
		}
		return campManager.requireViewable(campId, me);
	}

	private static Long departmentFilter(AppUserDetails me) {
		return me.getRole() == Role.MEDICAL_STAFF ? me.getDepartmentId() : null;
	}

	private static FeedbackItem toItem(Feedback entry, ComplaintBrief complaint, boolean showContact) {
		Map<String, Integer> ratings = new LinkedHashMap<>();
		entry.getRatings()
			.stream()
			.sorted(Comparator.comparingInt(rating -> rating.getCategory().getSortOrder()))
			.forEach(rating -> ratings.put(rating.getCategory().getCode(), rating.getScore()));
		CampService service = entry.getService();
		Contact contact = showContact && entry.isFollowUpConsent()
				? new Contact(entry.getContactName(), entry.getContactPhone(), entry.getContactEmail()) : null;
		return new FeedbackItem(entry.getId(), entry.getSubmittedAt(), service == null ? null : service.getId(),
				service == null ? null : service.getName(),
				service == null ? null : service.getDepartment().getName(), ratings, entry.getComment(),
				entry.isProblemReported(), complaint, entry.getLanguage(), !entry.isFollowUpConsent(),
				entry.isAssisted(),
				entry.getAssistedBy() == null ? null : entry.getAssistedBy().getFullName(), contact);
	}

}
