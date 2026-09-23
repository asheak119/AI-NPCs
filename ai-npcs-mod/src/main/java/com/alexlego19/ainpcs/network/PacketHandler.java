package com.alexlego19.ainpcs.network;

import com.alexlego19.ainpcs.AiNpcsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(AiNpcsMod.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, EndInteractionPacket.class, EndInteractionPacket::toBytes, EndInteractionPacket::new, EndInteractionPacket::handle);
        INSTANCE.registerMessage(id++, SyncQuestPacket.class, SyncQuestPacket::toBytes, SyncQuestPacket::new, SyncQuestPacket::handle);
        INSTANCE.registerMessage(id++, QuestProgressPacket.class, QuestProgressPacket::toBytes, QuestProgressPacket::new, QuestProgressPacket::handle);
        INSTANCE.registerMessage(id++, CancelQuestPacket.class, CancelQuestPacket::toBytes, CancelQuestPacket::new, CancelQuestPacket::handle);
    }
}
