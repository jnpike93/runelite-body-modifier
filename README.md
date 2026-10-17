# Body Modifier RuneLite Plugin

A standalone client-sided visual modification plugin for Old School RuneScape (OSRS) that procedurally deforms character chest geometry to provide adjustable bust proportions. Fully integrated and compatible with RuneLite's **GPU plugin** and **117 HD**.

---

## Features

* **Adjustable Bust Size (0 to 250):** Smoothly displaces local mesh geometry forward along the chest plane.
* **On-Screen Color Picker:** Eyedropper overlay tool to quickly sample and match player skin tones.
* **Natural Gravity / Sag (0 to 100):** Applies downward teardrop curvature under weight to keep the shape natural rather than pointed.
* **Cleavage Spacing (5 to 40):** Configures apex focal distance from the center sternum.
* **Influence Radius (15 to 65):** Controls the falloff radius over the surrounding ribcage and torso to prevent sharp jagged edges.
* **Vertical Positioning (-40 to 40):** Fine-tune where the bust sits along the torso height.
* **Target Filtering:**
  * All Players
  * Female Players Only
  * Male Players Only
  * Local Player Only
  * Optional Humanoid NPCs (Bankers, Nieve, etc.)
* **Zero Model Cache Corruption:** Uses preallocated, thread-safe scratch buffers to modify vertices immediately before the GPU draw call and restores them immediately after, preventing cumulative inflation bugs.
* **Dual Pipeline Support:**
  * **GPU Mode:** Intercepts `DrawCallbacks.drawDynamic` so deformed vertices are uploaded directly to OpenGL/OpenCL VBOs.
  * **CPU Mode:** Automatic fallback that handles scene rendering when the GPU plugin is disabled.

---

## Running the Plugin Locally

To start RuneLite in developer mode with this plugin loaded:

```powershell
.\gradlew.bat run
```

---

## Building the Plugin JAR

To compile and package the plugin into a standalone JAR:

```powershell
.\gradlew.bat shadowJar
```

The output JAR will be generated in `build/libs/`.

---

## How It Works

1. **OSRS Model Coordinates:** In RuneLite, models are provided in local model space (centered around `[0, 0, 0]`, where `Y` is height, `X` is lateral, and `Z` is depth).
2. **GPU Interception:** When the GPU plugin is running, `DrawCallbacks.drawDynamic` is wrapped by `GpuDrawCallbackProxy`. Right before the GPU plugin copies model vertices into its Vertex Array Objects (VAOs), `BustDeformer` applies a dual-sphere cosine hermite falloff displacement vector.
3. **Immediate Restoration:** After the GPU plugin finishes its buffer write for that draw call, original vertex coordinates are restored in memory, preserving the client's internal model cache integrity.

