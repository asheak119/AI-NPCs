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
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

public class NpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> VARIANT = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> IS_SLIM = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);

    public NpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, "");
        this.entityData.define(IS_SLIM, false);
    }

    public String getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(String variantId) {
        this.entityData.set(VARIANT, variantId);
        updateFromProfile(variantId);
    }

    public boolean isSlim() {
        return this.entityData.get(IS_SLIM);
    }

    public void setSlim(boolean isSlim) {
        this.entityData.set(IS_SLIM, isSlim);
    }

    private void updateFromProfile(String variantId) {
        if (!this.level().isClientSide() && variantId != null && !variantId.isEmpty()) {
            NPCProfile profile = NPCProfileManager.getProfile(variantId);
            this.setSlim(profile.isSlim());
            this.setCustomName(Component.literal(profile.getName()));
            this.setCustomNameVisible(true);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("variant", this.getVariant());
        tag.putBoolean("IsSlim", this.isSlim());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("variant")) {
            this.setVariant(tag.getString("variant"));
        } else if (tag.contains("ProfileId")) { // Backwards compat
            this.setVariant(tag.getString("ProfileId"));
        }
        if (tag.contains("IsSlim")) {
            this.setSlim(tag.getBoolean("IsSlim"));
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        // If spawned normally without a set variant
        if (this.getVariant().isEmpty()) {
            NPCProfile profile = NPCProfileManager.getRandomEnabledProfile();
            this.setVariant(profile.getId());
        }
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 20 == 0) {
            String variant = this.getVariant();
            // If the profile was deleted from the manager (and it's not the default), discard the entity
            if (!NPCProfileManager.DEFAULT_PROFILE_ID.equals(variant) && NPCProfileManager.getProfiles().get(variant) == null) {
                this.discard();
            }
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
