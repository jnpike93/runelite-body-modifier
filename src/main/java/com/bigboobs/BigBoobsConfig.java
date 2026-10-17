package com.bigboobs;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup("bigboobs")
public interface BigBoobsConfig extends Config
{
	@ConfigSection(
		name = "General",
		description = "General plugin settings and target filters",
		position = 0
	)
	String generalSection = "general";

	@ConfigSection(
		name = "Bust Proportions",
		description = "Adjustable boob dimensions and shape",
		position = 1
	)
	String proportionsSection = "proportions";

	@ConfigSection(
		name = "Color Palette",
		description = "Optional color tinting for the deformed mesh region",
		position = 2
	)
	String colorSection = "color";

	@ConfigSection(
		name = "Advanced & GPU",
		description = "Rendering pipeline and coordinate adjustments",
		position = 3
	)
	String advancedSection = "advanced";

	// --- General Section ---

	@ConfigItem(
		keyName = "enabled",
		name = "Enable Mod",
		description = "Toggle character bust modification on/off",
		section = generalSection,
		position = 0
	)
	default boolean enabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "targetGroup",
		name = "Target Characters",
		description = "Which characters to modify",
		section = generalSection,
		position = 1
	)
	default TargetGroup targetGroup()
	{
		return TargetGroup.ALL_PLAYERS;
	}

	@ConfigItem(
		keyName = "affectNpcs",
		name = "Affect Humanoid NPCs",
		description = "Also apply bust modifications to female humanoid NPCs (e.g. Bankers, Nieve)",
		section = generalSection,
		position = 2
	)
	default boolean affectNpcs()
	{
		return false;
	}

	// --- Bust Proportions Section ---

	@Range(min = 0, max = 250)
	@ConfigItem(
		keyName = "breastSize",
		name = "Bust Size",
		description = "Forward projection scale (huge sizes supported)",
		section = proportionsSection,
		position = 0
	)
	default int breastSize()
	{
		return 85;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "sagAmount",
		name = "Gravity / Sag",
		description = "Natural downward droop and teardrop curve under weight",
		section = proportionsSection,
		position = 1
	)
	default int sagAmount()
	{
		return 28;
	}

	@Range(min = 5, max = 40)
	@ConfigItem(
		keyName = "cleavage",
		name = "Cleavage Spacing",
		description = "Horizontal distance from center sternum to each breast apex",
		section = proportionsSection,
		position = 2
	)
	default int cleavage()
	{
		return 15;
	}

	@Range(min = -40, max = 40)
	@ConfigItem(
		keyName = "verticalOffset",
		name = "Vertical Position",
		description = "Shift the bust higher or lower on the torso",
		section = proportionsSection,
		position = 3
	)
	default int verticalOffset()
	{
		return 0;
	}

	@Range(min = 15, max = 65)
	@ConfigItem(
		keyName = "influenceRadius",
		name = "Influence Radius",
		description = "Radius of mesh smoothing to ensure soft transitions",
		section = proportionsSection,
		position = 4
	)
	default int influenceRadius()
	{
		return 42;
	}

	// --- Color Palette Section ---

	@ConfigItem(
		keyName = "enableColorTint",
		name = "Enable Color Override",
		description = "Apply a custom color override to the deformed polygons",
		section = colorSection,
		position = 0
	)
	default boolean enableColorTint()
	{
		return false;
	}

	@ConfigItem(
		keyName = "tintColor",
		name = "Tint Color",
		description = "Choose any color from the color palette",
		section = colorSection,
		position = 1
	)
	default Color tintColor()
	{
		return new Color(225, 185, 150);
	}

	@ConfigItem(
		keyName = "colorPickerHotkey",
		name = "Sample Color Hotkey",
		description = "Press this hotkey while hovering your cursor over any pixel on screen to sample that color",
		section = colorSection,
		position = 2
	)
	default Keybind colorPickerHotkey()
	{
		return new Keybind(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "eyedropperMode",
		name = "Eyedropper Click Mode",
		description = "When toggled on, click anywhere on screen to sample that color and auto-apply",
		section = colorSection,
		position = 3
	)
	default boolean eyedropperMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enableDividingLine",
		name = "Dividing Line",
		description = "Apply a dividing crease line color down the center between the breasts",
		section = colorSection,
		position = 4
	)
	default boolean enableDividingLine()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dividingLineColor",
		name = "Dividing Line Color",
		description = "Color of the center dividing line / cleavage seam",
		section = colorSection,
		position = 5
	)
	default Color dividingLineColor()
	{
		return new Color(50, 30, 20);
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = "dividingLineWidth",
		name = "Dividing Line Width",
		description = "Width threshold for faces belonging to the center dividing crease (smaller = thinner line)",
		section = colorSection,
		position = 6
	)
	default int dividingLineWidth()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "dividingLineAlwaysOnTop",
		name = "Always On Top",
		description = "Ensure the dividing crease line renders on top and is not hidden or occluded by surrounding mesh",
		section = colorSection,
		position = 7
	)
	default boolean dividingLineAlwaysOnTop()
	{
		return true;
	}

	// --- Advanced Section ---

	@ConfigItem(
		keyName = "gpuHookEnabled",
		name = "GPU Plugin Hook",
		description = "Intercept DrawCallbacks for seamless compatibility with the GPU plugin and 117 HD",
		section = advancedSection,
		position = 0
	)
	default boolean gpuHookEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "invertZ",
		name = "Invert Forward Direction",
		description = "Invert the forward displacement axis if models appear inverted",
		section = advancedSection,
		position = 1
	)
	default boolean invertZ()
	{
		return false;
	}

	enum TargetGroup
	{
		ALL_PLAYERS("All Players"),
		FEMALE_ONLY("Female Players Only"),
		MALE_ONLY("Male Players Only"),
		LOCAL_PLAYER_ONLY("Local Player Only");

		private final String name;

		TargetGroup(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}
}
