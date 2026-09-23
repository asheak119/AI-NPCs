package com.alexlego19.ainpcs.network;

import com.alexlego19.ainpcs.entity.NpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EndInteractionPacket {
    public EndInteractionPacket() {
    }

    public EndInteractionPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // We find the NPC the player is interacting with and end the interaction.
                player.level().getEntitiesOfClass(NpcEntity.class, player.getBoundingBox().inflate(32.0D))
                    .stream()
                    .filter(npc -> player.getUUID().equals(npc.getCurrentTarget()))
                    .forEach(npc -> npc.endInteraction(player, true));
            }
        });
        context.setPacketHandled(true);
    }
}
