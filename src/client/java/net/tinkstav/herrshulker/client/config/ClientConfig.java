package net.tinkstav.herrshulker.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.tinkstav.herrshulker.HerrShulker;
import net.minecraft.world.item.ItemDisplayContext;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "mrshulker_client.json");
    public static final float DEFAULT_DEFAULT_SCALE = 0.5f;
    public static final float DEFAULT_GUI_SCALE = 0.75f;
    public static final boolean DEFAULT_SHOW_CUSTOM_SCALES = true;

    // Changed from final Hashtable to Map for better performance and null repair support
    private Map<String, Float> lidItemScales;
    private Boolean showCustomScales = true;

    public ClientConfig() {
        this.lidItemScales = new HashMap<>();
        this.lidItemScales.putIfAbsent("default", DEFAULT_DEFAULT_SCALE);
        this.lidItemScales.putIfAbsent(ItemDisplayContext.GUI.getSerializedName(), DEFAULT_GUI_SCALE);
    }

    public Float getLidItemScale(String displayContext) {
        if (lidItemScales.containsKey(displayContext)) {
            return lidItemScales.get(displayContext);
        }
        return lidItemScales.get("default");
    }

    public void setLidItemScale(String displayContext, Float scale) {
        lidItemScales.put(displayContext, scale);
        save();
    }

    public Map<String, Float> getLidItemDisplayContextScales() {
        return lidItemScales;
    }

    public void setShowCustomScales(Boolean show) {
        showCustomScales = show;
        save();
    }

    public boolean getShowCustomScales() {
        return showCustomScales;
    }

    public void resetLidItemScales() {
        lidItemScales.clear();
        lidItemScales.put("default", DEFAULT_DEFAULT_SCALE);
        lidItemScales.put(ItemDisplayContext.GUI.getSerializedName(), DEFAULT_GUI_SCALE);
        save();
    }

    public void resetLidItemScale(String displayContext) {
        lidItemScales.remove(displayContext);
        if (displayContext.equals("default")) {
            lidItemScales.put("default", DEFAULT_DEFAULT_SCALE);
        }
        if (displayContext.equals(ItemDisplayContext.GUI.getSerializedName())) {
            lidItemScales.put(ItemDisplayContext.GUI.getSerializedName(), DEFAULT_GUI_SCALE);
        }
        save();
    }

    public void resetShowCustomScales() {
        setShowCustomScales(DEFAULT_SHOW_CUSTOM_SCALES);
    }

    public void save() {
        save(this);
    }

    /**
     * Repairs null fields that may occur when Gson deserializes old/incomplete config files.
     * Gson bypasses the constructor, so fields may be null if not present in JSON.
     */
    private void repairNullFields() {
        if (this.lidItemScales == null) {
            this.lidItemScales = new HashMap<>();
        }
        if (this.showCustomScales == null) {
            this.showCustomScales = DEFAULT_SHOW_CUSTOM_SCALES;
        }
        // Ensure required defaults exist
        this.lidItemScales.putIfAbsent("default", DEFAULT_DEFAULT_SCALE);
        this.lidItemScales.putIfAbsent(ItemDisplayContext.GUI.getSerializedName(), DEFAULT_GUI_SCALE);
    }

    public static ClientConfig load() {
        var config = new ClientConfig();
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    ClientConfig loaded = GSON.fromJson(reader, ClientConfig.class);
                    if (loaded != null) {
                        // Repair any null fields from old/corrupt configs
                        loaded.repairNullFields();
                        config = loaded;
                    } else {
                        HerrShulker.LOGGER.warn("HerrShulker Client config was empty or invalid, using defaults");
                    }
                }
            } else {
                save(config);
            }
        } catch (IOException e) {
            HerrShulker.LOGGER.error("Failed to load HerrShulker Client config: ", e);
        }
        return config;
    }

    public static void save(ClientConfig config) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            HerrShulker.LOGGER.error("Failed to save HerrShulker Client config: ", e);
        }
    }
}
