package com.viscriptshop.event;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.compat.JeiHelper;
import com.viscriptshop.gui.ShopUI;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = ViscriptShop.MOD_ID, value = Dist.CLIENT)
public class ShopClientEvent {
    @SubscribeEvent
    public static void shopUiOpening(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof ModularUIScreen screen && screen.modularUI.ui.rootElement instanceof ShopUI shopUI) {
            MinecraftForge.EVENT_BUS.post(new com.viscriptshop.event.neoforge.ShopClientEvent.Opening(shopUI));
        }
    }

    @SubscribeEvent
    public static void shopUiClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof ModularUIScreen screen && screen.modularUI.ui.rootElement instanceof ShopUI shopUI) {
            MinecraftForge.EVENT_BUS.post(new com.viscriptshop.event.neoforge.ShopClientEvent.Closing(shopUI));
        }
    }

    @SubscribeEvent
    public static void shopUiKeyDown(ScreenEvent.KeyPressed.Pre event) {
        if (!ViscriptShop.isJEILoaded()
                || !(event.getScreen() instanceof ModularUIScreen screen)
                || !(screen.modularUI.ui.rootElement instanceof ShopUI shopUI)) {
            return;
        }

        ItemSlot itemSlot = getHoveredMerchantItem(screen, shopUI);
        if (itemSlot != null && JeiHelper.handleRecipeLookupKey(
                itemSlot.getValue(),
                event.getKeyCode(),
                event.getScanCode()
        )) {
            event.setCanceled(true);
        }
    }

    @Nullable
    private static ItemSlot getHoveredMerchantItem(ModularUIScreen screen, ShopUI shopUI) {
        ItemSlot hoveredItem = null;
        boolean merchantDisplay = false;
        UIElement merchants = shopUI.merchantsView.viewContainer;
        for (UIElement element = screen.modularUI.getLastHoveredElement(); element != null; element = element.getParent()) {
            if (hoveredItem == null && element instanceof ItemSlot itemSlot) {
                hoveredItem = itemSlot;
            }
            merchantDisplay |= element.hasClass("merchant-item-display");
            if (element == merchants) {
                return merchantDisplay ? hoveredItem : null;
            }
        }
        return null;
    }
}
