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

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import com.alexlego19.ainpcs.data.Temperament;
import com.alexlego19.ainpcs.data.Fortitude;
import java.util.stream.Collectors;
import net.minecraft.world.phys.AABB;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import javax.annotation.Nullable;

public class NpcEntity extends PathfinderMob {
    private UUID currentTarget = null;
    private List<UUID> audience = new ArrayList<>();
    private float accumulatedDamage = 0.0f;
    private int messageCount = 0;

    public UUID getCurrentTarget() { return currentTarget; }
    public List<UUID> getAudience() { return audience; }

    public void endInteraction(net.minecraft.world.entity.player.Player player, boolean isHotkey) {
        if (!this.level().isClientSide() && currentTarget != null && (player == null || currentTarget.equals(player.getUUID()))) {
            this.currentTarget = null;
            this.audience.clear();
            this.messageCount = 0;
            if (isHotkey && player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("<System> Interaction ended."));
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide() && source.getEntity() instanceof Player) {
            accumulatedDamage += amount;
            NPCProfile profile = NPCProfileManager.getProfile(this.getVariant());
            if (profile != null) {
                float threshold = profile.getFortitude() != null ? profile.getFortitude().getDamageThreshold() : Fortitude.MEDIUM.getDamageThreshold();
                if (accumulatedDamage >= threshold) {
                    Temperament temp = profile.getTemperament() != null ? profile.getTemperament() : Temperament.PASSIVE;
                    if (temp == Temperament.PASSIVE) {
                        if (this.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof PanicGoal)) {
                            this.goalSelector.addGoal(0, new PanicGoal(this, 1.25D));
                        }
                        this.setLastHurtByMob((net.minecraft.world.entity.LivingEntity) source.getEntity());
                    } else if (temp == Temperament.NEUTRAL) {
                        if (this.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof MeleeAttackGoal)) {
                            this.goalSelector.addGoal(0, new MeleeAttackGoal(this, 1.0D, true));
                        }
                        this.setTarget((net.minecraft.world.entity.LivingEntity) source.getEntity());
                    }
                    accumulatedDamage = 0.0f; // Reset after reacting
                }
            }
        }
        return super.hurt(source, amount);
    }

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
        if (!this.level().isClientSide() && currentTarget != null) {
            Player targetPlayer = this.level().getPlayerByUUID(currentTarget);
            if (targetPlayer != null) {
                if (this.distanceTo(targetPlayer) > 30.0D) {
                    String npcName = this.getCustomName() != null ? this.getCustomName().getString() : "NPC";
                    targetPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("<" + npcName + "> Farewell!"));
                    endInteraction(targetPlayer, false);
                }
            } else {
                endInteraction(null, false); // Target player left
            }
        }
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
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            if (currentTarget != null && !player.getUUID().equals(currentTarget)) {
                return InteractionResult.PASS;
            }
            MinecraftServer server = this.level().getServer();
            if (server != null) {
                if (currentTarget == null) {
                    currentTarget = player.getUUID();
                    audience = this.level().getEntitiesOfClass(Player.class, new AABB(this.blockPosition()).inflate(30.0D))
                        .stream().map(Player::getUUID).collect(Collectors.toList());
                }

                String npcName = this.getCustomName() != null ? this.getCustomName().getString() : "NPC";
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("<" + npcName + "> *thinking.*"));

                // Preprogrammed messages
                String[] msgs = {
                    "Hello traveler, what brings you here?",
                    "That is very interesting. Tell me more.",
                    "I must be going now. Farewell!"
                };
                String text = messageCount < msgs.length ? msgs[messageCount] : msgs[msgs.length - 1];
                messageCount++;

                // Instead of actually calling LLM, we just wait and output the preprogrammed message
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try { Thread.sleep(2000); } catch (Exception e) {}
                    return text;
                }).thenAccept(responseTxt -> {
                        server.execute(() -> {
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("<" + npcName + "> " + responseTxt));
                                // Broadcast to audience too
                                for (UUID audId : audience) {
                                    if (!audId.equals(player.getUUID())) {
                                        Player audPlayer = server.getPlayerList().getPlayer(audId);
                                        if (audPlayer != null && distanceTo(audPlayer) <= 30.0D) {
                                            audPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("<" + npcName + "> " + responseTxt));
                                        }
                                    }
                                }
                                if (messageCount >= 3) {
                                    endInteraction(player, false);
                                }
                        });
                    });
            }
            return InteractionResult.SUCCESS;
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(this.level().isClientSide());
    }
}
