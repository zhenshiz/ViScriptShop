package com.viscriptshop.network.s2c;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.viscriptshop.compat.ShopJeiBridge;
import com.viscriptshop.gui.data.ShopDisplayBatch;

/** 按有序批次传输服务端商店的展示数据。 */
public final class ShopJeiS2CPayload {
    public static final String SNAPSHOT = "viscript_shop:jei_snapshot";

    @RPCPacket(SNAPSHOT)
    public static void snapshot(RPCSender sender, ShopDisplayBatch batch) {
        if (sender.isServer()) ShopJeiBridge.receive(batch.getRevision(), batch.getIndex(), batch.isLast(), batch.getEntries());
    }
}
