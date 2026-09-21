package com.alexlego19.ainpcs.client.gui;

import com.alexlego19.ainpcs.data.NPCProfile;
import com.alexlego19.ainpcs.data.NPCProfileManager;
import com.alexlego19.ainpcs.entity.NpcEntity;
import com.alexlego19.ainpcs.init.EntityInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

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

    private NpcEntity previewEntity;

    public NPCManagementScreen(Screen parent) {
        super(Component.literal("NPC Manager"));
        this.parent = parent;
        this.loadAvailableSkins();
    }

    private void loadAvailableSkins() {
        this.availableSkins.clear();
        this.availableSkins.add("steve"); // Default fallback
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
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Force creation of preview entity even without a level for Title Screen rendering
        if (this.minecraft.level != null) {
            this.previewEntity = new NpcEntity(EntityInit.NPC.get(), this.minecraft.level);
        }

        // Left List Navigation
        this.profileList = new ProfileList(this.minecraft, 150, this.height, 40, this.height - 40, 25);
        this.addRenderableWidget(this.profileList);

        this.addRenderableWidget(Button.builder(Component.literal("New NPC"), b -> createNewProfile())
                .bounds(25, 10, 100, 20).build());

        // Editor panel
        this.nameEditBox = new EditBox(this.font, centerX, centerY - 60, 150, 20, Component.literal("Name"));
        this.nameEditBox.setResponder(s -> {
            if (profileList.getSelected() != null) {
                profileList.getSelected().getProfile().setName(s);
            }
        });
        this.addRenderableWidget(this.nameEditBox);

        this.skinDropdownBtn = Button.builder(Component.literal("Skin: steve"), b -> {
            if (profileList.getSelected() != null) {
                currentSkinIndex = (currentSkinIndex + 1) % availableSkins.size();
                String newSkin = availableSkins.get(currentSkinIndex);
                profileList.getSelected().getProfile().setSkinId(newSkin);
                b.setMessage(Component.literal("Skin: " + newSkin));
                updatePreviewEntity();
            }
        }).bounds(centerX, centerY - 30, 150, 20).build();
        this.addRenderableWidget(this.skinDropdownBtn);

        this.toggleModelBtn = Button.builder(Component.literal("Model: Classic"), b -> {
            if (profileList.getSelected() != null) {
                NPCProfile p = profileList.getSelected().getProfile();
                p.setSlim(!p.isSlim());
                updatePreviewEntity();
                b.setMessage(Component.literal("Model: " + (p.isSlim() ? "Slim" : "Classic")));
            }
        }).bounds(centerX, centerY, 150, 20).build();
        this.addRenderableWidget(this.toggleModelBtn);

        this.toggleEnableBtn = Button.builder(Component.literal("Enabled: Yes"), b -> {
            if (profileList.getSelected() != null) {
                NPCProfile p = profileList.getSelected().getProfile();
                p.setEnabled(!p.isEnabled());
                b.setMessage(Component.literal("Enabled: " + (p.isEnabled() ? "Yes" : "No")));
            }
        }).bounds(centerX, centerY + 30, 150, 20).build();
        this.addRenderableWidget(this.toggleEnableBtn);

        this.deleteBtn = Button.builder(Component.literal("Delete"), b -> {
            if (profileList.getSelected() != null) {
                NPCProfileManager.removeProfile(profileList.getSelected().getProfile().getId());
                profileList.refreshList();
                updateEditorFields();
            }
        }).bounds(centerX, centerY + 60, 150, 20).build();
        this.addRenderableWidget(this.deleteBtn);

        Button doneBtn = Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20).build();
        this.addRenderableWidget(doneBtn);

        updateEditorFields();
    }

    private void createNewProfile() {
        String id = UUID.randomUUID().toString();
        String defaultName = "New NPC";
        String skinId = "steve";

        // Auto-assign skin if one matching the lowercase name exists
        String expectedSkinName = defaultName.toLowerCase().replace(" ", "_");
        if (availableSkins.contains(expectedSkinName)) {
            skinId = expectedSkinName;
        }

        NPCProfile newProfile = new NPCProfile(id, defaultName, skinId, false, true);
        NPCProfileManager.addProfile(newProfile);
        this.profileList.refreshList();

        // Auto-select the newly created profile
        this.profileList.setSelected(this.profileList.children().get(this.profileList.children().size() - 1));
        updateEditorFields();
    }

    private void updateEditorFields() {
        boolean active = profileList.getSelected() != null;
        this.nameEditBox.active = active;
        this.skinDropdownBtn.active = active;
        this.toggleModelBtn.active = active;
        this.toggleEnableBtn.active = active;
        this.deleteBtn.active = active;

        if (active) {
            NPCProfile p = profileList.getSelected().getProfile();
            this.nameEditBox.setValue(p.getName());

            this.currentSkinIndex = Math.max(0, availableSkins.indexOf(p.getSkinId()));
            this.skinDropdownBtn.setMessage(Component.literal("Skin: " + (p.getSkinId() != null ? p.getSkinId() : "steve")));

            this.toggleModelBtn.setMessage(Component.literal("Model: " + (p.isSlim() ? "Slim" : "Classic")));
            this.toggleEnableBtn.setMessage(Component.literal("Enabled: " + (p.isEnabled() ? "Yes" : "No")));
            updatePreviewEntity();
        } else {
            this.nameEditBox.setValue("");
            this.skinDropdownBtn.setMessage(Component.literal("Skin: steve"));
        }
    }

    private void updatePreviewEntity() {
        if (this.previewEntity != null && profileList.getSelected() != null) {
            NPCProfile p = profileList.getSelected().getProfile();
            this.previewEntity.setSlim(p.isSlim());
            this.previewEntity.setProfileId(p.getId());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.profileList.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 16777215);

        if (profileList.getSelected() != null && this.previewEntity != null) {
            int renderX = this.width / 2 - 60;
            int renderY = this.height / 2 + 60;

            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, renderX, renderY, 50, (float) renderX - mouseX, (float) (renderY - 50) - mouseY, this.previewEntity);
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
            refreshList();
        }

        public void refreshList() {
            this.clearEntries();
            for (NPCProfile profile : NPCProfileManager.getProfiles().values()) {
                this.addEntry(new Entry(profile));
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
                guiGraphics.drawString(font, profile.getName(), left + 5, top + 5, 0xFFFFFF);
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
