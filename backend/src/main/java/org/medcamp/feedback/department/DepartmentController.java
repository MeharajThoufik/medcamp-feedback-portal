package org.medcamp.feedback.department;

import java.util.List;

import jakarta.validation.Valid;
import org.medcamp.feedback.department.DepartmentDtos.DepartmentRequest;
import org.medcamp.feedback.department.DepartmentDtos.DepartmentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

	private final DepartmentService departmentService;

	public DepartmentController(DepartmentService departmentService) {
		this.departmentService = departmentService;
	}

	/** Any signed-in staff member can read departments (needed for filters and forms). */
	@GetMapping
	public List<DepartmentResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
		return departmentService.list(includeInactive);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public DepartmentResponse create(@Valid @RequestBody DepartmentRequest request) {
		return departmentService.create(request);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public DepartmentResponse update(@PathVariable long id, @Valid @RequestBody DepartmentRequest request) {
		return departmentService.update(id, request);
	}

}
