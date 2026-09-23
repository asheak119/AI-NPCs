package com.alexlego19.ainpcs.event;

import com.alexlego19.ainpcs.AiNpcsMod;
import com.alexlego19.ainpcs.entity.NpcEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.chat.Component;

import java.util.List;

@Mod.EventBusSubscriber(modid = AiNpcsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) return;

        // Find if this player is the target of any NPC within 32 blocks
        List<NpcEntity> npcs = player.level().getEntitiesOfClass(NpcEntity.class, player.getBoundingBox().inflate(32.0D));
        NpcEntity interactingNpc = null;

        for (NpcEntity npc : npcs) {
            if (player.getUUID().equals(npc.getCurrentTarget())) {
                interactingNpc = npc;
                break;
            }
        }

        if (interactingNpc != null) {
            // Cancel global broadcast
            event.setCanceled(true);

            // Send message to player and audience
            Component message = Component.literal("<" + player.getName().getString() + "> " + event.getRawText());
            player.sendSystemMessage(message);

            for (java.util.UUID audId : interactingNpc.getAudience()) {
                if (!audId.equals(player.getUUID())) {
                    Player audPlayer = player.getServer().getPlayerList().getPlayer(audId);
                    if (audPlayer != null && interactingNpc.distanceTo(audPlayer) <= 30.0D) {
                        audPlayer.sendSystemMessage(message);
                    }
                }
            }

            // Trigger AI response (simulating interaction)
            interactingNpc.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }
}
