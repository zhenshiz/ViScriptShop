package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import lombok.Data;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

//商店信息
@Data
public class ShopInfo implements IConfigurable, IPersistedSerializable {
    @Persisted
    @ReadOnlyManaged(serializeMethod = "writeMerchantInfo", deserializeMethod = "readMerchantInfo")
    List<MerchantInfo> merchants = new ArrayList<>();

    private Tag writeMerchantInfo(List<MerchantInfo> value) {
        return IntTag.valueOf(value.size());
    }

    private List<MerchantInfo> readMerchantInfo(IntTag tag) {
        var groups = new ArrayList<MerchantInfo>();
        for (int i = 0; i < tag.getAsInt(); i++) {
            groups.add(new MerchantInfo());
        }
        return groups;
    }
}
