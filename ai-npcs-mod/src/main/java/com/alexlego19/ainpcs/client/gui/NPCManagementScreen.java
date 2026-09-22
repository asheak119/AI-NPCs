package com.alexlego19.ainpcs.client.gui;

import com.alexlego19.ainpcs.client.manager.DynamicSkinManager;
import com.alexlego19.ainpcs.data.NPCProfile;
import com.alexlego19.ainpcs.data.NPCProfileManager;
import com.alexlego19.ainpcs.entity.NpcEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import org.joml.Quaternionf;
import com.mojang.math.Axis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NPCManagementScreen extends Screen {
    private final Screen parent;
    private EditBox nameEditBox;
    private Button skinDropdownBtn;
    private Button toggleModelBtn;
    private Button toggleEnableBtn;
    private Button deleteBtn;
    private ProfileList profileList;

    private List<String> availableSkins = new ArrayList<>();
    private int currentSkinIndex = 0;

    private PlayerModel<?> classicModel;
    private PlayerModel<?> slimModel;

    private NpcEntity previewEntity;

    private NPCProfile selectedProfile;
    public NPCManagementScreen(Screen parent) {
        super(Component.literal("NPC Manager"));
        this.parent = parent;
        this.selectedProfile = NPCProfileManager.STEVE_PROFILE;
        this.loadAvailableSkins();
    }

    private void loadAvailableSkins() {
        this.availableSkins.clear();
        this.availableSkins.add("alex");
        File skinsDir = NPCProfileManager.getSkinsDir().toFile();
        if (skinsDir.exists() && skinsDir.isDirectory()) {
            File[] files = skinsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                for (File file : files) {
                    this.availableSkins.add(file.getName().replace(".png", ""));
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        this.classicModel = new PlayerModel<>(this.minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        this.slimModel = new PlayerModel<>(this.minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);

        if (this.previewEntity == null && this.minecraft.level != null) {
            this.previewEntity = new com.alexlego19.ainpcs.entity.NpcEntity(com.alexlego19.ainpcs.init.EntityInit.NPC.get(), this.minecraft.level);
        }

        // Layout Parameters
        int listWidth = (int) (this.width * 0.28);
        int previewWidth = (int) (this.width * 0.25);
        int centerAreaWidth = this.width - listWidth - previewWidth;
        int editorWidth = Math.min(180, centerAreaWidth - 20);
        int centerX = listWidth + (centerAreaWidth / 2);
        int centerY = this.height / 2;

        // Top Bar "New NPC" Button
        this.addRenderableWidget(Button.builder(Component.literal("New NPC"), b -> createNewProfile())
                .bounds(this.width - 110, 10, 100, 20).build());

        // Left Column (Profile List)
        this.profileList = new ProfileList(this.minecraft, listWidth - 20, this.height, 45, this.height - 40, 25);
        this.profileList.setLeftPos(10);
        this.addRenderableWidget(this.profileList);

        // Center Column (Editor Controls)
        this.nameEditBox = new EditBox(this.font, centerX - (editorWidth / 2), centerY - 70, editorWidth, 20, Component.literal("Name"));
        this.nameEditBox.setResponder(s -> {
            if (isEditable()) {
                selectedProfile.setName(s);
            }
        });
        this.addRenderableWidget(this.nameEditBox);

        this.skinDropdownBtn = Button.builder(Component.literal("Skin: alex"), b -> {
            if (isEditable()) {
                currentSkinIndex = (currentSkinIndex + 1) % availableSkins.size();
                String newSkin = availableSkins.get(currentSkinIndex);
                selectedProfile.setSkinId(newSkin);
                b.setMessage(Component.literal("Skin: " + newSkin));
            }
        }).bounds(centerX - (editorWidth / 2), centerY - 40, editorWidth, 20).build();
        this.addRenderableWidget(this.skinDropdownBtn);

        this.toggleModelBtn = Button.builder(Component.literal("Model: Slim"), b -> {
            if (isEditable()) {
                selectedProfile.setSlim(!selectedProfile.isSlim());
                b.setMessage(Component.literal("Model: " + (selectedProfile.isSlim() ? "Slim" : "Classic")));
            }
        }).bounds(centerX - (editorWidth / 2), centerY - 10, editorWidth, 20).build();
        this.addRenderableWidget(this.toggleModelBtn);

        this.toggleEnableBtn = Button.builder(Component.literal("Enabled: Yes"), b -> {
            if (isEditable()) {
                selectedProfile.setEnabled(!selectedProfile.isEnabled());
                b.setMessage(Component.literal("Enabled: " + (selectedProfile.isEnabled() ? "Yes" : "No")));
            }
        }).bounds(centerX - (editorWidth / 2), centerY + 20, editorWidth, 20).build();
        this.addRenderableWidget(this.toggleEnableBtn);

        this.deleteBtn = Button.builder(Component.literal("Delete"), b -> {
            if (isEditable()) {
                this.minecraft.setScreen(new ConfirmScreen(
                        this::confirmDelete,
                        Component.literal("Delete Profile?"),
                        Component.literal("Deleting this NPC profile will permanently remove it and despawn all existing instances. Are you sure you want to proceed?")
                ));
            }
        }).bounds(centerX - (editorWidth / 2), centerY + 50, editorWidth, 20).build();
        this.addRenderableWidget(this.deleteBtn);

        Button doneBtn = Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20).build();
        this.addRenderableWidget(doneBtn);

        updateEditorFields();
    }

    private boolean isEditable() {
        return selectedProfile != null && !selectedProfile.getId().equals(NPCProfileManager.STEVE_PROFILE_ID) && !selectedProfile.getId().equals(NPCProfileManager.ALEX_PROFILE_ID);
    }

    private void confirmDelete(boolean confirmed) {
        if (confirmed && isEditable()) {
            NPCProfileManager.removeProfile(selectedProfile.getId());
            this.selectedProfile = NPCProfileManager.STEVE_PROFILE;
            profileList.refreshList();
        }
        this.minecraft.setScreen(this);
        updateEditorFields();
    }

    private void createNewProfile() {
        String id = UUID.randomUUID().toString();
        String defaultName = "New NPC";
        String skinId = "alex";

        String expectedSkinName = defaultName.toLowerCase().replace(" ", "_");
        if (availableSkins.contains(expectedSkinName)) {
            skinId = expectedSkinName;
        }

        NPCProfile newProfile = new NPCProfile(id, defaultName, skinId, true, true);
        NPCProfileManager.addProfile(newProfile);
        this.selectedProfile = newProfile;

        this.profileList.refreshList();


        updateEditorFields();
    }

    private void updateEditorFields() {
        boolean editable = isEditable();

        this.nameEditBox.active = editable;
        this.skinDropdownBtn.active = editable;
        this.toggleModelBtn.active = editable;
        this.toggleEnableBtn.active = editable;
        this.deleteBtn.active = editable;

        this.nameEditBox.setValue(selectedProfile.getName());
        this.currentSkinIndex = Math.max(0, availableSkins.indexOf(selectedProfile.getSkinId()));
        this.skinDropdownBtn.setMessage(Component.literal("Skin: " + (selectedProfile.getSkinId() != null && !selectedProfile.getSkinId().isEmpty() ? selectedProfile.getSkinId() : "alex")));
        this.toggleModelBtn.setMessage(Component.literal("Model: " + (selectedProfile.isSlim() ? "Slim" : "Classic")));
        this.toggleEnableBtn.setMessage(Component.literal("Enabled: " + (selectedProfile.isEnabled() ? "Yes" : "No")));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blit(new net.minecraft.resources.ResourceLocation("textures/gui/options_background.png"), 0, 0, 0, 0.0F, 0.0F, this.width, 40, 32, 32);

        int listWidth = (int) (this.width * 0.28);
        int previewWidth = (int) (this.width * 0.25);
        int previewLeft = this.width - previewWidth;

        guiGraphics.fill(0, 40, listWidth, this.height, 0xFF202020);
        guiGraphics.fill(listWidth, 40, previewLeft, this.height, 0xFF101010);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (selectedProfile != null) {
            int renderX = previewLeft + (previewWidth / 2);
            int renderY = this.height - 60; // Anchor to feet

            guiGraphics.fill(previewLeft + 10, 60, this.width - 10, this.height - 20, 0x88000000);

            if (previewEntity != null) {
                previewEntity.setSlim(selectedProfile.isSlim());
                previewEntity.setVariant(selectedProfile.getId());
                previewEntity.setYRot(0.0F);
                previewEntity.setXRot(0.0F);
                previewEntity.yBodyRot = 0.0F;
                previewEntity.yHeadRot = 0.0F;
                previewEntity.yHeadRotO = 0.0F;
            }

            guiGraphics.enableScissor(previewLeft + 10, 60, this.width - 10, this.height - 20);

            float mouseXRot = (float)Math.atan((renderX - mouseX) / 40.0f);
            float mouseYRot = (float)Math.atan((renderY - mouseY - 100) / 40.0f);

            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(renderX, renderY, 1050.0F);
            // Fix upside-down issue: Scale positively, flip with ZP
            poseStack.scale(70.0F, 70.0F, 70.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(mouseYRot * 20.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(mouseXRot * 20.0F));

            PlayerModel<NpcEntity> model = (PlayerModel<NpcEntity>) (selectedProfile.isSlim() ? slimModel : classicModel);
            model.young = false;

            if (previewEntity != null) {
                model.setupAnim(previewEntity, 0.0F, 0.0F, this.minecraft.player != null ? this.minecraft.player.tickCount + partialTick : 0.0F, mouseXRot * 20.0F, mouseYRot * 20.0F);
            } else {
                model.setupAnim(null, 0.0F, 0.0F, 0.0F, mouseXRot * 20.0F, mouseYRot * 20.0F);
            }

            MultiBufferSource.BufferSource bufferSource = this.minecraft.renderBuffers().bufferSource();
            ResourceLocation skinLocation = DynamicSkinManager.getSkin(selectedProfile.getSkinId(), selectedProfile.isSlim());
            if (skinLocation == null) {
                skinLocation = new ResourceLocation("minecraft", "textures/entity/steve.png");
            }

            com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = bufferSource.getBuffer(model.renderType(skinLocation));
            model.renderToBuffer(poseStack, vertexConsumer, 15728880, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            bufferSource.endBatch();

            poseStack.popPose();
            guiGraphics.disableScissor();
        }
    }



    @Override
    public void onClose() {
        NPCProfileManager.save();
        this.minecraft.setScreen(this.parent);
    }

    class ProfileList extends ObjectSelectionList<ProfileList.Entry> {
        public ProfileList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
            this.setRenderBackground(false);
            refreshList();
        }

        public void refreshList() {
            this.clearEntries();
            this.addEntry(new Entry(NPCProfileManager.STEVE_PROFILE));
            this.addEntry(new Entry(NPCProfileManager.ALEX_PROFILE));
            for (NPCProfile profile : NPCProfileManager.getProfiles().values()) {
                if (!profile.getId().equals(NPCProfileManager.STEVE_PROFILE_ID) && !profile.getId().equals(NPCProfileManager.ALEX_PROFILE_ID)) {
                    this.addEntry(new Entry(profile));
                }
            }
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final NPCProfile profile;

            public Entry(NPCProfile profile) {
                this.profile = profile;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
                String label = profile.getName();
                if (profile.getId().equals(NPCProfileManager.STEVE_PROFILE_ID) || profile.getId().equals(NPCProfileManager.ALEX_PROFILE_ID)) {
                    label = "[" + profile.getName() + "]";
                }
                guiGraphics.drawString(font, label, left + 5, top + 5, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                selectedProfile = profile;

                updateEditorFields();
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(profile.getName());
            }
        }
    }
}
