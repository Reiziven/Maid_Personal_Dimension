package com.tlmpersonal.tlmpersonaldimension.client.screen;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import com.tlmpersonal.tlmpersonaldimension.client.gui.PersonalDimensionGui;
import com.tlmpersonal.tlmpersonaldimension.client.gui.PersonalDimensionMainGui;
import com.tlmpersonal.tlmpersonaldimension.inventory.PersonalDimensionMenu;
import com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket;
import com.tlmpersonal.tlmpersonaldimension.network.TeleportToPersonalDimensionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class PersonalDimensionMaidScreen extends AbstractMaidContainerGui<PersonalDimensionMenu> {
    private static final ResourceLocation ISLAND_ART = new ResourceLocation(
            Touhoulittlemaidpersonaldimension.MODID, "textures/gui/default_task_config.png");

    private static final int PAGE_X_OFFSET = 80;
    private static final int PAGE_Y_OFFSET = 28;
    private static final int PAGE_WIDTH = 176;
    private static final int PAGE_HEIGHT = 137;

    private final EntityMaid maid;
    private long cooldownEndTime = 0;
    private Button getTeleporterButton;

    public PersonalDimensionMaidScreen(PersonalDimensionMenu container, Inventory inv, Component titleIn) {
        super(container, inv, titleIn);
        this.imageHeight = 256;
        this.imageWidth = 256;
        this.maid = menu.getMaid();
    }

    @Override
    protected void init() {
        super.init();
        int left = leftPos + PAGE_X_OFFSET;
        int top = topPos + PAGE_Y_OFFSET;

        if (Config.USE_MAID_GUI_CONTROLS.get()) {
            this.addRenderableWidget(new TransparentButton(left + 8, top + 20, 160, 20,
                    Component.literal("Teleport (with Maid)"), button -> {
                if (maid.getFavorabilityManager().getLevel() >= 3) {
                    Touhoulittlemaidpersonaldimension.NETWORK.sendToServer(
                            new TeleportToPersonalDimensionPacket(maid.getId(), true));
                    this.onClose();
                }
            }));

            getTeleporterButton = new TransparentButton(left + 8, top + 45, 160, 20,
                    Component.literal("Get teleporter"), button ->
                    Touhoulittlemaidpersonaldimension.NETWORK.sendToServer(
                            new PersonalDimensionGuiPacket(PersonalDimensionGuiPacket.Action.GET_TELEPORTER, "", maid.getId())));
            this.addRenderableWidget(getTeleporterButton);

            this.addRenderableWidget(new TransparentButton(left + 8, top + 105, 160, 20,
                    Component.literal("Dimensional Control"), button -> {
                if (this.minecraft != null) {
                    this.onClose();
                    this.minecraft.setScreen(new PersonalDimensionGui(maid));
                }
            }));
        }

        Touhoulittlemaidpersonaldimension.NETWORK.sendToServer(
                new PersonalDimensionGuiPacket(PersonalDimensionGuiPacket.Action.REQUEST_TELEPORTER_COOLDOWN, "", maid.getId()));
    }

    public void updateCooldown(long cooldownEndMs) {
        this.cooldownEndTime = cooldownEndMs;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        // Delegate fully to parent — draws background texture, maid character, HP bars, tabs, etc.
        super.renderBg(graphics, partialTicks, x, y);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void renderAddition(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int left = leftPos + PAGE_X_OFFSET;
        int top = topPos + PAGE_Y_OFFSET;
        int centerX = left + (PAGE_WIDTH / 2);

        // Draw our island art panel (on top of the base maid GUI which is already rendered)
        graphics.fill(left, top, left + PAGE_WIDTH, top + PAGE_HEIGHT, 0xFFC6C6C6);
        graphics.blit(ISLAND_ART, left, top, 0, 0, PAGE_WIDTH, PAGE_HEIGHT, 256, 256);
        graphics.renderOutline(left, top, PAGE_WIDTH, PAGE_HEIGHT, 0xFF000000);

        if (Config.USE_MAID_GUI_CONTROLS.get()) {
            if (getTeleporterButton != null) {
                long now = System.currentTimeMillis();
                if (cooldownEndTime > 0 && now < cooldownEndTime) {
                    long remainingSec = (cooldownEndTime - now) / 1000;
                    getTeleporterButton.setMessage(Component.literal(
                            String.format("Cooldown: %dm %ds", remainingSec / 60, remainingSec % 60)));
                    getTeleporterButton.active = false;
                } else {
                    getTeleporterButton.setMessage(Component.literal("Get teleporter"));
                    getTeleporterButton.active = true;
                }
            }
            graphics.drawCenteredString(this.font, "Maid Personal Dimension", centerX, top + 8, 0xFCB5C3);
            graphics.drawCenteredString(this.font, "Favorability Level Req: 3", centerX, top + 75, 0xAAF953F9);
            Double powerCost = Config.TELEPORT_COST_POWER_POINTS.get();
            int xpCost = Config.TELEPORT_COST_XP.get();
            graphics.drawCenteredString(this.font, "Power: " + powerCost + " | XP: " + xpCost, centerX, top + 90, 0xAAFCA800);
            if (mouseX >= left && mouseX < left + PAGE_WIDTH && mouseY >= top && mouseY < top + PAGE_HEIGHT) {
                graphics.renderComponentTooltip(this.font, List.of(
                        Component.literal("Click to open dimension GUI").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Right Click to teleport to dimension")
                                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFfb7c5)))), mouseX, mouseY);
            }
        } else {
            graphics.drawString(this.font, Component.literal("Maid Personal Dimension").withStyle(ChatFormatting.BOLD),
                    left + 8, top + 8, 0xFCB5C3, false);
            int wrapY = top + 18;
            for (net.minecraft.util.FormattedCharSequence line : this.font.split(
                    Component.literal("A private space for your maid where she has total authority over the environment."),
                    PAGE_WIDTH - 16)) {
                graphics.drawString(this.font, line, left + 8, wrapY, 0x545454, false);
                wrapY += this.font.lineHeight + 1;
            }
            if (mouseX >= left && mouseX < left + PAGE_WIDTH && mouseY >= top && mouseY < top + PAGE_HEIGHT) {
                graphics.fill(left, top, left + PAGE_WIDTH, top + PAGE_HEIGHT, 0x25FFFFFF);
                graphics.renderComponentTooltip(this.font, List.of(
                        Component.literal("Click to open dimension GUI").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Right Click to teleport to dimension")
                                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFfb7c5)))), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = leftPos + PAGE_X_OFFSET;
        int top = topPos + PAGE_Y_OFFSET;
        if (mouseX >= left && mouseX < left + PAGE_WIDTH && mouseY >= top && mouseY < top + PAGE_HEIGHT) {
            if (button == 1 && maid.getFavorabilityManager().getLevel() >= 3) {
                Touhoulittlemaidpersonaldimension.NETWORK.sendToServer(
                        new TeleportToPersonalDimensionPacket(maid.getId(), true));
                this.onClose();
                return true;
            } else if (button == 0 && !Config.USE_MAID_GUI_CONTROLS.get() && this.minecraft != null) {
                this.onClose();
                this.minecraft.setScreen(new PersonalDimensionMainGui(maid));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private class TransparentButton extends Button {
        public TransparentButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                if (this.isHoveredOrFocused())
                    graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x15FFFFFF);
                graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                        this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, 0xAA545454);
            }
        }
    }
}
