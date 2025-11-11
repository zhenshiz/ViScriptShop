package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIScreen;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.network.s2c.OpenShopEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ViScriptShopUtil {

    public static void serverOpenShopEditor(ServerPlayer player) {
        player.connection.send(new OpenShopEditor());
    }

    public static void clientOpenNpcEditor() {
        Minecraft minecraft = Minecraft.getInstance();
        ModularUI ui = new ShopEditor().createUI();
        minecraft.setScreen(new ModularUIScreen(ui, Component.empty()));
    }
}
