package com.alexlego19.ainpcs.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.function.Supplier;

public class SyncQuestPacket {
    private final CompoundTag questData;

    public SyncQuestPacket(CompoundTag questData) {
        this.questData = questData;
    }

    public SyncQuestPacket(FriendlyByteBuf buf) {
        this.questData = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.questData);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (net.minecraft.client.Minecraft.getInstance().player != null) {
                    net.minecraft.client.Minecraft.getInstance().player.getPersistentData().put("AiNpcsQuest", this.questData);
                }
            });
        });
        context.setPacketHandled(true);
    }
}