package org.medcamp.feedback.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class DepartmentDtos {

	private DepartmentDtos() {
	}

	public record DepartmentRequest(@NotBlank @Size(max = 100) String name, Boolean active) {
	}

	public record DepartmentResponse(Long id, String name, boolean active) {

		static DepartmentResponse from(Department department) {
			return new DepartmentResponse(department.getId(), department.getName(), department.isActive());
		}

	}

}
