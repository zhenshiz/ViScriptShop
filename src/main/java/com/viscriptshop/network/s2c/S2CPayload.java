package com.viscriptshop.network.s2c;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.mojang.serialization.Codec;
import com.viscript_lib.util.CodecUtil;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.components.DialogSelect;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.util.ViScriptShopClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class S2CPayload {
    public static final String MOD_ID = ViscriptShop.MOD_ID + ":";
    public static final String OPEN_SHOP_EDITOR = MOD_ID + "open_shop_editor";
    private static final String LEGACY_SHOP_INFO_PROJECT = MOD_ID + "shop_info_project";
    public static final String OPEN_SHOP_SELECTOR = MOD_ID + "open_shop_selector";
    public static final String OPEN_SHOP_UI = MOD_ID + "open_shop_ui";
    public static final String SEND_MESSAGE = MOD_ID + "send_message";
    public static final String GET_SHOP_INFO_S2C = MOD_ID + "get_shop_info_s2c";
    public static final String GET_ITEM_COUNT = MOD_ID + "get_item_count_s2c";
    public static final String RELOAD_SHOP_UI = MOD_ID + "reload_shop_ui";
    public static final String UPDATE_OUT_OF_STOCK = MOD_ID + "update_out_of_stock";

    @RPCPacket(OPEN_SHOP_EDITOR)
    public static void openShopEditor(RPCSender sender, ShopInfo shopInfo) {
        EditorWindow editorWindow = getCurrentEditorWindow();
        if (editorWindow == null) return;

        Editor editor = editorWindow.getCurrentEditor();
        if (editor == null) return;
        try {
            Shop shop = new Shop();
            shop.shopInfo = shopInfo;
            editor.loadProject(shop, null);
        } catch (Exception ignored) {
        }
    }

    @RPCPacket(LEGACY_SHOP_INFO_PROJECT)
    public static void openShopEditorLegacy(RPCSender sender, ShopInfo shopInfo) {
        openShopEditor(sender, shopInfo);
    }

    @RPCPacket(OPEN_SHOP_SELECTOR)
    public static void openShopSelector(RPCSender sender) {
        ViScriptShopClientUtil.clientOpenShopSelector();
    }

    @RPCPacket(OPEN_SHOP_UI)
    public static void openShopUI(RPCSender sender, String shopLocation, ShopInfo shopInfo, String categoryId, String merchantId) {
        ViScriptShopClientUtil.clientOpenShop(shopLocation, shopInfo, categoryId, merchantId);
    }

    @RPCPacket(SEND_MESSAGE)
    public static void sendMessage(RPCSender sender, Message.Type messageType, Component message) {
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen) {
            Message.send(messageType, message.getString(), screen.modularUI.ui.rootElement);
        }
    }

    private static EditorWindow getCurrentEditorWindow() {
        if (Minecraft.getInstance().screen instanceof ModularUIContainerScreen screen
                && screen.getMenu().getModularUI().ui.rootElement instanceof EditorWindow editorWindow) {
            return editorWindow;
        }
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen
                && screen.modularUI.ui.rootElement instanceof EditorWindow editorWindow) {
            return editorWindow;
        }
        return null;
    }

    @RPCPacket(GET_SHOP_INFO_S2C)
    public static void getShopInfoS2C(RPCSender sender, CompoundTag compoundTag) {
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen && screen.modularUI.ui.rootElement instanceof DialogSelect dialogSelect) {
            Codec<Map<String, String>> codec = Codec.unboundedMap(Codec.STRING, Codec.STRING);
            dialogSelect.reload(CodecUtil.deserializeNBT(codec, compoundTag, Platform.getFrozenRegistry()));
        }
    }

    @RPCPacket(RELOAD_SHOP_UI)
    public static void reloadShopUI(RPCSender sender, ShopInfo shopInfo) {
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen
                && screen.modularUI.ui.rootElement instanceof ShopUI shopUI) {
            String selectedCategoryId = shopUI.getSelectedCategory() != null ? shopUI.getSelectedCategory().getId() : null;

            shopUI.currentShopInfo = shopInfo;

            if (selectedCategoryId != null) {
                shopUI.currentShopInfo.getCategoryInfos().stream()
                        .filter(c -> c.getId().equals(selectedCategoryId))
                        .findFirst()
                        .ifPresent(shopUI::setSelectedCategory);
            }

            shopUI.reloadMerchants();
            shopUI.reloadInventoryItem();
            shopUI.reloadShoppingItem();
            shopUI.reloadSearchComponent();
        }
    }

    @RPCPacket(UPDATE_OUT_OF_STOCK)
    public static void updateOutOfStock(RPCSender sender, String categoryId, String merchantId, int stock) {
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen
                && screen.modularUI.ui.rootElement instanceof ShopUI shopUI) {
            // 找到对应的分类和商品
            shopUI.currentShopInfo.getCategoryInfos().stream()
                    .filter(c -> c.getId().equals(categoryId))
                    .findFirst().flatMap(categoryInfo -> categoryInfo.getMerchants().stream()
                            .filter(m -> m.getId().equals(merchantId))
                            .findFirst()).ifPresent(merchantInfo -> {
                        // 将库存不足的商品的buyCount设置为0
                        merchantInfo.setBuyCount(0);
                        // 更新库存显示
                        merchantInfo.setStock(stock);
                        // 刷新UI
                        shopUI.reloadMerchants();
                        shopUI.reloadShoppingItem();
                        shopUI.reloadInventoryItem();
                    });
        }
    }

    @RPCPacket(GET_ITEM_COUNT)
    public static void getItemCount(RPCSender sender, CompoundTag tag) {
        var itemEntries = CodecUtil.deserializeList(tag, AggregatedResources.ItemEntry.CODEC, Platform.getFrozenRegistry());
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen
                && screen.modularUI.ui.rootElement instanceof ShopUI shopUI) {
            shopUI.playerItems.clear();
            itemEntries.forEach(shopUI::setItemCount);
            shopUI.reloadInventoryItem();
            shopUI.reloadShoppingItem();
            shopUI.reloadSearchComponent();
        }
    }
}
