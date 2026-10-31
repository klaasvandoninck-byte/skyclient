package dev.skyclient;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
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

	private static void applyPerformance(Options o) {
		Config.set("perf.prev.view", Integer.toString(o.renderDistance().get()));
		Config.set("perf.prev.sim", Integer.toString(o.simulationDistance().get()));
		Config.set("perf.prev.entityScale", Double.toString(o.entityDistanceScaling().get()));
		Config.set("perf.prev.particles", o.particles().get().name());
		Config.set("perf.prev.clouds", o.cloudStatus().get().name());
		Config.set("perf.prev.shadows", Boolean.toString(o.entityShadows().get()));
		Config.set("perf.prev.blend", Integer.toString(o.biomeBlendRadius().get()));

		o.renderDistance().set(Math.min(o.renderDistance().get(), 10));
		o.simulationDistance().set(Math.min(o.simulationDistance().get(), 6));
		o.entityDistanceScaling().set(0.6);
		o.particles().set(ParticleStatus.MINIMAL);
		o.cloudStatus().set(CloudStatus.OFF);
		o.entityShadows().set(false);
		o.biomeBlendRadius().set(1);
	}

	private static void restorePerformance(Options o) {
		try {
			o.renderDistance().set(Integer.parseInt(Config.get("perf.prev.view")));
			o.simulationDistance().set(Integer.parseInt(Config.get("perf.prev.sim")));
			o.entityDistanceScaling().set(Double.parseDouble(Config.get("perf.prev.entityScale")));
			o.particles().set(ParticleStatus.valueOf(Config.get("perf.prev.particles")));
			o.cloudStatus().set(CloudStatus.valueOf(Config.get("perf.prev.clouds")));
			o.entityShadows().set(Boolean.parseBoolean(Config.get("perf.prev.shadows")));
			o.biomeBlendRadius().set(Integer.parseInt(Config.get("perf.prev.blend")));
		} catch (RuntimeException ignored) {
		}
		for (String k : new String[]{"view", "sim", "entityScale", "particles", "clouds", "shadows", "blend"}) {
			Config.remove("perf.prev." + k);
		}
	}
}
