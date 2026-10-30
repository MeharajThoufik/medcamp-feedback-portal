package org.medcamp.feedback.camp;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CampRepository extends JpaRepository<Camp, Long>, JpaSpecificationExecutor<Camp> {

	boolean existsByPublicToken(String publicToken);

	Optional<Camp> findByPublicToken(String publicToken);

	@EntityGraph(attributePaths = { "organizer", "services", "services.department" })
	Optional<Camp> findWithDetailsById(Long id);

	@Override
	@EntityGraph(attributePaths = "organizer")
	Page<Camp> findAll(Specification<Camp> spec, Pageable pageable);

}
