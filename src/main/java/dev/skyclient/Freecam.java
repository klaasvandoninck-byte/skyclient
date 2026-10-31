package dev.skyclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/**
 * Freecam: een onzichtbare camera-entity vliegt rond terwijl je echte speler stilstaat.
 * De look-richting volgt je muis, de bewegingstoetsen sturen de camera.
 */
public final class Freecam {
	private static ArmorStand cam;
	private static ClientInput savedInput;
	private static boolean active;

	private Freecam() {}

	public static boolean isActive() {
		return active;
	}

	public static void reset() {
		cam = null;
		savedInput = null;
		active = false;
	}

	public static void tick(Minecraft mc) {
		if (active && (cam == null || cam.level() != mc.level)) {
			// wereld gewisseld
			reset();
		}

		if (Module.FREECAM.enabled && !active) {
			enable(mc);
		} else if (!Module.FREECAM.enabled && active) {
			disable(mc);
		}

		if (!active) return;

		// camera volgt de kijkrichting van de speler (muis)
		cam.setOldPosAndRot();
		cam.setYRot(mc.player.getYRot());
		cam.setXRot(mc.player.getXRot());

		double fwd = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
		double side = (mc.options.keyRight.isDown() ? 1 : 0) - (mc.options.keyLeft.isDown() ? 1 : 0);
		double vert = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);

		double yaw = Math.toRadians(cam.getYRot());
		Vec3 look = cam.getLookAngle();
		Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));

		Vec3 move = look.scale(fwd).add(right.scale(side)).add(0, vert, 0);
		if (move.lengthSqr() > 1.0E-6) {
			double speed = mc.options.keySprint.isDown() ? 1.2 : 0.55;
			move = move.normalize().scale(speed);
			cam.setPos(cam.getX() + move.x, cam.getY() + move.y, cam.getZ() + move.z);
		}
	}

	private static void enable(Minecraft mc) {
		cam = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
		cam.setInvisible(true);
		cam.setNoGravity(true);

		Vec3 eye = mc.player.getEyePosition();
		cam.setPos(eye.x, eye.y - cam.getEyeHeight(), eye.z);
		cam.setYRot(mc.player.getYRot());
		cam.setXRot(mc.player.getXRot());
		cam.setOldPosAndRot();

		// echte speler loopt niet mee
		savedInput = mc.player.input;
		mc.player.input = new ClientInput() {};

		mc.setCameraEntity(cam);
		active = true;
	}

	private static void disable(Minecraft mc) {
		if (mc.player != null) {
			if (savedInput != null) mc.player.input = savedInput;
			mc.setCameraEntity(mc.player);
		}
		cam = null;
		savedInput = null;
		active = false;
	}
}
