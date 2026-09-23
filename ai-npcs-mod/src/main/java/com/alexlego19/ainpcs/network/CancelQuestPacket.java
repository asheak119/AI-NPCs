package com.alexlego19.ainpcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Supplier;

public class CancelQuestPacket {
    public CancelQuestPacket() {}

    public CancelQuestPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getPersistentData().remove("AiNpcsQuest");
                PacketHandler.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new SyncQuestPacket(new CompoundTag())
                );
            }
        });
        context.setPacketHandled(true);
    }
}