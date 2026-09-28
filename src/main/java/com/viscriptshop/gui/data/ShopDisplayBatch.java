package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** 商店快照的一批数据，通过已注册的直接访问器传输。 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class ShopDisplayBatch implements IPersistedSerializable {
    @Persisted private long revision;
    @Persisted private int index;
    @Persisted private boolean last;
    @Persisted private List<ShopDisplayEntry> entries = new ArrayList<>();
}
