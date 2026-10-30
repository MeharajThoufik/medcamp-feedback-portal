package org.medcamp.feedback.camp;

import java.time.LocalDate;

import jakarta.validation.Valid;
import org.medcamp.feedback.auth.AppUserDetails;
import org.medcamp.feedback.camp.CampDtos.CampFilter;
import org.medcamp.feedback.camp.CampDtos.CampRequest;
import org.medcamp.feedback.camp.CampDtos.CampResponse;
import org.medcamp.feedback.camp.CampDtos.CampSummary;
import org.medcamp.feedback.camp.CampDtos.StatusRequest;
import org.medcamp.feedback.common.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Every signed-in role can read the camps it is allowed to see; see {@link CampManager} for the rules. */
@RestController
@RequestMapping("/api/camps")
public class CampController {

	private final CampManager campManager;

	public CampController(CampManager campManager) {
		this.campManager = campManager;
	}

	@GetMapping
	public PageResponse<CampSummary> list(@RequestParam(required = false) String search,
			@RequestParam(required = false) CampStatus status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long organizerId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, @AuthenticationPrincipal AppUserDetails currentUser) {
		return campManager.list(new CampFilter(search, status, from, to, organizerId), page, size, currentUser);
	}

	@GetMapping("/{id}")
	public CampResponse get(@PathVariable long id, @AuthenticationPrincipal AppUserDetails currentUser) {
		return campManager.get(id, currentUser);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANIZER')")
	public CampResponse create(@Valid @RequestBody CampRequest request,
			@AuthenticationPrincipal AppUserDetails currentUser) {
		return campManager.create(request, currentUser);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANIZER')")
	public CampResponse update(@PathVariable long id, @Valid @RequestBody CampRequest request,
			@AuthenticationPrincipal AppUserDetails currentUser) {
		return campManager.update(id, request, currentUser);
	}

	@PutMapping("/{id}/status")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANIZER')")
	public CampResponse changeStatus(@PathVariable long id, @Valid @RequestBody StatusRequest request,
			@AuthenticationPrincipal AppUserDetails currentUser) {
		return campManager.changeStatus(id, request.status(), currentUser);
	}

}
