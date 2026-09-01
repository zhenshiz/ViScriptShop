package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.ConfiguratorParser;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscript_lib.util.item.ViScriptItemStack;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * 保存商品的实际物品及其独立图标配置。
 *
 * <p>实际物品参与交易、校验和库存处理，图标配置只决定客户端如何展示该物品。
 * 此类型用于不需要物品组件匹配规则的商品位置，例如 {@code itemResult}。
 */
@Data
@NoArgsConstructor
public class MerchantItemInfo implements IConfigurable, IPersistedSerializable {
    public static final StreamCodec<ByteBuf, MerchantItemInfo> STREAM_CODEC;
    public static final Codec<MerchantItemInfo> CODEC;

    @Configurable(name = "viscript_shop.data.merchant.item.actual")
    private ViScriptItemStack item = new ViScriptItemStack();

    @Configurable(showName = false, subConfigurable = true, subFlattenConfigurable = true)
    private MerchantItemDisplay display = new MerchantItemDisplay();

    static {
        CODEC = PersistedParser.createCodec(MerchantItemInfo::new);
        STREAM_CODEC = PersistedParser.createStreamCodec(MerchantItemInfo::new);
    }

    /**
     * 使用原版物品堆创建商品物品信息。
     *
     * @param item 参与交易的原版物品堆
     * @param display 图标展示配置
     */
    public MerchantItemInfo(ItemStack item, MerchantItemDisplay display) {
        this(new ViScriptItemStack(item == null ? ItemStack.EMPTY : item), display);
    }

    /**
     * 使用容错物品数据创建商品物品信息。
     *
     * @param item 容错物品数据
     * @param display 图标展示配置
     */
    protected MerchantItemInfo(ViScriptItemStack item, MerchantItemDisplay display) {
        this.item = item == null ? new ViScriptItemStack() : item;
        this.display = display == null ? new MerchantItemDisplay() : display;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        getSerializedItem();
        getDisplay();
        addFieldConfigurator(father, MerchantItemInfo.class, "item")
                .addClass("merchant-item-actual");
        addFieldConfigurator(father, MerchantItemInfo.class, "display")
                .addClass("merchant-item-display-settings");
    }

    /**
     * 获取参与持久化和网络传输的容错物品数据。
     *
     * @return 非 {@code null} 的容错物品数据
     */
    public ViScriptItemStack getSerializedItem() {
        if (item == null) {
            item = new ViScriptItemStack();
        }
        return item;
    }

    /**
     * 替换参与持久化和网络传输的容错物品数据。
     *
     * @param item 容错物品数据；传入 {@code null} 时使用空物品
     */
    public void setSerializedItem(ViScriptItemStack item) {
        this.item = item == null ? new ViScriptItemStack() : item;
    }

    /**
     * 获取供渲染或游戏逻辑使用的独立原版物品堆。
     *
     * <p>物品 ID 不存在时返回 {@link ViScriptItemStack} 提供的占位物品；修改返回值
     * 不会影响已持久化的数据。
     *
     * @return 已解析的物品堆或缺失物品占位符
     */
    public ItemStack getItem() {
        return getSerializedItem().toItemStack();
    }

    /**
     * 使用原版物品堆的副本替换持久化数据。
     *
     * @param item 原版物品堆；传入 {@code null} 时使用空物品
     */
    public void setItem(ItemStack item) {
        setSerializedItem(new ViScriptItemStack(item == null ? ItemStack.EMPTY : item));
    }

    /**
     * 判断序列化物品 ID 是否未注册。
     *
     * @return 使用缺失物品占位符时返回 {@code true}
     */
    public boolean isMissingItem() {
        return getSerializedItem().isMissingItem();
    }

    /**
     * 获取只影响客户端图标的展示配置。
     *
     * @return 非 {@code null} 的图标展示配置
     */
    public MerchantItemDisplay getDisplay() {
        if (display == null) {
            display = new MerchantItemDisplay();
        }
        return display;
    }

    /**
     * 为指定字段创建一个配置组件。
     *
     * @param father 配置组件的父分组
     * @param declaringClass 声明目标字段的类
     * @param fieldName 目标字段名称
     * @return 新增到父分组的配置组件
     */
    protected Configurator addFieldConfigurator(ConfiguratorGroup father,
                                                Class<?> declaringClass,
                                                String fieldName) {
        try {
            Field field = declaringClass.getDeclaredField(fieldName);
            int previousSize = father.getConfigurators().size();
            ConfiguratorParser.createFieldConfigurator(
                    field,
                    father,
                    declaringClass,
                    new HashMap<>(),
                    this
            );
            if (father.getConfigurators().size() <= previousSize) {
                throw new IllegalStateException("No configurator created for merchant item field: " + fieldName);
            }
            return father.getConfigurators().getLast();
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("Missing merchant item field: " + fieldName, exception);
        }
    }
}
