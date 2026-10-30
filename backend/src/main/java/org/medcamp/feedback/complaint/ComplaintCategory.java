package org.medcamp.feedback.complaint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** What a reported problem is about, such as cleanliness. Seeded by the V5 migration. */
@Entity
@Table(name = "complaint_categories")
public class ComplaintCategory {

	@Id
	private Long id;

	@Column(nullable = false, length = 30)
	private String code;

	@Column(nullable = false, length = 100)
	private String labelEn;

	@Column(nullable = false, length = 100)
	private String labelHi;

	@Column(nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean active;

	protected ComplaintCategory() {
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getLabelEn() {
		return labelEn;
	}

	public String getLabelHi() {
		return labelHi;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public boolean isActive() {
		return active;
	}

}
