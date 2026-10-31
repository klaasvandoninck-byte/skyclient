package dev.skyclient;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MenuScreen extends Screen {
	public MenuScreen() {
		super(Component.literal("SkyClient"));
	}

	private static Component label(Module m) {
		return Component.literal(m.label + ": " + (m.enabled ? "AAN" : "UIT"));
	}

	@Override
	protected void init() {
		int cols = 3;
		int bw = 128;
		int bh = 20;
		int gap = 4;
		int totalW = cols * bw + (cols - 1) * gap;
		int x0 = (width - totalW) / 2;
		int y0 = Math.max(34, height / 2 - 80);

		Module[] modules = Module.values();
		for (int i = 0; i < modules.length; i++) {
			Module m = modules[i];
			int cx = x0 + (i % cols) * (bw + gap);
			int cy = y0 + (i / cols) * (bh + gap);
			addRenderableWidget(Button.builder(label(m), b -> {
				SkyClient.setEnabled(m, !m.enabled);
				b.setMessage(label(m));
			}).bounds(cx, cy, bw, bh).build());
		}

		int rows = (modules.length + cols - 1) / cols;
		int ey = y0 + rows * (bh + gap) + 8;

		addRenderableWidget(Button.builder(Component.literal("Lyrics (her)starten"), b -> {
			Lyrics.restart();
			onClose();
		}).bounds(x0, ey, bw, bh).build());

		addRenderableWidget(Button.builder(Component.literal("Lyrics-map openen"),
				b -> Util.getPlatform().openPath(Config.DIR)).bounds(x0 + bw + gap, ey, bw, bh).build());

		addRenderableWidget(Button.builder(Component.literal("Klaar"), b -> onClose())
				.bounds(x0 + 2 * (bw + gap), ey, bw, bh).build());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		g.drawCenteredString(font, "SkyClient  -  Right Shift = menu, V = freecam, G = lyrics, C = zoom",
				width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
