package com.wiesel.client;

import com.wiesel.client.config.ConfigManager;
import com.wiesel.client.pathfinder.AutoMapLoader;
import com.wiesel.client.pathfinder.PathWalker;
import com.wiesel.client.pathfinder.PathfinderManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WieselClient implements ClientModInitializer {
    public static final String MOD_ID = "wieselclient";
    public static final String MOD_NAME = "Wiesel Client";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {}", MOD_NAME);

        // Load config
        ConfigManager.load();

        // Initialize pathfinder
        PathfinderManager.initialize();

        // Register events
        registerEvents();

        LOGGER.info("{} initialized successfully", MOD_NAME);
    }

    private void registerEvents() {
        // Welcome message on server join
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var player = client.player;
            if (player == null) return;

            String playerName = player.getName().getString();
            client.execute(() -> {
                player.sendMessage(createWelcomeMessage(playerName), false);
            });
        });

        // Disconnect event
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PathWalker.stopWalking();
            AutoMapLoader.reset();
        });

        // Client tick event for path walking and auto map loading
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PathWalker.tick();
            AutoMapLoader.tick();
        });
    }

    private Text createWelcomeMessage(String playerName) {
        Text message = Text.literal("");

        // "Wiesel Client" with darker green gradient effect with noticeable variations
        int[] colors = {
            0x00AA00, 0x00B500, 0x00C000, 0x00CB00,
            0x00D600, 0x00E100, 0x00D600, 0x00CB00,
            0x00C000, 0x00B500, 0x00AA00, 0x009F00,
            0x00AA00
        };

        String text = "Wiesel Client";
        for (int i = 0; i < text.length(); i++) {
            final int index = i;
            message = ((net.minecraft.text.MutableText) message).append(
                Text.literal(String.valueOf(text.charAt(i)))
                    .styled(style -> style.withColor(colors[index % colors.length]))
            );
        }

        // " >> " in dark grey
        message = ((net.minecraft.text.MutableText) message).append(
            Text.literal(" >> ").styled(style -> style.withColor(0x555555))
        );

        // "Hello [PlayerName]" - both white
        message = ((net.minecraft.text.MutableText) message).append(
            Text.literal("Hello ").styled(style -> style.withColor(0xFFFFFF))
        );

        message = ((net.minecraft.text.MutableText) message).append(
            Text.literal(playerName).styled(style -> style.withColor(0xFFFFFF))
        );

        return message;
    }
}
