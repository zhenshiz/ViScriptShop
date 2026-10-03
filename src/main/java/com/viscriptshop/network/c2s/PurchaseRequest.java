package com.viscriptshop.network.c2s;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptshop.gui.data.AggregatedResources;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端购买清单，仅携带分类 ID、商品 ID 和购买数量。
 *
 * <p>价格、物品、货币、经验和指令不属于请求协议，由服务端根据清单重新计算。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseRequest implements IPersistedSerializable {
    @Persisted
    private List<AggregatedResources.PurchaseEntry> purchases = new ArrayList<>();
}
