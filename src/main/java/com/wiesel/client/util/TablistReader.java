package com.wiesel.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.Collection;

public class TablistReader {

    public static String getCurrentArea() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.networkHandler == null) {
            return null;
        }

        Collection<PlayerListEntry> playerList = mc.player.networkHandler.getPlayerList();

        for (PlayerListEntry entry : playerList) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;

            String text = displayName.getString();

            // Look for "Area: {name}" pattern
            if (text.contains("Area:")) {
                String[] parts = text.split("Area:");
                if (parts.length > 1) {
                    String area = parts[1].trim();
                    // Remove any color codes or extra characters
                    area = stripFormatting(area);
                    return area;
                }
            }
        }

        return null;
    }

    private static String stripFormatting(String text) {
        // Remove color codes and special characters, get first word
        text = text.replaceAll("§[0-9a-fk-or]", ""); // Minecraft color codes
        text = text.replaceAll("[^a-zA-Z0-9 ]", ""); // Non-alphanumeric
        text = text.trim();

        // Get first word (the area name)
        String[] words = text.split("\\s+");
        if (words.length > 0) {
            return words[0].toLowerCase(); // Convert to lowercase for matching
        }

        return text.toLowerCase();
    }

    public static boolean isInWorld() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && mc.world != null;
    }
}
