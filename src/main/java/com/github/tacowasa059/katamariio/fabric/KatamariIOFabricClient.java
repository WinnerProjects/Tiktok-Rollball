package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.common.networks.S2CPlayerPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class KatamariIOFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> FabricClientStateSync.flushPending());
        ClientPlayNetworking.registerGlobalReceiver(S2CPlayerPacket.TYPE,
                (payload, context) -> FabricClientStateSync.receive(payload));
    }
}
