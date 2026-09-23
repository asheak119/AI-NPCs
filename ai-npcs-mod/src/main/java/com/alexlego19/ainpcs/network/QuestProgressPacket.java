package com.alexlego19.ainpcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.function.Supplier;

public class QuestProgressPacket {
    private final String message;

    public QuestProgressPacket(String message) {
        this.message = message;
    }

    public QuestProgressPacket(FriendlyByteBuf buf) {
        this.message = buf.readUtf(256);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.message, 256);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.alexlego19.ainpcs.event.ClientModEvents.ForgeBusEvents.setQuestProgressNotification(this.message);
            });
        });
        context.setPacketHandled(true);
    }
}