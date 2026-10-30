package org.medcamp.feedback.feedback;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingCategoryRepository extends JpaRepository<RatingCategory, Long> {

	List<RatingCategory> findByActiveTrueOrderBySortOrderAsc();

	List<RatingCategory> findAllByOrderBySortOrderAsc();

}
