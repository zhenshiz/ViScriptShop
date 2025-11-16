package com.viscriptshop.event;

import com.viscriptshop.ViscriptShop;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = ViscriptShop.MOD_ID)
public class TestEvent {

    @SubscribeEvent
    public static void reloadCommandSuggestions(ScreenEvent.Opening event) {
    }
}
