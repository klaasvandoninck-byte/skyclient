package dev.skyclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Storage ESP: boven elke kist, vat, shulker en ender chest verschijnt een
 * client-side gloeiend item-icoon dat door muren heen zichtbaar is.
 * De entities bestaan alleen op jouw client en worden nooit naar de server gestuurd.
 */
public final class StorageEsp {
	private static final int FAKE_BASE = -1_000_000;
	private static final int MAX_MARKERS = 250;

	private static final Map<BlockPos, ItemEntity> markers = new HashMap<>();
	private static int nextId = FAKE_BASE;
	private static Object lastLevel;

	private StorageEsp() {}

	public static boolean isFake(Entity e) {
		return e.getId() <= FAKE_BASE;
	}

	public static void reset() {
		markers.clear();
		lastLevel = null;
	}

	private static void clear() {
		for (ItemEntity e : markers.values()) {
			e.discard();
		}
		markers.clear();
	}

	private static boolean isStorage(BlockEntity be) {
		return be instanceof ChestBlockEntity
				|| be instanceof BarrelBlockEntity
				|| be instanceof ShulkerBoxBlockEntity
				|| be instanceof EnderChestBlockEntity;
	}

	public static void tick(Minecraft mc) {
		if (lastLevel != mc.level) {
			markers.clear(); // oude wereld is weg, entities zijn al verdwenen
			lastLevel = mc.level;
		}

		if (!Module.STORAGE_ESP.enabled) {
			if (!markers.isEmpty()) clear();
			return;
		}
		if (mc.player.tickCount % 20 != 0) return;

		int pcx = mc.player.getBlockX() >> 4;
		int pcz = mc.player.getBlockZ() >> 4;
		int radius = Math.min(mc.options.renderDistance().get(), 8);

		Set<BlockPos> found = new HashSet<>();
		outer:
		for (int cx = pcx - radius; cx <= pcx + radius; cx++) {
			for (int cz = pcz - radius; cz <= pcz + radius; cz++) {
				LevelChunk chunk = mc.level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) continue;
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (!isStorage(be)) continue;
					found.add(be.getBlockPos().immutable());
					if (found.size() >= MAX_MARKERS) break outer;
				}
			}
		}

		// verwijderde / niet meer bestaande kisten opruimen
		Iterator<Map.Entry<BlockPos, ItemEntity>> it = markers.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<BlockPos, ItemEntity> entry = it.next();
			if (!found.contains(entry.getKey()) || entry.getValue().isRemoved()) {
				entry.getValue().discard();
				it.remove();
			}
		}

		// nieuwe markers aanmaken
		for (BlockPos pos : found) {
			if (markers.containsKey(pos)) continue;
			BlockEntity be = mc.level.getBlockEntity(pos);
			if (be == null) continue;

			ItemStack stack = new ItemStack(be.getBlockState().getBlock());
			Vec3 c = Vec3.atCenterOf(pos);
			ItemEntity marker = new ItemEntity(mc.level, c.x, c.y, c.z, stack);
			marker.setId(nextId--);
			marker.setNoGravity(true);
			marker.setDeltaMovement(Vec3.ZERO);
			marker.setUnlimitedLifetime();
			marker.setNeverPickUp();
			marker.setGlowingTag(true);
			mc.level.addEntity(marker);
			markers.put(pos, marker);
		}
	}
}
