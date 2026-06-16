package com.tlmpersonal.tlmpersonaldimension.client.gui;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket;
import com.tlmpersonal.tlmpersonaldimension.network.TeleportToPersonalDimensionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class PersonalDimensionMainGui extends Screen {
    private final EntityMaid maid;
    private static final int GUI_WIDTH = 250;
    private static final int GUI_HEIGHT = 200;
    private long cooldownEndTime = 0;
    private Button getTeleporterButton;

    public PersonalDimensionMainGui(EntityMaid maid) {
        super(Component.translatable("gui.tlmpersonaldimension.main_title"));
        this.maid = maid;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = (this.width - GUI_WIDTH) / 2;
        int centerY = (this.height - GUI_HEIGHT) / 2;

        // Teleport Button
        this.addRenderableWidget(
            Button.builder(Component.literal("Teleport (with Maid)"),
                button -> {
                    if (checkFavorability()) {
                        PacketDistributor.sendToServer(new TeleportToPersonalDimensionPacket(maid.getId(), true));
                        this.onClose();
                    }
                })
            .bounds(this.width / 2 - 80, centerY + 40, 160, 20)
            .build()
        );

        // Get Teleporter Button
        getTeleporterButton = Button.builder(
            Component.literal("Get teleporter"),
            button -> {
                PacketDistributor.sendToServer(new PersonalDimensionGuiPacket(
                    PersonalDimensionGuiPacket.Action.GET_TELEPORTER,
                    "",
                    maid.getId()
                ));
            }
        ).bounds(this.width / 2 - 80, centerY + 70, 160, 20).build();
        this.addRenderableWidget(getTeleporterButton);

        // Dimensional Control Button
        this.addRenderableWidget(
            Button.builder(Component.literal("Dimensional Control"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new PersonalDimensionGui(maid));
                    }
                })
            .bounds(this.width / 2 - 80, centerY + 135, 160, 20)
            .build()
        );

        // Request cooldown sync
        PacketDistributor.sendToServer(new PersonalDimensionGuiPacket(
            PersonalDimensionGuiPacket.Action.REQUEST_TELEPORTER_COOLDOWN,
            "",
            maid.getId()
        ));
    }

    public void updateCooldown(long cooldownEndMs) {
        this.cooldownEndTime = cooldownEndMs;
    }

    private boolean checkFavorability() {
        return maid.getFavorabilityManager().getLevel() >= 3;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Update cooldown text on button
        long now = System.currentTimeMillis();
        if (cooldownEndTime > 0 && now < cooldownEndTime) {
            long remainingSec = (cooldownEndTime - now) / 1000;
            getTeleporterButton.setMessage(Component.literal(String.format("Cooldown: %dm %ds", remainingSec / 60, remainingSec % 60)));
            getTeleporterButton.active = false;
        } else {
            getTeleporterButton.setMessage(Component.literal("Get teleporter"));
            getTeleporterButton.active = true;
        }

        // 1. Render Background
        this.renderTransparentBackground(graphics);
        
        // 2. Render Widgets
        super.render(graphics, mouseX, mouseY, partialTicks);
        
        // 3. Render Text
        int centerX = this.width / 2;
        int centerY = (this.height - GUI_HEIGHT) / 2;

        graphics.drawCenteredString(this.font, "Maid Personal Dimension", centerX, centerY + 15, 0xFfb7c5);
        
        // Hardcoded descriptions for reliability
        graphics.drawCenteredString(this.font, "Requires Maid Favorability Level: 3", centerX, centerY + 95, 0xFC54FC);
        
        Double powerCost = Config.TELEPORT_COST_POWER_POINTS.get();
        int xpCost = Config.TELEPORT_COST_XP.get();
        graphics.drawCenteredString(this.font, "Power Cost: " + powerCost + " | XP Cost: " + xpCost, centerX, centerY + 110, 0xFFAA00);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
