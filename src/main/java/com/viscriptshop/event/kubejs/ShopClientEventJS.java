package com.viscriptshop.event.kubejs;

import com.viscriptshop.event.neoforge.ShopClientEvent;
import com.viscriptshop.gui.ShopUI;
import dev.latvian.mods.kubejs.event.EventJS;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ShopClientEventJS extends EventJS {
    private final ShopUI shopUI;

    public static class Opening extends ShopClientEventJS {
        public Opening(ShopClientEvent.Opening event) {
            super(event.getShopUI());
        }
    }

    public static class Closing extends ShopClientEventJS {
        public Closing(ShopClientEvent.Closing event) {
            super(event.getShopUI());
        }
    }

    public static class Tick extends ShopClientEventJS {
        public Tick(ShopClientEvent.Tick event) {
            super(event.getShopUI());
        }
    }
}
