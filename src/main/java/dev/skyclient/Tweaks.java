package dev.skyclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** FPS-boost, fullbright, auto sprint en zoom. */
public final class Tweaks {
	private static int oldFov = -1;
	private static boolean fullbrightWasOn;

	private Tweaks() {}

	public static void tick(Minecraft mc) {
		zoom(mc);
		performance(mc);
		fullbright(mc);

		if (Module.AUTO_SPRINT.enabled && !Module.FREECAM.enabled && mc.screen == null
				&& mc.options.keyUp.isDown() && !mc.player.isShiftKeyDown()) {
			mc.options.keySprint.setDown(true);
		}
	}

	private static void zoom(Minecraft mc) {
		if (SkyClient.KEY_ZOOM.isDown() && mc.screen == null) {
			if (oldFov < 0) oldFov = mc.options.fov().get();
			mc.options.fov().set(30);
		} else if (oldFov > 0) {
			mc.options.fov().set(oldFov);
			oldFov = -1;
		}
	}

	private static void fullbright(Minecraft mc) {
		if (Module.FULLBRIGHT.enabled) {
			if (!mc.player.hasEffect(MobEffects.NIGHT_VISION) || mc.player.tickCount % 100 == 0) {
				mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, false, false, false));
			}
			fullbrightWasOn = true;
		} else if (fullbrightWasOn) {
			mc.player.removeEffect(MobEffects.NIGHT_VISION);
			fullbrightWasOn = false;
		}
	}

	private static void performance(Minecraft mc) {
		boolean applied = Config.get("perf.prev.view") != null;
		if (Module.PERFORMANCE.enabled && !applied) {
			applyPerformance(mc.options);
		} else if (!Module.PERFORMANCE.enabled && applied) {
			restorePerformance(mc.options);
		}
	}

	/** Leest de naam van een enum-optie (particles, clouds) zonder de klasse te hoeven kennen. */
	private static String enumName(Object option) {
		try {
			Object cur = option.getClass().getMethod("get").invoke(option);
			return ((Enum<?>) cur).name();
		} catch (Exception e) {
			return null;
		}
	}

	/** Zet een enum-optie op de waarde met deze naam, zonder de klasse te hoeven kennen. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void setEnum(Object option, String name) {
		if (name == null) return;
		try {
			Object cur = option.getClass().getMethod("get").invoke(option);
			Class<?> type = ((Enum<?>) cur).getDeclaringClass();
			Object value = Enum.valueOf((Class) type, name);
			option.getClass().getMethod("set", Object.class).invoke(option, value);
		} catch (Exception ignored) {
		}
	}

	private static void applyPerformance(Options o) {
		Config.set("perf.prev.view", Integer.toString(o.renderDistance().get()));
		Config.set("perf.prev.sim", Integer.toString(o.simulationDistance().get()));
		Config.set("perf.prev.entityScale", Double.toString(o.entityDistanceScaling().get()));
		String particles = enumName(o.particles());
		String clouds = enumName(o.cloudStatus());
		Config.set("perf.prev.particles", particles == null ? "" : particles);
		Config.set("perf.prev.clouds", clouds == null ? "" : clouds);
		Config.set("perf.prev.shadows", Boolean.toString(o.entityShadows().get()));
		Config.set("perf.prev.blend", Integer.toString(o.biomeBlendRadius().get()));

		o.renderDistance().set(Math.min(o.renderDistance().get(), 10));
		o.simulationDistance().set(Math.min(o.simulationDistance().get(), 6));
		o.entityDistanceScaling().set(0.6);
		setEnum(o.particles(), "MINIMAL");
		setEnum(o.cloudStatus(), "OFF");
		o.entityShadows().set(false);
		o.biomeBlendRadius().set(1);
	}

	private static void restorePerformance(Options o) {
		try {
			o.renderDistance().set(Integer.parseInt(Config.get("perf.prev.view")));
			o.simulationDistance().set(Integer.parseInt(Config.get("perf.prev.sim")));
			o.entityDistanceScaling().set(Double.parseDouble(Config.get("perf.prev.entityScale")));
			setEnum(o.particles(), emptyToNull(Config.get("perf.prev.particles")));
			setEnum(o.cloudStatus(), emptyToNull(Config.get("perf.prev.clouds")));
			o.entityShadows().set(Boolean.parseBoolean(Config.get("perf.prev.shadows")));
			o.biomeBlendRadius().set(Integer.parseInt(Config.get("perf.prev.blend")));
		} catch (RuntimeException ignored) {
		}
		for (String k : new String[]{"view", "sim", "entityScale", "particles", "clouds", "shadows", "blend"}) {
			Config.remove("perf.prev." + k);
		}
	}

	private static String emptyToNull(String s) {
		return s == null || s.isEmpty() ? null : s;
	}
}
