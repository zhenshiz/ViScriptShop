package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIScreen;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.network.s2c.OpenShopEditor;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ViScriptShopUtil {

    @Info("服务端打开商店编辑器")
    public static void serverOpenShopEditor(ServerPlayer player) {
        player.connection.send(new OpenShopEditor());
    }

    @Info("客户端打开商店编辑器")
    public static void clientOpenNpcEditor() {
        Minecraft minecraft = Minecraft.getInstance();
        ModularUI ui = new ShopEditor().createUI();
        minecraft.setScreen(new ModularUIScreen(ui, Component.empty()));
    }

    @Info("服务端打开商店")
    public static void serverOpenShop(ServerPlayer player, String shop) {
    }

    @Info("客户端打开商店")
    public static void clientOpenShop(String shop) {

    }

    @Info("重置商店信息")
    public static void reloadOpenShop(String shop) {
    }

    @Info("设置当前商店的阶段值")
    public static void setStageShop(String shop, int stage) {
    }
}
