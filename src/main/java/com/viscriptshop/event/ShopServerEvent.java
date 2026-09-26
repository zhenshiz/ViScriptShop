package com.viscriptshop.event;

import com.lowdragmc.lowdraglib2.Platform;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.ShopSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ViscriptShop.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShopServerEvent {
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        //只需要保存在主世界的data目录下即可
        if (levelAccessor instanceof ServerLevel world && world.dimension() == Level.OVERWORLD) {
            ViscriptShop.setShopSavedData(world.getDataStorage().computeIfAbsent(nbt -> ShopSavedData.fromNbt(nbt, Platform.getFrozenRegistry()), ShopSavedData::new, "shop_info"));
        }
    }
}
