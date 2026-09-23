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
import java.util.Map;
import java.util.HashMap;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import javax.annotation.Nullable;

public class NpcEntity extends PathfinderMob {
    private UUID currentTarget = null;
    private List<UUID> audience = new ArrayList<>();
    private float accumulatedDamage = 0.0f;
    private int messageCount = 0;
    private net.minecraft.core.BlockPos spawnerPos = null;
    private boolean hasGivenQuest = false;

    private Map<UUID, Float> personalReputations = new HashMap<>();
    private float communalReputation = 0.0f;
    private float societalReputation = 0.0f;

    public void modifyPersonalReputation(UUID player, float delta) {
        float current = personalReputations.getOrDefault(player, 0.0f);
        personalReputations.put(player, current + delta);
        updateCommunalReputation();
    }

    private void updateCommunalReputation() {
        if (personalReputations.isEmpty()) {
            communalReputation = 0.0f;
            return;
        }
        float sum = 0;
        for (float rep : personalReputations.values()) sum += rep;
        communalReputation = sum / personalReputations.size();
    }

    public void updateSocietalReputation(float reputation) {
        this.societalReputation = reputation;
    }

    public UUID getCurrentTarget() { return currentTarget; }
    public List<UUID> getAudience() { return audience; }

    public void endInteraction(net.minecraft.world.entity.player.Player player, boolean isHotkey) {
        if (!this.level().isClientSide() && currentTarget != null && (player == null || currentTarget.equals(player.getUUID()))) {
            this.currentTarget = null;
            this.messageCount = 0;
            this.audience.clear();

            // Inform everyone interaction ended
            String npcName = this.getCustomName() != null ? this.getCustomName().getString() : "NPC";
            if (isHotkey && player != null) {
                 player.sendSystemMessage(net.minecraft.network.chat.Component.literal("<" + npcName + "> Interaction ended."));
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide() && source.getEntity() instanceof Player) {
            Player attackingPlayer = (Player) source.getEntity();
            modifyPersonalReputation(attackingPlayer.getUUID(), -amount * 0.1f);

            accumulatedDamage += amount;
            NPCProfile profile = NPCProfileManager.getProfile(this.getVariant());
            if (profile != null) {
                float threshold = profile.getFortitude() != null ? profile.getFortitude().getDamageThreshold() : Fortitude.MEDIUM.getDamageThreshold();
                if (accumulatedDamage >= threshold) {
                    Temperament temp = profile.getTemperament() != null ? profile.getTemperament() : Temperament.PASSIVE;
                    String npcName = this.getCustomName() != null ? this.getCustomName().getString() : "NPC";
                    if (temp == Temperament.PASSIVE) {
                        if (this.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof PanicGoal)) {
                            this.goalSelector.addGoal(0, new PanicGoal(this, 1.25D));

                            // Send scared disposition message
                            net.minecraft.server.level.ServerPlayer targetP = (net.minecraft.server.level.ServerPlayer) source.getEntity();
                            if (targetP != null) {
                                targetP.sendSystemMessage(net.minecraft.network.chat.Component.literal("<" + npcName + "> Please don't hurt me! I'm getting out of here!"));
                            }
                        }
                        this.setLastHurtByMob((net.minecraft.world.entity.LivingEntity) source.getEntity());
                    } else if (temp == Temperament.NEUTRAL) {
                        if (this.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof MeleeAttackGoal)) {
                            this.goalSelector.addGoal(0, new MeleeAttackGoal(this, 1.0D, true));

                            // Send aggressive disposition message
                            net.minecraft.server.level.ServerPlayer targetP = (net.minecraft.server.level.ServerPlayer) source.getEntity();
                            if (targetP != null) {
                                targetP.sendSystemMessage(net.minecraft.network.chat.Component.literal("<" + npcName + "> You'll pay for that!"));
                            }
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
    private static final EntityDataAccessor<Integer> QUEST_STATUS = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);

    public NpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, "");
        this.entityData.define(IS_SLIM, false);
        this.entityData.define(QUEST_STATUS, 0);
    }

    public String getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(String variantId) {
        this.entityData.set(VARIANT, variantId);
        updateFromProfile(variantId);
    }

    public int getQuestStatus() {
        return this.entityData.get(QUEST_STATUS);
    }

    public void setQuestStatus(int status) {
        this.entityData.set(QUEST_STATUS, status);
    }

    public net.minecraft.core.BlockPos getSpawnerPos() {
        return this.spawnerPos;
    }

    public void setSpawnerPos(net.minecraft.core.BlockPos pos) {
        this.spawnerPos = pos;
    }

    public boolean hasGivenQuest() {
        return this.hasGivenQuest;
    }

    public void setHasGivenQuest(boolean given) {
        this.hasGivenQuest = given;
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

        tag.putInt("QuestStatus", this.getQuestStatus());
        if (this.spawnerPos != null) {
            tag.putLong("SpawnerPos", this.spawnerPos.asLong());
        }
        tag.putBoolean("HasGivenQuest", this.hasGivenQuest);

        tag.putFloat("CommunalReputation", this.communalReputation);
        tag.putFloat("SocietalReputation", this.societalReputation);

        ListTag repsTag = new ListTag();
        for (Map.Entry<UUID, Float> entry : personalReputations.entrySet()) {
            net.minecraft.nbt.CompoundTag repTag = new net.minecraft.nbt.CompoundTag();
            repTag.putUUID("UUID", entry.getKey());
            repTag.putFloat("Reputation", entry.getValue());
            repsTag.add(repTag);
        }
        tag.put("PersonalReputations", repsTag);
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

        if (tag.contains("QuestStatus")) {
            this.setQuestStatus(tag.getInt("QuestStatus"));
        }
        if (tag.contains("SpawnerPos")) {
            this.spawnerPos = net.minecraft.core.BlockPos.of(tag.getLong("SpawnerPos"));
        }
        if (tag.contains("HasGivenQuest")) {
            this.hasGivenQuest = tag.getBoolean("HasGivenQuest");
        }

        if (tag.contains("CommunalReputation")) {
            this.communalReputation = tag.getFloat("CommunalReputation");
        }
        if (tag.contains("SocietalReputation")) {
            this.societalReputation = tag.getFloat("SocietalReputation");
        }

        if (tag.contains("PersonalReputations", Tag.TAG_LIST)) {
            ListTag repsTag = tag.getList("PersonalReputations", Tag.TAG_COMPOUND);
            for (int i = 0; i < repsTag.size(); i++) {
                net.minecraft.nbt.CompoundTag repTag = repsTag.getCompound(i);
                if (repTag.hasUUID("UUID")) {
                    personalReputations.put(repTag.getUUID("UUID"), repTag.getFloat("Reputation"));
                }
            }
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
        if (!this.level().isClientSide() && this.getQuestStatus() == QUEST_NONE && Math.random() < 0.5) {
            this.setQuestStatus(QUEST_GENERATING);
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try { Thread.sleep(2000); } catch (Exception e) {}
            }).thenRun(() -> {
                if (this.level().getServer() != null) {
                    this.level().getServer().execute(() -> {
                        if (!this.isRemoved()) {
                            net.minecraft.core.BlockPos pos = this.blockPosition();
                            net.minecraft.core.BlockPos foundPos = null;
                            if (this.level() instanceof net.minecraft.server.level.ServerLevel) {
                                net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) this.level();
                                foundPos = serverLevel.findNearestMapStructure(net.minecraft.tags.StructureTags.VILLAGE, pos, 100, false);
                            }
                            if (foundPos != null) {
                                this.setSpawnerPos(foundPos);
                                this.setQuestStatus(QUEST_READY);
                                this.setPersistenceRequired();
                            } else {
                                this.setQuestStatus(QUEST_FAILED);
                            }
                        }
                    });
                }
            });
        }
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        if (this.getQuestStatus() == QUEST_READY || this.hasGivenQuest) {
            return false;
        }
        return super.removeWhenFarAway(distanceToClosestPlayer);
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

                // Quest Checks
                net.minecraft.nbt.CompoundTag playerQuest = player.getPersistentData().getCompound("AiNpcsQuest");
                boolean playerHasThisQuest = playerQuest.contains("npcId") && playerQuest.getUUID("npcId").equals(this.getUUID());
                if (playerHasThisQuest && playerQuest.getString("status").equals("IN_PROGRESS")) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("<" + npcName + "> Have you killed 5 zombies to protect the village yet?"));
                    endInteraction(player, false);
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
                if (playerHasThisQuest && playerQuest.getString("status").equals("READY_TO_TURN_IN")) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("<" + npcName + "> Thank you for clearing them out! Here is your reward."));
                    player.spawnAtLocation(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, 10));
                    player.getPersistentData().remove("AiNpcsQuest");
                    com.alexlego19.ainpcs.network.PacketHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer)player), new com.alexlego19.ainpcs.network.SyncQuestPacket(new net.minecraft.nbt.CompoundTag()));
                    this.setQuestStatus(QUEST_NONE);
                    endInteraction(player, false);
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
                if (this.getQuestStatus() == QUEST_READY && !this.hasGivenQuest) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("<" + npcName + "> I found a village nearby that needs protection! Will you kill 5 zombies for me? (Reply yes or accept)"));
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }

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

    public static final int QUEST_NONE = 0;
    public static final int QUEST_GENERATING = 1;
    public static final int QUEST_READY = 2;
    public static final int QUEST_FAILED = 3;
}
