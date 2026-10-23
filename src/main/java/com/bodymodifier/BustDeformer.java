package com.bodymodifier;

import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;

/**
 * Applies procedural geometric deformation and shader-safe depth biasing to character models.
 */
@Slf4j
public class BustDeformer
{
	// ThreadLocal scratch buffer to prevent GC allocations and guarantee thread safety
	private static final ThreadLocal<ScratchBuffer> THREAD_SCRATCH = ThreadLocal.withInitial(ScratchBuffer::new);

	private static final AtomicLong LAST_DIAGNOSTIC_LOG_TIME = new AtomicLong(0);

	public static class ScratchBuffer
	{
		private static final int INITIAL_CAPACITY = 2048;
		private static final int MAX_RETAINED_CAPACITY = 8192;

		public int count = 0;
		public int[] indices = new int[INITIAL_CAPACITY];
		public float[] origX = new float[INITIAL_CAPACITY];
		public float[] origY = new float[INITIAL_CAPACITY];
		public float[] origZ = new float[INITIAL_CAPACITY];

		public int faceCount = 0;
		public int[] faceIndices = new int[INITIAL_CAPACITY];
		public int[] origFaceColors1 = new int[INITIAL_CAPACITY];
		public int[] origFaceColors2 = new int[INITIAL_CAPACITY];
		public int[] origFaceColors3 = new int[INITIAL_CAPACITY];

		public boolean[] deformedVertexMask = new boolean[INITIAL_CAPACITY];

		public void ensureCapacity(int minCapacity)
		{
			if (minCapacity > indices.length)
			{
				int newCap = Math.max(indices.length * 2, minCapacity);
				int[] newIndices = new int[newCap];
				float[] newX = new float[newCap];
				float[] newY = new float[newCap];
				float[] newZ = new float[newCap];

				System.arraycopy(indices, 0, newIndices, 0, count);
				System.arraycopy(origX, 0, newX, 0, count);
				System.arraycopy(origY, 0, newY, 0, count);
				System.arraycopy(origZ, 0, newZ, 0, count);

				indices = newIndices;
				origX = newX;
				origY = newY;
				origZ = newZ;
			}
		}

		public void ensureFaceCapacity(int minCapacity)
		{
			if (minCapacity > faceIndices.length)
			{
				int newCap = Math.max(faceIndices.length * 2, minCapacity);
				int[] newIndices = new int[newCap];
				int[] newC1 = new int[newCap];
				int[] newC2 = new int[newCap];
				int[] newC3 = new int[newCap];

				System.arraycopy(faceIndices, 0, newIndices, 0, faceCount);
				System.arraycopy(origFaceColors1, 0, newC1, 0, faceCount);
				System.arraycopy(origFaceColors2, 0, newC2, 0, faceCount);
				System.arraycopy(origFaceColors3, 0, newC3, 0, faceCount);

				faceIndices = newIndices;
				origFaceColors1 = newC1;
				origFaceColors2 = newC2;
				origFaceColors3 = newC3;
			}
		}

		public boolean[] acquireVertexMask(int vertexCount)
		{
			if (vertexCount > deformedVertexMask.length)
			{
				int newCap = Math.max(deformedVertexMask.length * 2, vertexCount);
				deformedVertexMask = new boolean[newCap];
			}
			return deformedVertexMask;
		}

		public void trimIfExcessive()
		{
			if (indices.length > MAX_RETAINED_CAPACITY)
			{
				indices = new int[INITIAL_CAPACITY];
				origX = new float[INITIAL_CAPACITY];
				origY = new float[INITIAL_CAPACITY];
				origZ = new float[INITIAL_CAPACITY];
			}
			if (faceIndices.length > MAX_RETAINED_CAPACITY)
			{
				faceIndices = new int[INITIAL_CAPACITY];
				origFaceColors1 = new int[INITIAL_CAPACITY];
				origFaceColors2 = new int[INITIAL_CAPACITY];
				origFaceColors3 = new int[INITIAL_CAPACITY];
			}
			if (deformedVertexMask.length > MAX_RETAINED_CAPACITY)
			{
				deformedVertexMask = new boolean[INITIAL_CAPACITY];
			}
		}
	}

	/**
	 * Removes the current thread's scratch buffer to prevent ThreadLocal retention on shutdown.
	 */
	public static void cleanup()
	{
		THREAD_SCRATCH.remove();
	}

	/**
	 * Deforms the chest vertices of the given model according to configuration.
	 *
	 * @param model  The model to modify
	 * @param config User settings
	 * @return The ScratchBuffer containing original vertices, or null if no vertices were changed
	 */
	public static ScratchBuffer apply(Model model, BodyModifierConfig config)
	{
		if (model == null || !config.enabled())
		{
			return null;
		}

		final float[] vx = model.getVerticesX();
		final float[] vy = model.getVerticesY();
		final float[] vz = model.getVerticesZ();
		final int vertexCount = model.getVerticesCount();

		if (vx == null || vy == null || vz == null || vertexCount == 0)
		{
			return null;
		}

		// 1. Calculate model extents
		float modelMinY = Float.MAX_VALUE;
		float modelMaxY = -Float.MAX_VALUE;
		float modelMinZ = Float.MAX_VALUE;
		float modelMaxZ = -Float.MAX_VALUE;
		float modelMinX = Float.MAX_VALUE;
		float modelMaxX = -Float.MAX_VALUE;

		for (int i = 0; i < vertexCount; i++)
		{
			final float x = vx[i];
			final float y = vy[i];
			final float z = vz[i];

			if (x < modelMinX) modelMinX = x;
			if (x > modelMaxX) modelMaxX = x;
			if (y < modelMinY) modelMinY = y;
			if (y > modelMaxY) modelMaxY = y;
			if (z < modelMinZ) modelMinZ = z;
			if (z > modelMaxZ) modelMaxZ = z;
		}

		final float height = modelMaxY - modelMinY;
		// Skip non-humanoid or tiny models
		if (height < 60.0f)
		{
			return null;
		}

		final ScratchBuffer scratch = THREAD_SCRATCH.get();
		scratch.count = 0;
		scratch.faceCount = 0;

		try
		{
			// In OSRS local coordinates:
			// Ground/feet is around modelMaxY (typically ~0), head is modelMinY (typically ~ -200).
			// Bust line is approximately 68-72% up from feet towards head.
			final float centerY = modelMaxY - (0.69f * height) + config.verticalOffset();

		// Torso core Z-bounds: locate the chest and spine depth using only torso vertices.
		// Torso core width is |x| <= 35, height is centerY +/- 35, depth |z| <= 70 (excluding weapons/shields/capes).
		float torsoMinZ = Float.MAX_VALUE;
		float torsoMaxZ = -Float.MAX_VALUE;

		for (int i = 0; i < vertexCount; i++)
		{
			final float x = vx[i];
			final float y = vy[i];
			final float z = vz[i];

			if (Math.abs(x) <= 35.0f && Math.abs(y - centerY) <= 35.0f && Math.abs(z) <= 70.0f)
			{
				if (z < torsoMinZ) torsoMinZ = z;
				if (z > torsoMaxZ) torsoMaxZ = z;
			}
		}

		// Fallback if no vertices were in the strict core window
		if (torsoMinZ == Float.MAX_VALUE)
		{
			torsoMinZ = Math.max(-60.0f, modelMinZ);
			torsoMaxZ = Math.min(60.0f, modelMaxZ);
		}

		final float midZ = (torsoMinZ + torsoMaxZ) * 0.5f;

		// In OSRS local coordinates, default character orientation faces South (Negative Z is FRONT, Positive Z is BACK)
		final boolean inverted = config.invertZ();
		final float zSign = inverted ? 1.0f : -1.0f; // Push forward along facing direction
		final float frontZ = inverted ? torsoMaxZ : torsoMinZ;

		final float cleavage = (float) config.cleavage();
		final float radius = (float) config.influenceRadius();
		final float radiusSq = radius * radius;
		final float breastSize = (float) config.breastSize();
		final float sagAmount = (float) config.sagAmount();

		// Bounding box for early rejection in the frontal XY plane
		final float yMin = centerY - radius;
		final float yMax = centerY + radius;
		final float xBound = cleavage + radius;

		// Left apex (-X) and right apex (+X)
		final float leftCenterX = -cleavage;
		final float rightCenterX = cleavage;

		for (int i = 0; i < vertexCount; i++)
		{
			final float x = vx[i];
			final float y = vy[i];
			final float z = vz[i];

			// Exclude vertices that are far outside the torso depth (e.g. extended weapons or capes)
			if (Math.abs(z) > 75.0f)
			{
				continue;
			}

			// Only deform vertices on the FRONT half of the torso (skip spine/back)
			if (inverted ? (z < midZ - 12.0f) : (z > midZ + 12.0f))
			{
				continue;
			}

			// Broad-phase AABB check in XY frontal plane
			if (y < yMin || y > yMax || Math.abs(x) > xBound)
			{
				continue;
			}

			// Distance in the frontal plane to left and right apexes
			final float dxLeft = x - leftCenterX;
			final float dyLeft = y - centerY;
			final float distSqLeft = (dxLeft * dxLeft) + (dyLeft * dyLeft);

			final float dxRight = x - rightCenterX;
			final float dyRight = y - centerY;
			final float distSqRight = (dxRight * dxRight) + (dyRight * dyRight);

			final float minDistSq = Math.min(distSqLeft, distSqRight);

			if (minDistSq < radiusSq)
			{
				final float dist = (float) Math.sqrt(minDistSq);
				final float u = dist / radius; // 0.0 at apex, 1.0 at edge

				// Smooth cosine Hermite falloff: 1.0 at apex, 0.0 at edge
				final float weight = 0.5f * (1.0f + (float) Math.cos(Math.PI * u));

				// Depth weight: full effect on front surface, smooth falloff towards midZ
				final float depthDist = inverted ? (frontZ - z) : (z - frontZ);
				final float maxDepth = Math.max(15.0f, Math.abs(midZ - frontZ));
				final float depthWeight = Math.max(0.0f, Math.min(1.0f, 1.0f - (depthDist / maxDepth)));
				final float combinedWeight = weight * (0.35f + 0.65f * depthWeight);

				// Forward protrusion (pushing outward from the chest into negative Z)
				float deltaZ = breastSize * combinedWeight * zSign;

				// Always-on-top: lift center crease vertices forward so the crease is never sunken or occluded by bulging geometry
				if (config.enableDividingLine() && config.dividingLineAlwaysOnTop() && Math.abs(x) <= Math.max(1.5f, config.dividingLineWidth() * 1.5f))
				{
					deltaZ += (breastSize * 0.20f + 2.0f) * zSign;
				}

				// Downward gravity sag (+Y is down towards feet in OSRS)
				final float deltaY = sagAmount * combinedWeight * (0.5f + 0.5f * u);

				// Lateral spread (cleavage fullness pushing outward from center sternum)
				final float deltaX = Math.signum(x) * (cleavage * 0.35f) * combinedWeight;

				// Save original coordinates
				scratch.ensureCapacity(scratch.count + 1);
				scratch.indices[scratch.count] = i;
				scratch.origX[scratch.count] = x;
				scratch.origY[scratch.count] = y;
				scratch.origZ[scratch.count] = z;
				scratch.count++;

				// Mutate model vertices in place
				vx[i] = x + deltaX;
				vy[i] = y + deltaY;
				vz[i] = z + deltaZ;
			}
		}

		if (scratch.count > 0)
		{
			// Update bounds cylinder so GPU sorting assertions and frustum culling stay accurate
			model.calculateBoundsCylinder();

			final boolean doTint = config.enableColorTint() && config.tintColor() != null;
			final boolean doDividingLine = config.enableDividingLine() && config.dividingLineColor() != null;

			// Optional polygon color override and dividing line
			if (doTint || doDividingLine)
			{
				final int[] fc1 = model.getFaceColors1();
				final int[] fc2 = model.getFaceColors2();
				final int[] fc3 = model.getFaceColors3();
				final int[] ind1 = model.getFaceIndices1();
				final int[] ind2 = model.getFaceIndices2();
				final int[] ind3 = model.getFaceIndices3();
				final int numFaces = model.getFaceCount();

				if (fc1 != null && fc2 != null && fc3 != null && ind1 != null && ind2 != null && ind3 != null)
				{
					int safeFaces = Math.min(numFaces, Math.min(fc1.length, Math.min(fc2.length, Math.min(fc3.length,
						Math.min(ind1.length, Math.min(ind2.length, ind3.length))))));

					boolean[] isDeformedVertex = scratch.acquireVertexMask(vertexCount);
					try
					{
						for (int k = 0; k < scratch.count; k++)
						{
							int vIdx = scratch.indices[k];
							if (vIdx >= 0 && vIdx < vertexCount)
							{
								isDeformedVertex[vIdx] = true;
							}
						}

						short tintHsl = doTint ? JagexColor.rgbToHSL(config.tintColor().getRGB(), 1.0) : 0;
						int tintColorInt = tintHsl & 0xFFFF;

						short lineHsl = doDividingLine ? JagexColor.rgbToHSL(config.dividingLineColor().getRGB(), 1.0) : 0;
						int lineColorInt = lineHsl & 0xFFFF;

						final float lineWidth = Math.max(0.65f, (float) config.dividingLineWidth() * 0.85f);

						for (int f = 0; f < safeFaces; f++)
						{
							int v1 = ind1[f];
							int v2 = ind2[f];
							int v3 = ind3[f];

							if (v1 >= 0 && v1 < vertexCount && v2 >= 0 && v2 < vertexCount && v3 >= 0 && v3 < vertexCount)
							{
								if (isDeformedVertex[v1] || isDeformedVertex[v2] || isDeformedVertex[v3])
								{
									float faceCenterX = (vx[v1] + vx[v2] + vx[v3]) / 3.0f;
									float faceCenterY = (vy[v1] + vy[v2] + vy[v3]) / 3.0f;
									float faceCenterZ = (vz[v1] + vz[v2] + vz[v3]) / 3.0f;

									boolean isFront = !inverted ? (faceCenterZ <= midZ + 5.0f) : (faceCenterZ >= midZ - 5.0f);
									boolean isDividingLine = doDividingLine
										&& isFront
										&& Math.abs(faceCenterX) <= lineWidth
										&& Math.abs(faceCenterY - centerY) <= config.influenceRadius() * 1.05f;

									int targetColor = -1;
									if (isDividingLine)
									{
										targetColor = lineColorInt;
									}
									else if (doTint)
									{
										targetColor = tintColorInt;
									}

									if (targetColor != -1)
									{
										scratch.ensureFaceCapacity(scratch.faceCount + 1);
										scratch.faceIndices[scratch.faceCount] = f;
										scratch.origFaceColors1[scratch.faceCount] = fc1[f];
										scratch.origFaceColors2[scratch.faceCount] = fc2[f];
										scratch.origFaceColors3[scratch.faceCount] = fc3[f];
										scratch.faceCount++;

										fc1[f] = targetColor;
										fc2[f] = targetColor;
										fc3[f] = targetColor;
									}
								}
							}
						}
					}
					finally
					{
						for (int k = 0; k < scratch.count; k++)
						{
							int vIdx = scratch.indices[k];
							if (vIdx >= 0 && vIdx < isDeformedVertex.length)
							{
								isDeformedVertex[vIdx] = false;
							}
						}
					}
				}
			}
		}

		long now = System.currentTimeMillis();
		long prev = LAST_DIAGNOSTIC_LOG_TIME.get();
		if (now - prev > 4000 && LAST_DIAGNOSTIC_LOG_TIME.compareAndSet(prev, now))
		{
			log.info("BustDeformer: deformed {}/{} FRONT vertices, {} faces tinted (H={}, centerY={}, frontZ={}, midZ={})",
				scratch.count, vertexCount, scratch.faceCount, (int) height, (int) centerY, (int) frontZ, (int) midZ);
		}

		return scratch.count > 0 ? scratch : null;
	}
	catch (Throwable t)
	{
		log.error("Exception during BustDeformer deformation, restoring model state", t);
		restore(model, scratch);
		return null;
	}
}

	/**
	 * Restores original vertex positions and face colors from the scratch buffer.
	 *
	 * @param model   The model to restore
	 * @param scratch The scratch buffer containing pre-modification vertices and face colors
	 */
	public static void restore(Model model, ScratchBuffer scratch)
	{
		if (model == null || scratch == null)
		{
			return;
		}

		if (scratch.count > 0)
		{
			final float[] vx = model.getVerticesX();
			final float[] vy = model.getVerticesY();
			final float[] vz = model.getVerticesZ();

			if (vx != null && vy != null && vz != null)
			{
				for (int k = 0; k < scratch.count; k++)
				{
					final int i = scratch.indices[k];
					if (i >= 0 && i < vx.length && i < vy.length && i < vz.length)
					{
						vx[i] = scratch.origX[k];
						vy[i] = scratch.origY[k];
						vz[i] = scratch.origZ[k];
					}
				}
			}
			scratch.count = 0;
			try
			{
				model.calculateBoundsCylinder();
			}
			catch (Throwable ignored)
			{
			}
		}

		if (scratch.faceCount > 0)
		{
			final int[] fc1 = model.getFaceColors1();
			final int[] fc2 = model.getFaceColors2();
			final int[] fc3 = model.getFaceColors3();

			if (fc1 != null && fc2 != null && fc3 != null)
			{
				for (int k = 0; k < scratch.faceCount; k++)
				{
					int f = scratch.faceIndices[k];
					if (f >= 0 && f < fc1.length && f < fc2.length && f < fc3.length)
					{
						fc1[f] = scratch.origFaceColors1[k];
						fc2[f] = scratch.origFaceColors2[k];
						fc3[f] = scratch.origFaceColors3[k];
					}
				}
			}
			scratch.faceCount = 0;
		}

		scratch.trimIfExcessive();
	}
}

