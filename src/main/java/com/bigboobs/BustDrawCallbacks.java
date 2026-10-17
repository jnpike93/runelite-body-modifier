package com.bigboobs;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.Model;
import net.runelite.api.Projection;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;
import net.runelite.api.Texture;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.hooks.DrawCallbacks;

@Slf4j
public class BustDrawCallbacks implements DrawCallbacks
{
	private final DrawCallbacks delegate;
	private final BigBoobsConfig config;
	private final BigBoobsPlugin plugin;

	public BustDrawCallbacks(DrawCallbacks delegate, BigBoobsConfig config, BigBoobsPlugin plugin)
	{
		this.delegate = delegate;
		this.config = config;
		this.plugin = plugin;
	}

	public DrawCallbacks getDelegate()
	{
		return delegate;
	}

	@Override
	public void draw(Projection projection, Scene scene, Renderable renderable, int orientation, int x, int y, int z, long hash)
	{
		delegate.draw(projection, scene, renderable, orientation, x, y, z, hash);
	}

	@Override
	public void drawScenePaint(Scene scene, SceneTilePaint paint, int plane, int tileX, int tileZ)
	{
		delegate.drawScenePaint(scene, paint, plane, tileX, tileZ);
	}

	@Override
	public void drawSceneTileModel(Scene scene, SceneTileModel model, int tileX, int tileZ)
	{
		delegate.drawSceneTileModel(scene, model, tileX, tileZ);
	}

	@Override
	public void draw(int overlayColor)
	{
		delegate.draw(overlayColor);
	}

	@Override
	public void drawScene(double cameraX, double cameraY, double cameraZ, double cameraPitch, double cameraYaw, int plane)
	{
		delegate.drawScene(cameraX, cameraY, cameraZ, cameraPitch, cameraYaw, plane);
	}

	@Override
	public void postDrawScene()
	{
		delegate.postDrawScene();
	}

	@Override
	public void animate(Texture texture, int diff)
	{
		delegate.animate(texture, diff);
	}

	@Override
	public void loadScene(Scene scene)
	{
		delegate.loadScene(scene);
	}

	@Override
	public void swapScene(Scene scene)
	{
		delegate.swapScene(scene);
	}

	@Override
	public boolean tileInFrustum(Scene scene, float pitchSin, float pitchCos, float yawSin, float yawCos, int cameraX, int cameraY, int cameraZ, int plane, int msx, int msy)
	{
		return delegate.tileInFrustum(scene, pitchSin, pitchCos, yawSin, yawCos, cameraX, cameraY, cameraZ, plane, msx, msy);
	}

	@Override
	public boolean zoneInFrustum(int zoneX, int zoneZ, int maxY, int minY)
	{
		return delegate.zoneInFrustum(zoneX, zoneZ, maxY, minY);
	}

	@Override
	public void loadScene(WorldView worldView, Scene scene)
	{
		delegate.loadScene(worldView, scene);
	}

	@Override
	public void despawnWorldView(WorldView worldView)
	{
		delegate.despawnWorldView(worldView);
	}

	@Override
	public void preSceneDraw(Scene scene, Projection entityProjection, float cameraX, float cameraY, float cameraZ, float cameraPitch, float cameraYaw, int minLevel, int level, int maxLevel, Set<Integer> hideRoofIds)
	{
		delegate.preSceneDraw(scene, entityProjection, cameraX, cameraY, cameraZ, cameraPitch, cameraYaw, minLevel, level, maxLevel, hideRoofIds);
	}

	@Override
	@Deprecated
	public void preSceneDraw(Scene scene, float cameraX, float cameraY, float cameraZ, float cameraPitch, float cameraYaw, int minLevel, int level, int maxLevel, Set<Integer> hideRoofIds)
	{
		delegate.preSceneDraw(scene, cameraX, cameraY, cameraZ, cameraPitch, cameraYaw, minLevel, level, maxLevel, hideRoofIds);
	}

	@Override
	public void postSceneDraw(Scene scene)
	{
		delegate.postSceneDraw(scene);
	}

	@Override
	public void drawPass(Projection entityProjection, Scene scene, int pass)
	{
		delegate.drawPass(entityProjection, scene, pass);
	}

	@Override
	public void drawZoneOpaque(Projection entityProjection, Scene scene, int zx, int zz)
	{
		delegate.drawZoneOpaque(entityProjection, scene, zx, zz);
	}

	@Override
	public void drawZoneAlpha(Projection entityProjection, Scene scene, int level, int zx, int zz)
	{
		delegate.drawZoneAlpha(entityProjection, scene, level, zx, zz);
	}

	@Override
	public void drawDynamic(int renderThreadId, Projection worldProjection, Scene scene, TileObject tileObject, Renderable r, Model m, int orient, int x, int y, int z)
	{
		if (config.enabled() && m != null && plugin.shouldModify(r))
		{
			BustDeformer.ScratchBuffer scratch = BustDeformer.apply(m, config);
			Model modelToDraw = (m.getFaceTransparencies() != null || m.getTransparency() != 0) ? new OpaqueModelWrapper(m) : m;
			try
			{
				delegate.drawDynamic(renderThreadId, worldProjection, scene, tileObject, r, modelToDraw, orient, x, y, z);
			}
			catch (Throwable t)
			{
				log.debug("DrawDynamic thread error: {}", t.getMessage());
			}
			finally
			{
				if (scratch != null)
				{
					BustDeformer.restore(m, scratch);
				}
			}
			return;
		}
		delegate.drawDynamic(renderThreadId, worldProjection, scene, tileObject, r, m, orient, x, y, z);
	}

	@Override
	public void drawDynamic(Projection worldProjection, Scene scene, TileObject tileObject, Renderable r, Model m, int orient, int x, int y, int z)
	{
		if (config.enabled() && m != null && plugin.shouldModify(r))
		{
			BustDeformer.ScratchBuffer scratch = BustDeformer.apply(m, config);
			Model modelToDraw = (m.getFaceTransparencies() != null || m.getTransparency() != 0) ? new OpaqueModelWrapper(m) : m;
			try
			{
				delegate.drawDynamic(worldProjection, scene, tileObject, r, modelToDraw, orient, x, y, z);
			}
			catch (Throwable t)
			{
				log.debug("DrawDynamic error: {}", t.getMessage());
			}
			finally
			{
				if (scratch != null)
				{
					BustDeformer.restore(m, scratch);
				}
			}
			return;
		}
		delegate.drawDynamic(worldProjection, scene, tileObject, r, m, orient, x, y, z);
	}

	@Override
	public void drawTemp(Projection worldProjection, Scene scene, GameObject gameObject, Model m, int orient, int x, int y, int z)
	{
		Renderable r = gameObject != null ? gameObject.getRenderable() : null;
		if (config.enabled() && m != null && plugin.shouldModify(r))
		{
			BustDeformer.ScratchBuffer scratch = BustDeformer.apply(m, config);
			Model modelToDraw = (m.getFaceTransparencies() != null || m.getTransparency() != 0) ? new OpaqueModelWrapper(m) : m;
			try
			{
				delegate.drawTemp(worldProjection, scene, gameObject, modelToDraw, orient, x, y, z);
			}
			catch (Throwable t)
			{
				log.debug("DrawTemp error: {}", t.getMessage());
			}
			finally
			{
				if (scratch != null)
				{
					BustDeformer.restore(m, scratch);
				}
			}
			return;
		}
		delegate.drawTemp(worldProjection, scene, gameObject, m, orient, x, y, z);
	}

	@Override
	public void invalidateZone(Scene scene, int zx, int zz)
	{
		delegate.invalidateZone(scene, zx, zz);
	}
}
