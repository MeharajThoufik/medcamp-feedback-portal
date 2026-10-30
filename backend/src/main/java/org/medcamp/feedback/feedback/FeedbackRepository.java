package org.medcamp.feedback.feedback;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

	boolean existsBySubmissionToken(String submissionToken);

	/** {@code departmentId} limits the list to that department's services; null means all feedback. */
	@EntityGraph(attributePaths = { "service", "service.department", "assistedBy" })
	@Query(value = """
			select f from Feedback f left join f.service s left join s.department d
			where f.camp.id = :campId and (:departmentId is null or d.id = :departmentId)
			""", countQuery = """
			select count(f) from Feedback f left join f.service s left join s.department d
			where f.camp.id = :campId and (:departmentId is null or d.id = :departmentId)
			""")
	Page<Feedback> findForCamp(@Param("campId") Long campId, @Param("departmentId") Long departmentId,
			Pageable pageable);

	@Query("""
			select count(f) from Feedback f left join f.service s left join s.department d
			where f.camp.id = :campId and (:departmentId is null or d.id = :departmentId)
			""")
	long countForCamp(@Param("campId") Long campId, @Param("departmentId") Long departmentId);

	/** Average score and number of answers per category code. */
	@Query("""
			select new org.medcamp.feedback.feedback.CategoryAverage(
				c.code, avg(r.score), count(r))
			from FeedbackRating r join r.category c join r.feedback f
				left join f.service s left join s.department d
			where f.camp.id = :campId and (:departmentId is null or d.id = :departmentId)
			group by c.code
			""")
	List<CategoryAverage> averagesForCamp(@Param("campId") Long campId, @Param("departmentId") Long departmentId);

}
