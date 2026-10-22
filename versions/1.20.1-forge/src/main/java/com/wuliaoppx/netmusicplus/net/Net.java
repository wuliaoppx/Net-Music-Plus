package com.wuliaoppx.netmusicplus.net;

import com.wuliaoppx.netmusicplus.Mp3ServerPlayback;
import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The mod's own network channel.
 * <p>
 * 1.20.1 predates {@code CustomPacketPayload}: a channel id, a protocol version and a {@code FriendlyByteBuf} per
 * message are the whole protocol. Three messages are exchanged: the server tells clients which URL to stream and when
 * to stop, and clients ask the server to act on a transport keybind.
 */
public final class Net {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(NetMusicPlus.id("main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static int nextId;

    private Net() {
    }

    public static void register() {
        CHANNEL.registerMessage(nextId++, Mp3PlayPayload.class, Mp3PlayPayload::write, Mp3PlayPayload::read,
                (message, context) -> {
                    context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                            () -> () -> com.wuliaoppx.netmusicplus.client.Mp3Client.startSound(message)));
                    context.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(nextId++, Mp3StopPayload.class, Mp3StopPayload::write, Mp3StopPayload::read,
                (message, context) -> {
                    context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                            () -> () -> com.wuliaoppx.netmusicplus.client.Mp3Client.stopSound(message.playerId())));
                    context.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(nextId++, Mp3ControlPayload.class, Mp3ControlPayload::write, Mp3ControlPayload::read,
                (message, context) -> {
                    context.get().enqueueWork(() -> {
                        ServerPlayer sender = context.get().getSender();
                        if (sender != null) {
                            Mp3ServerPlayback.handleControl(sender, message.action());
                        }
                    });
                    context.get().setPacketHandled(true);
                });
    }

    public static void sendTo(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToNear(Level level, double x, double y, double z, double radius, Object message) {
        CHANNEL.send(PacketDistributor.NEAR.with(
                () -> new PacketDistributor.TargetPoint(x, y, z, radius, level.dimension())), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
