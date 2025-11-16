package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

//商店信息
@Data
public class ShopInfo implements IConfigurable, IPersistedSerializable {
    public static final StreamCodec<ByteBuf, ShopInfo> STREAM_CODEC;
    public static final Codec<ShopInfo> CODEC;

    @Persisted
    @ReadOnlyManaged(serializeMethod = "writeMerchantInfo", deserializeMethod = "readMerchantInfo")
    private List<MerchantInfo> merchants = new ArrayList<>();
    @Persisted
    private int stage = 0;

    static {
        CODEC = PersistedParser.createCodec(ShopInfo::new);
        STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    }

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
