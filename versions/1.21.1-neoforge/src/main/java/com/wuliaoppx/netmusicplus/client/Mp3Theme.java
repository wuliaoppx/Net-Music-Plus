package com.wuliaoppx.netmusicplus.client;

/**
 * Look of the MP3 player GUI. A theme is not just a palette: {@link #style()} decides how the frame and the slots are
 * drawn (flat, beveled or glowing), so the three themes really look like different GUIs.
 * <p>
 * The selected theme is remembered in {@code config/netmusicplus-hud.json} together with the overlay settings.
 */
public enum Mp3Theme {
    /** Flat, modern, dark blue-grey. */
    NIGHT("night", Style.FLAT, 0xF0141619, 0xFF3C444E, 0x28FFFFFF, 0x40000000, 0xFF262A30, 0xFF3A4048,
            0xFF8FB8D8, 0xFFE6E6E6, 0xFF9AA4AE, 0xFF2B3038, 0xFF39414C),
    /** Warm wood with a beveled, carved frame: reads like a vanilla container. */
    WOOD("wood", Style.BEVEL, 0xF02A1D13, 0xFF6B4A2B, 0x33FFD9A0, 0x50000000, 0xFF3A2A1B, 0xFF8A6438,
            0xFFE0B060, 0xFFF2E2C8, 0xFFC0A57E, 0xFF4A3520, 0xFF6B4A2B),
    /** Black-purple with a glowing outline. */
    NEON("neon", Style.GLOW, 0xF0120A1E, 0xFF7A3CC8, 0x308A4CE0, 0x50000000, 0xFF1E1030, 0xFF5A2E96,
            0xFFB47CFF, 0xFFE8D8FF, 0xFFA98CD8, 0xFF251340, 0xFF3A1E63);

    /** How the frame and the slots are drawn; kept in a nested class because enum constants cannot reference fields
     *  declared later in the same enum. */
    public static final class Style {
        public static final int FLAT = 0;
        public static final int BEVEL = 1;
        public static final int GLOW = 2;

        private Style() {
        }
    }

    private final String id;
    private final int style;
    private final int body;
    private final int border;
    private final int header;
    private final int inset;
    private final int slotBackground;
    private final int slotEdge;
    private final int accent;
    private final int text;
    private final int muted;
    private final int button;
    private final int buttonHover;

    Mp3Theme(String id, int style, int body, int border, int header, int inset, int slotBackground, int slotEdge,
             int accent, int text, int muted, int button, int buttonHover) {
        this.id = id;
        this.style = style;
        this.body = body;
        this.border = border;
        this.header = header;
        this.inset = inset;
        this.slotBackground = slotBackground;
        this.slotEdge = slotEdge;
        this.accent = accent;
        this.text = text;
        this.muted = muted;
        this.button = button;
        this.buttonHover = buttonHover;
    }

    public String id() {
        return this.id;
    }

    public int style() {
        return this.style;
    }

    public int body() {
        return this.body;
    }

    public int border() {
        return this.border;
    }

    public int header() {
        return this.header;
    }

    public int inset() {
        return this.inset;
    }

    public int slotBackground() {
        return this.slotBackground;
    }

    public int slotEdge() {
        return this.slotEdge;
    }

    public int accent() {
        return this.accent;
    }

    public int text() {
        return this.text;
    }

    public int muted() {
        return this.muted;
    }

    public int button() {
        return this.button;
    }

    public int buttonHover() {
        return this.buttonHover;
    }

    public static Mp3Theme of(int index) {
        Mp3Theme[] values = values();
        return values[Math.floorMod(index, values.length)];
    }

    public static Mp3Theme byId(String id) {
        for (Mp3Theme theme : values()) {
            if (theme.id.equals(id)) {
                return theme;
            }
        }
        return NIGHT;
    }
}
