package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.client.BlindOverlay;
import com.github.tacowasa059.katamariio.client.JumpscareOverlay;
import com.github.tacowasa059.katamariio.common.networks.BlindPacket;
import com.github.tacowasa059.katamariio.common.networks.JumpscarePacket;
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
        ClientPlayNetworking.registerGlobalReceiver(BlindPacket.TYPE,
                (payload, context) -> BlindOverlay.receive(payload.remainingMillis()));
        BlindOverlay.register();
        ClientPlayNetworking.registerGlobalReceiver(JumpscarePacket.TYPE,
                (payload, context) -> JumpscareOverlay.start());
        JumpscareOverlay.register();
    }
}
