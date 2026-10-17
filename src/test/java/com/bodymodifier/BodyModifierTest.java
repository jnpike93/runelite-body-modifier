package com.bodymodifier;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BodyModifierTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BodyModifierPlugin.class);
		RuneLite.main(args);
	}
}
