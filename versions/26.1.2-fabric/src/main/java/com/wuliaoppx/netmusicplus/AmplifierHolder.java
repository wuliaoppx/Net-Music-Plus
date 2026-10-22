package com.wuliaoppx.netmusicplus;

/**
 * Implemented by NetMusic's music player block entity through {@link com.wuliaoppx.netmusicplus.mixin.TileEntityMusicPlayerMixin}:
 * the amplifier level installed in the block, so it survives a reload and reaches the clients with the block entity's
 * own update tag.
 */
public interface AmplifierHolder {
    int netmusicplus$tier();

    void netmusicplus$setTier(int tier);
}
