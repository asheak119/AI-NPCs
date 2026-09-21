package com.alexlego19.ainpcs.event;

import com.alexlego19.ainpcs.AiNpcsMod;
import com.alexlego19.ainpcs.entity.NpcEntity;
import com.alexlego19.ainpcs.init.EntityInit;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AiNpcsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(EntityInit.NPC.get(), NpcEntity.createAttributes().build());
    }
}
