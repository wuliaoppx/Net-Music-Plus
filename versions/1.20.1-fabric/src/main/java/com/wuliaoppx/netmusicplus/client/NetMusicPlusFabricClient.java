package com.wuliaoppx.netmusicplus.client;

import com.wuliaoppx.netmusicplus.AmplifierTooltip;
import com.wuliaoppx.netmusicplus.net.Mp3PlayPayload;
import com.wuliaoppx.netmusicplus.net.Mp3StopPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client entry point: keybinds, the MP3 screen, the now-playing overlay and the tooltips. */
public final class NetMusicPlusFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Mp3Client.registerKeys();
        Mp3Client.registerScreens();
        Mp3Hud.register();
        AmplifierTooltip.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> Mp3Client.onClientTick());

        // 1.20.1 networking is channel + buffer; the receive callback runs on the network thread, so hop back to the
        // client thread before touching sounds or the HUD state.
        ClientPlayNetworking.registerGlobalReceiver(Mp3PlayPayload.ID, (client, handler, buf, sender) -> {
            Mp3PlayPayload payload = Mp3PlayPayload.read(buf);
            client.execute(() -> Mp3Client.startSound(payload));
        });
        ClientPlayNetworking.registerGlobalReceiver(Mp3StopPayload.ID, (client, handler, buf, sender) -> {
            Mp3StopPayload payload = Mp3StopPayload.read(buf);
            client.execute(() -> Mp3Client.stopSound(payload.playerId()));
        });
    }
}
