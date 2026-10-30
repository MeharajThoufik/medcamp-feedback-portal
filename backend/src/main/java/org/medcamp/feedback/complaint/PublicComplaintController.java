package org.medcamp.feedback.complaint;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import org.medcamp.feedback.common.ApiException;
import org.medcamp.feedback.common.RateLimiter;
import org.medcamp.feedback.complaint.ComplaintDtos.PublicComplaintStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public "check my complaint" lookup. Every lookup counts towards a per-address limit, found
 * or not, which makes guessing references by trying many of them impractical.
 */
@RestController
@RequestMapping("/api/public/complaints")
public class PublicComplaintController {

	static final int MAX_LOOKUPS = 20;
	static final Duration LOOKUP_WINDOW = Duration.ofMinutes(10);

	private final ComplaintRepository complaints;
	private final RateLimiter rateLimiter;

	public PublicComplaintController(ComplaintRepository complaints, RateLimiter rateLimiter) {
		this.complaints = complaints;
		this.rateLimiter = rateLimiter;
	}

	@GetMapping("/{reference}")
	@Transactional(readOnly = true)
	public PublicComplaintStatus status(@PathVariable String reference, HttpServletRequest request) {
		if (!rateLimiter.tryAcquire("complaint-lookup:" + request.getRemoteAddr(), MAX_LOOKUPS, LOOKUP_WINDOW)) {
			throw ApiException.tooManyRequests("TOO_MANY_LOOKUPS",
					"Too many lookups from this phone. Please wait a few minutes and try again.");
		}
		// Wrong, malformed and unknown references all get the same answer.
		return ReferenceNumbers.normalize(reference)
			.flatMap(complaints::findByReferenceNo)
			.map(c -> new PublicComplaintStatus(c.getReferenceNo(), c.getStatus(), c.getCreatedAt(), c.getUpdatedAt(),
					c.getPublicNote()))
			.orElseThrow(() -> ApiException.notFound("COMPLAINT_NOT_FOUND", "No complaint has this reference."));
	}

}
