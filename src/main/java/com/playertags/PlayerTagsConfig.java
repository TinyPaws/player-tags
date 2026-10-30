package com.playertags;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("playertags")
public interface PlayerTagsConfig extends Config
{
    @ConfigItem(
        keyName = "showOwnTag",
        name = "Show my tag",
        description = "Allow your custom information to appear when someone hovers over you",
        position = 0
    )
    default boolean showOwnTag()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showOwnTagLocally",
            name = "Show my own tag locally",
            description = "Only controls whether you see your own tag",
            position = 1
    )
    default boolean showOwnTagLocally()
    {
        return false;
    }

    @ConfigItem(
        keyName = "ownTag",
        name = "My custom tag",
        description = "Your custom tag text",
        position = 2
    )
    default String ownTag()
    {
        return "";
    }

    @ConfigItem(
        keyName = "ownPronouns",
        name = "My pronouns",
        description = "Your pronouns (for example, they/them)",
        position = 3
    )
    default String ownPronouns()
    {
        return "";
    }

    @ConfigItem(
        keyName = "showOtherTags",
        name = "Show other players",
        description = "Show information for players with a local override",
        position = 4
    )
    default boolean showOtherTags()
    {
        return true;
    }

    @ConfigItem(
        keyName = "localOverrides",
        name = "Local player overrides",
        description = "One player per line: PlayerName=Custom tag|pronouns (pronouns optional)",
        position = 5
    )
    default String localOverrides()
    {
        return "";
    }

    @ConfigItem(
        keyName = "hiddenPlayers",
        name = "Hidden players",
        description = "Comma-separated player names whose information should be hidden",
        position = 6
    )
    default String hiddenPlayers()
    {
        return "";
    }

    @ConfigItem(
        keyName = "tagColor",
        name = "Text colour",
        description = "Colour used for the custom tag and pronouns in the hover box",
        position = 7
    )
    default Color tagColor()
    {
        return new Color(220, 205, 255);
    }

    @Range(min = 10, max = 24)
    @ConfigItem(
        keyName = "fontSize",
        name = "Text size",
        description = "Text size inside the hover box",
        position = 8
    )
    default int fontSize()
    {
        return 12;
    }
}
