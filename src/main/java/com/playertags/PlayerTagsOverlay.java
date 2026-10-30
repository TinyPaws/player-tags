package com.playertags;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Draws a compact information box when the mouse is over a tagged player model. */
public class PlayerTagsOverlay extends Overlay
{
    private static final int BOX_PADDING = 9;
    private static final int CURSOR_OFFSET = 16;
    private static final Color BACKGROUND = new Color(28, 25, 35, 238);
    private static final Color BORDER = new Color(155, 130, 190, 230);
    private static final Color LABEL = new Color(190, 185, 200);

    private final Client client;
    private final PlayerTagsPlugin plugin;

    @Inject
    private PlayerTagsOverlay(Client client, PlayerTagsPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Point mouse = client.getMouseCanvasPosition();
        if (mouse == null)
        {
            return null;
        }

        Player hoveredPlayer = findHoveredPlayer(mouse);
        if (hoveredPlayer == null)
        {
            return null;
        }

        PlayerTagsPlugin.PlayerInfo info = plugin.getPlayerInfo(hoveredPlayer);
        if (info == null)
        {
            return null;
        }

        drawTooltip(graphics, mouse, info);
        return null;
    }

    private Player findHoveredPlayer(Point mouse)
    {
        // Iterate backwards so the last-drawn/foreground actor wins if model
        // hulls overlap on screen.
        java.util.List<Player> players = client.getPlayers();
        for (int i = players.size() - 1; i >= 0; i--)
        {
            Player player = players.get(i);
            if (player == null || player.getName() == null)
            {
                continue;
            }

            if (player == client.getLocalPlayer()
                    && !plugin.getConfig().showOwnTagLocally())
            {
                continue;
            }

            Shape hull = player.getConvexHull();
            if (hull != null && hull.contains(mouse.getX(), mouse.getY()))
            {
                return player;
            }
        }
        return null;
    }

    private void drawTooltip(Graphics2D parent, Point mouse, PlayerTagsPlugin.PlayerInfo info)
    {
        Graphics2D graphics = (Graphics2D) parent.create();
        try
        {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Font oldFont = graphics.getFont();
            Font valueFont = oldFont.deriveFont(Font.BOLD, (float) plugin.getConfig().fontSize());
            Font labelFont = oldFont.deriveFont(Font.PLAIN, Math.max(9f, plugin.getConfig().fontSize() - 2f));

            String tag = info.getTag();
            String pronouns = info.getPronouns();
            FontMetrics valueMetrics = graphics.getFontMetrics(valueFont);
            FontMetrics labelMetrics = graphics.getFontMetrics(labelFont);

            int valueColumnWidth = labelMetrics.stringWidth("PRONOUNS") + 8;
            int widestValue = Math.max(
                tag.isEmpty() ? 0 : valueMetrics.stringWidth(tag),
                pronouns.isEmpty() ? 0 : valueMetrics.stringWidth(pronouns));
            int contentWidth = valueColumnWidth + widestValue;

            int lineHeight = Math.max(valueMetrics.getHeight(), labelMetrics.getHeight());
            int rows = (tag.isEmpty() ? 0 : 1) + (pronouns.isEmpty() ? 0 : 1);
            int boxWidth = contentWidth + BOX_PADDING * 2;
            int boxHeight = rows * lineHeight + BOX_PADDING * 2 + (rows > 1 ? 3 : 0);

            int x = mouse.getX() + CURSOR_OFFSET;
            int y = mouse.getY() + CURSOR_OFFSET;
            int canvasWidth = client.getCanvasWidth();
            int canvasHeight = client.getCanvasHeight();
            if (x + boxWidth > canvasWidth - 4)
            {
                x = mouse.getX() - boxWidth - CURSOR_OFFSET;
            }
            if (y + boxHeight > canvasHeight - 4)
            {
                y = mouse.getY() - boxHeight - CURSOR_OFFSET;
            }
            x = Math.max(4, x);
            y = Math.max(4, y);

            graphics.setColor(BACKGROUND);
            graphics.fillRoundRect(x, y, boxWidth, boxHeight, 9, 9);
            graphics.setColor(BORDER);
            graphics.drawRoundRect(x, y, boxWidth, boxHeight, 9, 9);

            int baseline = y + BOX_PADDING + valueMetrics.getAscent();
            int labelX = x + BOX_PADDING;
            int valueX = labelX + labelMetrics.stringWidth("PRONOUNS") + 8;

            if (!tag.isEmpty())
            {
                graphics.setFont(labelFont);
                graphics.setColor(LABEL);
                graphics.drawString("TAG", labelX, baseline - (valueMetrics.getAscent() - labelMetrics.getAscent()) / 2);
                graphics.setFont(valueFont);
                graphics.setColor(plugin.getConfig().tagColor());
                graphics.drawString(tag, valueX, baseline);
                baseline += lineHeight + 3;
            }

            if (!pronouns.isEmpty())
            {
                graphics.setFont(labelFont);
                graphics.setColor(LABEL);
                graphics.drawString("PRONOUNS", labelX, baseline - (valueMetrics.getAscent() - labelMetrics.getAscent()) / 2);
                graphics.setFont(valueFont);
                graphics.setColor(plugin.getConfig().tagColor());
                graphics.drawString(pronouns, valueX, baseline);
            }
        }
        finally
        {
            graphics.dispose();
        }
    }
}
