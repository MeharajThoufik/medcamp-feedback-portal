package org.medcamp.feedback.camp;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.medcamp.feedback.department.Department;
import org.medcamp.feedback.qr.QrTestDecoder;
import org.medcamp.feedback.settings.Setting;
import org.medcamp.feedback.settings.SettingRepository;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** QR codes, posters, new links and the public camp lookup behind the QR code. */
class CampQrTests extends CampTestSupport {

	@Autowired
	private SettingRepository settings;

	private User organizer;
	private Department eyeCare;
	private Camp camp;

	@BeforeEach
	void setUp() {
		organizer = user("asha@test.org", Role.ORGANIZER);
		eyeCare = department("Eye Care");
		camp = camp("Rampur camp", organizer, eyeCare);
	}

	@Test
	void theQrCodeOpensTheCampsFeedbackLink() throws Exception {
		String feedbackUrl = "http://localhost:8080/f/" + camp.getPublicToken();
		mvc.perform(get("/api/camps/{id}", camp.getId()).with(as(organizer)))
			.andExpect(jsonPath("$.feedbackUrl").value(feedbackUrl));

		byte[] png = mvc.perform(get("/api/camps/{id}/qr.png", camp.getId()).param("size", "300").with(as(organizer)))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.IMAGE_PNG))
			.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
			.andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
		assertThat(QrTestDecoder.decode(png)).isEqualTo(feedbackUrl);
		assertThat(QrTestDecoder.width(png)).isEqualTo(300);

		mvc.perform(get("/api/camps/{id}/qr.png", camp.getId()).param("download", "true").with(as(organizer)))
			.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
	}

	@Test
	void onlyPeopleWhoCanSeeTheCampGetItsQrCode() throws Exception {
		User eyeDoctor = users.save(new User("eye@test.org", "Eye Doctor", "x", Role.MEDICAL_STAFF, eyeCare));

		mvc.perform(get("/api/camps/{id}/qr.png", camp.getId()).with(as(eyeDoctor))).andExpect(status().isOk());
		mvc.perform(get("/api/camps/{id}/qr.png", camp.getId()).with(as(user("other@test.org", Role.ORGANIZER))))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/camps/{id}/qr.png", camp.getId())).andExpect(status().isUnauthorized());
	}

	@Test
	void thePosterIncludesTheCampWifiWhenItIsSet() throws Exception {
		mvc.perform(get("/api/camps/{id}/poster", camp.getId()).with(as(organizer)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Rampur camp"))
			.andExpect(jsonPath("$.feedbackUrl").value("http://localhost:8080/f/" + camp.getPublicToken()))
			.andExpect(jsonPath("$.wifi").doesNotExist());

		settings.save(new Setting("wifi.ssid", "CampWiFi"));
		settings.save(new Setting("wifi.password", "secret123"));
		settings.save(new Setting("wifi.security", "WPA"));
		mvc.perform(get("/api/camps/{id}/poster", camp.getId()).with(as(organizer)))
			.andExpect(jsonPath("$.wifi.ssid").value("CampWiFi"))
			.andExpect(jsonPath("$.wifi.password").value("secret123"));
	}

	@ParameterizedTest
	@EnumSource(Role.class)
	void onlyTheCampsManagersGetThePosterOrANewLink(Role role) throws Exception {
		User caller = role == Role.ORGANIZER ? user("other@test.org", role) : user("caller@test.org", role);
		boolean allowed = role == Role.SUPER_ADMIN;

		mvc.perform(get("/api/camps/{id}/poster", camp.getId()).with(as(caller)))
			.andExpect(allowed ? status().isOk() : status().isForbidden());
		mvc.perform(post("/api/camps/{id}/public-token", camp.getId()).with(as(caller)).with(csrf()))
			.andExpect(allowed ? status().isOk() : status().isForbidden());
	}

	@Test
	void aNewLinkReplacesTheOldOne() throws Exception {
		String oldToken = camp.getPublicToken();

		String body = mvc.perform(post("/api/camps/{id}/public-token", camp.getId()).with(as(organizer)).with(csrf()))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String newToken = JsonPath.read(body, "$.publicToken");

		assertThat(newToken).isNotEqualTo(oldToken).matches("[A-Za-z2-9]{8}");
		mvc.perform(get("/api/public/camps/{token}", oldToken)).andExpect(status().isNotFound());
		mvc.perform(get("/api/public/camps/{token}", newToken)).andExpect(status().isOk());

		camp.setStatus(CampStatus.ARCHIVED);
		mvc.perform(post("/api/camps/{id}/public-token", camp.getId()).with(as(organizer)).with(csrf()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CAMP_ARCHIVED"));
	}

	@Test
	void anyoneWithTheLinkSeesOnlyPublicCampDetails() throws Exception {
		mvc.perform(get("/api/public/camps/{token}", camp.getPublicToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Rampur camp"))
			.andExpect(jsonPath("$.feedbackState").value("CAMP_NOT_ACTIVE"))
			.andExpect(jsonPath("$.id").doesNotExist())
			.andExpect(jsonPath("$.organizerName").doesNotExist())
			.andExpect(jsonPath("$.publicToken").doesNotExist());

		camp.setStatus(CampStatus.ACTIVE);
		mvc.perform(get("/api/public/camps/{token}", camp.getPublicToken()))
			.andExpect(jsonPath("$.feedbackState").value("OPEN"));
	}

	@Test
	void unknownMalformedAndArchivedLinksAreAllInvalid() throws Exception {
		mvc.perform(get("/api/public/camps/{token}", "nope1234"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CAMP_LINK_INVALID"));
		mvc.perform(get("/api/public/camps/{token}", "abc")).andExpect(status().isNotFound());
		mvc.perform(get("/api/public/camps/{token}", "token-001")).andExpect(status().isNotFound());

		camp.setStatus(CampStatus.ARCHIVED);
		mvc.perform(get("/api/public/camps/{token}", camp.getPublicToken())).andExpect(status().isNotFound());
	}

}
