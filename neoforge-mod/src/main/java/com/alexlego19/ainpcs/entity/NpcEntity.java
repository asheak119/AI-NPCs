package com.alexlego19.ainpcs.entity;

import com.alexlego19.ainpcs.backend.LLMService;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.MinecraftServer;

public class NpcEntity extends PathfinderMob {
    public NpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            MinecraftServer server = this.level().getServer();
            if (server != null) {
                // Initial immediate feedback
                player.sendSystemMessage(Component.literal("<NPC> *thinking...*"));

                // Fire off asynchronous request to the backend
                LLMService.requestDialogueAsync("Hello there!")
                    .thenAccept(responseJson -> {
                        // Ensure game-state mutations (sending chat) happen on the main thread
                        server.execute(() -> {
                            if (responseJson.has("spoken_text")) {
                                String text = responseJson.get("spoken_text").getAsString();
                                player.sendSystemMessage(Component.literal("<NPC> " + text));
                            } else {
                                player.sendSystemMessage(Component.literal("<NPC> *confused silence*"));
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        server.execute(() -> {
                            player.sendSystemMessage(Component.literal("<System> Error communicating with NPC."));
                        });
                        return null;
                    });
            }
            return InteractionResult.SUCCESS;
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(this.level().isClientSide());
    }
}
