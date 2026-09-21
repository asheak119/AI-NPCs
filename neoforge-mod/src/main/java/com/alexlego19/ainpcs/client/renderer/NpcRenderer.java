package com.alexlego19.ainpcs.client.renderer;

import com.alexlego19.ainpcs.AiNpcsMod;
import com.alexlego19.ainpcs.entity.NpcEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class NpcRenderer extends HumanoidMobRenderer<NpcEntity, HumanoidModel<NpcEntity>> {
    private static final ResourceLocation NPC_TEXTURE = new ResourceLocation(AiNpcsMod.MODID, "textures/entity/npc.png");

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity) {
        return NPC_TEXTURE;
    }
}
