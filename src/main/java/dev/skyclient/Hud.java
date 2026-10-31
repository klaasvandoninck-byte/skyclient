package dev.skyclient;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Random;

/** Alles wat op het scherm getekend wordt: sky glow, sterren, lyrics, info. */
public final class Hud {
	private static final String[] DIRS = {"Z", "ZW", "W", "NW", "N", "NO", "O", "ZO"};

	// sky-kleurverloop over een Minecraft-dag (ticks)
	private static final int[] KEY_T = {0, 1500, 6000, 11000, 12500, 14000, 18000, 22500, 24000};
	private static final int[] KEY_C = {
			0xFF966E, 0x8CBEFF, 0x6EB4FF, 0xFF8C5A, 0xC85A96, 0x463296, 0x322882, 0x965AAA, 0xFF966E};

	private static final int STAR_COUNT = 90;
	private static final float[] STAR_AZ = new float[STAR_COUNT];
	private static final float[] STAR_EL = new float[STAR_COUNT];
	private static final float[] STAR_PHASE = new float[STAR_COUNT];
	private static final int[] STAR_SIZE = new int[STAR_COUNT];

	static {
		Random r = new Random(1337);
		for (int i = 0; i < STAR_COUNT; i++) {
			STAR_AZ[i] = r.nextFloat() * 360f - 180f;
			STAR_EL[i] = 8f + r.nextFloat() * 75f;
			STAR_PHASE[i] = r.nextFloat() * 6.28f;
			STAR_SIZE[i] = r.nextInt(4) == 0 ? 2 : 1;
		}
	}

	private static float skyFade;
	private static int frames;
	private static int fps;
	private static long fpsStamp = System.currentTimeMillis();

	private Hud() {}

	public static void render(GuiGraphics g, DeltaTracker tracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.options.hideGui) return;

		// eigen FPS-teller
		frames++;
		long nowMs = System.currentTimeMillis();
		if (nowMs - fpsStamp >= 1000) {
			fps = frames;
			frames = 0;
			fpsStamp = nowMs;
		}

		float pt = tracker.getGameTimeDeltaPartialTick(false);
		int w = g.guiWidth();
		int h = g.guiHeight();

		Entity camEntity = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
		float yaw = camEntity.getViewYRot(pt);
		float pitch = camEntity.getViewXRot(pt);

		if (Module.SKY_GLOW.enabled || Module.SPARKLES.enabled) {
			sky(g, mc, w, h, yaw, pitch, pt);
		}
		Lyrics.render(g, mc, w, h, yaw, pitch);
		if (Module.INFO_HUD.enabled) {
			info(g, mc, yaw);
		}
	}

	// ---------------------------------------------------------------- sky

	private static void sky(GuiGraphics g, Minecraft mc, int w, int h, float yaw, float pitch, float pt) {
		boolean outside = mc.level.dimension() == Level.OVERWORLD
				&& mc.level.canSeeSky(mc.player.blockPosition());
		skyFade += ((outside ? 1f : 0f) - skyFade) * 0.05f;
		if (skyFade < 0.01f) return;

		long gameTime = mc.level.getGameTime();
		float t = (gameTime % 24000L) + pt;
		int rgb = skyColor(t);
		boolean night = t > 12500f && t < 23000f;

		if (Module.SKY_GLOW.enabled) {
			float strength = Config.skyStrength * skyFade * (night ? 1.0f : 0.75f);
			int a = Math.max(0, Math.min(255, (int) (strength * 255)));
			if (a > 2) {
				g.fillGradient(0, 0, w, (int) (h * 0.5f), (a << 24) | rgb, rgb);
			}
		}

		if (Module.SPARKLES.enabled && night) {
			float nightAmount = t < 14000f ? (t - 12500f) / 1500f : (t > 22000f ? (23000f - t) / 1000f : 1f);
			nightAmount = Math.max(0f, Math.min(1f, nightAmount)) * skyFade;
			float time = gameTime + pt;
			for (int i = 0; i < STAR_COUNT; i++) {
				float[] p = project(STAR_AZ[i], STAR_EL[i], yaw, pitch, w, h);
				if (p == null) continue;
				float twinkle = 0.35f + 0.65f * Math.abs(Mth.sin(time * 0.08f + STAR_PHASE[i]));
				int a = Math.max(4, Math.min(255, (int) (twinkle * nightAmount * 200)));
				int s = STAR_SIZE[i];
				int x = (int) p[0];
				int y = (int) p[1];
				g.fill(x, y, x + s, y + s, (a << 24) | 0xFFF0C8);
			}
		}
	}

	private static int skyColor(float t) {
		for (int i = 0; i < KEY_T.length - 1; i++) {
			if (t >= KEY_T[i] && t <= KEY_T[i + 1]) {
				float f = (t - KEY_T[i]) / (float) (KEY_T[i + 1] - KEY_T[i]);
				return lerpRgb(KEY_C[i], KEY_C[i + 1], f);
			}
		}
		return KEY_C[0];
	}

	private static int lerpRgb(int a, int b, float f) {
		int r = (int) Mth.lerp(f, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
		int g = (int) Mth.lerp(f, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
		int bl = (int) Mth.lerp(f, a & 0xFF, b & 0xFF);
		return (r << 16) | (g << 8) | bl;
	}

	// ---------------------------------------------------------------- info

	private static void info(GuiGraphics g, Minecraft mc, float yaw) {
		int ping = 0;
		if (mc.getConnection() != null) {
			PlayerInfo pi = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			if (pi != null) ping = pi.getLatency();
		}
		int dir = Math.floorMod((int) Math.floor(yaw / 45f + 0.5f), 8);

		String l1 = fps + " FPS   " + ping + " ms";
		String l2 = "XYZ " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ()
				+ "  " + DIRS[dir];

		int y = 6;
		g.drawString(mc.font, l1, 6, y, 0xFFFFFFFF, true);
		y += 11;
		g.drawString(mc.font, l2, 6, y, 0xFFE0E0E0, true);
		if (Freecam.isActive()) {
			y += 11;
			g.drawString(mc.font, "FREECAM actief (V om te stoppen)", 6, y, 0xFF7FD4FF, true);
		}
	}

	// ---------------------------------------------------------------- projectie

	/**
	 * Zet een punt in de lucht (azimut + hoogte in graden) om naar schermcoordinaten.
	 * Geeft null terug als het punt buiten beeld ligt.
	 */
	public static float[] project(float azimuth, float elevation, float camYaw, float camPitch, int w, int h) {
		float dYaw = Mth.wrapDegrees(azimuth - camYaw);
		float dEl = elevation + camPitch; // camPitch negatief = omhoog kijken
		if (Math.abs(dYaw) > 80f || Math.abs(dEl) > 70f) return null;

		double vfov = Math.toRadians(Minecraft.getInstance().options.fov().get());
		double th = Math.tan(vfov / 2.0);
		double aspect = (double) w / (double) h;

		float x = (float) (w / 2.0 + (Math.tan(Math.toRadians(dYaw)) / (th * aspect)) * (w / 2.0));
		float y = (float) (h / 2.0 - (Math.tan(Math.toRadians(dEl)) / th) * (h / 2.0));
		return new float[]{x, y};
	}
}
