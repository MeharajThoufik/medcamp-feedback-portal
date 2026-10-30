package org.medcamp.feedback.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.medcamp.feedback.settings.WifiNetwork.WifiSecurity;

public final class SettingsDtos {

	private SettingsDtos() {
	}

	/** {@code password} is required for WPA and ignored for an open network. */
	public record WifiRequest(@NotBlank @Size(max = 32) String ssid, @Size(max = 63) String password,
			@NotNull WifiSecurity security) {
	}

	public record WifiResponse(String ssid, String password, WifiSecurity security) {

		static WifiResponse from(WifiNetwork wifi) {
			return new WifiResponse(wifi.ssid(), wifi.password(), wifi.security());
		}

	}

	/**
	 * @param publicBaseUrl where feedback links point; set with {@code APP_PUBLIC_BASE_URL}, shown
	 * here so admins can check it before printing posters
	 * @param wifi null when no camp Wi-Fi is set
	 */
	public record SettingsResponse(String publicBaseUrl, WifiResponse wifi) {
	}

}
