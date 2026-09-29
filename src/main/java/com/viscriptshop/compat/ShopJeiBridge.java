package com.viscriptshop.compat;

import com.viscriptshop.ViscriptShop;
import com.viscriptshop.compat.jei.ShopJeiClient;
import com.viscriptshop.gui.data.ShopDisplayEntry;

import java.util.List;

/** 将可选的 JEI 客户端实现隔离在已安装 JEI 的调用分支中。 */
public final class ShopJeiBridge {
    private ShopJeiBridge() {
    }

    public static void onScreenOpening(net.minecraft.client.gui.screens.Screen screen) {
        if (ViscriptShop.isJEILoaded()) ShopJeiClient.onScreenOpening(screen);
    }

    public static void receive(long revision, int batch, boolean last, List<ShopDisplayEntry> entries) {
        if (ViscriptShop.isJEILoaded()) ShopJeiClient.receive(revision, batch, last, entries);
    }

    public static void clear() {
        if (ViscriptShop.isJEILoaded()) ShopJeiClient.clear();
    }
}
