package org.medcamp.feedback.common;

import java.util.List;

import org.springframework.data.domain.Page;

/** Stable JSON shape for paged lists, independent of the internal format of Spring Data Page. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

	public static <T> PageResponse<T> of(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages());
	}

}
