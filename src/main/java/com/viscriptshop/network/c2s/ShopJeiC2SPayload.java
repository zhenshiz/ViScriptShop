package com.viscriptshop.network.c2s;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.viscriptshop.util.ShopJeiCatalog;

/** JEI 按需展示和商店跳转请求；路径及商品权限始终在服务端校验。 */
public final class ShopJeiC2SPayload {
    public static final String REFRESH = "viscript_shop:jei_refresh";
    public static final String OPEN = "viscript_shop:jei_open";

    @RPCPacket(REFRESH)
    public static void refresh(RPCSender sender) {
        if (!sender.isServer()) ShopJeiCatalog.refresh(sender.asPlayer());
    }

    @RPCPacket(OPEN)
    public static void open(RPCSender sender, String location, String categoryId, String merchantId) {
        if (!sender.isServer()) ShopJeiCatalog.open(sender.asPlayer(), location, categoryId, merchantId);
    }
}
