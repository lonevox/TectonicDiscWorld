package org.lonevox.tectonicDiscWorld.forge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lonevox.tectonicDiscWorld.GeneratedPackWriter;
import org.lonevox.tectonicDiscWorld.TectonicDiscWorld;
import org.lonevox.tectonicDiscWorld.TectonicDiscWorldConfig;

import java.io.IOException;
import java.nio.file.Path;

public final class GeneratedPackRepository {
    private static final String PACK_ID = TectonicDiscWorld.MOD_ID + ":generated";

    private GeneratedPackRepository() {}

    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }

        TectonicDiscWorldForgeConfig.apply();

        final Path packRoot;
        try {
            packRoot = GeneratedPackWriter.writePack(FMLPaths.CONFIGDIR.get());
        } catch (IOException e) {
            TectonicDiscWorld.LOGGER.error("Failed to write Tectonic Disc World generated data pack", e);
            return;
        }

        event.addRepositorySource(consumer -> {
            Pack.ResourcesSupplier supplier = packId -> new PathPackResources(packId, packRoot, true);

            Pack pack = Pack.readMetaAndCreate(
                    PACK_ID,
                    Component.literal("Tectonic Disc World").withStyle(ChatFormatting.AQUA),
                    TectonicDiscWorldConfig.forceEnablePack,
                    supplier,
                    PackType.SERVER_DATA,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );

            if (pack != null) {
                consumer.accept(pack);
                TectonicDiscWorld.LOGGER.info("Registered Tectonic Disc World generated pack at {}", packRoot);
            } else {
                TectonicDiscWorld.LOGGER.error("Minecraft rejected the Tectonic Disc World generated pack at {}", packRoot);
            }
        });
    }
}
