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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import com.mojang.math.Axis;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

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

    public NPCManagementScreen(Screen parent) {
        super(Component.literal("NPC Manager"));
        this.parent = parent;
        this.loadAvailableSkins();
    }

    private void loadAvailableSkins() {
        this.availableSkins.clear();
        this.availableSkins.add(""); // Empty represents default steve/alex
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

        int listWidth = Math.max(100, (int)(this.width * 0.2));
        int previewWidth = Math.max(120, (int)(this.width * 0.25));

        int centerAreaWidth = this.width - listWidth - previewWidth;
        int editorWidth = Math.min(180, centerAreaWidth - 20);
        int centerX = listWidth + (centerAreaWidth / 2);
        int centerY = this.height / 2;

        this.classicModel = new PlayerModel<>(this.minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        this.slimModel = new PlayerModel<>(this.minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);

        this.profileList = new ProfileList(this.minecraft, listWidth, this.height, 40, this.height - 40, 25);
        this.addRenderableWidget(this.profileList);

        this.addRenderableWidget(Button.builder(Component.literal("New NPC"), b -> createNewProfile())
                .bounds(10, 10, listWidth - 20, 20).build());

        this.nameEditBox = new EditBox(this.font, centerX - (editorWidth / 2), centerY - 70, editorWidth, 20, Component.literal("Name"));
        this.nameEditBox.setResponder(s -> {
            if (hasEditableSelected()) {
                profileList.getSelected().getProfile().setName(s);
            }
        });
        this.addRenderableWidget(this.nameEditBox);

        this.skinDropdownBtn = Button.builder(Component.literal("Skin: Default"), b -> {
            if (hasEditableSelected()) {
                currentSkinIndex = (currentSkinIndex + 1) % availableSkins.size();
                String newSkin = availableSkins.get(currentSkinIndex);
                profileList.getSelected().getProfile().setSkinId(newSkin);
                b.setMessage(Component.literal("Skin: " + (newSkin.isEmpty() ? "Default" : newSkin)));
            }
        }).bounds(centerX - (editorWidth / 2), centerY - 40, editorWidth, 20).build();
        this.addRenderableWidget(this.skinDropdownBtn);

        this.toggleModelBtn = Button.builder(Component.literal("Model: Classic"), b -> {
            if (hasEditableSelected()) {
                NPCProfile p = profileList.getSelected().getProfile();
                p.setSlim(!p.isSlim());
                b.setMessage(Component.literal("Model: " + (p.isSlim() ? "Slim" : "Classic")));
            }
        }).bounds(centerX - (editorWidth / 2), centerY - 10, editorWidth, 20).build();
        this.addRenderableWidget(this.toggleModelBtn);

        this.toggleEnableBtn = Button.builder(Component.literal("Enabled: Yes"), b -> {
            if (hasEditableSelected()) {
                NPCProfile p = profileList.getSelected().getProfile();
                p.setEnabled(!p.isEnabled());
                b.setMessage(Component.literal("Enabled: " + (p.isEnabled() ? "Yes" : "No")));
            }
        }).bounds(centerX - (editorWidth / 2), centerY + 20, editorWidth, 20).build();
        this.addRenderableWidget(this.toggleEnableBtn);

        this.deleteBtn = Button.builder(Component.literal("Delete"), b -> {
            if (hasEditableSelected()) {
                this.minecraft.setScreen(new ConfirmScreen(
                        this::confirmDelete,
                        Component.literal("Delete Profile?"),
                        Component.literal("Deleting this NPC profile will permanently remove it and despawn all existing instances currently in your worlds. Are you sure you want to proceed?")
                ));
            }
        }).bounds(centerX - (editorWidth / 2), centerY + 50, editorWidth, 20).build();
        this.addRenderableWidget(this.deleteBtn);

        Button doneBtn = Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(this.width - previewWidth - 100, this.height - 30, 90, 20).build();
        this.addRenderableWidget(doneBtn);

        updateEditorFields();
    }

    private void confirmDelete(boolean confirmed) {
        if (confirmed && hasEditableSelected()) {
            NPCProfileManager.removeProfile(profileList.getSelected().getProfile().getId());
            profileList.refreshList();
        }
        this.minecraft.setScreen(this);
        updateEditorFields();
    }

    private boolean hasEditableSelected() {
        return profileList.getSelected() != null && !profileList.getSelected().getProfile().getId().equals(NPCProfileManager.STEVE_PROFILE_ID) && !profileList.getSelected().getProfile().getId().equals(NPCProfileManager.ALEX_PROFILE_ID);
    }

    private void createNewProfile() {
        String id = UUID.randomUUID().toString();
        String defaultName = "New NPC";
        String skinId = "";

        String expectedSkinName = defaultName.toLowerCase().replace(" ", "_");
        if (availableSkins.contains(expectedSkinName)) {
            skinId = expectedSkinName;
        }

        NPCProfile newProfile = new NPCProfile(id, defaultName, skinId, false, true);
        NPCProfileManager.addProfile(newProfile);
        this.profileList.refreshList();
        this.profileList.setSelected(this.profileList.children().get(this.profileList.children().size() - 1));
        updateEditorFields();
    }

    private void updateEditorFields() {
        boolean selected = profileList.getSelected() != null;
        boolean editable = hasEditableSelected();

        this.nameEditBox.active = editable;
        this.skinDropdownBtn.active = editable;
        this.toggleModelBtn.active = editable;
        this.toggleEnableBtn.active = editable;
        this.deleteBtn.active = editable;

        if (selected) {
            NPCProfile p = profileList.getSelected().getProfile();
            this.nameEditBox.setValue(p.getName());

            this.currentSkinIndex = Math.max(0, availableSkins.indexOf(p.getSkinId()));
            this.skinDropdownBtn.setMessage(Component.literal("Skin: " + (p.getSkinId() != null && !p.getSkinId().isEmpty() ? p.getSkinId() : "Default")));

            this.toggleModelBtn.setMessage(Component.literal("Model: " + (p.isSlim() ? "Slim" : "Classic")));
            this.toggleEnableBtn.setMessage(Component.literal("Enabled: " + (p.isEnabled() ? "Yes" : "No")));
        } else {
            this.nameEditBox.setValue("");
            this.skinDropdownBtn.setMessage(Component.literal("Skin: Default"));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.profileList.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 16777215);

        int listWidth = Math.max(100, (int)(this.width * 0.2));
        int previewWidth = Math.max(120, (int)(this.width * 0.25));

        guiGraphics.fill(listWidth, 0, listWidth + 1, this.height, 0x80FFFFFF);
        guiGraphics.fill(this.width - previewWidth, 0, this.width - previewWidth + 1, this.height, 0x80FFFFFF);

        if (profileList.getSelected() != null) {
            NPCProfile p = profileList.getSelected().getProfile();

            int renderX = this.width - (previewWidth / 2);
            int renderY = this.height / 2 + 50;
            renderPreview(guiGraphics, renderX, renderY, 50, mouseX, mouseY, p);
        }
    }

    private void renderPreview(GuiGraphics guiGraphics, int x, int y, int scale, float mouseX, float mouseY, NPCProfile profile) {
        float rotX = (float)Math.atan((x - mouseX) / 40.0F);
        float rotY = (float)Math.atan((y - 50 - mouseY) / 40.0F);

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 1050.0D);
        poseStack.scale(1.0F, 1.0F, -1.0F);
        poseStack.translate(0.0D, 0.0D, 1000.0D);
        poseStack.scale((float)scale, (float)scale, (float)scale);

        Quaternionf quaternionf = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf quaternionf1 = Axis.XP.rotationDegrees(rotY * 20.0F);
        quaternionf.mul(quaternionf1);
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

        @Override
        public void setSelected(Entry entry) {
            super.setSelected(entry);
            updateEditorFields();
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final NPCProfile profile;

            public Entry(NPCProfile profile) {
                this.profile = profile;
            }

            public NPCProfile getProfile() {
                return profile;
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
                setSelected(this);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(profile.getName());
            }
        }
    }
}
