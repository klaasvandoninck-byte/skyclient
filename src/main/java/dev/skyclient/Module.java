package dev.skyclient;

/** Alle aan/uit-functies van SkyClient. */
public enum Module {
	LYRICS("Lyrics in lucht", true),
	SKY_GLOW("Sky glow", true),
	SPARKLES("Sterren", true),
	INFO_HUD("Info HUD", true),
	PERFORMANCE("FPS-boost", false),
	FULLBRIGHT("Fullbright", false),
	AUTO_SPRINT("Auto sprint", false),
	FREECAM("Freecam", false),
	ESP_PLAYERS("ESP spelers", false),
	ESP_HOSTILE("ESP vijanden", false),
	ESP_ANIMALS("ESP dieren", false),
	ESP_ITEMS("ESP items", false),
	STORAGE_ESP("Storage ESP", false);

	public final String label;
	public boolean enabled;

	Module(String label, boolean defaultEnabled) {
		this.label = label;
		this.enabled = defaultEnabled;
	}
}
