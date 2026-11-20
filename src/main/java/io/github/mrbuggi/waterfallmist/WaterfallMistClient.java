package io.github.mrbuggi.waterfallmist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaterfallMistClient implements ClientModInitializer {
    public static final String MOD_ID = "waterfallmist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final WaterfallManager waterfallManager = new WaterfallManager();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(waterfallManager::tick);
        LOGGER.info("Waterfall Mist initialized");
    }
}
