package org.lonevox.tectonicDiscWorld.fabric.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.fabricmc.loader.api.FabricLoader;
import org.lonevox.tectonicDiscWorld.GeneratedPackWriter;
import org.lonevox.tectonicDiscWorld.TectonicDiscWorld;
import org.lonevox.tectonicDiscWorld.TectonicDiscWorldConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

@Mixin(PackRepository.class)
public class PackRepositoryMixin {
    @Final
	@Shadow
    private Set<RepositorySource> sources;

    @Unique
    private boolean tectonicDiscWorld$sourceAdded = false;

    @Inject(method = "reload", at = @At("HEAD"))
    private void tectonicDiscWorld$addPackSource(CallbackInfo ci) {
        if (tectonicDiscWorld$sourceAdded) {
            return;
        }
        tectonicDiscWorld$sourceAdded = true;
        sources.add(consumer -> {
            final Path packRoot;
            try {
                packRoot = GeneratedPackWriter.writePack(FabricLoader.getInstance().getConfigDir());
            } catch (IOException e) {
                TectonicDiscWorld.LOGGER.error("Failed to write Tectonic Disc World generated data pack", e);
                return;
            }

            Pack.ResourcesSupplier supplier = packId -> new PathPackResources(packId, packRoot, true);

            Pack pack = Pack.readMetaAndCreate(
                    TectonicDiscWorld.MOD_ID + ":generated",
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
