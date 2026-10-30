package org.medcamp.feedback.complaint;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintCategoryRepository extends JpaRepository<ComplaintCategory, Long> {

	List<ComplaintCategory> findByActiveTrueOrderBySortOrderAsc();

	Optional<ComplaintCategory> findByCodeAndActiveTrue(String code);

}
