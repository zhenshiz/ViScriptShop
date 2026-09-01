package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.viscriptshop.Config;
import com.viscriptshop.ShopRegistries;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.components.DialogSelect;
import com.viscriptshop.gui.data.ShopInfo;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.sirgrantd.sg_economy.api.SGEconomyApi;

import java.util.List;

public class ViScriptShopClientUtil {
    @Info("客户端打开快捷商店选择界面")
    public static void clientOpenShopSelector() {
        DialogSelect dialogSelect = new DialogSelect();
        ModularUI modularUI = new ModularUI(UI.of(dialogSelect));
        Minecraft.getInstance().setScreen(new ModularUIScreen(modularUI, Component.empty()));
    }

    @Info("客户端打开商店（带分类和商品参数）")
    public static void clientOpenShop(String shopLocation, ShopInfo shopInfo, String categoryId, String merchantId) {
        Minecraft minecraft = Minecraft.getInstance();
        ShopUI shopUI = new ShopUI(shopLocation, shopInfo, shopInfo.getName().isEmpty() ? "viscript_shop.ui.title" : shopInfo.getName(), categoryId, merchantId);
        if (shopInfo.getCategoryInfos().isEmpty()) {
            ViscriptShop.LOGGER.error("不合规的商店信息：商店分类栏为空");
            return;
        }
        ModularUI modularUI = new ModularUI(UI.of(shopUI, ShopUI::getAutoGuiScaledSize));
        minecraft.setScreen(new ModularUIScreen(modularUI, Component.empty()));
    }

    @Info("获取玩家钱")
    public static double getMoney(LocalPlayer player) {
        if (ViscriptShop.isMagicCoinsLoaded() && Config.isReplaceMoneyToMagicCoin.get()) {
            return MoneyUtil.normalize(SGEconomyApi.getBalance(player));
        }
        return MoneyUtil.normalize(player.getData(ShopRegistries.MONEY).getMoney());
    }

    @Info("获取玩家阶段标记")
    public static List<String> getStageFlags(LocalPlayer player) {
        return player.getData(ShopRegistries.MONEY).getFlags();
    }
}
