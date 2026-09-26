package com.viscriptshop.event.neoforge;

import com.viscriptshop.gui.ShopUI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraftforge.eventbus.api.Event;

@Getter
@AllArgsConstructor
public abstract class ShopClientEvent extends Event {
    private final ShopUI shopUI;

    public static class Opening extends ShopClientEvent {
        public Opening(ShopUI shopUI) {
            super(shopUI);
        }
    }

    public static class Closing extends ShopClientEvent {
        public Closing(ShopUI shopUI) {
            super(shopUI);
        }
    }

    public static class Tick extends ShopClientEvent {
        public Tick(ShopUI shopUI) {
            super(shopUI);
        }
    }
}
