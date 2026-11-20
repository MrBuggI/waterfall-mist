package io.github.mrbuggi.waterfallmist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaterfallMistClient implements ClientModInitializer {
    public static final String MOD_ID = "waterfallmist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final DefaultParticleType MIST = FabricParticleTypes.simple(true);

    private final WaterfallManager waterfallManager = new WaterfallManager();

    @Override
    public void onInitializeClient() {
        WaterfallConfig.load();

        Registry.register(Registries.PARTICLE_TYPE, new Identifier(MOD_ID, "mist"), MIST);
        ParticleFactoryRegistry.getInstance().register(MIST, WhiteSmokeParticle.Factory::new);

        ClientTickEvents.END_CLIENT_TICK.register(waterfallManager::tick);
        LOGGER.info("Waterfall Mist initialized with config (mistOpacity={}, minFallHeight={})",
                WaterfallConfig.get().mistOpacity, WaterfallConfig.get().minFallHeight);
    }
}
