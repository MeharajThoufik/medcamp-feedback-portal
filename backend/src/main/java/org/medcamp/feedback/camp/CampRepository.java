package org.medcamp.feedback.camp;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampRepository extends JpaRepository<Camp, Long>, JpaSpecificationExecutor<Camp> {

	boolean existsByPublicToken(String publicToken);

	@EntityGraph(attributePaths = { "services", "services.department" })
	Optional<Camp> findWithDetailsByPublicToken(String publicToken);

	/** Services with feedback are kept, so the feedback still says which service it was about. */
	@Query("select count(f) > 0 from Feedback f where f.service.id = :serviceId")
	boolean hasFeedbackForService(@Param("serviceId") Long serviceId);

	@EntityGraph(attributePaths = { "organizer", "services", "services.department" })
	Optional<Camp> findWithDetailsById(Long id);

	@Override
	@EntityGraph(attributePaths = "organizer")
	Page<Camp> findAll(Specification<Camp> spec, Pageable pageable);

}
