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

            String text = event.getRawText().toLowerCase();
            if (interactingNpc.getQuestStatus() == NpcEntity.QUEST_READY && !interactingNpc.hasGivenQuest() && (text.contains("yes") || text.contains("accept") || text.contains("sure") || text.contains("okay"))) {
                interactingNpc.setHasGivenQuest(true);
                net.minecraft.nbt.CompoundTag questData = new net.minecraft.nbt.CompoundTag();
                questData.putUUID("npcId", interactingNpc.getUUID());
                questData.putString("status", "IN_PROGRESS");
                questData.putInt("targetAmount", 5);
                questData.putInt("currentAmount", 0);
                if (interactingNpc.getSpawnerPos() != null) {
                    questData.putLong("spawnerPos", interactingNpc.getSpawnerPos().asLong());
                }
                player.getPersistentData().put("AiNpcsQuest", questData);
                com.alexlego19.ainpcs.network.PacketHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new com.alexlego19.ainpcs.network.SyncQuestPacket(questData));

                net.minecraft.world.item.ItemStack mapItem = net.minecraft.world.item.MapItem.create(player.level(), interactingNpc.getSpawnerPos().getX(), interactingNpc.getSpawnerPos().getZ(), (byte)3, true, true);
                net.minecraft.world.level.saveddata.maps.MapItemSavedData mapData = net.minecraft.world.item.MapItem.getSavedData(mapItem, player.level());
                if (mapData != null) {
                    net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(mapItem, interactingNpc.getSpawnerPos(), "+", net.minecraft.world.level.saveddata.maps.MapDecoration.Type.TARGET_X);
                }
                mapItem.setHoverName(Component.literal("Quest Map"));
                player.getInventory().add(mapItem);

                player.sendSystemMessage(Component.literal("<" + interactingNpc.getCustomName().getString() + "> Excellent! I have given you a Quest Map. Good luck!"));
                interactingNpc.endInteraction(player, false);
                return;
            }

            // Trigger AI response (simulating interaction)
            interactingNpc.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // Handle NPC Death
        if (event.getEntity() instanceof NpcEntity) {
            NpcEntity deadNpc = (NpcEntity) event.getEntity();
            net.minecraft.server.MinecraftServer server = deadNpc.level().getServer();
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    net.minecraft.nbt.CompoundTag questData = player.getPersistentData().getCompound("AiNpcsQuest");
                    if (questData.contains("npcId") && questData.getUUID("npcId").equals(deadNpc.getUUID())) {
                        player.getPersistentData().remove("AiNpcsQuest");
                        com.alexlego19.ainpcs.network.PacketHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new com.alexlego19.ainpcs.network.SyncQuestPacket(new net.minecraft.nbt.CompoundTag()));
                        player.sendSystemMessage(Component.literal("Quest Cancelled: The quest giver has died."));
                    }
                }
            }
            return;
        }

        // Handle Mob Death
        net.minecraft.world.damagesource.DamageSource source = event.getSource();
        if (source != null && source.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) source.getEntity();
            net.minecraft.nbt.CompoundTag questData = player.getPersistentData().getCompound("AiNpcsQuest");
            if (questData.contains("npcId") && "IN_PROGRESS".equals(questData.getString("status"))) {
                int current = questData.getInt("currentAmount");
                int target = questData.getInt("targetAmount");
                if (current < target) {
                    if (event.getEntity() instanceof net.minecraft.world.entity.monster.Zombie) {
                        current++;
                        questData.putInt("currentAmount", current);
                        if (current >= target) {
                            questData.putString("status", "READY_TO_TURN_IN");
                            com.alexlego19.ainpcs.network.PacketHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new com.alexlego19.ainpcs.network.QuestProgressPacket("Quest Complete: Return to the NPC!"));
                        } else {
                            com.alexlego19.ainpcs.network.PacketHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new com.alexlego19.ainpcs.network.QuestProgressPacket("Quest Progress: " + current + " / " + target + " Zombies Killed"));
                        }
                        com.alexlego19.ainpcs.network.PacketHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new com.alexlego19.ainpcs.network.SyncQuestPacket(questData));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            net.minecraft.nbt.CompoundTag questData = player.getPersistentData().getCompound("AiNpcsQuest");
            com.alexlego19.ainpcs.network.PacketHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new com.alexlego19.ainpcs.network.SyncQuestPacket(questData));
        }
    }
}
