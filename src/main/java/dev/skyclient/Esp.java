package dev.skyclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Entity ESP via het glow-effect (outline zichtbaar door muren heen).
 * De glow-vlag wordt alleen client-side gezet.
 */
public final class Esp {
	private static final Set<Integer> marked = new HashSet<>();

	private Esp() {}

	public static void reset() {
		marked.clear();
	}

	private static boolean anyEnabled() {
		return Module.ESP_PLAYERS.enabled || Module.ESP_HOSTILE.enabled
				|| Module.ESP_ANIMALS.enabled || Module.ESP_ITEMS.enabled;
	}

	private static boolean wanted(Minecraft mc, Entity e) {
		if (e == mc.player || e == mc.getCameraEntity() || StorageEsp.isFake(e)) return false;
		if (e instanceof Player) return Module.ESP_PLAYERS.enabled;
		if (e instanceof Enemy) return Module.ESP_HOSTILE.enabled;
		if (e instanceof Animal) return Module.ESP_ANIMALS.enabled;
		if (e instanceof ItemEntity) return Module.ESP_ITEMS.enabled;
		return false;
	}

	public static void tick(Minecraft mc) {
		if (!anyEnabled() && marked.isEmpty()) return;
		if (mc.player.tickCount % 4 != 0) return;

		Set<Integer> seen = new HashSet<>();
		for (Entity e : mc.level.entitiesForRendering()) {
			int id = e.getId();
			if (wanted(mc, e)) {
				e.setGlowingTag(true);
				marked.add(id);
				seen.add(id);
			} else if (marked.contains(id)) {
				e.setGlowingTag(false);
				marked.remove(id);
			}
		}

		// entities die niet meer bestaan opruimen
		Iterator<Integer> it = marked.iterator();
		while (it.hasNext()) {
			if (!seen.contains(it.next())) it.remove();
		}
	}
}
