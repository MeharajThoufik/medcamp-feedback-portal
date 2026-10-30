package org.medcamp.feedback.feedback;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.medcamp.feedback.auth.AppUserDetails;
import org.medcamp.feedback.camp.Camp;
import org.medcamp.feedback.camp.CampCalendar;
import org.medcamp.feedback.camp.CampManager;
import org.medcamp.feedback.camp.CampService;
import org.medcamp.feedback.camp.FeedbackState;
import org.medcamp.feedback.common.ApiException;
import org.medcamp.feedback.common.RateLimiter;
import org.medcamp.feedback.feedback.FeedbackDtos.CategoryResponse;
import org.medcamp.feedback.feedback.FeedbackDtos.FeedbackRequest;
import org.medcamp.feedback.feedback.FeedbackDtos.FollowUpRequest;
import org.medcamp.feedback.feedback.FeedbackDtos.FormTokenResponse;
import org.medcamp.feedback.feedback.FeedbackDtos.PublicFormResponse;
import org.medcamp.feedback.feedback.FeedbackDtos.PublicService;
import org.medcamp.feedback.feedback.FeedbackDtos.SubmissionResponse;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The attendee side of feedback: open the form, get a one-time token, submit. */
@Service
@Transactional
public class PublicFeedbackService {

	/** Per phone (IP address) and camp. A family sharing one phone still fits. */
	static final int MAX_SUBMISSIONS = 5;
	static final int MAX_FORM_TOKENS = 30;
	static final Duration LIMIT_WINDOW = Duration.ofMinutes(10);

	private final CampManager campManager;
	private final CampCalendar calendar;
	private final RatingCategoryRepository categories;
	private final FeedbackRepository feedback;
	private final UserRepository users;
	private final FormTokens formTokens;
	private final RateLimiter rateLimiter;
	private final Clock clock;

	public PublicFeedbackService(CampManager campManager, CampCalendar calendar, RatingCategoryRepository categories,
			FeedbackRepository feedback, UserRepository users, FormTokens formTokens, RateLimiter rateLimiter,
			Clock clock) {
		this.campManager = campManager;
		this.calendar = calendar;
		this.categories = categories;
		this.feedback = feedback;
		this.users = users;
		this.formTokens = formTokens;
		this.rateLimiter = rateLimiter;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public PublicFormResponse form(String token) {
		Camp camp = findCamp(token);
		List<PublicService> services = camp.getServices()
			.stream()
			.map(s -> new PublicService(s.getId(), s.getName(), s.getDepartment().getName()))
			.toList();
		List<CategoryResponse> categoryList = categories.findByActiveTrueOrderBySortOrderAsc()
			.stream()
			.map(CategoryResponse::from)
			.toList();
		return new PublicFormResponse(camp.getName(), camp.getLocation(), camp.getStartDate(), camp.getEndDate(),
				camp.feedbackState(calendar.today()), services, categoryList);
	}

	@Transactional(readOnly = true)
	public FormTokenResponse issueFormToken(String token, String clientIp) {
		Camp camp = requireOpen(findCamp(token));
		if (!rateLimiter.tryAcquire("form-token:" + clientIp + ":" + camp.getId(), MAX_FORM_TOKENS, LIMIT_WINDOW)) {
			throw tooMany();
		}
		return new FormTokenResponse(formTokens.issue(camp.getPublicToken()));
	}

	public SubmissionResponse submit(String token, FeedbackRequest request, String clientIp, AppUserDetails me) {
		Camp camp = requireOpen(findCamp(token));
		// Analysts only see combined results, so they do not fill in forms for attendees.
		if (request.assisted() && (me == null || me.getRole() == Role.ANALYST)) {
			throw ApiException.forbidden("ASSISTED_NOT_ALLOWED",
					"Sign in as camp staff to fill in the form for an attendee.");
		}
		switch (formTokens.check(request.submissionToken(), camp.getPublicToken())) {
			case INVALID -> throw ApiException.badRequest("FORM_TOKEN_INVALID", "This form is not valid. Reload it.");
			case EXPIRED -> throw ApiException.badRequest("FORM_TOKEN_EXPIRED", "This form has expired. Reload it.");
			case VALID -> {
			}
		}
		if (feedback.existsBySubmissionToken(request.submissionToken())) {
			throw ApiException.conflict("ALREADY_SUBMITTED", "This form has already been submitted.");
		}
		// Counted after the token checks, so a phone retrying a form that already arrived loses nothing.
		// Assisted forms are not limited: one staff laptop fills in many.
		if (!request.assisted()
				&& !rateLimiter.tryAcquire("feedback:" + clientIp + ":" + camp.getId(), MAX_SUBMISSIONS, LIMIT_WINDOW)) {
			throw tooMany();
		}

		Feedback entry = new Feedback(camp, resolveService(camp, request.serviceId()), request.language(),
				request.submissionToken(), clock.instant());
		addRatings(entry, request.ratings());
		entry.setComment(blankToNull(request.comment()));
		entry.setProblemReported(request.problemReported());
		applyFollowUp(entry, request.followUp());
		if (request.assisted()) {
			entry.markAssisted(users.getReferenceById(me.getId()));
		}
		return new SubmissionResponse(feedback.save(entry).getSubmittedAt());
	}

	private Camp findCamp(String token) {
		return campManager.findByPublicLink(token)
			.orElseThrow(() -> ApiException.notFound("CAMP_LINK_INVALID", "This feedback link is not valid."));
	}

	private Camp requireOpen(Camp camp) {
		if (camp.feedbackState(calendar.today()) != FeedbackState.OPEN) {
			throw ApiException.conflict("FORM_CLOSED", "This camp is not taking feedback right now.");
		}
		return camp;
	}

	private static CampService resolveService(Camp camp, Long serviceId) {
		if (serviceId == null) {
			return null;
		}
		return camp.getServices()
			.stream()
			.filter(service -> service.getId().equals(serviceId))
			.findFirst()
			.orElseThrow(() -> ApiException.badRequest("SERVICE_NOT_FOUND", "This service is not offered at the camp."));
	}

	private void addRatings(Feedback entry, Map<String, Integer> ratings) {
		Map<String, RatingCategory> byCode = categories.findByActiveTrueOrderBySortOrderAsc()
			.stream()
			.collect(Collectors.toMap(RatingCategory::getCode, Function.identity()));
		for (String code : ratings.keySet()) {
			if (!byCode.containsKey(code)) {
				throw ApiException.badRequest("UNKNOWN_RATING_CATEGORY", "Unknown rating: " + code);
			}
		}
		for (RatingCategory category : byCode.values()) {
			Integer score = ratings.get(category.getCode());
			if (score != null) {
				entry.addRating(category, score);
			}
			else if (category.isRequired()) {
				throw ApiException.badRequest("RATING_REQUIRED", "Please rate: " + category.getLabelEn());
			}
		}
	}

	/** Without consent nothing identifying is stored, even if contact details were sent. */
	private static void applyFollowUp(Feedback entry, FollowUpRequest followUp) {
		if (followUp == null || !followUp.consent()) {
			return;
		}
		String phone = normalizePhone(followUp.phone());
		String email = blankToNull(followUp.email());
		if (phone == null && email == null) {
			throw ApiException.badRequest("CONTACT_REQUIRED", "Give a phone number or email so we can contact you.");
		}
		entry.giveFollowUpConsent(blankToNull(followUp.name()), phone, email);
	}

	/** Keeps digits and a leading +; a phone number needs 7 to 15 digits. */
	static String normalizePhone(String phone) {
		String trimmed = blankToNull(phone);
		if (trimmed == null) {
			return null;
		}
		if (!trimmed.matches("\\+?[0-9(][0-9 ()-]*")) {
			throw invalidPhone();
		}
		String digits = trimmed.replaceAll("[^0-9]", "");
		if (digits.length() < 7 || digits.length() > 15) {
			throw invalidPhone();
		}
		return (trimmed.startsWith("+") ? "+" : "") + digits;
	}

	private static ApiException invalidPhone() {
		return ApiException.badRequest("INVALID_PHONE", "Enter a phone number with 7 to 15 digits.");
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static ApiException tooMany() {
		return ApiException.tooManyRequests("TOO_MANY_SUBMISSIONS",
				"Too many forms from this phone. Please wait a few minutes and try again.");
	}

}
