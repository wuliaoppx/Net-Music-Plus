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

        ClientPlayNetworking.registerGlobalReceiver(Mp3PlayPayload.TYPE, (payload, context) ->
                context.client().execute(() -> Mp3Client.startSound(payload)));
        ClientPlayNetworking.registerGlobalReceiver(Mp3StopPayload.TYPE, (payload, context) ->
                context.client().execute(() -> Mp3Client.stopSound(payload.playerId())));
    }
}
