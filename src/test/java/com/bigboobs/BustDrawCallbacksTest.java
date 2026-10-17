package com.bigboobs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;
import net.runelite.api.GameObject;
import net.runelite.api.Model;
import net.runelite.api.Player;
import net.runelite.api.Projection;
import net.runelite.api.Scene;
import net.runelite.api.TileObject;
import net.runelite.api.hooks.DrawCallbacks;
import org.junit.Test;

public class BustDrawCallbacksTest
{
	@Test
	public void testDrawDynamicInterceptsAndRestores()
	{
		BigBoobsPlugin plugin = mock(BigBoobsPlugin.class);
		BigBoobsConfig config = mock(BigBoobsConfig.class);
		when(config.enabled()).thenReturn(true);
		when(config.breastSize()).thenReturn(90);
		when(config.sagAmount()).thenReturn(30);
		when(config.cleavage()).thenReturn(15);
		when(config.verticalOffset()).thenReturn(0);
		when(config.influenceRadius()).thenReturn(40);
		when(config.invertZ()).thenReturn(false);

		// Realistic player bounds in OSRS (height 200, front is negative Z)
		float[] vx = new float[]{15.0f, 0.0f, 0.0f};
		float[] vy = new float[]{-138.0f, 0.0f, -200.0f};
		float[] vz = new float[]{-15.0f, 0.0f, 0.0f};

		Model model = mock(Model.class);
		when(model.getVerticesX()).thenReturn(vx);
		when(model.getVerticesY()).thenReturn(vy);
		when(model.getVerticesZ()).thenReturn(vz);
		when(model.getVerticesCount()).thenReturn(3);

		Player player = mock(Player.class);
		when(plugin.shouldModify(player)).thenReturn(true);

		Projection projection = mock(Projection.class);
		Scene scene = mock(Scene.class);
		TileObject tileObject = mock(TileObject.class);

		AtomicBoolean delegateExecutedWithDeformedVertices = new AtomicBoolean(false);

		DrawCallbacks realCallbacks = new DrawCallbacks()
		{
			@Override
			public void draw(int overlayColor)
			{
			}

			@Override
			public void swapScene(Scene scene)
			{
			}

			@Override
			public void drawDynamic(int renderThreadId, Projection proj, Scene scn, TileObject to, net.runelite.api.Renderable r, Model m, int orient, int x, int y, int z)
			{
				if (vz[0] < -15.0f - 50.0f)
				{
					delegateExecutedWithDeformedVertices.set(true);
				}
			}
		};

		BustDrawCallbacks callbacks = new BustDrawCallbacks(realCallbacks, config, plugin);
		callbacks.drawDynamic(0, projection, scene, tileObject, player, model, 0, 100, 200, 300);

		assertTrue("Delegate must receive deformed vertices during GPU call", delegateExecutedWithDeformedVertices.get());
		assertEquals("Vertices must be completely restored after draw call", -15.0f, vz[0], 0.001f);
	}

	@Test
	public void testDrawTempInterceptsAndRestores()
	{
		BigBoobsPlugin plugin = mock(BigBoobsPlugin.class);
		BigBoobsConfig config = mock(BigBoobsConfig.class);
		when(config.enabled()).thenReturn(true);
		when(config.breastSize()).thenReturn(90);
		when(config.sagAmount()).thenReturn(30);
		when(config.cleavage()).thenReturn(15);
		when(config.verticalOffset()).thenReturn(0);
		when(config.influenceRadius()).thenReturn(40);
		when(config.invertZ()).thenReturn(false);

		float[] vx = new float[]{15.0f, 0.0f, 0.0f};
		float[] vy = new float[]{-138.0f, 0.0f, -200.0f};
		float[] vz = new float[]{-15.0f, 0.0f, 0.0f};

		Model model = mock(Model.class);
		when(model.getVerticesX()).thenReturn(vx);
		when(model.getVerticesY()).thenReturn(vy);
		when(model.getVerticesZ()).thenReturn(vz);
		when(model.getVerticesCount()).thenReturn(3);

		Player player = mock(Player.class);
		when(plugin.shouldModify(player)).thenReturn(true);

		GameObject gameObject = mock(GameObject.class);
		when(gameObject.getRenderable()).thenReturn(player);

		Projection projection = mock(Projection.class);
		Scene scene = mock(Scene.class);

		AtomicBoolean delegateExecutedWithDeformedVertices = new AtomicBoolean(false);

		DrawCallbacks realCallbacks = new DrawCallbacks()
		{
			@Override
			public void draw(int overlayColor)
			{
			}

			@Override
			public void swapScene(Scene scene)
			{
			}

			@Override
			public void drawTemp(Projection proj, Scene scn, GameObject go, Model m, int orient, int x, int y, int z)
			{
				if (vz[0] < -15.0f - 50.0f)
				{
					delegateExecutedWithDeformedVertices.set(true);
				}
			}
		};

		BustDrawCallbacks callbacks = new BustDrawCallbacks(realCallbacks, config, plugin);
		callbacks.drawTemp(projection, scene, gameObject, model, 0, 100, 200, 300);

		assertTrue("Delegate must receive deformed vertices during drawTemp GPU call", delegateExecutedWithDeformedVertices.get());
		assertEquals("Vertices must be completely restored after drawTemp call", -15.0f, vz[0], 0.001f);
	}
}

