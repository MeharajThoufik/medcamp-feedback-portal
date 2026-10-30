package org.medcamp.feedback.complaint;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.medcamp.feedback.camp.Camp;
import org.medcamp.feedback.feedback.Feedback;
import org.medcamp.feedback.user.User;

/** A problem reported in a feedback form, tracked by its reference number. */
@Entity
@Table(name = "complaints")
public class Complaint {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "feedback_id", updatable = false)
	private Feedback feedback;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "camp_id", updatable = false)
	private Camp camp;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id")
	private ComplaintCategory category;

	@Column(nullable = false, length = 1000, updatable = false)
	private String description;

	@Column(nullable = false, length = 20, updatable = false)
	private String referenceNo;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 10)
	private ComplaintPriority priority = ComplaintPriority.MEDIUM;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private ComplaintStatus status = ComplaintStatus.OPEN;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	private User assignee;

	private LocalDate dueDate;

	/** The only staff text attendees can see on the status page. */
	@Column(length = 500)
	private String publicNote;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Complaint() {
	}

	Complaint(Feedback feedback, ComplaintCategory category, String description, String referenceNo) {
		this.feedback = feedback;
		this.camp = feedback.getCamp();
		this.category = category;
		this.description = description;
		this.referenceNo = referenceNo;
	}

	public Long getId() {
		return id;
	}

	public Feedback getFeedback() {
		return feedback;
	}

	public Camp getCamp() {
		return camp;
	}

	public ComplaintCategory getCategory() {
		return category;
	}

	public String getDescription() {
		return description;
	}

	public String getReferenceNo() {
		return referenceNo;
	}

	public ComplaintPriority getPriority() {
		return priority;
	}

	public ComplaintStatus getStatus() {
		return status;
	}

	public User getAssignee() {
		return assignee;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public String getPublicNote() {
		return publicNote;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
