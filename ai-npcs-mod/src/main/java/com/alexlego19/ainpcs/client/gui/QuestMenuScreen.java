package com.alexlego19.ainpcs.client.gui;

import com.alexlego19.ainpcs.network.CancelQuestPacket;
import com.alexlego19.ainpcs.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

public class QuestMenuScreen extends Screen {
    public QuestMenuScreen() {
        super(Component.literal("Active Quests"));
    }

    @Override
    protected void init() {
        super.init();

        CompoundTag questData = null;
        if (this.minecraft != null && this.minecraft.player != null) {
            questData = this.minecraft.player.getPersistentData().getCompound("AiNpcsQuest");
        }

        if (questData != null && questData.contains("npcId")) {
            this.addRenderableWidget(Button.builder(Component.literal("Cancel Quest"), button -> {
                PacketHandler.INSTANCE.sendToServer(new CancelQuestPacket());
                this.minecraft.setScreen(null);
            }).bounds(this.width / 2 - 50, this.height / 2 + 30, 100, 20).build());
        } else {
            this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> {
                this.minecraft.setScreen(null);
            }).bounds(this.width / 2 - 50, this.height / 2 + 30, 100, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        CompoundTag questData = null;
        if (this.minecraft != null && this.minecraft.player != null) {
            questData = this.minecraft.player.getPersistentData().getCompound("AiNpcsQuest");
        }

        if (questData != null && questData.contains("npcId")) {
            String status = questData.getString("status");
            int current = questData.getInt("currentAmount");
            int target = questData.getInt("targetAmount");

            guiGraphics.drawString(this.font, "Quest: Kill Monsters at Spawner", this.width / 2 - 80, this.height / 2 - 20, 0xFFFFFF);
            guiGraphics.drawString(this.font, "Status: " + status, this.width / 2 - 80, this.height / 2, 0xAAAAAA);
            guiGraphics.drawString(this.font, "Progress: " + current + " / " + target, this.width / 2 - 80, this.height / 2 + 10, 0x55FF55);
        } else {
            guiGraphics.drawCenteredString(this.font, "No Active Quests", this.width / 2, this.height / 2, 0xFF5555);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}