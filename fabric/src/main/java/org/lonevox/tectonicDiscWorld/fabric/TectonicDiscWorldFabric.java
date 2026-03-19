package org.lonevox.tectonicDiscWorld.fabric;

import net.fabricmc.api.ModInitializer;
import org.lonevox.tectonicDiscWorld.TectonicDiscWorld;

public final class TectonicDiscWorldFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		TectonicDiscWorldFabricConfig.load();
		TectonicDiscWorld.init();
	}
}
