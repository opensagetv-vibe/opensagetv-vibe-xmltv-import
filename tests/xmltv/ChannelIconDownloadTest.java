package xmltv;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import javax.imageio.ImageIO;

/** Regression coverage for legacy configuration and safe channel-logo output. */
public final class ChannelIconDownloadTest {
  public static void main(String[] args) throws Exception {
    if (args.length == 2) {
      File destination = new File(args[1]);
      assertTrue(XMLTVImportPlugin.downloadAndNormalizeChannelIcon(
          args[0], destination, 256, 256), "channel icon probe failed");
      BufferedImage output = ImageIO.read(destination);
      assertTrue(output != null, "channel icon probe output did not decode");
      assertPng(destination);
      System.out.println("[PASS] channel icon probe wrote " + destination
          + " as " + output.getWidth() + "x" + output.getHeight() + " PNG");
      return;
    }
    if (args.length != 0) {
      throw new IllegalArgumentException("usage: ChannelIconDownloadTest [URL output.png]");
    }
    File work = Files.createTempDirectory("xmltv-channel-icon-").toFile();
    try {
      verifyLegacySetting();
      verifyDownloadAndResize(work);
      verifySmallImageIsNotUpscaled(work);
      verifyExistingJpegIsNormalized(work);
      verifyMalformedImageFailsSafely(work);
      System.out.println("[PASS] XMLTV channel icons download and normalize safely");
    } finally {
      deleteRecursively(work);
    }
  }

  private static void verifyLegacySetting() {
    Properties properties = new Properties();
    properties.setProperty("xmltv.channel.IconDownload", "true");
    assertTrue(XMLTVImportPlugin.isChannelIconDownloadEnabled(properties),
        "legacy xmltv.channel.IconDownload was ignored");
    properties.setProperty("sagetv.channel.IconDownload", "false");
    assertTrue(!XMLTVImportPlugin.isChannelIconDownloadEnabled(properties),
        "modern setting must take precedence when both names are present");
  }

  private static void verifyDownloadAndResize(File work) throws Exception {
    File source = new File(work, "remote-logo-without-extension");
    BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(new Color(15, 120, 220, 180));
    graphics.fillRect(80, 60, 640, 480);
    graphics.dispose();
    ImageIO.write(image, "png", source);

    File destination = new File(work, "missing/ChannelLogos/Test.png");
    assertTrue(XMLTVImportPlugin.downloadAndNormalizeChannelIcon(
        source.toURI().toURL().toString(), destination, 256, 256),
        "valid icon download failed");
    BufferedImage output = ImageIO.read(destination);
    assertDimensions(output, 256, 192, "large icon resize");
    assertPng(destination);
  }

  private static void verifySmallImageIsNotUpscaled(File work) throws Exception {
    File source = new File(work, "small-source");
    BufferedImage image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_RGB);
    ImageIO.write(image, "jpg", source);
    File destination = new File(work, "small.png");
    assertTrue(XMLTVImportPlugin.downloadAndNormalizeChannelIcon(
        source.toURI().toURL().toString(), destination, 256, 256),
        "small icon conversion failed");
    assertDimensions(ImageIO.read(destination), 64, 32, "small icon");
    assertPng(destination);
  }

  private static void verifyExistingJpegIsNormalized(File work) throws Exception {
    File destination = new File(work, "existing.png");
    BufferedImage image = new BufferedImage(600, 200, BufferedImage.TYPE_INT_RGB);
    ImageIO.write(image, "jpg", destination);
    assertTrue(XMLTVImportPlugin.downloadAndNormalizeChannelIcon(
        "file:/does-not-need-to-exist", destination, 256, 256),
        "existing icon normalization failed");
    assertDimensions(ImageIO.read(destination), 256, 85, "existing icon");
    assertPng(destination);
  }

  private static void verifyMalformedImageFailsSafely(File work) throws Exception {
    File source = new File(work, "malformed");
    Files.write(source.toPath(), "not an image".getBytes(StandardCharsets.UTF_8));
    File destination = new File(work, "malformed.png");
    assertTrue(!XMLTVImportPlugin.downloadAndNormalizeChannelIcon(
        source.toURI().toURL().toString(), destination, 256, 256),
        "malformed icon unexpectedly succeeded");
    assertTrue(!destination.exists(), "malformed icon left an output file");
  }

  private static void assertPng(File file) throws Exception {
    byte[] signature = new byte[] {(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    try (FileInputStream input = new FileInputStream(file)) {
      for (byte expected : signature) {
        if (input.read() != (expected & 0xff)) {
          throw new AssertionError(file + " is not encoded as PNG");
        }
      }
    }
  }

  private static void assertDimensions(BufferedImage image, int width, int height,
      String description) {
    assertTrue(image != null, description + " did not decode");
    assertTrue(image.getWidth() == width && image.getHeight() == height,
        description + " expected " + width + "x" + height + " but got "
            + image.getWidth() + "x" + image.getHeight());
  }

  private static void assertTrue(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static void deleteRecursively(File file) {
    if (file == null || !file.exists()) return;
    File[] children = file.listFiles();
    if (children != null) {
      for (File child : children) deleteRecursively(child);
    }
    if (!file.delete()) file.deleteOnExit();
  }
}
