package com.wuliaoppx.netmusicplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * A button drawn with the current {@link Mp3Theme} instead of the vanilla grey texture, so switching the theme also
 * changes the buttons and not only the panel colours. Each button can carry a short hover hint (one component per line)
 * that explains what it does, including the keybind gestures.
 * <p>
 * A resource pack can take over the look: {@code textures/gui/button.png}, {@code button_hover.png},
 * {@code button_disabled.png} (20x20, nine-sliced with a 4 pixel border) and the optional icons
 * {@code icon_play.png} / {@code icon_pause.png} / {@code icon_prev.png} / {@code icon_next.png} (12x12).
 */
public class Mp3Button extends Button {
    private static final int SLICE = 4;
    private static final int SKIN_SIZE = 20;
    private static final int ICON_SIZE = 12;

    private List<Component> tooltip = List.of();
    private String iconName;

    public Mp3Button(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
    }

    /** Sets the hover hint; every argument is one line. */
    public Mp3Button tooltip(Component... lines) {
        this.tooltip = List.of(lines);
        return this;
    }

    public List<Component> tooltipLines() {
        return this.tooltip;
    }

    /** Name of an optional 12x12 icon in {@code textures/gui/}; when absent the message text is drawn instead. */
    public Mp3Button icon(String name) {
        this.iconName = name;
        return this;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Mp3Theme theme = Mp3HudConfig.theme();
        boolean hover = this.isHoveredOrFocused();
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();

        Identifier skin = Mp3Textures.gui(!this.active ? "button_disabled" : hover ? "button_hover" : "button");
        if (skin != null) {
            blitNineSlice(graphics, skin, x, y, width, height);
        } else {
            int background = hover ? theme.buttonHover() : theme.button();
            int edge = hover ? theme.accent() : theme.border();
            // one pixel chamfer on the corners, matching the panel
            graphics.fill(x + 1, y, x + width - 1, y + height, background);
            graphics.fill(x, y + 1, x + width, y + height - 1, background);
            if (theme.style() == Mp3Theme.Style.GLOW) {
                graphics.outline(x - 1, y - 1, width + 2, height + 2, 0x40 << 24 | (theme.accent() & 0xFFFFFF));
            }
            graphics.outline(x, y, width, height, edge);
        }

        Identifier icon = this.iconName == null ? null : Mp3Textures.gui(this.iconName);
        if (icon != null) {
            blitRegion(graphics, icon, x + (width - ICON_SIZE) / 2, y + (height - ICON_SIZE) / 2,
                    ICON_SIZE, ICON_SIZE, 0, 0);
        } else {
            graphics.centeredText(Minecraft.getInstance().font, this.getMessage(),
                    x + width / 2, y + (height - 8) / 2, theme.text());
        }
    }

    /**
     * Copies one pixel rectangle of a texture. Game code works in pixels while the render state works in normalised
     * texture coordinates, which is why the source rectangle is divided here.
     */
    private static void blitRegion(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height,
                                   int u, int v) {
        graphics.blit(texture, x, y, x + width, y + height,
                u / (float) SKIN_SIZE, (u + width) / (float) SKIN_SIZE,
                v / (float) SKIN_SIZE, (v + height) / (float) SKIN_SIZE);
    }

    /** Stretches a 20x20 button skin to any button size while keeping the 4 pixel border intact. */
    private static void blitNineSlice(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height) {
        int slice = Math.min(SLICE, Math.min(width, height) / 2);
        int inner = width - slice * 2;
        int innerHeight = height - slice * 2;
        blitRegion(graphics, texture, x, y, slice, slice, 0, 0);
        blitRegion(graphics, texture, x + width - slice, y, slice, slice, SKIN_SIZE - slice, 0);
        blitRegion(graphics, texture, x, y + height - slice, slice, slice, 0, SKIN_SIZE - slice);
        blitRegion(graphics, texture, x + width - slice, y + height - slice, slice, slice,
                SKIN_SIZE - slice, SKIN_SIZE - slice);
        blitRegion(graphics, texture, x + slice, y, inner, slice, slice, 0);
        blitRegion(graphics, texture, x + slice, y + height - slice, inner, slice, slice, SKIN_SIZE - slice);
        blitRegion(graphics, texture, x, y + slice, slice, innerHeight, 0, slice);
        blitRegion(graphics, texture, x + width - slice, y + slice, slice, innerHeight, SKIN_SIZE - slice, slice);
        blitRegion(graphics, texture, x + slice, y + slice, inner, innerHeight, slice, slice);
    }
}
