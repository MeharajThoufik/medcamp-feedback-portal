package org.medcamp.feedback.settings;

import jakarta.validation.Valid;
import org.medcamp.feedback.common.ApiException;
import org.medcamp.feedback.qr.FeedbackLinks;
import org.medcamp.feedback.qr.QrCodeWriter;
import org.medcamp.feedback.qr.QrResponses;
import org.medcamp.feedback.settings.SettingsDtos.SettingsResponse;
import org.medcamp.feedback.settings.SettingsDtos.WifiRequest;
import org.medcamp.feedback.settings.SettingsDtos.WifiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

	private final SettingsService settingsService;
	private final FeedbackLinks feedbackLinks;
	private final QrCodeWriter qrCodeWriter;

	public SettingsController(SettingsService settingsService, FeedbackLinks feedbackLinks,
			QrCodeWriter qrCodeWriter) {
		this.settingsService = settingsService;
		this.feedbackLinks = feedbackLinks;
		this.qrCodeWriter = qrCodeWriter;
	}

	@GetMapping
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public SettingsResponse get() {
		return new SettingsResponse(feedbackLinks.baseUrl(),
				settingsService.wifi().map(WifiResponse::from).orElse(null));
	}

	@PutMapping("/wifi")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public WifiResponse saveWifi(@Valid @RequestBody WifiRequest request) {
		return WifiResponse.from(settingsService.saveWifi(request));
	}

	@DeleteMapping("/wifi")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	public void clearWifi() {
		settingsService.clearWifi();
	}

	/** Organizers print it on their camp posters. */
	@GetMapping("/wifi/qr.png")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANIZER')")
	public ResponseEntity<byte[]> wifiQr(@RequestParam(defaultValue = "512") int size,
			@RequestParam(defaultValue = "false") boolean download) {
		WifiNetwork wifi = settingsService.wifi()
			.orElseThrow(() -> ApiException.notFound("WIFI_NOT_SET", "No camp Wi-Fi has been set."));
		return QrResponses.png(qrCodeWriter.png(wifi.qrContent(), size), download, "camp-wifi-qr.png");
	}

}
