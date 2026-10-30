package org.medcamp.feedback.feedback;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.medcamp.feedback.auth.AppUserDetails;
import org.medcamp.feedback.feedback.FeedbackDtos.FeedbackRequest;
import org.medcamp.feedback.feedback.FeedbackDtos.FormTokenResponse;
import org.medcamp.feedback.feedback.FeedbackDtos.PublicFormResponse;
import org.medcamp.feedback.feedback.FeedbackDtos.SubmissionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Open to attendees without signing in; everything is found through the camp's public token. A
 * signed-in staff member may also submit, to fill the form in for an attendee.
 */
@RestController
@RequestMapping("/api/public/camps/{token}")
public class PublicFeedbackController {

	private final PublicFeedbackService publicFeedbackService;

	public PublicFeedbackController(PublicFeedbackService publicFeedbackService) {
		this.publicFeedbackService = publicFeedbackService;
	}

	@GetMapping
	public PublicFormResponse form(@PathVariable String token) {
		return publicFeedbackService.form(token);
	}

	@GetMapping("/form-token")
	public FormTokenResponse formToken(@PathVariable String token, HttpServletRequest request) {
		return publicFeedbackService.issueFormToken(token, request.getRemoteAddr());
	}

	@PostMapping("/feedback")
	@ResponseStatus(HttpStatus.CREATED)
	public SubmissionResponse submit(@PathVariable String token, @Valid @RequestBody FeedbackRequest body,
			HttpServletRequest request, @AuthenticationPrincipal AppUserDetails currentUser) {
		return publicFeedbackService.submit(token, body, request.getRemoteAddr(), currentUser);
	}

}
