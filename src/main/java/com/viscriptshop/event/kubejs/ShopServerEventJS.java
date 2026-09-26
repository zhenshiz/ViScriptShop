package com.viscriptshop.event.kubejs;

import com.viscriptshop.event.neoforge.ShopServerEvent;
import com.viscriptshop.gui.data.ShopInfo;
import dev.latvian.mods.kubejs.event.EventJS;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;

@Getter
@AllArgsConstructor
public class ShopServerEventJS extends EventJS {
    private final ServerPlayer player;
    private final ShopInfo shopInfo;

    public static class BuyPre extends ShopServerEventJS {
        public BuyPre(ShopServerEvent.BuyPre event) {
            super(event.getPlayer(), event.getShopInfo());
        }
    }

    public static class BuyFail extends ShopServerEventJS {
        public BuyFail(ShopServerEvent.BuyFail event) {
            super(event.getPlayer(), event.getShopInfo());
        }
    }

    public static class BuySuccess extends ShopServerEventJS {
        public BuySuccess(ShopServerEvent.BuySuccess event) {
            super(event.getPlayer(), event.getShopInfo());
        }
    }
}
