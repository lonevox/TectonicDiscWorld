package org.lonevox.tectonicDiscWorld.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.lonevox.tectonicDiscWorld.TectonicDiscWorld;
import org.lonevox.tectonicDiscWorld.TectonicDiscWorldConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class TectonicDiscWorldFabricConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = TectonicDiscWorld.MOD_ID + ".json";

    static void load() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        if (Files.exists(configFile)) {
            try {
                String content = Files.readString(configFile, StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                readFrom(json);
            } catch (IOException | JsonParseException e) {
                TectonicDiscWorld.LOGGER.error("Failed to read config {}, using defaults", configFile, e);
            }
        } else {
            save(configFile);
        }
    }

    private static void readFrom(JsonObject json) {
        if (json.has("unaffectedRadius")) TectonicDiscWorldConfig.unaffectedRadius = json.get("unaffectedRadius").getAsInt();
        if (json.has("falloffRadius")) TectonicDiscWorldConfig.falloffRadius = json.get("falloffRadius").getAsInt();
        if (json.has("offsetX")) TectonicDiscWorldConfig.offsetX = json.get("offsetX").getAsInt();
        if (json.has("offsetZ")) TectonicDiscWorldConfig.offsetZ = json.get("offsetZ").getAsInt();
        if (json.has("forceEnablePack")) TectonicDiscWorldConfig.forceEnablePack = json.get("forceEnablePack").getAsBoolean();
    }

    private static void save(Path configFile) {
        JsonObject json = new JsonObject();
        json.addProperty("unaffectedRadius", TectonicDiscWorldConfig.unaffectedRadius);
        json.addProperty("falloffRadius", TectonicDiscWorldConfig.falloffRadius);
        json.addProperty("offsetX", TectonicDiscWorldConfig.offsetX);
        json.addProperty("offsetZ", TectonicDiscWorldConfig.offsetZ);
        json.addProperty("forceEnablePack", TectonicDiscWorldConfig.forceEnablePack);
        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            TectonicDiscWorld.LOGGER.error("Failed to write default config {}", configFile, e);
        }
    }

    private TectonicDiscWorldFabricConfig() {}
}
