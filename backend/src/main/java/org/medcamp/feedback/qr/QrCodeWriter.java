package org.medcamp.feedback.qr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

/** Draws QR codes as black-on-white PNG images. */
@Component
public class QrCodeWriter {

	public static final int MIN_SIZE = 128;
	public static final int MAX_SIZE = 1024;

	private static final int BLACK = 0xFF000000;
	private static final int WHITE = 0xFFFFFFFF;

	/**
	 * Medium error correction still scans when a printed poster is creased or a little dirty. The
	 * 4-module quiet zone is what the QR standard asks for, so cameras find the code reliably.
	 */
	private static final Map<EncodeHintType, Object> HINTS = Map.of(EncodeHintType.ERROR_CORRECTION,
			ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 4, EncodeHintType.CHARACTER_SET, "UTF-8");

	/** @param size width and height in pixels, clamped to {@value #MIN_SIZE}–{@value #MAX_SIZE} */
	public byte[] png(String content, int size) {
		int pixels = Math.clamp(size, MIN_SIZE, MAX_SIZE);
		BitMatrix matrix;
		try {
			matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, pixels, pixels, HINTS);
		}
		catch (WriterException ex) {
			throw new IllegalArgumentException("Cannot encode this text as a QR code", ex);
		}
		BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		for (int y = 0; y < matrix.getHeight(); y++) {
			for (int x = 0; x < matrix.getWidth(); x++) {
				image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "png", out);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return out.toByteArray();
	}

}
