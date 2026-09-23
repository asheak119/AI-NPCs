package com.alexlego19.ainpcs.client.renderer;

import com.alexlego19.ainpcs.client.manager.DynamicSkinManager;
import com.alexlego19.ainpcs.data.NPCProfile;
import com.alexlego19.ainpcs.data.NPCProfileManager;
import com.alexlego19.ainpcs.entity.NpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

public class NpcRenderer extends HumanoidMobRenderer<NpcEntity, PlayerModel<NpcEntity>> {
    private final PlayerModel<NpcEntity> defaultModel;
    private final PlayerModel<NpcEntity> slimModel;

    public NpcRenderer(EntityRendererProvider.Context context) {
        // Initialize with default standard model
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.defaultModel = this.model;
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(NpcEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isSlim()) {
            this.model = this.slimModel;
        } else {
            this.model = this.defaultModel;
        }
                super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        // Quest Indicators
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String indicator = null;
            int color = 0xFFFFFF;

            net.minecraft.nbt.CompoundTag questData = mc.player.getPersistentData().getCompound("AiNpcsQuest");
            if (questData.contains("npcId") && questData.getUUID("npcId").equals(entity.getUUID())) {
                if ("IN_PROGRESS".equals(questData.getString("status"))) {
                    indicator = "?";
                    color = 0xAAAAAA; // Gray
                } else if ("READY_TO_TURN_IN".equals(questData.getString("status"))) {
                    indicator = "?";
                    color = 0xFFFF55; // Yellow
                }
            } else if (entity.getQuestStatus() == NpcEntity.QUEST_READY && !entity.hasGivenQuest()) {
                indicator = "!";
                color = 0xFFFF55; // Yellow
            }

            if (indicator != null) {
                poseStack.pushPose();
                poseStack.translate(0.0D, entity.getBbHeight() + 0.5F, 0.0D);
                poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
                poseStack.scale(-0.025F, -0.025F, 0.025F);
                Matrix4f matrix4f = poseStack.last().pose();
                float bgOpacity = mc.options.getBackgroundOpacity(0.25F);
                int j = (int)(bgOpacity * 255.0F) << 24;
                net.minecraft.client.gui.Font font = this.getFont();
                float width = (float)(-font.width(indicator) / 2);
                font.drawInBatch(indicator, width, 0, color, false, matrix4f, buffer, net.minecraft.client.gui.Font.DisplayMode.NORMAL, j, packedLight);
                font.drawInBatch(indicator, width, 0, -1, false, matrix4f, buffer, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight);
                poseStack.popPose();
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity) {
        String profileId = entity.getVariant();
        if (profileId != null && !profileId.isEmpty()) {
            // Read client side profiles via config
            NPCProfile profile = NPCProfileManager.getProfile(profileId);
            if (profile != null && profile.getSkinId() != null) {
                return DynamicSkinManager.getSkin(profile.getSkinId(), entity.isSlim());
            }
        }
        return DynamicSkinManager.getSkin(null, entity.isSlim());
    }
}
