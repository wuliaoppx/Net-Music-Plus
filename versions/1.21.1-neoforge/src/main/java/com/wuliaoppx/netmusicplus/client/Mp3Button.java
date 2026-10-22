package com.wuliaoppx.netmusicplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Mp3Theme theme = Mp3HudConfig.theme();
        boolean hover = this.isHoveredOrFocused();
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();

        ResourceLocation skin = Mp3Textures.gui(!this.active ? "button_disabled" : hover ? "button_hover" : "button");
        if (skin != null) {
            blitNineSlice(graphics, skin, x, y, width, height);
        } else {
            int background = hover ? theme.buttonHover() : theme.button();
            int edge = hover ? theme.accent() : theme.border();
            // one pixel chamfer on the corners, matching the panel
            graphics.fill(x + 1, y, x + width - 1, y + height, background);
            graphics.fill(x, y + 1, x + width, y + height - 1, background);
            if (theme.style() == Mp3Theme.Style.GLOW) {
                graphics.renderOutline(x - 1, y - 1, width + 2, height + 2, 0x40 << 24 | (theme.accent() & 0xFFFFFF));
            }
            graphics.renderOutline(x, y, width, height, edge);
        }

        ResourceLocation icon = this.iconName == null ? null : Mp3Textures.gui(this.iconName);
        if (icon != null) {
            graphics.blit(icon, x + (width - ICON_SIZE) / 2, y + (height - ICON_SIZE) / 2, 0, 0,
                    ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        } else {
            graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                    x + width / 2, y + (height - 8) / 2, theme.text());
        }
    }

    /** Stretches a 20x20 button skin to any button size while keeping the 4 pixel border intact. */
    private static void blitNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        int slice = Math.min(SLICE, Math.min(width, height) / 2);
        int inner = width - slice * 2;
        int innerHeight = height - slice * 2;
        graphics.blit(texture, x, y, 0, 0, slice, slice, SKIN_SIZE, SKIN_SIZE);
        graphics.blit(texture, x + width - slice, y, SKIN_SIZE - slice, 0, slice, slice, SKIN_SIZE, SKIN_SIZE);
        graphics.blit(texture, x, y + height - slice, 0, SKIN_SIZE - slice, slice, slice, SKIN_SIZE, SKIN_SIZE);
        graphics.blit(texture, x + width - slice, y + height - slice, SKIN_SIZE - slice, SKIN_SIZE - slice,
                slice, slice, SKIN_SIZE, SKIN_SIZE);
        graphics.blit(texture, x + slice, y, slice, 0, inner, slice, SKIN_SIZE, SKIN_SIZE);
        graphics.blit(texture, x + slice, y + height - slice, slice, SKIN_SIZE - slice, inner, slice,
                SKIN_SIZE, SKIN_SIZE);
        graphics.blit(texture, x, y + slice, 0, slice, slice, innerHeight, SKIN_SIZE, SKIN_SIZE);
        graphics.blit(texture, x + width - slice, y + slice, SKIN_SIZE - slice, slice, slice, innerHeight,
                SKIN_SIZE, SKIN_SIZE);
        graphics.blit(texture, x + slice, y + slice, slice, slice, inner, innerHeight, SKIN_SIZE, SKIN_SIZE);
    }
}
