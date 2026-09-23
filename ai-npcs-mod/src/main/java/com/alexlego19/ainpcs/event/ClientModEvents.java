package com.alexlego19.ainpcs.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import com.alexlego19.ainpcs.AiNpcsMod;
import com.alexlego19.ainpcs.client.renderer.NpcRenderer;
import com.alexlego19.ainpcs.init.EntityInit;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import com.alexlego19.ainpcs.network.PacketHandler;
import com.alexlego19.ainpcs.network.EndInteractionPacket;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class ClientModEvents {
    @Mod.EventBusSubscriber(modid = AiNpcsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(ForgeBusEvents.END_INTERACTION_KEY);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(EntityInit.NPC.get(), NpcRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = AiNpcsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {
        public static final KeyMapping END_INTERACTION_KEY = new KeyMapping(
                "key.ainpcs.end_interaction",
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "key.categories.ainpcs"
        );

        private static int typingTick = 0;
        private static int fadeOutTick = 0;

        @SubscribeEvent
        public static void onClientChat(ClientChatReceivedEvent event) {
            String msg = event.getMessage().getString();
            if (msg.startsWith("<") && msg.endsWith("> *thinking.*")) {
                setInteractionStarted();
            } else if (msg.startsWith("<") && !msg.contains("*thinking")) {
                // If it's an NPC response, let's remove any thinking messages from that NPC
                String namePart = msg.substring(0, msg.indexOf(">") + 1);
                Minecraft mc = Minecraft.getInstance();
                ChatComponent chat = mc.gui.getChat();
                if (chat != null && chat.allMessages != null) {
                    boolean removed = false;
                    for (int i = 0; i < chat.allMessages.size(); i++) {
                        GuiMessage m = chat.allMessages.get(i);
                        if (m != null && m.content() != null) {
                            String t = m.content().getString();
                            if (t.startsWith(namePart) && t.contains("*thinking")) {
                                chat.allMessages.remove(i);
                                i--;
                                removed = true;
                            }
                        }
                    }
                    if (removed) {
                        chat.refreshTrimmedMessage();
                    }
                }
            }
        }

        public static void setInteractionStarted() {
            fadeOutTick = 100; // 5 seconds fade out
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    while (END_INTERACTION_KEY.consumeClick()) {
                        PacketHandler.INSTANCE.sendToServer(new EndInteractionPacket());
                    }

                    if (fadeOutTick > 0) {
                        fadeOutTick--;
                    }

                    // Animate typing dots
                    typingTick++;
                    if (typingTick % 10 == 0) {
                        ChatComponent chat = mc.gui.getChat();
                        if (chat != null && chat.allMessages != null && !chat.allMessages.isEmpty()) {
                            for (int i = 0; i < Math.min(10, chat.allMessages.size()); i++) {
                                GuiMessage msg = chat.allMessages.get(i);
                                if (msg != null && msg.content() != null) {
                                    String text = msg.content().getString();
                                    if (text.startsWith("<") && (text.endsWith("> *thinking.*") || text.endsWith("> *thinking..*") || text.endsWith("> *thinking...*"))) {
                                        String namePart = text.substring(0, text.indexOf(">") + 1);
                                        String nextText = namePart + " *thinking.*";
                                        if (text.endsWith("> *thinking.*")) nextText = namePart + " *thinking..*";
                                        else if (text.endsWith("> *thinking..*")) nextText = namePart + " *thinking...*";

                                        GuiMessage newMsg = new GuiMessage(msg.addedTime(), Component.literal(nextText), msg.signature(), msg.tag());
                                        chat.allMessages.set(i, newMsg);
                                        chat.refreshTrimmedMessage();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
            if (fadeOutTick > 0) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    float alpha = Math.min(1.0f, fadeOutTick / 20.0f);
                    int color = ((int)(alpha * 255)) << 24 | 0xFFFFFF;
                    String text = "Press " + END_INTERACTION_KEY.getTranslatedKeyMessage().getString() + " to leave conversation";
                    int width = mc.getWindow().getGuiScaledWidth();
                    mc.font.drawInBatch(text, (width - mc.font.width(text)) / 2f, 20f, color, true, event.getGuiGraphics().pose().last().pose(), event.getGuiGraphics().bufferSource(), net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880);
                    event.getGuiGraphics().bufferSource().endBatch();
                }
            }
        }
    }
}
