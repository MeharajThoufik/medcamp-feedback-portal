package org.medcamp.feedback.qr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeWriterTests {

	private final QrCodeWriter writer = new QrCodeWriter();

	@Test
	void writesAScannablePng() throws Exception {
		byte[] png = writer.png("http://192.168.137.1:8080/f/7Kq2xPm4", 400);

		assertThat(QrTestDecoder.decode(png)).isEqualTo("http://192.168.137.1:8080/f/7Kq2xPm4");
		assertThat(QrTestDecoder.width(png)).isEqualTo(400);
	}

	@Test
	void keepsTheSizeWithinLimits() throws Exception {
		assertThat(QrTestDecoder.width(writer.png("x", 10))).isEqualTo(QrCodeWriter.MIN_SIZE);
		assertThat(QrTestDecoder.width(writer.png("x", 50_000))).isEqualTo(QrCodeWriter.MAX_SIZE);
	}

	@Test
	void encodesNonEnglishTextAsUtf8() throws Exception {
		assertThat(QrTestDecoder.decode(writer.png("शिविर वाई-फ़ाई", 300))).isEqualTo("शिविर वाई-फ़ाई");
	}

}
