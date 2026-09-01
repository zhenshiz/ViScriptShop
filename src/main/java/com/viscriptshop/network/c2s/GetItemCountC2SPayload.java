package com.viscriptshop.network.c2s;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.viscript_lib.util.CodecUtil;
import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.network.s2c.S2CPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public class GetItemCountC2SPayload {
    public static final String GET_ITEM_COUNT = C2SPayload.MOD_ID + "get_item_count";

    @RPCPacket(GET_ITEM_COUNT)
    public static void getItemCount(RPCSender sender, ShopInfo shopInfo) {
        ServerPlayer player = sender.asPlayer();
        if (player == null) return;

        sendItemCountSnapshot(player, shopInfo);
    }

    /**
     * 将玩家当前持有的商店成本物品作为完整快照推送给客户端。
     *
     * <p>快照覆盖所有分类中可能作为成本的物品。数量为零的条目不会写入快照，
     * 客户端通过整体替换已有数据来清除这些条目。
     *
     * @param  player 接收库存快照的服务端玩家
     * @param  shopInfo 用于收集潜在成本物品的商店信息
     */
    public static void sendItemCountSnapshot(ServerPlayer player, ShopInfo shopInfo) {
        AggregatedResources resources = new AggregatedResources();
        shopInfo.getCategoryInfos().forEach(categoryInfo -> {
            categoryInfo.getMerchants().forEach(merchantInfo -> {
                if (categoryInfo.getShopType().equals(CategoryInfo.ShopType.ITEM_FOR_ITEM)) {
                    resources.addItemEntry(merchantInfo.getSerializedItemA(), 1, merchantInfo.getItemAMatchRule());
                    resources.addItemEntry(merchantInfo.getSerializedItemB(), 1, merchantInfo.getItemBMatchRule());
                } else if (merchantInfo.getTradeType() == MerchantInfo.TradeType.SELL) {
                    resources.addItemEntry(merchantInfo.getSerializedItemResult(), 1, null);
                }
            });
        });

        resources.getItemEntries().removeIf(entry -> {
            int count = entry.getItemForPlayerCount(player);
            if (count <= 0) return true;
            entry.setCount(count);
            return false;
        });
        CompoundTag tag = CodecUtil.serializeList(resources.getItemEntries(), AggregatedResources.ItemEntry.CODEC, Platform.getFrozenRegistry());
        RPCPacketDistributor.rpcToPlayer(player, S2CPayload.GET_ITEM_COUNT, tag);
    }
}
