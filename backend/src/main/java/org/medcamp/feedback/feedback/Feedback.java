package org.medcamp.feedback.feedback;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.medcamp.feedback.camp.Camp;
import org.medcamp.feedback.camp.CampService;
import org.medcamp.feedback.user.User;

/**
 * One submitted feedback form. It is never edited after submission. It is anonymous unless the
 * attendee asked for follow-up and agreed to be contacted; only then are contact details kept.
 */
@Entity
@Table(name = "feedback")
public class Feedback {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "camp_id")
	private Camp camp;

	/** Null when the attendee chose "general visit / not sure". */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "camp_service_id")
	private CampService service;

	@Column(length = 1000)
	private String comment;

	@Column(nullable = false)
	private boolean problemReported;

	@Column(nullable = false)
	private boolean followUpConsent;

	@Column(length = 100)
	private String contactName;

	@Column(length = 20)
	private String contactPhone;

	private String contactEmail;

	/** A signed-in staff member filled the form in for the attendee. */
	@Column(nullable = false)
	private boolean assisted;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assisted_by_id")
	private User assistedBy;

	@Column(nullable = false, length = 5)
	private String language;

	@Column(nullable = false, length = 120, updatable = false)
	private String submissionToken;

	@Column(nullable = false, updatable = false)
	private Instant submittedAt;

	@OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
	@BatchSize(size = 50)
	private List<FeedbackRating> ratings = new ArrayList<>();

	protected Feedback() {
	}

	Feedback(Camp camp, CampService service, String language, String submissionToken, Instant submittedAt) {
		this.camp = camp;
		this.service = service;
		this.language = language;
		this.submissionToken = submissionToken;
		this.submittedAt = submittedAt;
	}

	void addRating(RatingCategory category, int score) {
		ratings.add(new FeedbackRating(this, category, score));
	}

	void setComment(String comment) {
		this.comment = comment;
	}

	void setProblemReported(boolean problemReported) {
		this.problemReported = problemReported;
	}

	/** Contact details are kept only with consent; the database checks this too. */
	void giveFollowUpConsent(String name, String phone, String email) {
		this.followUpConsent = true;
		this.contactName = name;
		this.contactPhone = phone;
		this.contactEmail = email;
	}

	void markAssisted(User staffMember) {
		this.assisted = true;
		this.assistedBy = staffMember;
	}

	public Long getId() {
		return id;
	}

	public Camp getCamp() {
		return camp;
	}

	public CampService getService() {
		return service;
	}

	public String getComment() {
		return comment;
	}

	public boolean isProblemReported() {
		return problemReported;
	}

	public boolean isFollowUpConsent() {
		return followUpConsent;
	}

	public String getContactName() {
		return contactName;
	}

	public String getContactPhone() {
		return contactPhone;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public boolean isAssisted() {
		return assisted;
	}

	public User getAssistedBy() {
		return assistedBy;
	}

	public String getLanguage() {
		return language;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public List<FeedbackRating> getRatings() {
		return ratings;
	}

}
