package org.medcamp.feedback.feedback;

import org.medcamp.feedback.auth.AppUserDetails;
import org.medcamp.feedback.common.PageResponse;
import org.medcamp.feedback.feedback.FeedbackDtos.FeedbackItem;
import org.medcamp.feedback.feedback.FeedbackDtos.FeedbackSummary;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** A camp's feedback for staff; see {@link FeedbackQueryService} for who sees what. */
@RestController
@RequestMapping("/api/camps/{campId}/feedback")
public class FeedbackController {

	private final FeedbackQueryService feedbackQueryService;

	public FeedbackController(FeedbackQueryService feedbackQueryService) {
		this.feedbackQueryService = feedbackQueryService;
	}

	@GetMapping
	public PageResponse<FeedbackItem> list(@PathVariable long campId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, @AuthenticationPrincipal AppUserDetails currentUser) {
		return feedbackQueryService.list(campId, page, size, currentUser);
	}

	@GetMapping("/summary")
	public FeedbackSummary summary(@PathVariable long campId, @AuthenticationPrincipal AppUserDetails currentUser) {
		return feedbackQueryService.summary(campId, currentUser);
	}

}
