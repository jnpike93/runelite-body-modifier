package com.bodymodifier;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.BufferProvider;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Interactive on-screen overlay rendering an eyedropper preview box around the cursor.
 * Uses safe in-canvas BufferProvider pixel sampling without java.awt.Robot or OS screen-scraping.
 */
public class ColorPickerOverlay extends Overlay
{
	private final Client client;
	private final BodyModifierConfig config;

	@Inject
	public ColorPickerOverlay(Client client, BodyModifierConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(PRIORITY_HIGH);
	}

	/**
	 * Samples the color directly from the client canvas buffer at the given canvas coordinates.
	 * Returns null if the position is out of bounds or buffer is unavailable.
	 */
	public Color getSampledColor(Point mouseCanvasPos)
	{
		if (mouseCanvasPos == null || client == null)
		{
			return null;
		}

		BufferProvider bp = client.getBufferProvider();
		if (bp == null)
		{
			return null;
		}

		int[] pixels = bp.getPixels();
		int width = bp.getWidth();
		int height = bp.getHeight();
		int x = mouseCanvasPos.getX();
		int y = mouseCanvasPos.getY();

		if (pixels != null && x >= 0 && x < width && y >= 0 && y < height)
		{
			int rgb = pixels[y * width + x];
			return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
		}

		return null;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.eyedropperMode())
		{
			return null;
		}

		Point mouseCanvasPos = client.getMouseCanvasPosition();
		if (mouseCanvasPos == null)
		{
			return null;
		}

		Color hoveredColor = getSampledColor(mouseCanvasPos);
		if (hoveredColor == null)
		{
			return null;
		}

		int x = mouseCanvasPos.getX() + 18;
		int y = mouseCanvasPos.getY() + 18;

		// Draw preview box
		graphics.setColor(Color.BLACK);
		graphics.fillRect(x - 1, y - 1, 22, 22);
		graphics.setColor(hoveredColor);
		graphics.fillRect(x, y, 20, 20);

		// Draw text
		String hex = String.format("#%02X%02X%02X", hoveredColor.getRed(), hoveredColor.getGreen(), hoveredColor.getBlue());
		graphics.setColor(Color.BLACK);
		graphics.drawString("Click to sample: " + hex, x + 26, y + 16);
		graphics.setColor(Color.WHITE);
		graphics.drawString("Click to sample: " + hex, x + 25, y + 15);

		return null;
	}
}
