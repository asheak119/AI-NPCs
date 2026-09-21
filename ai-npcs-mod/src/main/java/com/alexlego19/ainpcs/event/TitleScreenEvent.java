package com.alexlego19.ainpcs.event;

import com.alexlego19.ainpcs.AiNpcsMod;
import com.alexlego19.ainpcs.client.gui.NPCManagementScreen;
import com.alexlego19.ainpcs.config.AiNpcsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AiNpcsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TitleScreenEvent {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen && AiNpcsClientConfig.showTitleScreenMenu.get()) {
            int buttonWidth = 100;
            int buttonHeight = 20;
            int x = event.getScreen().width - buttonWidth - 5;
            int y = 5;

            event.addListener(Button.builder(Component.literal("NPC Manager"), button -> {
                Minecraft.getInstance().setScreen(new NPCManagementScreen(event.getScreen()));
            }).bounds(x, y, buttonWidth, buttonHeight).build());
        }
    }
}
