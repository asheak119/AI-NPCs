package com.alexlego19.ainpcs.event;

import com.alexlego19.ainpcs.AiNpcsMod;
import com.alexlego19.ainpcs.client.renderer.NpcRenderer;
import com.alexlego19.ainpcs.init.EntityInit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AiNpcsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityInit.NPC.get(), NpcRenderer::new);
    }
}
