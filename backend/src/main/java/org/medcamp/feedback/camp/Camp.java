package org.medcamp.feedback.camp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.medcamp.feedback.department.Department;
import org.medcamp.feedback.user.User;

@Entity
@Table(name = "camps")
public class Camp {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(nullable = false, length = 200)
	private String location;

	@Column(nullable = false)
	private LocalDate startDate;

	@Column(nullable = false)
	private LocalDate endDate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organizer_id")
	private User organizer;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private CampStatus status = CampStatus.UPCOMING;

	/** First day attendees can give feedback (inclusive). */
	@Column(nullable = false)
	private LocalDate feedbackOpensOn;

	/** Last day attendees can give feedback (inclusive). */
	@Column(nullable = false)
	private LocalDate feedbackClosesOn;

	/** Random code used in the public feedback link, so camp links cannot be guessed. */
	@Column(nullable = false, length = 16, updatable = false)
	private String publicToken;

	@OneToMany(mappedBy = "camp", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder")
	@BatchSize(size = 50)
	private List<CampService> services = new ArrayList<>();

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Camp() {
	}

	public Camp(String publicToken, User organizer) {
		this.publicToken = publicToken;
		this.organizer = organizer;
	}

	/**
	 * Attendees can give feedback only while the camp is Active and today is inside the feedback
	 * window.
	 */
	public FeedbackState feedbackState(LocalDate today) {
		if (status != CampStatus.ACTIVE) {
			return FeedbackState.CAMP_NOT_ACTIVE;
		}
		if (today.isBefore(feedbackOpensOn)) {
			return FeedbackState.NOT_STARTED;
		}
		if (today.isAfter(feedbackClosesOn)) {
			return FeedbackState.ENDED;
		}
		return FeedbackState.OPEN;
	}

	CampService addService(Department department, String serviceName) {
		CampService service = new CampService(this, department, serviceName);
		services.add(service);
		return service;
	}

	/** Keeps only these services (removing the rest) and numbers them in this order. */
	void keepServices(List<CampService> ordered) {
		services.retainAll(ordered);
		for (int i = 0; i < ordered.size(); i++) {
			ordered.get(i).setSortOrder(i);
		}
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setDates(LocalDate startDate, LocalDate endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public User getOrganizer() {
		return organizer;
	}

	public void setOrganizer(User organizer) {
		this.organizer = organizer;
	}

	public CampStatus getStatus() {
		return status;
	}

	void setStatus(CampStatus status) {
		this.status = status;
	}

	public LocalDate getFeedbackOpensOn() {
		return feedbackOpensOn;
	}

	public LocalDate getFeedbackClosesOn() {
		return feedbackClosesOn;
	}

	public void setFeedbackWindow(LocalDate opensOn, LocalDate closesOn) {
		this.feedbackOpensOn = opensOn;
		this.feedbackClosesOn = closesOn;
	}

	public String getPublicToken() {
		return publicToken;
	}

	/** In display order. */
	public List<CampService> getServices() {
		return services.stream().sorted(Comparator.comparingInt(CampService::getSortOrder)).toList();
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
