package com.viscriptshop.event.neoforge;

import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.ShopInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 表示服务端商店购买流程事件。
 *
 * <p>事件监听器属于 Minecraft 运行时边界，可通过
 * {@link AggregatedResources.ItemEntry#getItemStack()} 获取用于匹配或渲染的原版物品模板，
 * 并通过 {@link AggregatedResources.ItemEntry#getCount()} 读取完整长整型数量。
 * {@link AggregatedResources#getItems()} 仅作为旧接口兼容视图保留，超过整数上限的数量会被截断。
 */
@Getter
@AllArgsConstructor
public class ShopServerEvent extends Event {
    private final ServerPlayer player;
    private final ShopInfo shopInfo;
    private final AggregatedResources costSummary;
    private final AggregatedResources gainSummary;

    public static class BuyPre extends ShopServerEvent implements ICancellableEvent {
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
