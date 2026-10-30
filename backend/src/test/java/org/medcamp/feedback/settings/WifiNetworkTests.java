package org.medcamp.feedback.settings;

import org.junit.jupiter.api.Test;
import org.medcamp.feedback.settings.WifiNetwork.WifiSecurity;

import static org.assertj.core.api.Assertions.assertThat;

class WifiNetworkTests {

	@Test
	void buildsTheStandardWifiQrText() {
		assertThat(new WifiNetwork("CampWiFi", "secret123", WifiSecurity.WPA).qrContent())
			.isEqualTo("WIFI:T:WPA;S:CampWiFi;P:secret123;;");
	}

	@Test
	void escapesCharactersThatHaveAMeaningInTheFormat() {
		assertThat(new WifiNetwork("Camp;Net", "a:b,c\\d\"e", WifiSecurity.WPA).qrContent())
			.isEqualTo("WIFI:T:WPA;S:Camp\\;Net;P:a\\:b\\,c\\\\d\\\"e;;");
	}

	@Test
	void openNetworksHaveNoPassword() {
		assertThat(new WifiNetwork("Camp", null, WifiSecurity.NONE).qrContent()).isEqualTo("WIFI:T:nopass;S:Camp;;");
	}

}
