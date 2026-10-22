package com.wuliaoppx.netmusicplus.compat;

import com.github.tartaricacid.netmusic.block.BlockMusicPlayer;
import com.wuliaoppx.netmusicplus.AmplifierData;
import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Shows the installed amplifier tier in Jade's tooltip for NetMusic music players.
 * <p>
 * On Fabric the plugin is discovered through the {@code jade} entrypoint in {@code fabric.mod.json}, so the class is
 * only touched when Jade is actually installed; nothing else in the mod refers to it.
 */
@WailaPlugin(NetMusicPlus.MOD_ID)
public class JadeAmplifierPlugin implements IWailaPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(NetMusicPlus.MOD_ID, "amplifier_tier");
    private static final String NBT_KEY = AmplifierData.TIER_KEY;

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ServerDataProvider.INSTANCE, BlockMusicPlayer.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ClientComponentProvider.INSTANCE, BlockMusicPlayer.class);
    }

    private enum ServerDataProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            int tier = AmplifierData.getTier(accessor.getBlockEntity());
            if (tier > 0) {
                tag.putInt(NBT_KEY, tier);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum ClientComponentProvider implements IComponentProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            int tier = accessor.getServerData().getInt(NBT_KEY);
            if (tier <= 0) {
                return;
            }
            tooltip.add(Component.translatable("jade." + NetMusicPlus.MOD_ID + ".tier",
                    AmplifierData.roman(tier), AmplifierData.range(tier)).withStyle(ChatFormatting.AQUA));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
