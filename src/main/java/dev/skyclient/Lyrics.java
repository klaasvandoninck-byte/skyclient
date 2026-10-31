package dev.skyclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lyrics in de lucht. Je zet je EIGEN tekst in config/skyclient/lyrics.txt.
 * Ondersteunt LRC-tijdstempels ([01:23.45] tekst) of gewone regels
 * (dan wisselt elke regel om de paar seconden).
 */
public final class Lyrics {
	private record Line(long ms, String text) {}

	private static final Pattern STAMP = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]");
	private static final List<Line> lines = new ArrayList<>();

	private static boolean running;
	private static long startMs;
	private static int lastIdx = -2;
	private static float anchorYaw;

	private Lyrics() {}

	private static final String DEFAULT_FILE = """
			# SkyClient lyrics
			# Zet hier je eigen tekst (regel per regel) en druk in het spel op G om te starten.
			# Gewone regels wisselen automatisch. Wil je exacte timing? Gebruik LRC-formaat:
			#   [00:05.00] Eerste regel
			#   [00:09.50] Tweede regel
			# Optioneel: '#seconds=4' op een eigen regel bepaalt de tijd per regel bij gewone tekst.
			#seconds=4
			Welkom bij SkyClient
			Kijk omhoog naar de lucht
			Hier komen jouw eigen lyrics
			Druk op G om opnieuw te starten
			""";

	public static boolean isRunning() {
		return running;
	}

	public static boolean hasLines() {
		return !lines.isEmpty();
	}

	public static void load() {
		lines.clear();
		try {
			Files.createDirectories(Config.DIR);
			var file = Config.DIR.resolve("lyrics.txt");
			if (!Files.exists(file)) {
				Files.writeString(file, DEFAULT_FILE);
			}
			parse(Files.readAllLines(file));
		} catch (IOException ignored) {
		}
	}

	private static void parse(List<String> raw) {
		double seconds = 4.0;
		List<String> plain = new ArrayList<>();
		List<Line> timed = new ArrayList<>();

		for (String line : raw) {
			String s = line.strip();
			if (s.startsWith("#seconds=")) {
				try {
					seconds = Double.parseDouble(s.substring(9).strip());
				} catch (NumberFormatException ignored) {
				}
				continue;
			}
			if (s.startsWith("#")) continue;
			if (s.matches("^\\[[a-zA-Z]+:.*]$")) continue; // [ar:..] [ti:..] metadata

			Matcher m = STAMP.matcher(s);
			List<Long> stamps = new ArrayList<>();
			int end = 0;
			while (m.find() && m.start() == end) {
				long min = Long.parseLong(m.group(1));
				long sec = Long.parseLong(m.group(2));
				long frac = 0;
				if (m.group(3) != null) {
					String f = (m.group(3) + "00").substring(0, 3);
					frac = Long.parseLong(f);
				}
				stamps.add(min * 60_000 + sec * 1000 + frac);
				end = m.end();
			}
			if (!stamps.isEmpty()) {
				String text = s.substring(end).strip();
				for (long t : stamps) timed.add(new Line(t, text));
			} else if (!s.isEmpty()) {
				plain.add(s);
			}
		}

		if (!timed.isEmpty()) {
			timed.sort((a, b) -> Long.compare(a.ms(), b.ms()));
			lines.addAll(timed);
		} else {
			long step = (long) (seconds * 1000);
			for (int i = 0; i < plain.size(); i++) {
				lines.add(new Line(i * step, plain.get(i)));
			}
		}
	}

	public static void restart() {
		load();
		startMs = System.currentTimeMillis();
		running = !lines.isEmpty();
		lastIdx = -2;
	}

	public static void render(GuiGraphics g, Minecraft mc, int w, int h, float camYaw, float camPitch) {
		if (!Module.LYRICS.enabled) return;

		if (!running) {
			if (hasLines() && mc.screen == null) {
				String hint = "Druk op G om lyrics te starten";
				g.drawString(mc.font, hint, w - mc.font.width(hint) - 6, 6, 0x80FFFFFF, true);
			}
			return;
		}

		long now = System.currentTimeMillis() - startMs;
		int idx = -1;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).ms() <= now) idx = i;
			else break;
		}
		if (idx < 0) return;

		Line cur = lines.get(idx);
		boolean last = idx + 1 >= lines.size();
		long nextMs = last ? cur.ms() + 6000 : lines.get(idx + 1).ms();
		if (last && now > cur.ms() + 6000) {
			running = false;
			return;
		}
		if (cur.text().isBlank()) return;

		if (idx != lastIdx) {
			lastIdx = idx;
			anchorYaw = camYaw; // regel blijft "hangen" op deze plek in de lucht
		}

		boolean gap = last || nextMs - cur.ms() > 2500;
		float fadeIn = clamp((now - cur.ms()) / 500f);
		float fadeOut = 1f;
		if (gap) {
			long shown = Math.min(nextMs - cur.ms(), 6000);
			fadeOut = clamp((cur.ms() + shown - now) / 500f);
		}
		float alpha = fadeIn * fadeOut;
		if (alpha < 0.02f) return;

		float[] p = Hud.project(anchorYaw, 24f, camYaw, camPitch, w, h);
		if (p == null) return;

		drawText(g, mc, cur.text(), p[0], p[1], 2.4f, alpha, true);

		if (!last) {
			String next = lines.get(idx + 1).text();
			if (!next.isBlank()) {
				drawText(g, mc, next, p[0], p[1] + 30f, 1.3f, alpha * 0.45f, false);
			}
		}
	}

	private static void drawText(GuiGraphics g, Minecraft mc, String text, float x, float y,
								 float scale, float alpha, boolean glow) {
		int a = Math.max(4, Math.min(255, (int) (alpha * 255)));
		int tw = mc.font.width(text);

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);

		if (glow) {
			int ga = Math.max(4, a / 4);
			int glowColor = (ga << 24) | 0x9CC8FF;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (dx == 0 && dy == 0) continue;
					g.drawString(mc.font, text, -tw / 2 + dx, -4 + dy, glowColor, false);
				}
			}
		}
		int color = (a << 24) | 0xFFF5FF;
		g.drawString(mc.font, text, -tw / 2, -4, color, true);

		g.pose().popMatrix();
	}

	private static float clamp(float v) {
		return Math.max(0f, Math.min(1f, v));
	}
}
