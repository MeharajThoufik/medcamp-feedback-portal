package org.medcamp.feedback.qr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

/** Reads a QR PNG back, the way a phone camera would, so tests can check what it encodes. */
public final class QrTestDecoder {

	private QrTestDecoder() {
	}

	public static String decode(byte[] png) throws Exception {
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
		int width = image.getWidth();
		int height = image.getHeight();
		int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
		RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
		return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(source))).getText();
	}

	public static int width(byte[] png) throws Exception {
		return ImageIO.read(new ByteArrayInputStream(png)).getWidth();
	}

}
