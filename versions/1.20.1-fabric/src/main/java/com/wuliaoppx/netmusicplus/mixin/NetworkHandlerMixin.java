package com.wuliaoppx.netmusicplus.mixin;

import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.github.tartaricacid.netmusic.network.message.Message;
import com.wuliaoppx.netmusicplus.AmplifierData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Widens the radius NetMusic uses when it broadcasts the "start playing" packet.
 * <p>
 * 1.20.1 NetMusic's {@code NetworkHandler#sendToNearBy(Level, BlockPos, Message)} collects the players tracking the
 * block's chunk and drops everyone farther away than a hardcoded 96 blocks (9216 = 96²). The sound engine can already
 * be told to attenuate over a larger distance, but without this the players standing between 96 blocks and the
 * amplified radius never receive the packet and hear nothing at all.
 * <p>
 * The multiplier comes from the amplifier enchantment stored on the block entity, so a level 3 player's music is sent
 * to everyone within 96 × 4 blocks. Sending goes through NetMusic's own {@code sendToClientPlayer} so the packet id and
 * buffer layout stay exactly what NetMusic expects.
 */
@Mixin(NetworkHandler.class)
public abstract class NetworkHandlerMixin {
    @Inject(method = "sendToNearBy", at = @At("HEAD"), cancellable = true)
    private static void netmusicplus$widenBroadcast(Level level, BlockPos pos, Message message, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        float multiplier = AmplifierData.multiplierAt(serverLevel, pos);
        if (multiplier <= 1.0F) {
            return;
        }
        double radius = 96.0D * multiplier;
        double radiusSquared = radius * radius;
        Vec3 center = Vec3.atCenterOf(pos);
        // Every player in the dimension, not just the ones tracking this chunk: the amplified radius spans many chunks.
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(center.x, center.y, center.z) <= radiusSquared) {
                NetworkHandler.sendToClientPlayer(message, player);
            }
        }
        ci.cancel();
    }
}
