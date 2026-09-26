package com.viscriptshop;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscript_lib.util.item.ItemOutputTargets;
import com.viscriptshop.util.MoneyUtil;
import lombok.Data;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.registries.DeferredRegister;
import net.nikdo53.neobackports.io.StreamCodec;
import net.nikdo53.neobackports.io.attachment.AdvancedCapabilityType;
import net.nikdo53.neobackports.io.attachment.AttachmentType;
import net.nikdo53.neobackports.io.attachment.DataAttachment;
import net.nikdo53.neobackports.registry.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ShopRegistries {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ViscriptShop.MOD_ID);
    public static final Capability<MoneyAttachment> MONEY_CAP = CapabilityManager.get(new CapabilityToken<>() {});

    public static final Supplier<AttachmentType<Money>> MONEY = ATTACHMENT_TYPES.register("money",
            () -> AttachmentType.builder(MONEY_CAP, Money::new)
                    .canAttachTo(AdvancedCapabilityType.PLAYER)
                    .serialize(Money.CODEC).sync(Money.STREAM_CODEC)
                    .copyOnDeath().build()
    );

    public static class MoneyAttachment extends DataAttachment<Money> {}

    @Data
    public static class Money implements IPersistedSerializable {
        public static final Codec<Money> CODEC = PersistedParser.createCodec(Money::new);
        public static final StreamCodec<Money> STREAM_CODEC = PersistedParser.createStreamCodec(Money::new);
        @Persisted
        private double money;
        @Persisted
        private List<String> flags = new ArrayList<>();
        @Persisted
        private String outputTargetId = ItemOutputTargets.PLAYER_INVENTORY;
        /** 玩家偏好的虚拟货币商品布局；旧存档未配置时仍使用列表。 */
        @Persisted
        private boolean currencyGridLayout;

        /**
         * 设置玩家持有的 VSS 货币余额。
         *
         * <p>保留负余额；非数字和无穷值会被规范化为零。
         *
         * @param money 新的货币余额
         */
        public void setMoney(double money) {
            this.money = MoneyUtil.normalizeBalance(money);
        }
    }
}
