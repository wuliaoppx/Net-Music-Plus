package com.wuliaoppx.netmusicplus.client;

import com.wuliaoppx.netmusicplus.Mp3Menu;
import com.wuliaoppx.netmusicplus.Mp3State;
import com.wuliaoppx.netmusicplus.Mp3Support;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The MP3 player GUI: the disc grid (which grows with every installed storage expansion), the expansion slot, the
 * transport buttons and the switches for the "now playing" overlay. The overlay position is edited in place: the
 * {@code HUD} button hides the whole panel and dims the game so the player drags the real overlay around, instead of
 * opening a second screen (replacing an open container screen would close the menu).
 * <p>
 * The panel itself can be replaced by a resource pack: if {@code assets/netmusicplus/textures/gui/mp3_player_<upgrades>.png}
 * exists it is drawn as the whole background (one texture per storage-upgrade state, so the panel height always
 * matches), otherwise the panel is painted procedurally with the selected theme.
 */
public class Mp3Screen extends AbstractContainerScreen<Mp3Menu> {
    private static final int PREVIEW_Y = 55;
    /** Size of the little theme chip in the title bar. */
    private static final int THEME_WIDTH = 16;
    private static final int THEME_HEIGHT = 12;

    private Mp3Button modeButton;
    private Mp3Button outputButton;
    private Mp3Button playButton;
    private Mp3Button nowPlayingButton;
    private Mp3Button lyricsButton;

    private boolean editingHud;
    private boolean draggingHud;
    private int hudOffsetX;
    private int hudOffsetY;
    /** Cached lookup: the resource-pack panel texture, or {@code null} to paint the panel ourselves. */
    private Identifier guiTexture;
    private boolean guiTextureResolved;

    public Mp3Screen(Mp3Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, Mp3Menu.PANEL_WIDTH, menu.imageHeight());
        // re-resolve the optional resource-pack art every time the screen opens (packs may have changed since)
        Mp3Textures.clear();
        this.titleLabelX = 10;
        this.titleLabelY = 6;
        this.inventoryLabelX = 12;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        int row1 = this.topPos + Mp3Menu.buttonsY1(this.menu.capacity());
        int row2 = this.topPos + Mp3Menu.buttonsY2(this.menu.capacity());
        int left = this.leftPos;
        Component key = keyName();
        this.playButton = this.addRenderableWidget(new Mp3Button(left + 8, row1, 22, 20, Component.empty(),
                button -> this.click(Mp3Menu.BUTTON_PLAY))
                .tooltip(Component.translatable("gui.netmusicplus.button.play"),
                        Component.translatable("gui.netmusicplus.button.key_hold", key)));
        this.addRenderableWidget(new Mp3Button(left + 32, row1, 22, 20, Component.literal("<<"),
                button -> this.click(Mp3Menu.BUTTON_PREV)).icon("icon_prev")
                .tooltip(Component.translatable("gui.netmusicplus.button.prev"),
                        Component.translatable("gui.netmusicplus.button.key_double", key)));
        this.addRenderableWidget(new Mp3Button(left + 56, row1, 22, 20, Component.literal(">>"),
                button -> this.click(Mp3Menu.BUTTON_NEXT)).icon("icon_next")
                .tooltip(Component.translatable("gui.netmusicplus.button.next"),
                        Component.translatable("gui.netmusicplus.button.key_single", key)));
        this.modeButton = this.addRenderableWidget(new Mp3Button(left + 84, row1, 48, 20, Component.empty(),
                button -> this.click(Mp3Menu.BUTTON_MODE))
                .tooltip(Component.translatable("gui.netmusicplus.button.mode"),
                        Component.translatable("gui.netmusicplus.button.mode.hint")));
        this.outputButton = this.addRenderableWidget(new Mp3Button(left + 136, row1, 52, 20, Component.empty(),
                button -> this.click(Mp3Menu.BUTTON_OUTPUT))
                .tooltip(Component.translatable("gui.netmusicplus.button.output"),
                        Component.translatable("gui.netmusicplus.button.output.hint")));
        this.nowPlayingButton = this.addRenderableWidget(new Mp3Button(left + 8, row2, 58, 20, Component.empty(),
                button -> Mp3HudConfig.toggleNowPlaying())
                .tooltip(Component.translatable("gui.netmusicplus.button.now_playing")));
        this.lyricsButton = this.addRenderableWidget(new Mp3Button(left + 70, row2, 58, 20, Component.empty(),
                button -> Mp3HudConfig.toggleLyrics())
                .tooltip(Component.translatable("gui.netmusicplus.button.lyrics")));
        this.addRenderableWidget(new Mp3Button(left + 132, row2, 56, 20,
                Component.translatable("gui.netmusicplus.hud"), button -> this.startHudEditing())
                .tooltip(Component.translatable("gui.netmusicplus.button.hud"),
                        Component.translatable("gui.netmusicplus.button.hud.hint")));
        this.addRenderableWidget(new Mp3Button(left + Mp3Menu.PANEL_WIDTH - 24,
                this.topPos + Mp3Menu.upgradeY(this.menu.capacity()), 16, 16, Component.literal("-"),
                button -> this.click(Mp3Menu.BUTTON_REMOVE_EXPANSION))
                .tooltip(Component.translatable("gui.netmusicplus.button.remove"),
                        Component.translatable("gui.netmusicplus.button.remove.hint")));
    }

    /** The key the transport gestures are bound to, or a "not bound" placeholder. */
    private static Component keyName() {
        return Mp3Client.TOGGLE.isUnbound()
                ? Component.translatable("gui.netmusicplus.unbound")
                : Mp3Client.TOGGLE.getTranslatedKeyMessage();
    }

    private void click(int button) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
        }
    }

    private void startHudEditing() {
        this.editingHud = true;
        this.draggingHud = false;
        this.hudOffsetX = Mp3HudConfig.offsetX();
        this.hudOffsetY = Mp3HudConfig.offsetY();
    }

    private void stopHudEditing() {
        this.editingHud = false;
        this.draggingHud = false;
        Mp3HudConfig.setOffset(this.hudOffsetX, this.hudOffsetY);
    }

    /** Top left corner of the theme chip inside the title bar. */
    private int themeChipX() {
        return this.leftPos + this.imageWidth - THEME_WIDTH - 6;
    }

    private int themeChipY() {
        return this.topPos + 3;
    }

    private boolean hoveringChip(int mouseX, int mouseY) {
        int x = this.themeChipX();
        int y = this.themeChipY();
        return mouseX >= x && mouseX < x + THEME_WIDTH && mouseY >= y && mouseY < y + THEME_HEIGHT;
    }

    /**
     * The panel texture for the current capacity, or {@code null} when no resource pack provides one (the panel is then
     * painted procedurally). Resolved once per screen because the resource manager lookup is not free.
     */
    private Identifier guiTexture() {
        if (!this.guiTextureResolved) {
            this.guiTextureResolved = true;
            int upgrades = Mth.clamp((this.menu.capacity() - Mp3State.BASE_DISCS) / Mp3State.DISCS_PER_UPGRADE,
                    0, Mp3State.MAX_UPGRADES);
            this.guiTexture = Mp3Textures.gui("mp3_player_" + upgrades);
        }
        return this.guiTexture;
    }

    /** Draws the panel itself, behind the slots: the vanilla menu background is already on screen here. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.editingHud) {
            // nothing behind the overlay: no blurred background, no panel
            return;
        }
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        this.drawPanel(graphics, partialTick, mouseX, mouseY);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        Mp3Theme theme = Mp3HudConfig.theme();
        int left = this.leftPos;
        int top = this.topPos;
        int right = left + this.imageWidth;
        int bottom = top + this.imageHeight;

        graphics.fill(left + 3, top + 3, right + 3, bottom + 3, 0x50000000);
        Identifier texture = this.guiTexture();
        if (texture != null) {
            // a resource pack supplies the whole panel (frame, title band, insets and slot wells)
            graphics.blit(texture, left, top, this.imageWidth, this.imageHeight,
                    0.0F, 0.0F, this.imageWidth / 256.0F, 1.0F);
        } else {
            graphics.fill(left + 1, top, right - 1, bottom, theme.body());
            graphics.fill(left, top + 1, right, bottom - 1, theme.body());
            this.drawFrame(graphics, theme, left, top, right, bottom);

            int headerBottom = top + 16;
            graphics.fill(left + 2, top + 2, right - 2, headerBottom, theme.header());
            graphics.fill(left + 2, headerBottom, right - 2, headerBottom + 1, theme.border());

            int gridTop = top + Mp3Menu.GRID_Y - 3;
            int gridBottom = top + Mp3Menu.gridBottom(this.menu.capacity()) - 3;
            graphics.fill(left + 6, gridTop, right - 6, gridBottom, theme.inset());

            int inventoryY = top + Mp3Menu.inventoryY(this.menu.capacity());
            graphics.fill(left + 8, inventoryY - 6, right - 8, inventoryY - 5, theme.border());
            graphics.fill(left + 6, inventoryY - 4, right - 6, bottom - 6, theme.inset());
        }

        long millis = net.minecraft.util.Util.getMillis();
        int hoverAlpha = 0x60 + (int) (0x30 * (0.5 + 0.5 * Math.sin(millis / 260.0)));
        int playing = this.menu.playing() ? this.menu.selectedIndex() : -1;
        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            boolean container = i <= this.menu.capacity();
            if (texture == null) {
                this.drawSlot(graphics, theme, left + slot.x - 1, top + slot.y - 1, container);
            }
            if (i == playing) {
                // the slot that is playing right now breathes in the accent colour
                int alpha = 0x80 + (int) (0x7F * (0.5 + 0.5 * Math.sin(millis / 300.0)));
                graphics.outline(left + slot.x - 2, top + slot.y - 2, 20, 20,
                        (alpha << 24) | (theme.accent() & 0xFFFFFF));
            }
            if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                graphics.outline(left + slot.x - 2, top + slot.y - 2, 20, 20,
                        (hoverAlpha << 24) | (theme.accent() & 0xFFFFFF));
            }
        }

        this.renderThemeChip(graphics, theme, mouseX, mouseY);
    }

    private void drawFrame(GuiGraphicsExtractor graphics, Mp3Theme theme, int left, int top, int right, int bottom) {
        int accent = theme.accent() & 0xFFFFFF;
        switch (theme.style()) {
            case Mp3Theme.Style.BEVEL -> {
                // carved wooden frame: two pixels thick with a stud in every corner
                graphics.fill(left + 2, top, right - 2, top + 2, theme.border());
                graphics.fill(left + 2, bottom - 2, right - 2, bottom, theme.border());
                graphics.fill(left, top + 2, left + 2, bottom - 2, theme.border());
                graphics.fill(right - 2, top + 2, right, bottom - 2, theme.border());
                graphics.fill(left + 2, top + 2, right - 2, top + 3, 0x30FFFFFF);
                int[][] studs = {{left + 3, top + 3}, {right - 7, top + 3}, {left + 3, bottom - 7}, {right - 7, bottom - 7}};
                for (int[] stud : studs) {
                    graphics.fill(stud[0], stud[1], stud[0] + 4, stud[1] + 4, theme.accent());
                    graphics.fill(stud[0], stud[1], stud[0] + 2, stud[1] + 2, 0x60FFFFFF);
                }
            }
            case Mp3Theme.Style.GLOW -> {
                // neon: two translucent outlines outside the panel, then a bright one pixel edge
                graphics.outline(left - 3, top - 3, right - left + 6, bottom - top + 6, 0x20 << 24 | accent);
                graphics.outline(left - 2, top - 2, right - left + 4, bottom - top + 4, 0x38 << 24 | accent);
                graphics.outline(left - 1, top - 1, right - left + 2, bottom - top + 2, 0x60 << 24 | accent);
                graphics.fill(left + 1, top, right - 1, top + 1, theme.accent());
                graphics.fill(left + 1, bottom - 1, right - 1, bottom, theme.accent());
                graphics.fill(left, top + 1, left + 1, bottom - 1, theme.accent());
                graphics.fill(right - 1, top + 1, right, bottom - 1, theme.accent());
            }
            default -> {
                graphics.fill(left + 1, top, right - 1, top + 1, theme.border());
                graphics.fill(left + 1, bottom - 1, right - 1, bottom, theme.border());
                graphics.fill(left, top + 1, left + 1, bottom - 1, theme.border());
                graphics.fill(right - 1, top + 1, right, bottom - 1, theme.border());
            }
        }
    }

    private void drawSlot(GuiGraphicsExtractor graphics, Mp3Theme theme, int x, int y, boolean container) {
        if (!container) {
            graphics.fill(x, y, x + 18, y + 18, 0x30FFFFFF);
            graphics.outline(x, y, 18, 18, 0x40FFFFFF);
            return;
        }
        graphics.fill(x, y, x + 18, y + 18, theme.slotBackground());
        switch (theme.style()) {
            case Mp3Theme.Style.BEVEL -> {
                // sunken slot: light on the top/left, dark on the bottom/right
                graphics.fill(x, y, x + 18, y + 1, 0x50FFD9A0);
                graphics.fill(x, y, x + 1, y + 18, 0x50FFD9A0);
                graphics.fill(x, y + 17, x + 18, y + 18, 0x90000000);
                graphics.fill(x + 17, y, x + 18, y + 18, 0x90000000);
            }
            case Mp3Theme.Style.GLOW -> {
                graphics.outline(x, y, 18, 18, 0x50 << 24 | (theme.accent() & 0xFFFFFF));
                graphics.fill(x + 1, y + 1, x + 17, y + 2, 0x20 << 24 | (theme.accent() & 0xFFFFFF));
            }
            default -> {
                graphics.fill(x, y, x + 18, y + 1, theme.slotEdge());
                graphics.fill(x, y + 17, x + 18, y + 18, theme.slotEdge());
                graphics.fill(x, y, x + 1, y + 18, theme.slotEdge());
                graphics.fill(x + 17, y, x + 18, y + 18, theme.slotEdge());
            }
        }
    }

    private void renderThemeChip(GuiGraphicsExtractor graphics, Mp3Theme theme, int mouseX, int mouseY) {
        int x = this.themeChipX();
        int y = this.themeChipY();
        boolean hover = this.hoveringChip(mouseX, mouseY);
        graphics.fill(x, y, x + THEME_WIDTH, y + THEME_HEIGHT, hover ? 0x60FFFFFF : 0x30FFFFFF);
        graphics.outline(x, y, THEME_WIDTH, THEME_HEIGHT, theme.accent());
        for (int i = 0; i < 3; i++) {
            graphics.fill(x + 4, y + 3 + i * 3, x + THEME_WIDTH - 4, y + 4 + i * 3, theme.accent());
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.editingHud) {
            return;
        }
        Mp3Theme theme = Mp3HudConfig.theme();
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, theme.text(), false);
        Component right;
        if (this.hoveringChip(mouseX, mouseY)) {
            right = Component.translatable("gui.netmusicplus.theme",
                    Component.translatable("gui.netmusicplus.theme." + theme.id()));
        } else {
            ItemStack disc = this.menu.selectedDisc();
            String track = disc.isEmpty()
                    ? Component.translatable("gui.netmusicplus.no_track").getString()
                    : Mp3Support.trackName(disc);
            right = Component.literal(this.font.plainSubstrByWidth(track, 92));
        }
        graphics.text(this.font, right, this.imageWidth - 28 - this.font.width(right), 6, theme.muted(), false);
        int upgradeY = Mp3Menu.upgradeY(this.menu.capacity());
        graphics.text(this.font, Component.translatable("gui.netmusicplus.upgrades", this.menu.upgrades(),
                Mp3State.MAX_UPGRADES), 30, upgradeY + 2, theme.muted(), false);
        graphics.text(this.font, Component.translatable("gui.netmusicplus.discs",
                this.menu.filledSlots(), this.menu.capacity()), 30, upgradeY + 12, theme.muted(), false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.editingHud) {
            // hide the whole panel (and everything the game would blur behind it) so the overlay can be placed
            this.renderHudEditor(graphics, mouseX, mouseY);
            return;
        }
        if (this.modeButton != null) {
            this.modeButton.setMessage(this.menu.mode().displayName());
            this.outputButton.setMessage(Component.translatable(this.menu.external()
                    ? "gui.netmusicplus.output.out_loud" : "gui.netmusicplus.output.personal"));
            this.playButton.setMessage(Component.literal(this.menu.playing() ? "||" : ">"));
            this.playButton.icon(this.menu.playing() ? "icon_pause" : "icon_play");
            this.nowPlayingButton.setMessage(Component.translatable(Mp3HudConfig.showNowPlaying()
                    ? "gui.netmusicplus.hud.now_playing.on" : "gui.netmusicplus.hud.now_playing.off"));
            this.lyricsButton.setMessage(Component.translatable(Mp3HudConfig.showLyrics()
                    ? "gui.netmusicplus.hud.lyrics.on" : "gui.netmusicplus.hud.lyrics.off"));
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.renderButtonTooltips(graphics, mouseX, mouseY);
    }

    /** Hover hints for the themed buttons (queued last so they sit on top of everything else). */
    private void renderButtonTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (var child : this.children()) {
            if (child instanceof Mp3Button button && !button.tooltipLines().isEmpty()
                    && button.isMouseOver(mouseX, mouseY)) {
                graphics.setComponentTooltipForNextFrame(this.font, button.tooltipLines(), mouseX, mouseY);
                return;
            }
        }
    }

    private void renderHudEditor(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Mp3Theme theme = Mp3HudConfig.theme();
        graphics.fill(0, 0, this.width, this.height, 0x66000000);
        graphics.centeredText(this.font, Component.translatable("gui.netmusicplus.hud_position"),
                this.width / 2, 14, theme.text());
        graphics.centeredText(this.font, Component.translatable("gui.netmusicplus.hud_position.hint"),
                this.width / 2, 28, theme.muted());
        graphics.centeredText(this.font, Component.translatable("gui.netmusicplus.hud_position.done"),
                this.width / 2, this.height - 20, theme.accent());

        int centerX = this.width / 2 + this.hudOffsetX;
        int y = this.height - PREVIEW_Y + this.hudOffsetY;
        Component name = Mp3HudConfig.showNowPlaying()
                ? Component.translatable("hud.netmusicplus.now_playing", Component.translatable("gui.netmusicplus.demo_song"))
                : null;
        String lyric = Mp3HudConfig.showLyrics()
                ? Component.translatable("gui.netmusicplus.demo_lyric").getString()
                : null;
        Mp3Hud.drawPreview(graphics, name, lyric, centerX, y);

        int width = 0;
        if (name != null) {
            width = Math.max(width, this.font.width(name));
        }
        if (lyric != null) {
            width = Math.max(width, this.font.width(lyric));
        }
        int height = (name != null && lyric != null) ? 22 : 11;
        graphics.outline(centerX - width / 2 - 4, y - 4, width + 8, height + 6, 0xC0FFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.editingHud) {
            if (event.button() == 0) {
                this.draggingHud = true;
                this.dragHud(event.x(), event.y());
            }
            return true;
        }
        if (event.button() == 0 && this.hoveringChip((int) event.x(), (int) event.y())) {
            Mp3HudConfig.nextTheme();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.editingHud) {
            if (this.draggingHud) {
                this.dragHud(event.x(), event.y());
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.editingHud) {
            this.draggingHud = false;
            Mp3HudConfig.setOffset(this.hudOffsetX, this.hudOffsetY);
            return true;
        }
        return super.mouseReleased(event);
    }

    private void dragHud(double mouseX, double mouseY) {
        this.hudOffsetX = Mth.clamp((int) Math.round(mouseX) - this.width / 2, -this.width / 2, this.width / 2);
        this.hudOffsetY = Mth.clamp((int) Math.round(mouseY) - (this.height - PREVIEW_Y),
                -this.height + PREVIEW_Y, PREVIEW_Y - 24);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.editingHud && (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER)) {
            this.stopHudEditing();
            return true;
        }
        return super.keyPressed(event);
    }
}
