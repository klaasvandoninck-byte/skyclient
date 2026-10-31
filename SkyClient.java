package dev.skyclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class SkyClient implements ClientModInitializer {
	public static final String ID = "skyclient";

	public static KeyMapping KEY_MENU;
	public static KeyMapping KEY_FREECAM;
	public static KeyMapping KEY_LYRICS;
	public static KeyMapping KEY_ZOOM;

	@Override
	public void onInitializeClient() {
		Config.load();
		Lyrics.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ID, "main"));
		KEY_MENU = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.skyclient.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
		KEY_FREECAM = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.skyclient.freecam", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, category));
		KEY_LYRICS = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.skyclient.lyrics", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, category));
		KEY_ZOOM = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.skyclient.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, category));

		ClientTickEvents.END_CLIENT_TICK.register(SkyClient::tick);
		HudElementRegistry.attachElementAfter(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(ID, "hud"),
				Hud::render);
	}

	public static void setEnabled(Module m, boolean value) {
		m.enabled = value;
		Config.save();
	}

	private static void tick(Minecraft mc) {
		while (KEY_MENU.consumeClick()) {
			mc.setScreen(new MenuScreen());
		}
		while (KEY_FREECAM.consumeClick()) {
			setEnabled(Module.FREECAM, !Module.FREECAM.enabled);
		}
		while (KEY_LYRICS.consumeClick()) {
			Lyrics.restart();
		}

		if (mc.player == null || mc.level == null) {
			Freecam.reset();
			Esp.reset();
			StorageEsp.reset();
			return;
		}

		Freecam.tick(mc);
		Esp.tick(mc);
		StorageEsp.tick(mc);
		Tweaks.tick(mc);
	}
}
