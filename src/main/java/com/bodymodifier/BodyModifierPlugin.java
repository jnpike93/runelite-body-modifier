package com.bodymodifier;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Renderable;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

/**
 * Main plugin class managing lifecycle, hooks, and configuration for character proportions.
 */
@Slf4j
@PluginDescriptor(
	name = "Body Modifier",
	description = "Adjustable player character chest proportions with full GPU plugin compatibility",
	tags = {"model", "appearance", "cosmetic", "character", "bust", "proportions"}
)
public class BodyModifierPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BodyModifierConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ColorPickerOverlay colorPickerOverlay;

	private BustDrawCallbacks wrapper;

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.colorPickerHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			pickColorUnderCursor();
		}
	};

	private final MouseListener mouseListener = new MouseListener()
	{
		@Override
		public MouseEvent mouseClicked(MouseEvent e)
		{
			return e;
		}

		@Override
		public MouseEvent mousePressed(MouseEvent e)
		{
			if (config.eyedropperMode() && e.getButton() == MouseEvent.BUTTON1)
			{
				pickColorUnderCursor();
				configManager.setConfiguration("bodymodifier", "eyedropperMode", false);
				e.consume();
				return e;
			}
			return e;
		}

		@Override
		public MouseEvent mouseReleased(MouseEvent e)
		{
			return e;
		}

		@Override
		public MouseEvent mouseEntered(MouseEvent e)
		{
			return e;
		}

		@Override
		public MouseEvent mouseExited(MouseEvent e)
		{
			return e;
		}

		@Override
		public MouseEvent mouseDragged(MouseEvent e)
		{
			return e;
		}

		@Override
		public MouseEvent mouseMoved(MouseEvent e)
		{
			return e;
		}
	};

	@Provides
	BodyModifierConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BodyModifierConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		log.info("Body Modifier plugin started");
		keyManager.registerKeyListener(hotkeyListener);
		mouseManager.registerMouseListener(mouseListener);
		overlayManager.add(colorPickerOverlay);
		clientThread.invoke(this::attachGpuHook);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Body Modifier plugin stopping");
		keyManager.unregisterKeyListener(hotkeyListener);
		mouseManager.unregisterMouseListener(mouseListener);
		overlayManager.remove(colorPickerOverlay);
		clientThread.invoke(this::detachGpuHook);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("bodymodifier"))
		{
			return;
		}

		if (!config.enabled() || !config.gpuHookEnabled())
		{
			clientThread.invoke(this::detachGpuHook);
		}
		else
		{
			clientThread.invoke(this::attachGpuHook);
		}
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		if (config.enabled() && config.gpuHookEnabled())
		{
			clientThread.invoke(this::attachGpuHook);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (config.enabled() && config.gpuHookEnabled())
		{
			clientThread.invoke(this::attachGpuHook);
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		if (!config.enabled() || !config.gpuHookEnabled())
		{
			return;
		}

		DrawCallbacks current = client.getDrawCallbacks();
		if (current != null && current != wrapper)
		{
			attachGpuHook();
		}
	}

	public void pickColorUnderCursor()
	{
		try
		{
			PointerInfo pointerInfo = MouseInfo.getPointerInfo();
			if (pointerInfo == null)
			{
				return;
			}
			Point screenPos = pointerInfo.getLocation();
			Robot robot = new Robot();
			Color sampled = robot.getPixelColor(screenPos.x, screenPos.y);
			configManager.setConfiguration("bodymodifier", "tintColor", sampled);
			configManager.setConfiguration("bodymodifier", "enableColorTint", true);
			String hex = String.format("#%02X%02X%02X", sampled.getRed(), sampled.getGreen(), sampled.getBlue());
			log.info("Sampled screen color: {}", hex);
			clientThread.invoke(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[Body Modifier] Sampled color: " + hex, null));
		}
		catch (Exception ex)
		{
			log.warn("Failed to sample color under cursor", ex);
		}
	}

	private void attachGpuHook()
	{
		DrawCallbacks current = client.getDrawCallbacks();
		if (current == null || current instanceof BustDrawCallbacks)
		{
			return;
		}

		log.info("Attaching BustDrawCallbacks over {}", current.getClass().getName());
		wrapper = new BustDrawCallbacks(current, config, this);
		client.setDrawCallbacks(wrapper);
	}

	private void detachGpuHook()
	{
		DrawCallbacks current = client.getDrawCallbacks();
		if (current instanceof BustDrawCallbacks)
		{
			client.setDrawCallbacks(((BustDrawCallbacks) current).getDelegate());
			log.info("Detached BustDrawCallbacks");
		}
		wrapper = null;
	}

	public boolean shouldModify(Renderable renderable)
	{
		if (renderable instanceof Player)
		{
			Player player = (Player) renderable;
			switch (config.targetGroup())
			{
				case ALL_PLAYERS:
					return true;

				case FEMALE_ONLY:
				{
					PlayerComposition comp = player.getPlayerComposition();
					return comp != null && comp.isFemale();
				}

				case MALE_ONLY:
				{
					PlayerComposition comp = player.getPlayerComposition();
					return comp != null && !comp.isFemale();
				}

				case LOCAL_PLAYER_ONLY:
					return player == client.getLocalPlayer();

				default:
					return true;
			}
		}
		else if (renderable instanceof NPC)
		{
			return config.affectNpcs();
		}

		return false;
	}
}
