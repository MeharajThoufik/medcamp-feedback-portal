package org.medcamp.feedback.complaint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

	boolean existsByReferenceNo(String referenceNo);

	Optional<Complaint> findByReferenceNo(String referenceNo);

	/** The complaints raised with these feedback entries, for showing them in feedback lists. */
	@EntityGraph(attributePaths = "category")
	List<Complaint> findByFeedbackIdIn(Collection<Long> feedbackIds);

}
