package com.bodymodifier;

import javax.annotation.Nonnull;
import net.runelite.api.AABB;
import net.runelite.api.Model;
import net.runelite.api.Node;

public class OpaqueModelWrapper implements Model
{
	private final Model delegate;

	public OpaqueModelWrapper(Model delegate)
	{
		this.delegate = delegate;
	}

	public Model getDelegate()
	{
		return delegate;
	}

	@Override
	public byte[] getFaceTransparencies()
	{
		// Returning null routes RuneLite GPU and 117 HD through the ultra-fast uploadTempModel
		// pipeline, eliminating the camera-projection Z-sorter overflow (ArrayIndexOutOfBoundsException)
		// while rendering opaque player geometry at full 144+ FPS.
		return null;
	}

	@Override
	public byte getTransparency()
	{
		return 0;
	}

	@Override
	public int getRenderMode()
	{
		return RENDERMODE_UNSORTED;
	}

	@Override
	public int[] getFaceColors1() { return delegate.getFaceColors1(); }

	@Override
	public int[] getFaceColors2() { return delegate.getFaceColors2(); }

	@Override
	public int[] getFaceColors3() { return delegate.getFaceColors3(); }

	@Override
	public short[] getUnlitFaceColors() { return delegate.getUnlitFaceColors(); }

	@Override
	public int getSceneId() { return delegate.getSceneId(); }

	@Override
	public void setSceneId(int sceneId) { delegate.setSceneId(sceneId); }

	@Override
	public int getBufferOffset() { return delegate.getBufferOffset(); }

	@Override
	public void setBufferOffset(int bufferOffset) { delegate.setBufferOffset(bufferOffset); }

	@Override
	public int getUvBufferOffset() { return delegate.getUvBufferOffset(); }

	@Override
	public void setUvBufferOffset(int bufferOffset) { delegate.setUvBufferOffset(bufferOffset); }

	@Override
	public int getBottomY() { return delegate.getBottomY(); }

	@Override
	public void calculateBoundsCylinder() { delegate.calculateBoundsCylinder(); }

	@Override
	public byte[] getFaceRenderPriorities() { return delegate.getFaceRenderPriorities(); }

	@Override
	public byte[] getFaceBias() { return delegate.getFaceBias(); }

	@Override
	public int getRadius() { return delegate.getRadius(); }

	@Override
	public int getDiameter() { return delegate.getDiameter(); }

	@Override
	public void calculateExtreme(int orientation) { delegate.calculateExtreme(orientation); }

	@Nonnull
	@Override
	public AABB getAABB(int orientation) { return delegate.getAABB(orientation); }

	@Override
	public int getXYZMag() { return delegate.getXYZMag(); }

	@Override
	public boolean useBoundingBox() { return delegate.useBoundingBox(); }

	@Override
	public int[] getVertexNormalsX() { return delegate.getVertexNormalsX(); }

	@Override
	public int[] getVertexNormalsY() { return delegate.getVertexNormalsY(); }

	@Override
	public int[] getVertexNormalsZ() { return delegate.getVertexNormalsZ(); }

	@Override
	public byte getOverrideAmount() { return delegate.getOverrideAmount(); }

	@Override
	public byte getOverrideHue() { return delegate.getOverrideHue(); }

	@Override
	public byte getOverrideSaturation() { return delegate.getOverrideSaturation(); }

	@Override
	public byte getOverrideLuminance() { return delegate.getOverrideLuminance(); }

	@Override
	public byte[] getTextureFaces() { return delegate.getTextureFaces(); }

	@Override
	public int[] getTexIndices1() { return delegate.getTexIndices1(); }

	@Override
	public int[] getTexIndices2() { return delegate.getTexIndices2(); }

	@Override
	public int[] getTexIndices3() { return delegate.getTexIndices3(); }

	@Override
	public Model getUnskewedModel() { return delegate.getUnskewedModel(); }

	@Override
	public void drawFrustum(int zero, int xRotate, int yRotate, int zRotate, int xCamera, int yCamera, int zCamera)
	{
		delegate.drawFrustum(zero, xRotate, yRotate, zRotate, xCamera, yCamera, zCamera);
	}

	@Override
	public void drawOrtho(int zero, int xRotate, int yRotate, int zRotate, int xCamera, int yCamera, int zCamera, int zoom)
	{
		delegate.drawOrtho(zero, xRotate, yRotate, zRotate, xCamera, yCamera, zCamera, zoom);
	}

	@Override
	public int getVerticesCount() { return delegate.getVerticesCount(); }

	@Override
	public float[] getVerticesX() { return delegate.getVerticesX(); }

	@Override
	public float[] getVerticesY() { return delegate.getVerticesY(); }

	@Override
	public float[] getVerticesZ() { return delegate.getVerticesZ(); }

	@Override
	public int getFaceCount() { return delegate.getFaceCount(); }

	@Override
	public int[] getFaceIndices1() { return delegate.getFaceIndices1(); }

	@Override
	public int[] getFaceIndices2() { return delegate.getFaceIndices2(); }

	@Override
	public int[] getFaceIndices3() { return delegate.getFaceIndices3(); }

	@Override
	public short[] getFaceTextures() { return delegate.getFaceTextures(); }

	@Override
	public Model rotateY90Ccw() { return delegate.rotateY90Ccw(); }

	@Override
	public Model rotateY180Ccw() { return delegate.rotateY180Ccw(); }

	@Override
	public Model rotateY270Ccw() { return delegate.rotateY270Ccw(); }

	@Override
	public Model translate(int x, int y, int z) { return delegate.translate(x, y, z); }

	@Override
	public Model scale(int x, int y, int z) { return delegate.scale(x, y, z); }

	@Override
	public Model getModel() { return delegate.getModel(); }

	@Override
	public int getModelHeight() { return delegate.getModelHeight(); }

	@Override
	public void setModelHeight(int modelHeight) { delegate.setModelHeight(modelHeight); }

	@Override
	public int getAnimationHeightOffset() { return delegate.getAnimationHeightOffset(); }

	@Override
	public Node getNext() { return delegate.getNext(); }

	@Override
	public Node getPrevious() { return delegate.getPrevious(); }

	@Override
	public long getHash() { return delegate.getHash(); }
}


