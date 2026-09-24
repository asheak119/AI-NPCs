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
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import com.alexlego19.ainpcs.AiNpcsMod;

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
            ResourceLocation indicatorTex = null;

            net.minecraft.nbt.CompoundTag questData = mc.player.getPersistentData().getCompound("AiNpcsQuest");
            if (questData.contains("npcId") && questData.getUUID("npcId").equals(entity.getUUID())) {
                if ("IN_PROGRESS".equals(questData.getString("status"))) {
                    indicatorTex = new ResourceLocation(AiNpcsMod.MODID, "textures/entity/quest_in_progress.png");
                } else if ("READY_TO_TURN_IN".equals(questData.getString("status"))) {
                    indicatorTex = new ResourceLocation(AiNpcsMod.MODID, "textures/entity/quest_ready_turn_in.png");
                }
            } else if (entity.getQuestStatus() == NpcEntity.QUEST_READY && !entity.hasGivenQuest()) {
                indicatorTex = new ResourceLocation(AiNpcsMod.MODID, "textures/entity/quest_available.png");
            }

            if (indicatorTex != null) {
                poseStack.pushPose();
                poseStack.translate(0.0D, entity.getBbHeight() + 1.25F, 0.0D); // Above nametag
                poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
                poseStack.scale(-0.5F, -0.5F, 0.5F); // Billboard scale

                Matrix4f matrix4f = poseStack.last().pose();
                com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(indicatorTex));

                // Draw a simple quad
                vertexConsumer.vertex(matrix4f, -0.5f, -0.5f, 0).color(255, 255, 255, 255).uv(0, 0).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
                vertexConsumer.vertex(matrix4f, -0.5f, 0.5f, 0).color(255, 255, 255, 255).uv(0, 1).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
                vertexConsumer.vertex(matrix4f, 0.5f, 0.5f, 0).color(255, 255, 255, 255).uv(1, 1).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
                vertexConsumer.vertex(matrix4f, 0.5f, -0.5f, 0).color(255, 255, 255, 255).uv(1, 0).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();

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
