package com.alexlego19.ainpcs.client.gui;

import com.alexlego19.ainpcs.client.manager.DynamicSkinManager;
import com.alexlego19.ainpcs.data.NPCProfile;
import com.alexlego19.ainpcs.data.NPCProfileManager;
import com.alexlego19.ainpcs.data.Temperament;
import com.alexlego19.ainpcs.data.Fortitude;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
    private Button temperamentBtn;
    private Button fortitudeBtn;
    private Button deleteBtn;
    private Button doneBtn;
    private Button profileDropdownBtn;

    private List<String> availableSkins = new ArrayList<>();
    private int currentSkinIndex = 0;

    private PlayerModel<?> classicModel;
    private PlayerModel<?> slimModel;

    private NPCProfile selectedProfile;

    public void setSelectedProfile(NPCProfile profile) {
        this.selectedProfile = profile;
        this.updateEditorFields();
    }

    public NPCManagementScreen(Screen parent) {
        super(Component.literal("NPC Manager"));
        this.parent = parent;
        this.selectedProfile = NPCProfileManager.DEFAULT_PROFILE;
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

        // Top Bar Dropdown Button
        this.profileDropdownBtn = Button.builder(Component.literal("Profile: " + selectedProfile.getName()), b -> {
            this.minecraft.setScreen(new NPCProfileSelectionScreen(this, selectedProfile));
        }).bounds(10, 10, this.width - 130, 20).build();
        this.addRenderableWidget(this.profileDropdownBtn);

        // Top Bar "New NPC" Button
        this.addRenderableWidget(Button.builder(Component.literal("New NPC"), b -> createNewProfile())
                .bounds(this.width - 110, 10, 100, 20).build());

        // Profile List (Hidden by default, shown when dropdown clicked)
                // Do not call this.addRenderableWidget(this.profileList) here; it gets added in the toggle button action.



        int leftPanelWidth = (int) (this.width * 0.66);
        int centerX = leftPanelWidth / 2;
        int centerY = (this.height - 40) / 2 + 20;
        int editorWidth = Math.min(200, leftPanelWidth - 40);

        this.nameEditBox = new EditBox(this.font, centerX - (editorWidth / 2), centerY - 90, editorWidth, 20, Component.literal("Name"));
        this.nameEditBox.setResponder(s -> {
            if (isEditable()) {
                selectedProfile.setName(s);
                updateDropdownLabel();
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
        }).bounds(centerX - (editorWidth / 2), centerY - 60, editorWidth, 20).build();
        this.addRenderableWidget(this.skinDropdownBtn);

        this.toggleModelBtn = Button.builder(Component.literal("Model: Slim"), b -> {
            if (isEditable()) {
                selectedProfile.setSlim(!selectedProfile.isSlim());
                b.setMessage(Component.literal("Model: " + (selectedProfile.isSlim() ? "Slim" : "Classic")));
            }
        }).bounds(centerX - (editorWidth / 2), centerY - 30, editorWidth, 20).build();
        this.addRenderableWidget(this.toggleModelBtn);

        this.toggleEnableBtn = Button.builder(Component.literal("Enabled: Yes"), b -> {
            if (isEditable()) {
                selectedProfile.setEnabled(!selectedProfile.isEnabled());
                b.setMessage(Component.literal("Enabled: " + (selectedProfile.isEnabled() ? "Yes" : "No")));
            }
        }).bounds(centerX - (editorWidth / 2), centerY, editorWidth, 20).build();
        this.addRenderableWidget(this.toggleEnableBtn);

        this.temperamentBtn = Button.builder(Component.literal("Temperament: Passive"), b -> {
            if (isEditable()) {
                Temperament[] vals = Temperament.values();
                int idx = (selectedProfile.getTemperament().ordinal() + 1) % vals.length;
                selectedProfile.setTemperament(vals[idx]);
                b.setMessage(Component.literal("Temperament: " + selectedProfile.getTemperament().getDisplayName()));
            }
        }).bounds(centerX - (editorWidth / 2), centerY + 30, editorWidth, 20).build();
        this.addRenderableWidget(this.temperamentBtn);

        this.fortitudeBtn = Button.builder(Component.literal("Fortitude: Medium"), b -> {
            if (isEditable()) {
                Fortitude[] vals = Fortitude.values();
                int idx = (selectedProfile.getFortitude().ordinal() + 1) % vals.length;
                selectedProfile.setFortitude(vals[idx]);
                b.setMessage(Component.literal("Fortitude: " + selectedProfile.getFortitude().getDisplayName()));
            }
        }).bounds(centerX - (editorWidth / 2), centerY + 60, editorWidth, 20).build();
        this.addRenderableWidget(this.fortitudeBtn);

        this.deleteBtn = Button.builder(Component.literal("Delete"), b -> {
            if (isEditable()) {
                this.minecraft.setScreen(new ConfirmScreen(
                        this::confirmDelete,
                        Component.literal("Delete Profile?"),
                        Component.literal("Deleting this NPC profile will permanently remove it and despawn all existing instances. Are you sure you want to proceed?")
                ));
            }
        }).bounds(centerX - (editorWidth / 2), centerY + 90, editorWidth, 20).build();
        this.addRenderableWidget(this.deleteBtn);

        this.doneBtn = Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20).build();
        this.addRenderableWidget(this.doneBtn);

        updateEditorFields();
    }

    private void updateDropdownLabel() {
        if (this.profileDropdownBtn != null) {
            String label = selectedProfile.getId().equals(NPCProfileManager.DEFAULT_PROFILE_ID) ? "[Default]" : selectedProfile.getName();
            this.profileDropdownBtn.setMessage(Component.literal("Profile: " + label));
        }
    }

    private boolean isEditable() {
        return selectedProfile != null && !selectedProfile.getId().equals(NPCProfileManager.DEFAULT_PROFILE_ID);
    }

    private void confirmDelete(boolean confirmed) {
        if (confirmed && isEditable()) {
            NPCProfileManager.removeProfile(selectedProfile.getId());
            this.selectedProfile = NPCProfileManager.DEFAULT_PROFILE;
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

        updateEditorFields();
    }

    private void updateEditorFields() {
        updateDropdownLabel();
        boolean editable = isEditable();

        this.nameEditBox.active = editable;
        this.skinDropdownBtn.active = editable;
        this.toggleModelBtn.active = editable;
        this.toggleEnableBtn.active = editable;
        this.temperamentBtn.active = editable;
        this.fortitudeBtn.active = editable;
        this.deleteBtn.active = editable;

        this.nameEditBox.setValue(selectedProfile.getName());
        this.currentSkinIndex = Math.max(0, availableSkins.indexOf(selectedProfile.getSkinId()));
        this.skinDropdownBtn.setMessage(Component.literal("Skin: " + (selectedProfile.getSkinId() != null && !selectedProfile.getSkinId().isEmpty() ? selectedProfile.getSkinId() : "alex")));
        this.toggleModelBtn.setMessage(Component.literal("Model: " + (selectedProfile.isSlim() ? "Slim" : "Classic")));
        this.toggleEnableBtn.setMessage(Component.literal("Enabled: " + (selectedProfile.isEnabled() ? "Yes" : "No")));
        if (selectedProfile.getTemperament() == null) selectedProfile.setTemperament(Temperament.PASSIVE);
        if (selectedProfile.getFortitude() == null) selectedProfile.setFortitude(Fortitude.MEDIUM);
        this.temperamentBtn.setMessage(Component.literal("Temperament: " + selectedProfile.getTemperament().getDisplayName()));
        this.fortitudeBtn.setMessage(Component.literal("Fortitude: " + selectedProfile.getFortitude().getDisplayName()));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int leftWidth = (int) (this.width * 0.66);
        net.minecraft.resources.ResourceLocation dirt = new net.minecraft.resources.ResourceLocation("textures/gui/options_background.png");

        // Darker inset part for the middle options section
        guiGraphics.setColor(0.125F, 0.125F, 0.125F, 1.0F);
        guiGraphics.blit(dirt, 0, 40, 0, 0.0F, 40.0F, this.width, this.height - 80, 32, 32);

        // Top Bar
        guiGraphics.setColor(0.25F, 0.25F, 0.25F, 1.0F);
        guiGraphics.blit(dirt, 0, 0, 0, 0.0F, 0.0F, this.width, 40, 32, 32);

        // Bottom Bar
        guiGraphics.blit(dirt, 0, this.height - 40, 0, 0.0F, (float)(this.height - 40), this.width, 40, 32, 32);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F); // Reset color

        // Draw drop shadows for top and bottom bars
        guiGraphics.fillGradient(0, 40, this.width, 44, 0xFF000000, 0x00000000);
        guiGraphics.fillGradient(0, this.height - 44, this.width, this.height - 40, 0x00000000, 0xFF000000);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Preview Render inside the right 1/3 box
        int renderX = leftWidth + ((this.width - leftWidth) / 2);
        int renderY = (this.height - 40) / 2 + 10; // Moved further up
        renderPreview(guiGraphics, renderX, renderY, 70, mouseX, mouseY, selectedProfile);
    }

    private void renderPreview(GuiGraphics guiGraphics, int x, int y, int scale, float mouseX, float mouseY, NPCProfile profile) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 1050.0D);

        // This is the definitive fix for model inversion using standard Vanilla pattern
        poseStack.scale((float)scale, (float)-scale, (float)scale);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));

        PlayerModel<?> activeModel = profile.isSlim() ? this.slimModel : this.classicModel;
        activeModel.young = false;

        // Static pose, looking straight ahead
        activeModel.head.yRot = 0.0F;
        activeModel.head.xRot = 0.0F;
        activeModel.hat.yRot = 0.0F;
        activeModel.hat.xRot = 0.0F;
        activeModel.body.yRot = 0.0F;
        activeModel.rightArm.xRot = 0.0F;
        activeModel.leftArm.xRot = 0.0F;
        activeModel.rightLeg.xRot = 0.0F;
        activeModel.leftLeg.xRot = 0.0F;

        // No directional lighting, use flat lighting
        com.mojang.blaze3d.platform.Lighting.setupForFlatItems();

        net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource = this.minecraft.renderBuffers().bufferSource();
        activeModel.renderToBuffer(poseStack, bufferSource.getBuffer(activeModel.renderType(DynamicSkinManager.getSkin(profile.getSkinId(), profile.isSlim()))), net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        bufferSource.endBatch();

        // Restore normal 3D GUI lighting so we don't break other elements
        com.mojang.blaze3d.platform.Lighting.setupFor3DItems();

        poseStack.popPose();
    }

    @Override
    public void onClose() {
        NPCProfileManager.save();
        this.minecraft.setScreen(this.parent);
    }

    }
