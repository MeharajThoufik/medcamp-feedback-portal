package org.medcamp.feedback.feedback;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Something attendees rate from 1 to 5, such as waiting time. Seeded by the V4 migration. */
@Entity
@Table(name = "rating_categories")
public class RatingCategory {

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

	/** Attendees must answer it (only "Overall experience" for now). */
	@Column(nullable = false)
	private boolean required;

	@Column(nullable = false)
	private boolean active;

	protected RatingCategory() {
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

	public boolean isRequired() {
		return required;
	}

	public boolean isActive() {
		return active;
	}

}
