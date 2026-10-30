package org.medcamp.feedback.camp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.medcamp.feedback.department.Department;

/**
 * A service offered at a camp, such as "Cataract screening" under Eye Care. This is a JPA entity;
 * the business logic for camps lives in {@link CampManager}.
 */
@Entity
@Table(name = "camp_services")
public class CampService {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "camp_id")
	private Camp camp;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "department_id")
	private Department department;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false)
	private int sortOrder;

	protected CampService() {
	}

	CampService(Camp camp, Department department, String name) {
		this.camp = camp;
		this.department = department;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public Camp getCamp() {
		return camp;
	}

	public Department getDepartment() {
		return department;
	}

	void setDepartment(Department department) {
		this.department = department;
	}

	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

}
