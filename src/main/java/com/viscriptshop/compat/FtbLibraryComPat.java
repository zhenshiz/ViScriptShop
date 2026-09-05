package com.viscriptshop.compat;

import com.viscriptshop.Config;
import com.viscriptshop.ViscriptShop;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptshop.network.c2s.C2SPayload;
import dev.ftb.mods.ftblibrary.api.sidebar.SidebarButtonCreatedEvent;
import dev.ftb.mods.ftblibrary.sidebar.RegisteredSidebarButton;

public class FtbLibraryComPat {
    public static void init() {
        if (ViscriptShop.isFtbLibraryLoaded()) {
            SidebarButtonCreatedEvent.EVENT.register(event -> {
                RegisteredSidebarButton button = event.getButton();
                if (button.getId().equals(ViscriptShop.id("shop"))) {
                    button.addVisibilityCondition(() -> Config.showFtbLibraryButton.get());
                }
            });
        }
    }

    public static void click() {
        if (ViscriptShop.isFtbLibraryLoaded()) {
            RPCPacketDistributor.rpcToServer(C2SPayload.OPEN_FTB_SHOP_C2S);
        }
    }
}
