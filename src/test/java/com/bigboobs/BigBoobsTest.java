package com.bigboobs;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BigBoobsTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BigBoobsPlugin.class);
		RuneLite.main(args);
	}
}

