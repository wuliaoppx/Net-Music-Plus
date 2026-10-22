package com.wuliaoppx.netmusicplus.mixin;

import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.wuliaoppx.netmusicplus.AmplifierData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * NetMusic broadcasts the "start playing" packet with a hardcoded radius of 96 blocks, which is wider than the
 * audible radius (64) but still a hard limit. Players outside it never receive the packet, so widening only the
 * client-side volume would not be enough. Scale that radius by the installed amplifier's multiplier.
 * <p>
 * The radius is looked up from the block entity at the packet's origin, so payloads sent from a player without an
 * amplifier are broadcast untouched.
 */
@Mixin(NetworkHandler.class)
public abstract class NetworkHandlerMixin {
    @Redirect(method = "sendToNearby", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/network/PacketDistributor;sendToPlayersNear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;DDDDLnet/minecraft/network/protocol/common/custom/CustomPacketPayload;[Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"))
    private static void netmusicplus$widenMusicBroadcast(ServerLevel level, ServerPlayer excluded, double x, double y,
                                                            double z, double radius, CustomPacketPayload payload,
                                                            CustomPacketPayload[] payloads) {
        float multiplier = AmplifierData.multiplierAt(level, BlockPos.containing(x, y, z));
        double widenedRadius = multiplier > 1.0F ? radius * multiplier : radius;
        PacketDistributor.sendToPlayersNear(level, excluded, x, y, z, widenedRadius, payload, payloads);
    }
}
