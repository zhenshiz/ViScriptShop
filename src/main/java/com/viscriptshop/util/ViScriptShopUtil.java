package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIScreen;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.gui.data.ShopSavedData;
import com.viscriptshop.network.s2c.OpenShopEditorPayload;
import com.viscriptshop.network.s2c.OpenShopUIPayload;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ViScriptShopUtil {

    @Info("服务端打开商店编辑器")
    public static void serverOpenShopEditor(ServerPlayer player) {
        player.connection.send(new OpenShopEditorPayload());
    }

    @Info("客户端打开商店编辑器")
    public static void clientOpenNpcEditor() {
        Minecraft minecraft = Minecraft.getInstance();
        ModularUI ui = UIElementUtil.createUI(new ShopEditor()).shouldCloseOnEsc(false);
        minecraft.setScreen(new ModularUIScreen(ui, Component.empty()));
    }

    @Info("服务端打开商店")
    public static void serverOpenShop(ServerPlayer player, ResourceLocation res, Component title) {
        String shopPath = toPath(res);
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        ShopInfo shopInfo = shopSavedData.getShopInfo(shopPath);
        if (shopInfo == null) {
            Shop shop = ShopHelper.getShop(shopPath);
            if (shop != null) {
                shopInfo = shop.getShopInfo();
                shopSavedData.setShopInfo(shopPath, shopInfo);
            } else {
                ViscriptShop.LOGGER.error("shop file {} not found", shopPath);
                return;
            }
        }
        player.connection.send(new OpenShopUIPayload(shopInfo, title.getString()));
    }

    @Info("客户端打开商店")
    public static void clientOpenShop(ShopInfo shopInfo, String title) {
        Minecraft minecraft = Minecraft.getInstance();
        ModularUI ui = UIElementUtil.createUI(new ShopUI(shopInfo, title));
        minecraft.setScreen(new ModularUIScreen(ui, Component.empty()));
    }

    @Info("重置商店信息")
    public static void reloadOpenShop(ResourceLocation res) {
        String shop = toPath(res);
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        shopSavedData.resetShopInfo(shop);
        ShopHelper.removeCache(shop);
    }

    @Info("获取商店的信息")
    public static ShopInfo getShopInfo(ResourceLocation res) {
        String shop = toPath(res);
        return ViscriptShop.getShopSavedData().getShopInfo(shop);
    }

    @Info("设置当前商店的阶段值")
    public static void setStageShop(ResourceLocation res, int stage) {
        String shop = toPath(res);
        ShopSavedData shopSavedData = ViscriptShop.getShopSavedData();
        ShopInfo shopInfo = shopSavedData.getShopInfo(shop);
        shopInfo.setStage(stage);
        shopSavedData.setShopInfo(shop, shopInfo);
    }

    @HideFromJS
    public static int removeItem(Player player, ItemStack item, int count) {
        return player.getInventory().clearOrCountMatchingItems(itemStack -> ItemStack.isSameItemSameComponents(itemStack, item), count, player.inventoryMenu.getCraftSlots());
    }

    private static String toPath(ResourceLocation location) {
        return location.toString().replaceAll(ViscriptShop.MOD_ID + ":", "");
    }
}
