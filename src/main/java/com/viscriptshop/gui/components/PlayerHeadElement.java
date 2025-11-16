package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.appliedenergistics.yoga.YogaEdge;

public class PlayerHeadElement extends UIElement {
    public PlayerHeadElement() {
        super();
        layout(layout -> {
            layout.setWidth(16);
            layout.setHeight(16);
            layout.setMargin(YogaEdge.TOP, 5);
            layout.setMargin(YogaEdge.RIGHT, 5);
        });
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        RenderSystem.depthMask(false);
        guiContext.graphics.drawManaged(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;

            if (player != null) {
                ResourceLocation skin = player.getSkin().texture();
                var x = (int) getPositionX();
                var y = (int) getPositionY();
                var size = (int) getSizeWidth();

                guiContext.graphics.blit(skin, x, y, size, size, 8, 8, 8, 8, 64, 64);
            }
        });
        RenderSystem.depthMask(true);
    }
}
