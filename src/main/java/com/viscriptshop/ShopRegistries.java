package com.viscriptshop;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscript_lib.util.item.ItemOutputTargets;
import com.viscriptshop.util.MoneyUtil;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ShopRegistries {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ViscriptShop.MOD_ID);

    public static final Supplier<AttachmentType<Money>> MONEY = ATTACHMENT_TYPES.register("money", () -> AttachmentType.builder(Money::new)
            .serialize(Money.CODEC)
            .sync(Money.STREAM_CODEC)
            .copyOnDeath()
            .build()
    );


    @Data
    public static class Money implements IPersistedSerializable {
        public static final Codec<Money> CODEC = PersistedParser.createCodec(Money::new);
        public static final StreamCodec<ByteBuf, Money> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
        @Persisted
        private double money;
        @Persisted
        private List<String> flags = new ArrayList<>();
        @Persisted
        private String outputTargetId = ItemOutputTargets.PLAYER_INVENTORY;

        /**
         * 设置玩家持有的 VSS 货币余额。
         *
         * <p>负数、非数字和无穷值均会被规范化为零。
         *
         * @param money 新的货币余额
         */
        public void setMoney(double money) {
            this.money = MoneyUtil.normalize(money);
        }
    }
}
