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
import net.minecraft.resources.ResourceLocation;

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
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity) {
        String profileId = entity.getProfileId();
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
