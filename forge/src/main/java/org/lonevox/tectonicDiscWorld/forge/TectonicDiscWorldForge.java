package org.lonevox.tectonicDiscWorld.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lonevox.tectonicDiscWorld.TectonicDiscWorld;

@Mod(TectonicDiscWorld.MOD_ID)
public final class TectonicDiscWorldForge {
	public TectonicDiscWorldForge() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		EventBuses.registerModEventBus(TectonicDiscWorld.MOD_ID, modEventBus);

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TectonicDiscWorldForgeConfig.SPEC);
		modEventBus.addListener(this::onAddPackFinders);

		TectonicDiscWorld.init();
	}

	private void onAddPackFinders(AddPackFindersEvent event) {
		GeneratedPackRepository.register(event);
	}
}
