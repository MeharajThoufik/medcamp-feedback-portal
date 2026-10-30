package org.medcamp.feedback.feedback;

import java.util.LinkedHashMap;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import org.medcamp.feedback.camp.Camp;
import org.medcamp.feedback.camp.CampTestSupport;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class FeedbackTestSupport extends CampTestSupport {

	/** Each test gets its own address, so the per-phone limit never carries over between tests. */
	private static int ipCounter;

	protected final String phoneIp = "10.0." + (++ipCounter / 250) + "." + (ipCounter % 250 + 1);

	protected RequestPostProcessor fromPhone(String ip) {
		return request -> {
			request.setRemoteAddr(ip);
			return request;
		};
	}

	protected String formToken(Camp camp) throws Exception {
		String body = mvc.perform(get("/api/public/camps/{token}/form-token", camp.getPublicToken()).with(fromPhone(phoneIp)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.submissionToken");
	}

	/** A valid submission: only the required "overall" rating, in English. */
	protected Map<String, Object> answers(String submissionToken) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("submissionToken", submissionToken);
		body.put("language", "en");
		body.put("serviceId", null);
		body.put("ratings", Map.of("OVERALL", 4));
		body.put("comment", null);
		body.put("problemReported", false);
		body.put("followUp", null);
		body.put("assisted", false);
		return body;
	}

	protected static Map<String, Object> followUp(boolean consent, String name, String phone, String email) {
		Map<String, Object> followUp = new LinkedHashMap<>();
		followUp.put("consent", consent);
		followUp.put("name", name);
		followUp.put("phone", phone);
		followUp.put("email", email);
		return followUp;
	}

	protected ResultActions submit(Camp camp, Map<String, Object> body, RequestPostProcessor... extra) throws Exception {
		MockHttpServletRequestBuilder request = post("/api/public/camps/{token}/feedback", camp.getPublicToken())
			.with(csrf())
			.with(fromPhone(phoneIp))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json(body));
		for (RequestPostProcessor processor : extra) {
			request.with(processor);
		}
		return mvc.perform(request);
	}

	/** Gets a fresh form and submits it. */
	protected ResultActions submitNew(Camp camp, Map<String, Object> changes) throws Exception {
		Map<String, Object> body = answers(formToken(camp));
		body.putAll(changes);
		return submit(camp, body);
	}

}
