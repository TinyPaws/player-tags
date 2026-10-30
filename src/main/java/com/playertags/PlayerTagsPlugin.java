
package com.playertags;

import com.google.inject.Provides;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
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
    private static final Logger logger =
            Logger.getLogger(PlayerTagsPlugin.class.getName());

    private static final long CACHE_TIME_MS = 5 * 60 * 1000;
    private static final long FAILED_LOOKUP_TIME_MS = 30 * 1000;
    private static final long PUBLISH_RETRY_MS = 60 * 1000;

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PlayerTagsConfig config;

    @Inject
    private PlayerTagsOverlay overlay;

    @Inject
    private SharedTagService sharedTagService;

    private final ExecutorService networkExecutor =
            Executors.newSingleThreadExecutor(r ->
            {
                Thread thread = new Thread(r, "player-tags-network");
                thread.setDaemon(true);
                return thread;
            });

    private final Map<String, CachedInfo> sharedCache =
            new ConcurrentHashMap<>();

    private final Set<String> lookupsInProgress =
            ConcurrentHashMap.newKeySet();

    private volatile String lastPublishedSignature;
    private volatile String lastAttemptSignature;
    private volatile long lastPublishAttemptTime;

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
        networkExecutor.shutdownNow();
        sharedCache.clear();
        lookupsInProgress.clear();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        publishOwnTagIfNeeded();
    }

    private void publishOwnTagIfNeeded()
    {
        Player localPlayer = client.getLocalPlayer();

        if (localPlayer == null || localPlayer.getName() == null)
        {
            return;
        }

        String playerName = localPlayer.getName();
        boolean enabled = config.showOwnTag();

        String tag = enabled ? cleanText(config.ownTag()) : "";
        String pronouns = enabled ? cleanText(config.ownPronouns()) : "";

        String signature = playerName + "\0" + enabled
                + "\0" + tag + "\0" + pronouns;

        if (signature.equals(lastPublishedSignature))
        {
            return;
        }

        long now = System.currentTimeMillis();

        if (signature.equals(lastAttemptSignature)
                && now - lastPublishAttemptTime < PUBLISH_RETRY_MS)
        {
            return;
        }

        lastAttemptSignature = signature;
        lastPublishAttemptTime = now;

        networkExecutor.submit(() ->
        {
            try
            {
                sharedTagService.publishPlayerTag(
                        playerName,
                        tag,
                        pronouns
                );

                lastPublishedSignature = signature;
            }
            catch (Exception e)
            {
                logger.log(Level.WARNING,
                        "Could not publish Player Tags information", e);
            }
        });
    }

    PlayerInfo getPlayerInfo(Player player)
    {
        if (player == null || player.getName() == null)
        {
            return null;
        }

        Player localPlayer = client.getLocalPlayer();

        // Your own tooltip is controlled by a local-only setting.
        if (player == localPlayer)
        {
            if (!config.showOwnTagLocally())
            {
                return null;
            }

            return createPlayerInfo(
                    config.ownTag(),
                    config.ownPronouns()
            );
        }

        if (!config.showOtherTags())
        {
            return null;
        }

        String playerName = normalizeName(player.getName());

        // Hidden players take priority.
        if (isPlayerHidden(playerName))
        {
            return null;
        }

        // Local overrides take priority over shared tags.
        PlayerInfo override = getLocalOverride(playerName);

        if (override != null)
        {
            return override;
        }

        // Return cached shared information, if available.
        CachedInfo cached = sharedCache.get(playerName);

        if (cached != null)
        {
            if (System.currentTimeMillis() < cached.expiresAt)
            {
                return cached.info;
            }

            sharedCache.remove(playerName, cached);
        }

        // Queue at most one lookup per player.
        requestSharedLookup(playerName);

        // The tooltip will appear once the lookup completes.
        return null;
    }

    private void requestSharedLookup(String playerName)
    {
        if (!lookupsInProgress.add(playerName))
        {
            return;
        }

        networkExecutor.submit(() ->
        {
            try
            {
                JsonObject result =
                        sharedTagService.getPlayerTag(playerName);

                PlayerInfo info = null;

                if (result != null)
                {
                    String tag = getJsonString(result, "tag");
                    String pronouns = getJsonString(result, "pronouns");

                    info = createPlayerInfo(tag, pronouns);
                }

                long cacheTime = info == null
                        ? FAILED_LOOKUP_TIME_MS
                        : CACHE_TIME_MS;

                sharedCache.put(
                        playerName,
                        new CachedInfo(
                                info,
                                System.currentTimeMillis() + cacheTime
                        )
                );
            }
            catch (Exception e)
            {
                logger.log(Level.FINE,
                        "Shared tag lookup failed for " + playerName, e);

                sharedCache.put(
                        playerName,
                        new CachedInfo(
                                null,
                                System.currentTimeMillis() + FAILED_LOOKUP_TIME_MS
                        )
                );
            }
            finally
            {
                lookupsInProgress.remove(playerName);
            }
        });
    }

    private String getJsonString(JsonObject object, String key)
    {
        if (!object.has(key) || object.get(key).isJsonNull())
        {
            return "";
        }

        return object.get(key).getAsString();
    }

    private PlayerInfo getLocalOverride(String playerName)
    {
        String overrides = config.localOverrides();

        if (overrides == null || overrides.isEmpty())
        {
            return null;
        }

        for (String line : overrides.split("\\R"))
        {
            String[] entry = line.split("=", 2);

            if (entry.length != 2)
            {
                continue;
            }

            String name = normalizeName(entry[0]);

            if (!name.equals(playerName))
            {
                continue;
            }

            String[] values = entry[1].split("\\|", 2);

            String tag = values[0].trim();
            String pronouns = values.length > 1
                    ? values[1].trim()
                    : "";

            return createPlayerInfo(tag, pronouns);
        }

        return null;
    }

    private boolean isPlayerHidden(String playerName)
    {
        String hidden = config.hiddenPlayers();

        if (hidden == null || hidden.isEmpty())
        {
            return false;
        }

        for (String name : hidden.split(","))
        {
            if (normalizeName(name).equals(playerName))
            {
                return true;
            }
        }

        return false;
    }

    private String normalizeName(String name)
    {
        return name == null ? ""
                : name.replace('\u00A0', ' ')
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String cleanText(String text)
    {
        if (text == null)
        {
            return "";
        }

        String cleaned = text
                .replaceAll("[\\r\\n\\t]", " ")
                .trim();

        return cleaned.length() > 50
                ? cleaned.substring(0, 50)
                : cleaned;
    }

    private PlayerInfo createPlayerInfo(String tag, String pronouns)
    {
        String cleanTag = cleanText(tag);
        String cleanPronouns = cleanText(pronouns);

        if (cleanTag.isEmpty() && cleanPronouns.isEmpty())
        {
            return null;
        }

        return new PlayerInfo(cleanTag, cleanPronouns);
    }

    PlayerTagsConfig getConfig()
    {
        return config;
    }

    private static final class CachedInfo
    {
        private final PlayerInfo info;
        private final long expiresAt;

        private CachedInfo(PlayerInfo info, long expiresAt)
        {
            this.info = info;
            this.expiresAt = expiresAt;
        }
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
