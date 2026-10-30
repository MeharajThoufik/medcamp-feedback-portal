package org.medcamp.feedback.camp;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.medcamp.feedback.auth.AppUserDetails;
import org.medcamp.feedback.camp.CampDtos.CampFilter;
import org.medcamp.feedback.camp.CampDtos.CampRequest;
import org.medcamp.feedback.camp.CampDtos.CampResponse;
import org.medcamp.feedback.camp.CampDtos.CampSummary;
import org.medcamp.feedback.camp.CampDtos.ServiceRequest;
import org.medcamp.feedback.camp.CampDtos.ServiceResponse;
import org.medcamp.feedback.common.ApiException;
import org.medcamp.feedback.common.PageResponse;
import org.medcamp.feedback.config.AppProperties;
import org.medcamp.feedback.department.Department;
import org.medcamp.feedback.department.DepartmentService;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;
import org.medcamp.feedback.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Camp management. Called a manager because {@link CampService} is the entity for a service
 * offered at a camp.
 * <p>
 * Who can see a camp: Super Admins and Analysts see every camp, Organizers see their own camps,
 * and Medical Staff see camps that offer a service of their department. Only Super Admins and the
 * camp's own Organizer can change it.
 */
@Service
@Transactional
public class CampManager {

	private static final int MAX_PAGE_SIZE = 100;
	private static final int MAX_TOKEN_ATTEMPTS = 10;

	private final CampRepository camps;
	private final UserRepository users;
	private final DepartmentService departmentService;
	private final PublicTokenGenerator tokenGenerator;
	private final Clock clock;
	private final AppProperties properties;

	public CampManager(CampRepository camps, UserRepository users, DepartmentService departmentService,
			PublicTokenGenerator tokenGenerator, Clock clock, AppProperties properties) {
		this.camps = camps;
		this.users = users;
		this.departmentService = departmentService;
		this.tokenGenerator = tokenGenerator;
		this.clock = clock;
		this.properties = properties;
	}

	@Transactional(readOnly = true)
	public PageResponse<CampSummary> list(CampFilter filter, int page, int size, AppUserDetails me) {
		PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
				Sort.by(Sort.Direction.DESC, "startDate").and(Sort.by(Sort.Direction.DESC, "id")));
		LocalDate today = today();
		return PageResponse
			.of(camps.findAll(visibleTo(me).and(matching(filter)), pageRequest).map(c -> CampSummary.from(c, today)));
	}

	@Transactional(readOnly = true)
	public CampResponse get(long id, AppUserDetails me) {
		Camp camp = getEntity(id);
		if (!canView(camp, me)) {
			throw ApiException.forbidden("CAMP_ACCESS_DENIED", "You do not have access to this camp.");
		}
		return toResponse(camp, me);
	}

	public CampResponse create(CampRequest request, AppUserDetails me) {
		requireOrganizerOrAdmin(me);
		Camp camp = new Camp(newPublicToken(), resolveOrganizer(request.organizerId(), null, me));
		apply(camp, request);
		return toResponse(camps.save(camp), me);
	}

	public CampResponse update(long id, CampRequest request, AppUserDetails me) {
		Camp camp = getEntityForChange(id, me);
		if (camp.getStatus() == CampStatus.ARCHIVED) {
			throw ApiException.conflict("CAMP_ARCHIVED", "Archived camps cannot be edited. Restore the camp first.");
		}
		camp.setOrganizer(resolveOrganizer(request.organizerId(), camp.getOrganizer(), me));
		apply(camp, request);
		return toResponse(camp, me);
	}

	public CampResponse changeStatus(long id, CampStatus target, AppUserDetails me) {
		Camp camp = getEntityForChange(id, me);
		if (!camp.getStatus().canMoveTo(target)) {
			throw ApiException.badRequest("INVALID_STATUS_CHANGE",
					"A camp cannot move from " + camp.getStatus() + " to " + target + ".");
		}
		if (target == CampStatus.ACTIVE && camp.getServices().isEmpty()) {
			throw ApiException.badRequest("CAMP_HAS_NO_SERVICES", "Add at least one service before opening the camp.");
		}
		camp.setStatus(target);
		return toResponse(camp, me);
	}

	private void apply(Camp camp, CampRequest request) {
		if (request.endDate().isBefore(request.startDate())) {
			throw ApiException.badRequest("INVALID_DATES", "The end date must be on or after the start date.");
		}
		if (request.feedbackClosesOn().isBefore(request.feedbackOpensOn())
				|| request.feedbackClosesOn().isBefore(request.startDate())) {
			throw ApiException.badRequest("INVALID_FEEDBACK_WINDOW",
					"The feedback window must close on or after the day it opens, and not before the camp starts.");
		}
		camp.setName(request.name().trim());
		camp.setLocation(request.location().trim());
		camp.setDates(request.startDate(), request.endDate());
		camp.setFeedbackWindow(request.feedbackOpensOn(), request.feedbackClosesOn());
		applyServices(camp, request.services());
	}

	/** Makes the camp's services match the requested list, in the requested order. */
	private void applyServices(Camp camp, List<ServiceRequest> requested) {
		Set<String> names = new HashSet<>();
		for (ServiceRequest service : requested) {
			if (!names.add(service.name().trim().toLowerCase(Locale.ROOT))) {
				throw ApiException.badRequest("SERVICE_NAME_DUPLICATE",
						"Each service at a camp needs a different name: " + service.name().trim());
			}
		}
		Map<Long, CampService> existing = new HashMap<>();
		camp.getServices().forEach(service -> existing.put(service.getId(), service));

		List<CampService> ordered = new ArrayList<>();
		for (ServiceRequest request : requested) {
			String name = request.name().trim();
			CampService service;
			if (request.id() == null) {
				service = camp.addService(resolveDepartment(request.departmentId(), null), name);
			}
			else {
				// remove() also rejects the same id sent twice.
				service = existing.remove(request.id());
				if (service == null) {
					throw ApiException.badRequest("SERVICE_NOT_FOUND", "This service does not belong to the camp.");
				}
				service.setDepartment(resolveDepartment(request.departmentId(), service.getDepartment()));
				service.setName(name);
			}
			ordered.add(service);
		}
		camp.keepServices(ordered);
	}

	/** A newly chosen department must be active; a service may keep one that was deactivated later. */
	private Department resolveDepartment(Long departmentId, Department current) {
		if (current != null && departmentId.equals(current.getId())) {
			return current;
		}
		Department department = departmentService.getEntity(departmentId);
		if (!department.isActive()) {
			throw ApiException.badRequest("DEPARTMENT_INACTIVE", "This department is inactive.");
		}
		return department;
	}

	/**
	 * Organizers always run their own camps (on update the camp keeps its organizer). Super Admins
	 * choose an enabled Organizer; a camp may keep one who was disabled later.
	 */
	private User resolveOrganizer(Long organizerId, User current, AppUserDetails me) {
		if (me.getRole() == Role.ORGANIZER) {
			return current != null ? current : users.getReferenceById(me.getId());
		}
		if (organizerId == null) {
			throw ApiException.badRequest("ORGANIZER_REQUIRED", "Choose the organizer for this camp.");
		}
		if (current != null && organizerId.equals(current.getId())) {
			return current;
		}
		return users.findById(organizerId)
			.filter(user -> user.getRole() == Role.ORGANIZER && user.isEnabled())
			.orElseThrow(() -> ApiException.badRequest("ORGANIZER_INVALID",
					"The organizer must be an active user with the Organizer role."));
	}

	private String newPublicToken() {
		for (int attempt = 0; attempt < MAX_TOKEN_ATTEMPTS; attempt++) {
			String token = tokenGenerator.generate();
			if (!camps.existsByPublicToken(token)) {
				return token;
			}
		}
		throw new IllegalStateException("Could not create a unique public token");
	}

	private Camp getEntity(long id) {
		return camps.findWithDetailsById(id)
			.orElseThrow(() -> ApiException.notFound("CAMP_NOT_FOUND", "Camp not found."));
	}

	private Camp getEntityForChange(long id, AppUserDetails me) {
		requireOrganizerOrAdmin(me);
		Camp camp = getEntity(id);
		if (!canEdit(camp, me)) {
			throw ApiException.forbidden("NOT_CAMP_ORGANIZER", "Only this camp's organizer can change it.");
		}
		return camp;
	}

	/** Backs up the controller's role check, so the rule also holds if the service is called elsewhere. */
	private static void requireOrganizerOrAdmin(AppUserDetails me) {
		if (me.getRole() != Role.SUPER_ADMIN && me.getRole() != Role.ORGANIZER) {
			throw ApiException.forbidden("FORBIDDEN", "Only organizers can manage camps.");
		}
	}

	private static boolean canEdit(Camp camp, AppUserDetails me) {
		return switch (me.getRole()) {
			case SUPER_ADMIN -> true;
			case ORGANIZER -> camp.getOrganizer().getId().equals(me.getId());
			case MEDICAL_STAFF, ANALYST -> false;
		};
	}

	private static boolean canView(Camp camp, AppUserDetails me) {
		return switch (me.getRole()) {
			case SUPER_ADMIN, ANALYST -> true;
			case ORGANIZER -> camp.getOrganizer().getId().equals(me.getId());
			case MEDICAL_STAFF -> camp.getServices()
				.stream()
				.anyMatch(service -> service.getDepartment().getId().equals(me.getDepartmentId()));
		};
	}

	/** The list version of {@link #canView}. */
	private static Specification<Camp> visibleTo(AppUserDetails me) {
		return (root, query, cb) -> switch (me.getRole()) {
			case SUPER_ADMIN, ANALYST -> cb.conjunction();
			case ORGANIZER -> cb.equal(root.get("organizer").get("id"), me.getId());
			case MEDICAL_STAFF -> {
				if (me.getDepartmentId() == null) {
					yield cb.disjunction();
				}
				Subquery<Long> offered = query.subquery(Long.class);
				Root<CampService> service = offered.from(CampService.class);
				offered.select(service.get("id"))
					.where(cb.equal(service.get("camp"), root),
							cb.equal(service.get("department").get("id"), me.getDepartmentId()));
				yield cb.exists(offered);
			}
		};
	}

	private static Specification<Camp> matching(CampFilter filter) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (filter.status() == null) {
				predicates.add(cb.notEqual(root.get("status"), CampStatus.ARCHIVED));
			}
			else {
				predicates.add(cb.equal(root.get("status"), filter.status()));
			}
			if (filter.search() != null && !filter.search().isBlank()) {
				String pattern = "%" + escapeLike(filter.search().trim().toLowerCase(Locale.ROOT)) + "%";
				predicates.add(cb.or(cb.like(cb.lower(root.get("name")), pattern, '\\'),
						cb.like(cb.lower(root.get("location")), pattern, '\\')));
			}
			// A camp matches a date range when any of its days falls inside it.
			if (filter.from() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), filter.from()));
			}
			if (filter.to() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), filter.to()));
			}
			if (filter.organizerId() != null) {
				predicates.add(cb.equal(root.get("organizer").get("id"), filter.organizerId()));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private CampResponse toResponse(Camp camp, AppUserDetails me) {
		boolean canEdit = canEdit(camp, me);
		List<CampStatus> nextStatuses = canEdit
				? Arrays.stream(CampStatus.values()).filter(camp.getStatus()::canMoveTo).toList() : List.of();
		return new CampResponse(camp.getId(), camp.getName(), camp.getLocation(), camp.getStartDate(),
				camp.getEndDate(), camp.getStatus(), camp.feedbackState(today()), camp.getFeedbackOpensOn(),
				camp.getFeedbackClosesOn(), camp.getPublicToken(), camp.getOrganizer().getId(),
				camp.getOrganizer().getFullName(), camp.getServices().stream().map(ServiceResponse::from).toList(),
				canEdit, nextStatuses, camp.getCreatedAt(), camp.getUpdatedAt());
	}

	/** Camp dates are calendar days where the camps run, not in UTC. */
	private LocalDate today() {
		return LocalDate.now(clock.withZone(properties.timeZone()));
	}

}
