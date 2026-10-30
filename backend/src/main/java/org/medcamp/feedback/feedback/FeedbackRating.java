package org.medcamp.feedback.feedback;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "feedback_ratings")
public class FeedbackRating {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "feedback_id")
	private Feedback feedback;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id")
	private RatingCategory category;

	/** 1 (very poor) to 5 (excellent). */
	@Column(nullable = false)
	private short score;

	protected FeedbackRating() {
	}

	FeedbackRating(Feedback feedback, RatingCategory category, int score) {
		this.feedback = feedback;
		this.category = category;
		this.score = (short) score;
	}

	public RatingCategory getCategory() {
		return category;
	}

	public int getScore() {
		return score;
	}

}
