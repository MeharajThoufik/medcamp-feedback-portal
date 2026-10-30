package org.medcamp.feedback.camp;

import org.medcamp.feedback.camp.CampDtos.PublicCampResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Open to attendees without signing in: the page behind a camp's QR code starts here. */
@RestController
@RequestMapping("/api/public/camps")
public class PublicCampController {

	private final CampManager campManager;

	public PublicCampController(CampManager campManager) {
		this.campManager = campManager;
	}

	@GetMapping("/{token}")
	public PublicCampResponse get(@PathVariable String token) {
		return campManager.findPublic(token);
	}

}
