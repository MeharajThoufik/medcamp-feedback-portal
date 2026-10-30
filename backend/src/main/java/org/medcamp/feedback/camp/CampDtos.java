package org.medcamp.feedback.camp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CampDtos {

	private CampDtos() {
	}

	/**
	 * Used for both create and update. {@code organizerId} is read only from Super Admins; an
	 * organizer always organizes their own camps. {@code services} is the full list in display
	 * order: rows with an {@code id} are kept and updated, rows without one are added, and
	 * existing services left out are removed.
	 */
	public record CampRequest(@NotBlank @Size(max = 150) String name, @NotBlank @Size(max = 200) String location,
			@NotNull LocalDate startDate, @NotNull LocalDate endDate, @NotNull LocalDate feedbackOpensOn,
			@NotNull LocalDate feedbackClosesOn, Long organizerId,
			@NotNull @Size(max = 50) List<@Valid @NotNull ServiceRequest> services) {
	}

	public record ServiceRequest(Long id, @NotNull Long departmentId, @NotBlank @Size(max = 100) String name) {
	}

	public record StatusRequest(@NotNull CampStatus status) {
	}

	/** Filters for the camp list. A null status means every status except Archived. */
	public record CampFilter(String search, CampStatus status, LocalDate from, LocalDate to, Long organizerId) {
	}

	public record ServiceResponse(Long id, Long departmentId, String departmentName, String name) {

		static ServiceResponse from(CampService service) {
			return new ServiceResponse(service.getId(), service.getDepartment().getId(),
					service.getDepartment().getName(), service.getName());
		}

	}

	/** One row in the camp list. */
	public record CampSummary(Long id, String name, String location, LocalDate startDate, LocalDate endDate,
			CampStatus status, FeedbackState feedbackState, Long organizerId, String organizerName,
			int serviceCount) {

		static CampSummary from(Camp camp, LocalDate today) {
			return new CampSummary(camp.getId(), camp.getName(), camp.getLocation(), camp.getStartDate(),
					camp.getEndDate(), camp.getStatus(), camp.feedbackState(today), camp.getOrganizer().getId(),
					camp.getOrganizer().getFullName(), camp.getServices().size());
		}

	}

	/**
	 * Full camp details. {@code feedbackUrl} is the link inside the camp's QR code.
	 * {@code canEdit} and {@code nextStatuses} tell the screen which actions to offer; the server
	 * checks the same rules again on every change.
	 */
	public record CampResponse(Long id, String name, String location, LocalDate startDate, LocalDate endDate,
			CampStatus status, FeedbackState feedbackState, LocalDate feedbackOpensOn, LocalDate feedbackClosesOn,
			String publicToken, String feedbackUrl, Long organizerId, String organizerName,
			List<ServiceResponse> services, boolean canEdit, List<CampStatus> nextStatuses, Instant createdAt,
			Instant updatedAt) {
	}

	/** What the printable poster shows. {@code wifi} is null when no camp Wi-Fi is set. */
	public record PosterResponse(Long campId, String name, String location, LocalDate startDate, LocalDate endDate,
			String feedbackUrl, PosterWifi wifi) {
	}

	/** {@code password} is null for an open network. */
	public record PosterWifi(String ssid, String password) {
	}


}
