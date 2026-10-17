package com.bodymodifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import net.runelite.api.Model;
import org.junit.Test;

/**
 * Unit tests verifying mesh vertex deformation, scratch restoration, and depth biasing math.
 */
public class BustDeformerTest
{
	@Test
	public void testDeformationAndRestoration()
	{
		BodyModifierConfig config = mock(BodyModifierConfig.class);
		when(config.enabled()).thenReturn(true);
		when(config.breastSize()).thenReturn(85);
		when(config.sagAmount()).thenReturn(28);
		when(config.cleavage()).thenReturn(15);
		when(config.verticalOffset()).thenReturn(0);
		when(config.influenceRadius()).thenReturn(42);
		when(config.invertZ()).thenReturn(false);

		// Realistic player humanoid bounds in OSRS (Front is negative Z, Back is positive Z):
		// Vertex 0: Right breast apex on front of chest (15, -138, -15)
		// Vertex 1: At the back of the player (0, -138, +15)
		// Vertex 2: Feet ground level (0, 0, 0)
		// Vertex 3: Top of head (0, -200, 0)
		float[] vx = new float[]{15.0f, 0.0f, 0.0f, 0.0f};
		float[] vy = new float[]{-138.0f, -138.0f, 0.0f, -200.0f};
		float[] vz = new float[]{-15.0f, 15.0f, 0.0f, 0.0f};

		Model model = mock(Model.class);
		when(model.getVerticesX()).thenReturn(vx);
		when(model.getVerticesY()).thenReturn(vy);
		when(model.getVerticesZ()).thenReturn(vz);
		when(model.getVerticesCount()).thenReturn(4);

		// 1. Apply deformation
		BustDeformer.ScratchBuffer scratch = BustDeformer.apply(model, config);
		assertNotNull("Scratch buffer should not be null", scratch);
		assertTrue("At least 1 vertex should be deformed", scratch.count >= 1);

		// Front chest apex vertex (Vertex 0) should be pushed forward into negative Z
		assertTrue("Z should be pushed forward into negative Z by breastSize", vz[0] < -15.0f - 70.0f);
		// Sag should pull Y downward (+Y is down in OSRS)
		assertTrue("Y should drop downward due to gravity sag", vy[0] > -138.0f);
		// Lateral spread should push positive X outward
		assertTrue("X should be pushed slightly outward", vx[0] > 15.0f);

		// Back vertex (Vertex 1) and feet/head vertices should remain untouched
		assertEquals(15.0f, vz[1], 0.001f);
		assertEquals(0.0f, vz[2], 0.001f);
		assertEquals(0.0f, vz[3], 0.001f);

		// 2. Restore vertices
		BustDeformer.restore(model, scratch);

		// Check that apex vertex was completely restored to original coordinates
		assertEquals(15.0f, vx[0], 0.001f);
		assertEquals(-138.0f, vy[0], 0.001f);
		assertEquals(-15.0f, vz[0], 0.001f);
	}

	@Test
	public void testColorOverrideAndRestoration()
	{
		BodyModifierConfig config = mock(BodyModifierConfig.class);
		when(config.enabled()).thenReturn(true);
		when(config.breastSize()).thenReturn(85);
		when(config.sagAmount()).thenReturn(28);
		when(config.cleavage()).thenReturn(15);
		when(config.verticalOffset()).thenReturn(0);
		when(config.influenceRadius()).thenReturn(42);
		when(config.invertZ()).thenReturn(false);
		when(config.enableColorTint()).thenReturn(true);
		when(config.tintColor()).thenReturn(new Color(255, 0, 0));

		float[] vx = new float[]{15.0f, 0.0f, 0.0f, 0.0f};
		float[] vy = new float[]{-138.0f, -138.0f, 0.0f, -200.0f};
		float[] vz = new float[]{-15.0f, 15.0f, 0.0f, 0.0f};

		int[] ind1 = new int[]{0};
		int[] ind2 = new int[]{1};
		int[] ind3 = new int[]{2};

		int[] col1 = new int[]{100};
		int[] col2 = new int[]{100};
		int[] col3 = new int[]{100};

		Model model = mock(Model.class);
		when(model.getVerticesX()).thenReturn(vx);
		when(model.getVerticesY()).thenReturn(vy);
		when(model.getVerticesZ()).thenReturn(vz);
		when(model.getVerticesCount()).thenReturn(4);
		when(model.getFaceCount()).thenReturn(1);
		when(model.getFaceIndices1()).thenReturn(ind1);
		when(model.getFaceIndices2()).thenReturn(ind2);
		when(model.getFaceIndices3()).thenReturn(ind3);
		when(model.getFaceColors1()).thenReturn(col1);
		when(model.getFaceColors2()).thenReturn(col2);
		when(model.getFaceColors3()).thenReturn(col3);

		BustDeformer.ScratchBuffer scratch = BustDeformer.apply(model, config);
		assertNotNull(scratch);
		assertTrue(scratch.faceCount >= 1);
		assertTrue(col1[0] != 100);

		BustDeformer.restore(model, scratch);
		assertEquals(100, col1[0]);
		assertEquals(100, col2[0]);
		assertEquals(100, col3[0]);
	}

	@Test
	public void testDividingLineColoringAndRestoration()
	{
		BodyModifierConfig config = mock(BodyModifierConfig.class);
		when(config.enabled()).thenReturn(true);
		when(config.breastSize()).thenReturn(85);
		when(config.sagAmount()).thenReturn(28);
		when(config.cleavage()).thenReturn(15);
		when(config.verticalOffset()).thenReturn(0);
		when(config.influenceRadius()).thenReturn(42);
		when(config.invertZ()).thenReturn(false);
		when(config.enableColorTint()).thenReturn(true);
		when(config.tintColor()).thenReturn(new Color(255, 0, 0)); // Red tint
		when(config.enableDividingLine()).thenReturn(true);
		when(config.dividingLineColor()).thenReturn(new Color(0, 0, 255)); // Blue dividing line
		when(config.dividingLineWidth()).thenReturn(1);
		when(config.dividingLineAlwaysOnTop()).thenReturn(true);

		// Triangle 0: Centered on midline (faceCenterX = 0) -> should be colored with dividingLineColor (blue)
		// Triangle 1: Lateral breast apex (faceCenterX = 15) -> should be colored with tintColor (red)
		float[] vx = new float[]{0.0f, -2.0f, 2.0f, 15.0f, 16.0f, 17.0f, 0.0f, 0.0f};
		float[] vy = new float[]{-138.0f, -138.0f, -138.0f, -138.0f, -138.0f, -138.0f, 0.0f, -200.0f};
		float[] vz = new float[]{-15.0f, -15.0f, -15.0f, -15.0f, -15.0f, -15.0f, 0.0f, 0.0f};

		int[] ind1 = new int[]{0, 3};
		int[] ind2 = new int[]{1, 4};
		int[] ind3 = new int[]{2, 5};

		int[] col1 = new int[]{100, 200};
		int[] col2 = new int[]{100, 200};
		int[] col3 = new int[]{100, 200};

		Model model = mock(Model.class);
		when(model.getVerticesX()).thenReturn(vx);
		when(model.getVerticesY()).thenReturn(vy);
		when(model.getVerticesZ()).thenReturn(vz);
		when(model.getVerticesCount()).thenReturn(8);
		when(model.getFaceCount()).thenReturn(2);
		when(model.getFaceIndices1()).thenReturn(ind1);
		when(model.getFaceIndices2()).thenReturn(ind2);
		when(model.getFaceIndices3()).thenReturn(ind3);
		when(model.getFaceColors1()).thenReturn(col1);
		when(model.getFaceColors2()).thenReturn(col2);
		when(model.getFaceColors3()).thenReturn(col3);

		BustDeformer.ScratchBuffer scratch = BustDeformer.apply(model, config);
		assertNotNull(scratch);
		assertEquals(2, scratch.faceCount);

		// Dividing line face (face 0) and side face (face 1) should have different colors
		assertTrue(col1[0] != 100);
		assertTrue(col1[1] != 200);
		assertTrue("Dividing line face should have dividingLineColor distinct from side tint", col1[0] != col1[1]);

		// Verify restoration
		BustDeformer.restore(model, scratch);
		assertEquals(100, col1[0]);
		assertEquals(200, col1[1]);
	}
}

