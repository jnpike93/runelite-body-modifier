package com.bodymodifier;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Development test launcher to start RuneLite with the Body Modifier plugin loaded.
 */
public class BodyModifierTest
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BodyModifierPlugin.class);
		RuneLite.main(args);
	}
}
