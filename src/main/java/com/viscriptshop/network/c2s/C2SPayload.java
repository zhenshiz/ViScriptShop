package com.viscriptshop.network.c2s;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.mojang.serialization.Codec;
import com.viscript_lib.util.CodecUtil;
import com.viscript_lib.util.item.ItemOutputTargets;
import com.viscriptshop.ShopRegistries;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.command.ShopCommand;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.gui.util.ShopEditorUploads;
import com.viscriptshop.network.s2c.S2CPayload;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class C2SPayload {
    public static final String MOD_ID = ViscriptShop.MOD_ID + ":";
    public static final String GET_SHOP_INFO_C2S = MOD_ID + "get_shop_info_c2s";
    public static final String OPEN_SHOP_UI_C2S = MOD_ID + "open_shop_ui_c2s";
    public static final String OPEN_FTB_SHOP_C2S = MOD_ID + "open_ftb_shop_c2s";
    public static final String UPLOAD_SHOP_FILE_C2S = MOD_ID + "upload_shop_file_c2s";
    public static final String SET_OUTPUT_TARGET_C2S = MOD_ID + "set_output_target_c2s";
    public static final String SET_CURRENCY_LAYOUT_C2S = MOD_ID + "set_currency_layout_c2s";

    @RPCPacket(GET_SHOP_INFO_C2S)
    public static void getShopInfo(RPCSender sender) {
        Map<String, String> shopInfos = new HashMap<>();
        ShopCommand.getServerShopFiles().forEach(fileName -> {
            ShopInfo shopInfo = ViScriptShopServerUtil.getShopInfo(fileName);
            if (shopInfo != null && shopInfo.isQuickOpening()) {
                String name = shopInfo.getName();
                shopInfos.put(fileName, name.isEmpty() ? "viscript_shop.ui.title" : name);
            }
        });
        Codec<Map<String, String>> codec = Codec.unboundedMap(Codec.STRING, Codec.STRING);
        RPCPacketDistributor.rpcToPlayer(sender.asPlayer(), S2CPayload.GET_SHOP_INFO_S2C, CodecUtil.serializeNBT(codec, shopInfos, Platform.getFrozenRegistry()));
    }

    @RPCPacket(OPEN_SHOP_UI_C2S)
    public static void openShopUI(RPCSender sender, String shopFileName, String categoryId, String merchantId) {
        ViScriptShopServerUtil.serverOpenShop(sender.asPlayer(), shopFileName, categoryId, merchantId);
    }

    @RPCPacket(OPEN_FTB_SHOP_C2S)
    public static void openFtbShop(RPCSender sender) {
        ServerPlayer player = sender.asPlayer();
        if (player != null) ViScriptShopServerUtil.serverOpenFtbShop(player);
    }

    @RPCPacket(UPLOAD_SHOP_FILE_C2S)
    public static void uploadShopFile(RPCSender sender, CompoundTag request) {
        ShopEditorUploads.receiveShopUpload(sender, request);
    }

    @RPCPacket(SET_OUTPUT_TARGET_C2S)
    public static void setOutputTarget(RPCSender sender, String outputTargetId) {
        ServerPlayer player = sender.asPlayer();
        if (player == null) return;

        ShopRegistries.Money data = player.getData(ShopRegistries.MONEY);
        data.setOutputTargetId(ItemOutputTargets.resolve(outputTargetId).name());
        player.setData(ShopRegistries.MONEY, data);
    }

    @RPCPacket(SET_CURRENCY_LAYOUT_C2S)
    public static void setCurrencyLayout(RPCSender sender, boolean gridLayout) {
        ServerPlayer player = sender.asPlayer();
        if (player == null) return;

        ShopRegistries.Money data = player.getData(ShopRegistries.MONEY);
        data.setCurrencyGridLayout(gridLayout);
        player.setData(ShopRegistries.MONEY, data);
    }
}
