package com.viscriptshop.event.neoforge;

import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.ShopInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Getter
@AllArgsConstructor
public class ShopServerEvent extends Event {
    private final ServerPlayer player;
    private final ShopInfo shopInfo;
    private final AggregatedResources costSummary;
    private final AggregatedResources gainSummary;

    @Cancelable
    public static class BuyPre extends ShopServerEvent {
        public BuyPre(ServerPlayer player, ShopInfo shopInfo, AggregatedResources costSummary, AggregatedResources gainSummary) {
            super(player, shopInfo, costSummary, gainSummary);
        }
    }

    public static class BuyFail extends ShopServerEvent {
        public BuyFail(ServerPlayer player, ShopInfo shopInfo, AggregatedResources costSummary, AggregatedResources gainSummary) {
            super(player, shopInfo, costSummary, gainSummary);
        }
    }

    public static class BuySuccess extends ShopServerEvent {
        public BuySuccess(ServerPlayer player, ShopInfo shopInfo, AggregatedResources costSummary, AggregatedResources gainSummary) {
            super(player, shopInfo, costSummary, gainSummary);
        }
    }
}
