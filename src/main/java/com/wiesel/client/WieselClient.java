package com.wiesel.client;

import net.fabricmc.api.ClientModInitializer;
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

        registerEvents();

        LOGGER.info("{} initialized successfully", MOD_NAME);
    }

    private void registerEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var player = client.player;
            if (player == null) return;

            String playerName = player.getName().getString();
            client.execute(() -> {
                player.sendMessage(createWelcomeMessage(playerName), false);
            });
        });
    }

    private Text createWelcomeMessage(String playerName) {
        Text message = Text.literal("");

        // "Wiesel Client" with darker toxic green gradient effect
        int[] colors = {
            0x00DD00, 0x00E000, 0x00E300, 0x00E600,
            0x00E900, 0x00EC00, 0x00EF00, 0x00F200,
            0x00F500, 0x00F800, 0x00FB00, 0x00FE00,
            0x00DD00
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
