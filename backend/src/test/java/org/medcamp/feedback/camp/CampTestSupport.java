package org.medcamp.feedback.camp;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.medcamp.feedback.ApiTestSupport;
import org.medcamp.feedback.department.Department;
import org.medcamp.feedback.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

abstract class CampTestSupport extends ApiTestSupport {

	/** "Today" as the app sees it (the default camp time zone). */
	protected static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Kolkata"));

	@Autowired
	protected CampRepository camps;

	@Autowired
	private JsonMapper jsonMapper;

	private int tokenCounter;

	/** Saves an Upcoming camp running today, with one service per department. */
	protected Camp camp(String name, User organizer, Department... departments) {
		Camp camp = new Camp("token%03d".formatted(++tokenCounter), organizer);
		camp.setName(name);
		camp.setLocation("Village Hall");
		camp.setDates(TODAY, TODAY);
		camp.setFeedbackWindow(TODAY, TODAY.plusDays(7));
		for (Department department : departments) {
			camp.addService(department, department.getName() + " check-up");
		}
		camp.keepServices(new ArrayList<>(camp.getServices()));
		return camps.save(camp);
	}

	/** A valid create/update body; tests change single fields with {@code with}. */
	protected static Map<String, Object> campBody(Long organizerId, Map<?, ?>... services) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("name", "Rampur Health Camp");
		body.put("location", "Government School, Rampur");
		body.put("startDate", TODAY.toString());
		body.put("endDate", TODAY.plusDays(1).toString());
		body.put("feedbackOpensOn", TODAY.toString());
		body.put("feedbackClosesOn", TODAY.plusDays(8).toString());
		body.put("organizerId", organizerId);
		body.put("services", List.of(services));
		return body;
	}

	protected static Map<String, Object> service(Long id, Department department, String name) {
		Map<String, Object> service = new LinkedHashMap<>();
		service.put("id", id);
		service.put("departmentId", department.getId());
		service.put("name", name);
		return service;
	}

	protected static Map<String, Object> with(Map<String, Object> body, String field, Object value) {
		Map<String, Object> copy = new LinkedHashMap<>(body);
		copy.put(field, value);
		return copy;
	}

	protected ResultActions createCamp(User caller, Map<String, Object> body) throws Exception {
		return mvc.perform(post("/api/camps").with(as(caller))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(jsonMapper.writeValueAsString(body)));
	}

	protected ResultActions updateCamp(User caller, long id, Map<String, Object> body) throws Exception {
		return mvc.perform(put("/api/camps/{id}", id).with(as(caller))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(jsonMapper.writeValueAsString(body)));
	}

	protected ResultActions changeStatus(User caller, long id, String status) throws Exception {
		return mvc.perform(put("/api/camps/{id}/status", id).with(as(caller))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status": "%s"}
					""".formatted(status)));
	}

}
