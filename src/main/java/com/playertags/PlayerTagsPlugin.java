package com.playertags;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "Player Tags",
    description = "Shows custom player tags and pronouns in a hover tooltip",
    tags = {"players", "custom", "tags", "pronouns", "tooltip"}
)
public class PlayerTagsPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PlayerTagsConfig config;

    @Inject
    private PlayerTagsOverlay overlay;

    @Provides
    PlayerTagsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PlayerTagsConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
    }

    PlayerInfo getPlayerInfo(Player player)
    {
        if (player == null || player.getName() == null)
        {
            return null;
        }

        Player localPlayer = client.getLocalPlayer();
        String playerName = normalize(player.getName());
        String localName = localPlayer == null ? "" : normalize(localPlayer.getName());

        if (playerName.equals(localName))
        {
            if (!config.showOwnTag())
            {
                return null;
            }
            return makeInfo(config.ownTag(), config.ownPronouns());
        }

        if (!config.showOtherTags() || isHidden(playerName))
        {
            return null;
        }

        return findOverride(playerName);
    }

    private PlayerInfo findOverride(String normalizedName)
    {
        String mappings = config.localOverrides();
        if (mappings == null || mappings.trim().isEmpty())
        {
            return null;
        }

        for (String line : mappings.split("\\R"))
        {
            int separator = line.indexOf('=');
            if (separator <= 0)
            {
                continue;
            }

            String name = normalize(line.substring(0, separator).trim());
            if (!normalizedName.equals(name))
            {
                continue;
            }

            String[] values = line.substring(separator + 1).split("\\|", 2);
            String tag = values.length > 0 ? values[0] : "";
            String pronouns = values.length > 1 ? values[1] : "";
            return makeInfo(tag, pronouns);
        }
        return null;
    }

    private boolean isHidden(String normalizedName)
    {
        String hidden = config.hiddenPlayers();
        if (hidden == null)
        {
            return false;
        }

        for (String name : hidden.split(","))
        {
            if (normalizedName.equals(normalize(name.trim())))
            {
                return true;
            }
        }
        return false;
    }

    private static PlayerInfo makeInfo(String tag, String pronouns)
    {
        String cleanTag = cleanText(tag);
        String cleanPronouns = cleanText(pronouns);
        if (cleanTag.isEmpty() && cleanPronouns.isEmpty())
        {
            return null;
        }
        return new PlayerInfo(cleanTag, cleanPronouns);
    }

    private static String cleanText(String text)
    {
        if (text == null)
        {
            return "";
        }
        String cleaned = text.replaceAll("[\\r\\n\\t]", " ").trim();
        return cleaned.length() > 32 ? cleaned.substring(0, 32) : cleaned;
    }

    private static String normalize(String name)
    {
        return name == null ? "" : name.replace('\u00A0', ' ').trim().toLowerCase();
    }

    PlayerTagsConfig getConfig()
    {
        return config;
    }

    static final class PlayerInfo
    {
        private final String tag;
        private final String pronouns;

        private PlayerInfo(String tag, String pronouns)
        {
            this.tag = tag;
            this.pronouns = pronouns;
        }

        String getTag()
        {
            return tag;
        }

        String getPronouns()
        {
            return pronouns;
        }
    }
}
