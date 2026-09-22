package com.alexlego19.ainpcs.client.gui;

import com.alexlego19.ainpcs.data.NPCProfile;
import com.alexlego19.ainpcs.data.NPCProfileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NPCProfileSelectionScreen extends Screen {
    private final NPCManagementScreen parent;
    private ProfileList profileList;
    private NPCProfile selectedProfile;

    public NPCProfileSelectionScreen(NPCManagementScreen parent, NPCProfile currentProfile) {
        super(Component.literal("Select NPC Profile"));
        this.parent = parent;
        this.selectedProfile = currentProfile;
    }

    @Override
    protected void init() {
        super.init();

        this.profileList = new ProfileList(this.minecraft, this.width, this.height, 32, this.height - 40, 25);
        this.addRenderableWidget(this.profileList);

        this.addRenderableWidget(Button.builder(Component.literal("Select"), b -> {
            if (this.selectedProfile != null) {
                this.parent.setSelectedProfile(this.selectedProfile);
            }
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 - 105, this.height - 30, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> {
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 + 5, this.height - 30, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.profileList.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    class ProfileList extends ObjectSelectionList<ProfileList.Entry> {
        public ProfileList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
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

                int color = 0xFFFFFF;
                if (selectedProfile != null && selectedProfile.getId().equals(profile.getId())) {
                    color = 0xFFFF00; // Highlight selected
                }

                guiGraphics.drawString(font, label, left + 5, top + 5, color);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                selectedProfile = profile;
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(profile.getName());
            }
        }
    }
}
