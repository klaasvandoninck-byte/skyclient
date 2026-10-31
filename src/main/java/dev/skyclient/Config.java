package dev.skyclient;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Eenvoudige opslag in .minecraft/config/skyclient/settings.properties */
public final class Config {
	public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("skyclient");
	private static final Path FILE = DIR.resolve("settings.properties");
	private static final Properties P = new Properties();

	/** Sterkte van de sky glow (0 - 1). */
	public static float skyStrength = 0.30f;

	private Config() {}

	public static void load() {
		try {
			Files.createDirectories(DIR);
			if (Files.exists(FILE)) {
				try (InputStream in = Files.newInputStream(FILE)) {
					P.load(in);
				}
			}
		} catch (IOException ignored) {
		}
		for (Module m : Module.values()) {
			if (m == Module.FREECAM) continue;
			String v = P.getProperty("module." + m.name());
			if (v != null) m.enabled = Boolean.parseBoolean(v);
		}
		try {
			String s = P.getProperty("sky.strength");
			if (s != null) skyStrength = Float.parseFloat(s);
		} catch (NumberFormatException ignored) {
		}
	}

	public static void save() {
		for (Module m : Module.values()) {
			if (m == Module.FREECAM) continue;
			P.setProperty("module." + m.name(), Boolean.toString(m.enabled));
		}
		P.setProperty("sky.strength", Float.toString(skyStrength));
		try {
			Files.createDirectories(DIR);
			try (OutputStream out = Files.newOutputStream(FILE)) {
				P.store(out, "SkyClient");
			}
		} catch (IOException ignored) {
		}
	}

	public static String get(String key) {
		return P.getProperty(key);
	}

	public static void set(String key, String value) {
		P.setProperty(key, value);
		save();
	}

	public static void remove(String key) {
		P.remove(key);
		save();
	}
}
