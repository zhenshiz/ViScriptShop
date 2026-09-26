package com.viscriptshop.event;

import com.viscriptshop.event.kubejs.ShopClientEventJS;
import com.viscriptshop.event.kubejs.ShopServerEventJS;
import com.viscriptshop.event.neoforge.ShopClientEvent;
import com.viscriptshop.event.neoforge.ShopServerEvent;
import dev.latvian.mods.kubejs.event.EventResult;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CommonEventsPostJS {
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void shopOpening(ShopClientEvent.Opening event) {
        if (ViScriptShopEventsJS.OPENING.hasListeners()) {
            ViScriptShopEventsJS.OPENING.post(new ShopClientEventJS.Opening(event));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void shopClosing(ShopClientEvent.Closing event) {
        if (ViScriptShopEventsJS.CLOSING.hasListeners()) {
            ViScriptShopEventsJS.CLOSING.post(new ShopClientEventJS.Closing(event));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void shopTick(ShopClientEvent.Tick event) {
        if (ViScriptShopEventsJS.TICK.hasListeners()) {
            ViScriptShopEventsJS.TICK.post(new ShopClientEventJS.Tick(event));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void shopBuyPre(ShopServerEvent.BuyPre event) {
        if (ViScriptShopEventsJS.BUY_PRE.hasListeners()) {
            EventResult result = ViScriptShopEventsJS.BUY_PRE.post(new ShopServerEventJS.BuyPre(event));
            if (result.interruptFalse()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void shopBuyFail(ShopServerEvent.BuyFail event) {
        if (ViScriptShopEventsJS.BUY_FAIL.hasListeners()) {
            ViScriptShopEventsJS.BUY_FAIL.post(new ShopServerEventJS.BuyFail(event));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void shopBuyFail(ShopServerEvent.BuySuccess event) {
        if (ViScriptShopEventsJS.BUY_SUCCESS.hasListeners()) {
            ViScriptShopEventsJS.BUY_SUCCESS.post(new ShopServerEventJS.BuySuccess(event));
        }
    }
}
