package org.medcamp.feedback.common;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes JSON bodies from security filters and handlers, which run outside Spring MVC and so
 * cannot use {@link GlobalExceptionHandler}. Error bodies have the same shape as the MVC ones.
 */
@Component
public class ProblemResponseWriter {

	private final JsonMapper jsonMapper;

	public ProblemResponseWriter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public void writeProblem(HttpServletResponse response, HttpStatus status, String code, String detail)
			throws IOException {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", "about:blank");
		body.put("title", status.getReasonPhrase());
		body.put("status", status.value());
		body.put("detail", detail);
		body.put("code", code);
		write(response, status, MediaType.APPLICATION_PROBLEM_JSON_VALUE, body);
	}

	public void writeJson(HttpServletResponse response, HttpStatus status, Object body) throws IOException {
		write(response, status, MediaType.APPLICATION_JSON_VALUE, body);
	}

	private void write(HttpServletResponse response, HttpStatus status, String contentType, Object body)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(contentType);
		response.setCharacterEncoding("UTF-8");
		jsonMapper.writeValue(response.getOutputStream(), body);
	}

}
