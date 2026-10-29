package org.medcamp.feedback.department;

import java.util.List;

import org.medcamp.feedback.common.ApiException;
import org.medcamp.feedback.department.DepartmentDtos.DepartmentRequest;
import org.medcamp.feedback.department.DepartmentDtos.DepartmentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DepartmentService {

	private final DepartmentRepository departments;

	public DepartmentService(DepartmentRepository departments) {
		this.departments = departments;
	}

	@Transactional(readOnly = true)
	public List<DepartmentResponse> list(boolean includeInactive) {
		List<Department> found = includeInactive ? departments.findAllByOrderByNameAsc()
				: departments.findByActiveTrueOrderByNameAsc();
		return found.stream().map(DepartmentResponse::from).toList();
	}

	public DepartmentResponse create(DepartmentRequest request) {
		String name = request.name().trim();
		if (departments.existsByNameIgnoreCase(name)) {
			throw nameTaken();
		}
		Department department = new Department(name);
		if (request.active() != null) {
			department.setActive(request.active());
		}
		return DepartmentResponse.from(departments.save(department));
	}

	public DepartmentResponse update(long id, DepartmentRequest request) {
		Department department = getEntity(id);
		String name = request.name().trim();
		if (departments.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw nameTaken();
		}
		department.setName(name);
		if (request.active() != null) {
			department.setActive(request.active());
		}
		return DepartmentResponse.from(department);
	}

	@Transactional(readOnly = true)
	public Department getEntity(long id) {
		return departments.findById(id)
			.orElseThrow(() -> ApiException.notFound("DEPARTMENT_NOT_FOUND", "Department not found."));
	}

	private static ApiException nameTaken() {
		return ApiException.conflict("DEPARTMENT_NAME_TAKEN", "A department with this name already exists.");
	}

}
