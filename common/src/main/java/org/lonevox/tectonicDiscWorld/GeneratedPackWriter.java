package org.lonevox.tectonicDiscWorld;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the generated Tectonic compatibility data pack to disk.
 * The pack wraps Tectonic's density functions with a radial falloff that forces
 * outer-ocean continentalness outside the configured disc radius.
 */
public final class GeneratedPackWriter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String NS = TectonicDiscWorld.MOD_ID;

    private static final String TECTONIC_RAW_CONTINENTS =
            "resourcepacks/tectonic/data/tectonic/worldgen/density_function/noise/raw_continents.json";

    // island_selector activates when raw_continents ∈ [-1, -0.5), which creates island landmasses.
    // Targeting -0.499 keeps raw_continents just above -0.5, so island_selector = 0 always.
    // Because the full_continents continent-path spline extrapolates with derivative=1 below its
    // lowest control point (location=0.05), full_continents ≈ raw_continents for all negative values.
    // This means biomes and terrain automatically stay in sync — no mismatch.
    private static final double RAW_CONTINENTS_OCEAN_TARGET = -0.499D;

    private GeneratedPackWriter() {}

    /**
     * Writes the generated compatibility data pack to a subdirectory of the given config directory.
     *
     * @param configDir the platform config directory (e.g. .minecraft/config)
     * @return the root path of the generated pack
     */
    public static Path writePack(Path configDir) throws IOException {
        Path root = configDir.resolve("tectonic-disc-world_generated_pack");
        Files.createDirectories(root);
        writeString(root.resolve("pack.mcmeta"), buildPackMcmeta());

        JsonElement originalRawContinents = readBundledJson();

        writeJson(root.resolve("data/" + NS + "/worldgen/density_function/configurable/unaffected_radius.json"),
                constant(TectonicDiscWorldConfig.unaffectedRadius));
        writeJson(root.resolve("data/" + NS + "/worldgen/density_function/configurable/falloff_radius.json"),
                constant(TectonicDiscWorldConfig.falloffRadius));
        writeJson(root.resolve("data/" + NS + "/worldgen/density_function/configurable/offset_x.json"),
                constant(TectonicDiscWorldConfig.offsetX));
        writeJson(root.resolve("data/" + NS + "/worldgen/density_function/configurable/offset_z.json"),
                constant(TectonicDiscWorldConfig.offsetZ));

        writeJson(root.resolve("data/" + NS + "/worldgen/density_function/distance_to_origin.json"),
                buildDistanceToOrigin());
        writeJson(root.resolve("data/" + NS + "/worldgen/density_function/band_progress.json"),
                buildBandProgress());

        // f(t) = 2*sqrt(t) - t reaches 0.5 at t≈0.17, so even the most continental terrain
        // (raw_continents ≈ 0.35) goes negative within ~5% of the falloff band (~125 blocks),
        // eliminating depth_additive and producing consistent ocean biomes + terrain quickly.
        JsonElement fastCurve = obj(
                "type", prim("add"),
                "argument1", obj("type", prim("mul"), "argument1", prim(2.0D),
                        "argument2", obj("type", prim("moredfs:sqrt"), "argument", ref(NS + ":band_progress"))),
                "argument2", obj("type", prim("mul"), "argument1", prim(-1.0D), "argument2", ref(NS + ":band_progress"))
        );
        writeJson(root.resolve("data/tectonic/worldgen/density_function/noise/raw_continents.json"),
                buildLerpWrapper(originalRawContinents.deepCopy(), prim(RAW_CONTINENTS_OCEAN_TARGET), fastCurve));

        writeString(root.resolve("README.txt"), buildReadme());
        return root;
    }

    private static JsonElement buildDistanceToOrigin() {
        JsonObject xShifted = obj(
                "type", prim("moredfs:subtract"),
                "argument1", obj("type", prim("moredfs:x")),
                "argument2", ref(NS + ":configurable/offset_x")
        );
        JsonObject zShifted = obj(
                "type", prim("moredfs:subtract"),
                "argument1", obj("type", prim("moredfs:z")),
                "argument2", ref(NS + ":configurable/offset_z")
        );
        JsonObject xSquared = obj("type", prim("mul"), "argument1", xShifted, "argument2", xShifted.deepCopy());
        JsonObject zSquared = obj("type", prim("mul"), "argument1", zShifted, "argument2", zShifted.deepCopy());
        JsonObject sum = obj("type", prim("add"), "argument1", xSquared, "argument2", zSquared);
        return obj("type", prim("moredfs:sqrt"), "argument", sum);
    }

    private static JsonElement buildBandProgress() {
        JsonObject distanceMinusRadius = obj(
                "type", prim("moredfs:subtract"),
                "argument1", ref(NS + ":distance_to_origin"),
                "argument2", ref(NS + ":configurable/unaffected_radius")
        );
        JsonObject normalized = obj(
                "type", prim("moredfs:div"),
                "numerator", distanceMinusRadius,
                "denominator", ref(NS + ":configurable/falloff_radius")
        );
        return obj(
                "type", prim("clamp"),
                "input", normalized,
                "min", prim(0.0D),
                "max", prim(1.0D)
        );
    }

    private static JsonElement buildLerpWrapper(JsonElement minValue, JsonElement maxValue, JsonElement input) {
        // lerp(t, min, max) = min * (1-t) + max * t
        JsonObject oneMinusInput = obj(
                "type", prim("add"),
                "argument1", prim(1.0D),
                "argument2", obj("type", prim("mul"), "argument1", prim(-1.0D), "argument2", input)
        );
        return obj(
                "type", prim("add"),
                "argument1", obj("type", prim("mul"), "argument1", minValue, "argument2", oneMinusInput),
                "argument2", obj("type", prim("mul"), "argument1", maxValue, "argument2", input.deepCopy())
        );
    }

    private static JsonElement constant(Number value) {
        return prim(value);
    }

    private static JsonElement prim(Number value) {
        return GSON.toJsonTree(value);
    }

    private static JsonElement prim(String value) {
        return GSON.toJsonTree(value);
    }

    private static JsonElement ref(String id) {
        return prim(id);
    }

    private static JsonObject obj(Object... entries) {
        JsonObject json = new JsonObject();
        for (int i = 0; i < entries.length; i += 2) {
            json.add((String) entries[i], (JsonElement) entries[i + 1]);
        }
        return json;
    }

    private static JsonElement readBundledJson() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = loader.getResourceAsStream(GeneratedPackWriter.TECTONIC_RAW_CONTINENTS)) {
            if (stream == null) {
                throw new IOException(
                        "Missing required bundled resource: " + GeneratedPackWriter.TECTONIC_RAW_CONTINENTS
                        + ". Tectonic 3.x must be present on the classpath.");
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader);
            }
        }
    }

    private static void writeJson(Path path, JsonElement json) throws IOException {
        writeString(path, GSON.toJson(json) + System.lineSeparator());
    }

    private static void writeString(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static String buildPackMcmeta() {
        return """
                {
                  "pack": {
                    "pack_format": 15,
                    "description": "Tectonic Disc World generated compatibility pack"
                  }
                }
                """;
    }

    private static String buildReadme() {
        return """
                Tectonic Disc World generated data pack
                =======================================

                This folder is generated automatically by the mod at startup.
                The JSON inside is derived from the currently installed Tectonic resources,
                then wrapped with a radial falloff that forces outer-ocean continentalness.

                Forge config: config/tectonic-disc-world-common.toml
                Fabric config: config/tectonic-disc-world.json

                Restart Minecraft after changing the config before testing a new world.
                """;
    }
}
