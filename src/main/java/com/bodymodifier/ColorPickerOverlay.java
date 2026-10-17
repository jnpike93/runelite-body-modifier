package com.bodymodifier;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class ColorPickerOverlay extends Overlay
{
	private final Client client;
	private final BodyModifierConfig config;
	private Robot robot;

	@Inject
	public ColorPickerOverlay(Client client, BodyModifierConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(OverlayPriority.HIGH);
		try
		{
			this.robot = new Robot();
		}
		catch (Exception ignored)
		{
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.eyedropperMode() || robot == null)
		{
			return null;
		}

		net.runelite.api.Point mouseCanvasPos = client.getMouseCanvasPosition();
		if (mouseCanvasPos == null)
		{
			return null;
		}

		PointerInfo pointerInfo = MouseInfo.getPointerInfo();
		if (pointerInfo == null)
		{
			return null;
		}

		Point screenPos = pointerInfo.getLocation();
		Color hoveredColor = robot.getPixelColor(screenPos.x, screenPos.y);

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


