package org.medcamp.feedback.qr;

import java.nio.charset.StandardCharsets;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** HTTP responses for QR images. */
public final class QrResponses {

	private QrResponses() {
	}

	/**
	 * Never cached: the image changes when a camp's link is regenerated or the Wi-Fi settings
	 * change. With {@code download}, the browser saves the file as {@code fileName}.
	 */
	public static ResponseEntity<byte[]> png(byte[] image, boolean download, String fileName) {
		ResponseEntity.BodyBuilder response = ResponseEntity.ok()
			.contentType(MediaType.IMAGE_PNG)
			.cacheControl(CacheControl.noStore());
		if (download) {
			response.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString());
		}
		return response.body(image);
	}

}
