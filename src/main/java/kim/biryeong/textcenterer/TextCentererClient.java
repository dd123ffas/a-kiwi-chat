package kim.biryeong.textcenterer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(value=EnvType.CLIENT)
public final class TextCentererClient
implements ClientModInitializer {
    public static final String MOD_ID = "text-centerer";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"text-centerer");

    public void onInitializeClient() {
        kim.biryeong.textcenterer.chat.ChatHistory.configure();
        Runtime.getRuntime().addShutdownHook(new Thread(kim.biryeong.textcenterer.chat.ChatHistory::save, "text-centerer-history-save"));
        LOGGER.info("A kiwi chat initialized");
    }
}
