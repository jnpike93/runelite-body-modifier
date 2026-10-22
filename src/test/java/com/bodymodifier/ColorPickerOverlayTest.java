package com.bodymodifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import net.runelite.api.BufferProvider;
import net.runelite.api.Client;
import net.runelite.api.Point;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests verifying safe in-canvas pixel sampling without java.awt.Robot.
 */
public class ColorPickerOverlayTest
{
	private Client client;
	private BodyModifierConfig config;
	private BufferProvider bufferProvider;
	private ColorPickerOverlay overlay;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		config = mock(BodyModifierConfig.class);
		bufferProvider = mock(BufferProvider.class);

		when(client.getBufferProvider()).thenReturn(bufferProvider);
		overlay = new ColorPickerOverlay(client, config);
	}

	@Test
	public void testSampleColorFromCanvasBuffer()
	{
		int width = 100;
		int height = 100;
		int[] pixels = new int[width * height];

		// Set pixel at (10, 20) to RGB(255, 128, 64) -> 0x00FF8040
		int targetX = 10;
		int targetY = 20;
		int expectedR = 255;
		int expectedG = 128;
		int expectedB = 64;
		pixels[targetY * width + targetX] = (expectedR << 16) | (expectedG << 8) | expectedB;

		when(bufferProvider.getWidth()).thenReturn(width);
		when(bufferProvider.getHeight()).thenReturn(height);
		when(bufferProvider.getPixels()).thenReturn(pixels);

		Color sampled = overlay.getSampledColor(new Point(targetX, targetY));
		assertNotNull("Sampled color should not be null", sampled);
		assertEquals(expectedR, sampled.getRed());
		assertEquals(expectedG, sampled.getGreen());
		assertEquals(expectedB, sampled.getBlue());
	}

	@Test
	public void testOutOfBoundsReturnsNull()
	{
		int width = 100;
		int height = 100;
		int[] pixels = new int[width * height];

		when(bufferProvider.getWidth()).thenReturn(width);
		when(bufferProvider.getHeight()).thenReturn(height);
		when(bufferProvider.getPixels()).thenReturn(pixels);

		// Negative coordinates
		assertNull(overlay.getSampledColor(new Point(-1, 50)));
		assertNull(overlay.getSampledColor(new Point(50, -1)));

		// Beyond width/height
		assertNull(overlay.getSampledColor(new Point(100, 50)));
		assertNull(overlay.getSampledColor(new Point(50, 100)));
		assertNull(overlay.getSampledColor(new Point(999, 999)));

		// Null mouse position
		assertNull(overlay.getSampledColor(null));
	}

	@Test
	public void testNullBufferProviderReturnsNull()
	{
		when(client.getBufferProvider()).thenReturn(null);
		assertNull(overlay.getSampledColor(new Point(10, 10)));

		when(client.getBufferProvider()).thenReturn(bufferProvider);
		when(bufferProvider.getPixels()).thenReturn(null);
		assertNull(overlay.getSampledColor(new Point(10, 10)));
	}
}

