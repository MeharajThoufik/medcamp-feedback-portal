package org.medcamp.feedback.settings;

/**
 * The camp hotspot printed on posters, so attendees can join it by scanning instead of typing.
 *
 * @param password null for an open network
 */
public record WifiNetwork(String ssid, String password, WifiSecurity security) {

	public enum WifiSecurity {

		/** WPA, WPA2 or WPA3 with a password; phones treat them the same in a Wi-Fi QR code. */
		WPA,

		/** No password. */
		NONE

	}

	/**
	 * The text phone cameras understand as "join this network", for example
	 * {@code WIFI:T:WPA;S:CampWiFi;P:secret123;;}. Backslash, semicolon, comma, colon and double quote
	 * must be escaped with a backslash.
	 */
	public String qrContent() {
		StringBuilder content = new StringBuilder("WIFI:");
		content.append("T:").append(security == WifiSecurity.WPA ? "WPA" : "nopass").append(';');
		content.append("S:").append(escape(ssid)).append(';');
		if (security == WifiSecurity.WPA) {
			content.append("P:").append(escape(password)).append(';');
		}
		return content.append(';').toString();
	}

	private static String escape(String value) {
		return value.replaceAll("([\\\\;,:\"])", "\\\\$1");
	}

}
