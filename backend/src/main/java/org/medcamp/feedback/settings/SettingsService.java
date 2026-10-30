package org.medcamp.feedback.settings;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.medcamp.feedback.common.ApiException;
import org.medcamp.feedback.settings.SettingsDtos.WifiRequest;
import org.medcamp.feedback.settings.WifiNetwork.WifiSecurity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SettingsService {

	static final String WIFI_SSID = "wifi.ssid";
	static final String WIFI_PASSWORD = "wifi.password";
	static final String WIFI_SECURITY = "wifi.security";

	/** Wi-Fi network names are limited to 32 bytes, not characters. */
	private static final int MAX_SSID_BYTES = 32;

	private final SettingRepository settings;

	public SettingsService(SettingRepository settings) {
		this.settings = settings;
	}

	@Transactional(readOnly = true)
	public Optional<WifiNetwork> wifi() {
		Optional<String> ssid = value(WIFI_SSID);
		if (ssid.isEmpty()) {
			return Optional.empty();
		}
		WifiSecurity security = value(WIFI_SECURITY).map(WifiSecurity::valueOf).orElse(WifiSecurity.WPA);
		return Optional.of(new WifiNetwork(ssid.get(), value(WIFI_PASSWORD).orElse(null), security));
	}

	public WifiNetwork saveWifi(WifiRequest request) {
		String ssid = request.ssid().trim();
		if (ssid.getBytes(StandardCharsets.UTF_8).length > MAX_SSID_BYTES) {
			throw ApiException.badRequest("WIFI_NAME_TOO_LONG", "The Wi-Fi name can be at most 32 bytes long.");
		}
		String password = null;
		if (request.security() == WifiSecurity.WPA) {
			password = request.password() == null ? "" : request.password();
			// WPA passphrases are 8 to 63 printable ASCII characters.
			if (password.length() < 8 || password.length() > 63 || !password.matches("[\\x20-\\x7E]+")) {
				throw ApiException.badRequest("WIFI_PASSWORD_INVALID",
						"A Wi-Fi password must be 8 to 63 characters, using English letters, numbers and symbols.");
			}
		}
		put(WIFI_SSID, ssid);
		put(WIFI_SECURITY, request.security().name());
		if (password == null) {
			settings.deleteById(WIFI_PASSWORD);
		}
		else {
			put(WIFI_PASSWORD, password);
		}
		return new WifiNetwork(ssid, password, request.security());
	}

	public void clearWifi() {
		settings.deleteAllById(List.of(WIFI_SSID, WIFI_PASSWORD, WIFI_SECURITY));
	}

	private Optional<String> value(String key) {
		return settings.findById(key).map(Setting::getValue);
	}

	private void put(String key, String value) {
		settings.findById(key).ifPresentOrElse(setting -> setting.setValue(value),
				() -> settings.save(new Setting(key, value)));
	}

}
