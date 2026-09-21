package com.alexlego19.ainpcs.entity;

import com.alexlego19.ainpcs.backend.LLMService;
import com.alexlego19.ainpcs.data.NPCProfile;
import com.alexlego19.ainpcs.data.NPCProfileManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
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

public class NpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> PROFILE_ID = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> IS_SLIM = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);

    public NpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PROFILE_ID, "");
        this.entityData.define(IS_SLIM, false);
    }

    public String getProfileId() {
        return this.entityData.get(PROFILE_ID);
    }

    public void setProfileId(String profileId) {
        this.entityData.set(PROFILE_ID, profileId);
        updateFromProfile(profileId);
    }

    public boolean isSlim() {
        return this.entityData.get(IS_SLIM);
    }

    public void setSlim(boolean isSlim) {
        this.entityData.set(IS_SLIM, isSlim);
    }

    private void updateFromProfile(String profileId) {
        if (!this.level().isClientSide() && profileId != null && !profileId.isEmpty()) {
            NPCProfile profile = NPCProfileManager.getProfile(profileId);
            if (profile != null) {
                this.setSlim(profile.isSlim());
                this.setCustomName(Component.literal(profile.getName()));
                this.setCustomNameVisible(true);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("ProfileId", this.getProfileId());
        tag.putBoolean("IsSlim", this.isSlim());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ProfileId")) {
            this.setProfileId(tag.getString("ProfileId"));
        }
        if (tag.contains("IsSlim")) {
            this.setSlim(tag.getBoolean("IsSlim"));
        }
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
                player.sendSystemMessage(Component.literal("<NPC> *thinking...*"));

                LLMService.requestDialogueAsync("Hello there!")
                    .thenAccept(responseJson -> {
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
