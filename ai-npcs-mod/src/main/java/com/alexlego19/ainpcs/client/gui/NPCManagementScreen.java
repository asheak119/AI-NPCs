package com.alexlego19.ainpcs.client.gui;

import com.alexlego19.ainpcs.client.manager.DynamicSkinManager;
import com.alexlego19.ainpcs.data.NPCProfile;
import com.alexlego19.ainpcs.data.NPCProfileManager;
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
    private Button deleteBtn;
    private Button profileDropdownBtn;
    private ProfileList profileList;

    private List<String> availableSkins = new ArrayList<>();
    private int currentSkinIndex = 0;

    private PlayerModel<?> classicModel;
    private PlayerModel<?> slimModel;

    private NPCProfile selectedProfile;
    private boolean isDropdownOpen = false;

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
            isDropdownOpen = !isDropdownOpen;
            if (profileList != null) {

            }
        }).bounds(10, 10, this.width - 130, 20).build();
        this.addRenderableWidget(this.profileDropdownBtn);

        // Top Bar "New NPC" Button
        this.addRenderableWidget(Button.builder(Component.literal("New NPC"), b -> createNewProfile())
                .bounds(this.width - 110, 10, 100, 20).build());

        // Profile List (Hidden by default, shown when dropdown clicked)
        this.profileList = new ProfileList(this.minecraft, this.width - 130, this.height, 35, this.height - 40, 25);
        this.profileList.setLeftPos(10);
        this.addWidget(this.profileList); // Add to children for clicks, but not renderables it gets added in the toggle button action.



        int leftPanelWidth = (int) (this.width * 0.66);
        int centerX = leftPanelWidth / 2;
        int centerY = (this.height - 40) / 2 + 20;
        int editorWidth = Math.min(200, leftPanelWidth - 40);

        this.nameEditBox = new EditBox(this.font, centerX - (editorWidth / 2), centerY - 70, editorWidth, 20, Component.literal("Name"));
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
        this.isDropdownOpen = false;


        updateEditorFields();
    }

    private void updateEditorFields() {
        updateDropdownLabel();
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
        // Draw the top bar using the vanilla dirt background logic by drawing a texture
        guiGraphics.blit(new net.minecraft.resources.ResourceLocation("textures/gui/options_background.png"), 0, 0, 0, 0.0F, 0.0F, this.width, 40, 32, 32);

        // Draw the dark lower sections (Left 2/3 and Right 1/3 with no dividing line)
        int leftWidth = (int) (this.width * 0.66);
        guiGraphics.fill(0, 40, this.width, this.height, 0xFF101010); // Solid dark gray for the entire lower section

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Preview Render inside the right 1/3 box
        int renderX = leftWidth + ((this.width - leftWidth) / 2);
        int renderY = (this.height - 40) / 2 + 80;
        renderPreview(guiGraphics, renderX, renderY, 70, mouseX, mouseY, selectedProfile);

        // Draw dropdown list last so it renders over the content
        if (isDropdownOpen && profileList != null) {
            guiGraphics.fill(10, 35, this.width - 120, this.height - 40, 0xFF000000);
            profileList.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderPreview(GuiGraphics guiGraphics, int x, int y, int scale, float mouseX, float mouseY, NPCProfile profile) {
        float rotX = (float)Math.atan((x - mouseX) / 40.0F);
        float rotY = (float)Math.atan((y - 50 - mouseY) / 40.0F);

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 1050.0D);

        // This is the definitive fix for model inversion using standard Vanilla pattern
        poseStack.scale((float)scale, (float)-scale, (float)scale);

        Quaternionf quaternionf = Axis.XP.rotationDegrees(rotY * 20.0F);
        poseStack.mulPose(quaternionf);

        PlayerModel<?> activeModel = profile.isSlim() ? this.slimModel : this.classicModel;
        activeModel.young = false;

        activeModel.head.yRot = rotX * 40.0F * ((float)Math.PI / 180F);
        activeModel.head.xRot = -rotY * 20.0F * ((float)Math.PI / 180F);
        activeModel.hat.yRot = activeModel.head.yRot;
        activeModel.hat.xRot = activeModel.head.xRot;
        activeModel.body.yRot = rotX * 20.0F * ((float)Math.PI / 180F);

        RenderSystem.setShaderLights(new org.joml.Vector3f(0.2F, 1.0F, -0.7F), new org.joml.Vector3f(-0.2F, 1.0F, 0.7F));

        MultiBufferSource.BufferSource bufferSource = this.minecraft.renderBuffers().bufferSource();
        activeModel.renderToBuffer(poseStack, bufferSource.getBuffer(activeModel.renderType(DynamicSkinManager.getSkin(profile.getSkinId(), profile.isSlim()))), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        bufferSource.endBatch();

        poseStack.popPose();
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
            this.addEntry(new Entry(NPCProfileManager.DEFAULT_PROFILE));
            for (NPCProfile profile : NPCProfileManager.getProfiles().values()) {
                if (!profile.getId().equals(NPCProfileManager.DEFAULT_PROFILE_ID)) {
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
                if (profile.getId().equals(NPCProfileManager.DEFAULT_PROFILE_ID)) {
                    label = "[Default]";
                }
                guiGraphics.drawString(font, label, left + 5, top + 5, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                selectedProfile = profile;
                isDropdownOpen = false;

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
