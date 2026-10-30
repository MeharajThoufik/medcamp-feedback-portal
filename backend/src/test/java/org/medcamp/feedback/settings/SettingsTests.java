package org.medcamp.feedback.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.medcamp.feedback.ApiTestSupport;
import org.medcamp.feedback.qr.QrTestDecoder;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettingsTests extends ApiTestSupport {

	private User admin;

	@BeforeEach
	void setUp() {
		admin = user("admin@test.org", Role.SUPER_ADMIN);
	}

	@Test
	void adminSetsChangesAndClearsTheCampWifi() throws Exception {
		mvc.perform(get("/api/settings").with(as(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.publicBaseUrl").value("http://localhost:8080"))
			.andExpect(jsonPath("$.wifi").doesNotExist());

		saveWifi(admin, """
				{"ssid": " CampWiFi ", "password": "secret123", "security": "WPA"}
				""").andExpect(status().isOk()).andExpect(jsonPath("$.ssid").value("CampWiFi"));
		saveWifi(admin, """
				{"ssid": "OpenCamp", "password": "ignored1", "security": "NONE"}
				""").andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
		mvc.perform(get("/api/settings").with(as(admin)))
			.andExpect(jsonPath("$.wifi.ssid").value("OpenCamp"))
			.andExpect(jsonPath("$.wifi.security").value("NONE"));

		mvc.perform(delete("/api/settings/wifi").with(as(admin)).with(csrf())).andExpect(status().isNoContent());
		mvc.perform(get("/api/settings").with(as(admin))).andExpect(jsonPath("$.wifi").doesNotExist());
	}

	@Test
	void wifiDetailsAreChecked() throws Exception {
		saveWifi(admin, """
				{"ssid": "CampWiFi", "password": "short", "security": "WPA"}
				""").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WIFI_PASSWORD_INVALID"));
		saveWifi(admin, """
				{"ssid": "CampWiFi", "password": "पासवर्ड123456", "security": "WPA"}
				""").andExpect(jsonPath("$.code").value("WIFI_PASSWORD_INVALID"));
		// 12 Devanagari characters take 36 bytes: under 32 characters, but too long for Wi-Fi.
		saveWifi(admin, """
				{"ssid": "शिविरशिविरशि", "security": "NONE"}
				""").andExpect(jsonPath("$.code").value("WIFI_NAME_TOO_LONG"));
		saveWifi(admin, """
				{"ssid": "", "security": null}
				""").andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void theWifiQrCodeJoinsTheNetwork() throws Exception {
		User organizer = user("org@test.org", Role.ORGANIZER);
		mvc.perform(get("/api/settings/wifi/qr.png").with(as(organizer)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("WIFI_NOT_SET"));

		saveWifi(admin, """
				{"ssid": "CampWiFi", "password": "secret123", "security": "WPA"}
				""");
		byte[] png = mvc.perform(get("/api/settings/wifi/qr.png").with(as(organizer)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
		assertThat(QrTestDecoder.decode(png)).isEqualTo("WIFI:T:WPA;S:CampWiFi;P:secret123;;");
	}

	@ParameterizedTest
	@EnumSource(Role.class)
	void onlyAdminsManageSettingsAndOnlyCampManagersGetTheWifiQr(Role role) throws Exception {
		User caller = user("caller@test.org", role);
		saveWifi(admin, """
				{"ssid": "CampWiFi", "password": "secret123", "security": "WPA"}
				""");
		ResultMatcher adminOnly = role == Role.SUPER_ADMIN ? status().is2xxSuccessful() : status().isForbidden();

		mvc.perform(get("/api/settings").with(as(caller))).andExpect(adminOnly);
		saveWifi(caller, """
				{"ssid": "Other", "security": "NONE"}
				""").andExpect(adminOnly);
		mvc.perform(get("/api/settings/wifi/qr.png").with(as(caller)))
			.andExpect(role == Role.SUPER_ADMIN || role == Role.ORGANIZER ? status().isOk() : status().isForbidden());
		mvc.perform(delete("/api/settings/wifi").with(as(caller)).with(csrf())).andExpect(adminOnly);
	}

	private ResultActions saveWifi(User caller, String json) throws Exception {
		return mvc.perform(put("/api/settings/wifi").with(as(caller))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

}
