package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ConfiguratorParser;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.viscript_lib.gui.configurator.ViScriptItemStackAccessor;
import com.viscript_lib.util.item.ViScriptItemStack;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * 保存商品图标的客户端展示方式。
 *
 * <p>资源包图片和替代渲染物品只影响界面外观与悬浮提示，不参与交易匹配、
 * 物品校验、库存统计或物品扣除。
 */
@Data
@NoArgsConstructor
public class MerchantItemDisplay implements IConfigurable, IPersistedSerializable {
    public static final StreamCodec<ByteBuf, MerchantItemDisplay> STREAM_CODEC;
    public static final Codec<MerchantItemDisplay> CODEC;

    /** 当前图标展示方式，默认跟随实际物品。 */
    @Configurable(name = "viscript_shop.data.merchant.itemDisplay.renderMode")
    @ConfigSelector(subConfiguratorBuilder = "renderModeSubConfiguratorBuilder")
    private RenderMode renderMode = RenderMode.ITEM;

    /** {@link RenderMode#RESOURCE} 使用的资源包图片路径。 */
    @Persisted
    private String resourcePath = "";

    /** {@link RenderMode#RESOURCE} 使用的悬浮显示名称。 */
    @Persisted
    private String resourceName = "";

    /** {@link RenderMode#ITEM_RENDER} 使用的替代渲染物品。 */
    @Persisted
    private ViScriptItemStack renderItem = new ViScriptItemStack();

    static {
        CODEC = PersistedParser.createCodec(MerchantItemDisplay::new);
        STREAM_CODEC = PersistedParser.createStreamCodec(MerchantItemDisplay::new);
    }

    /**
     * 创建商品图标展示配置。
     *
     * @param renderMode 图标展示方式
     * @param resourcePath 资源图片路径
     * @param resourceName 资源图片悬浮名称
     * @param renderItem 替代渲染物品
     */
    public MerchantItemDisplay(RenderMode renderMode, String resourcePath, String resourceName,
                               ItemStack renderItem) {
        this.renderMode = renderMode == null ? RenderMode.ITEM : renderMode;
        this.resourcePath = resourcePath == null ? "" : resourcePath;
        this.resourceName = resourceName == null ? "" : resourceName;
        setRenderItem(renderItem);
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        renderMode = resolvedRenderMode();
        getSerializedRenderItem();
        resourcePath = resourcePath == null ? "" : resourcePath;
        resourceName = resourceName == null ? "" : resourceName;
        addFieldConfigurator(father, "renderMode")
                .addClass("merchant-item-display-render-mode");
    }

    /**
     * 获取可安全用于渲染的图标模式。
     *
     * @return 当前模式，未设置时返回 {@link RenderMode#ITEM}
     */
    public RenderMode resolvedRenderMode() {
        return renderMode == null ? RenderMode.ITEM : renderMode;
    }

    /**
     * 获取可安全用于替代渲染的物品。
     *
     * @return 非 {@code null} 的替代渲染物品堆
     */
    public ItemStack resolvedRenderItem() {
        return getSerializedRenderItem().toItemStack();
    }

    /**
     * 获取供界面渲染使用的替代物品副本。
     *
     * @return 已解析的原版物品堆或缺失物品占位符
     */
    public ItemStack getRenderItem() {
        return resolvedRenderItem();
    }

    /**
     * 获取替代图标的容错持久化数据。
     *
     * @return 非 {@code null} 的容错物品数据
     */
    public ViScriptItemStack getSerializedRenderItem() {
        if (renderItem == null) {
            renderItem = new ViScriptItemStack();
        }
        return renderItem;
    }

    /**
     * 替换替代图标的容错持久化数据。
     *
     * @param renderItem 容错物品数据；传入 {@code null} 时使用空物品
     */
    public void setSerializedRenderItem(ViScriptItemStack renderItem) {
        this.renderItem = renderItem == null ? new ViScriptItemStack() : renderItem;
    }

    /**
     * 使用原版物品堆替换图标数据。
     *
     * @param renderItem 原版物品堆；传入 {@code null} 时使用空物品
     */
    public void setRenderItem(ItemStack renderItem) {
        setSerializedRenderItem(new ViScriptItemStack(renderItem == null ? ItemStack.EMPTY : renderItem));
    }

    /**
     * 判断替代图标是否引用了当前未注册的物品。
     *
     * @return 使用缺失物品占位符时返回 {@code true}
     */
    public boolean isMissingRenderItem() {
        return getSerializedRenderItem().isMissingItem();
    }

    @SuppressWarnings("unused")
    private void renderModeSubConfiguratorBuilder(RenderMode value, ConfiguratorGroup group) {
        RenderMode mode = value == null ? RenderMode.ITEM : value;
        try {
            switch (mode) {
                case ITEM -> {
                    // 实际物品堆在 MerchantItemInfo 中配置，此模式不需要额外字段。
                }
                case RESOURCE -> {
                    StringConfigurator resourcePathConfigurator = new StringConfigurator(
                            "viscript_shop.data.merchant.itemDisplay.resourcePath",
                            this::getResourcePath,
                            this::setResourcePath,
                            getResourcePath(),
                            true
                    ).setResourceLocation(true);
                    resourcePathConfigurator.addClass("merchant-item-display-resource-path");
                    group.addConfigurator(resourcePathConfigurator);

                    StringConfigurator resourceNameConfigurator = new StringConfigurator(
                            "viscript_shop.data.merchant.itemDisplay.resourceName",
                            this::getResourceName,
                            this::setResourceName,
                            getResourceName(),
                            true
                    );
                    resourceNameConfigurator.addClass("merchant-item-display-resource-name");
                    group.addConfigurator(resourceNameConfigurator);
                }
                case ITEM_RENDER -> {
                    Field field = getClass().getDeclaredField("renderItem");
                    Configurator configurator = new ViScriptItemStackAccessor().create(
                            "viscript_shop.data.merchant.itemDisplay.renderItem",
                            this::getSerializedRenderItem,
                            this::setSerializedRenderItem,
                            true,
                            field,
                            this
                    );
                    configurator.addClass("merchant-item-display-render-item");
                    group.addConfigurator(configurator);
                }
            }
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("Missing merchant item display field", exception);
        }
    }

    private Configurator addFieldConfigurator(ConfiguratorGroup father, String fieldName) {
        try {
            int previousSize = father.getConfigurators().size();
            ConfiguratorParser.createFieldConfigurator(
                    getClass().getDeclaredField(fieldName),
                    father,
                    getClass(),
                    new HashMap<>(),
                    this
            );
            if (father.getConfigurators().size() <= previousSize) {
                throw new IllegalStateException("No configurator created for merchant item display field: " + fieldName);
            }
            return father.getConfigurators().getLast();
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("Missing merchant item display field: " + fieldName, exception);
        }
    }

    /** 商品图标可用的三种展示方式。 */
    @Getter
    @AllArgsConstructor
    public enum RenderMode implements StringRepresentable {
        /** 跟随 {@link MerchantItemInfo#getItem()} 渲染实际物品。 */
        ITEM("viscript_shop.data.merchant.itemDisplay.renderMode.item"),

        /** 渲染资源包图片，并以配置名称作为悬浮提示。 */
        RESOURCE("viscript_shop.data.merchant.itemDisplay.renderMode.resource"),

        /** 渲染独立物品堆，但不改变实际交易物品。 */
        ITEM_RENDER("viscript_shop.data.merchant.itemDisplay.renderMode.item_render");

        private final String name;

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
