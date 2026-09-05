package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.viscript_lib.util.CountTextUtil;
import com.viscriptshop.gui.data.MerchantItemInfo;
import com.viscriptshop.util.UIElementUtil;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * 在商品图标容器底部叠加数量，并在右侧补充促销信息。
 *
 * <p>实际物品、资源包图片和替代渲染物品仍由 {@link UIElementUtil#createMerchantItemDisplay}
 * 负责。数量与图标共用容器，右侧的现数量和折扣率紧邻图标并左对齐。
 */
public final class MerchantItemAmountDisplay extends UIElement {
    public static final float AMOUNT_FONT_SIZE = 4;
    public static final float COUNT_WIDTH = 20;
    public static final float PRICE_WIDTH = 40;
    public static final float PRICE_HEIGHT = 28;
    public static final float PRICE_DETAIL_INSET = 3;
    public static final float PRICE_RATE_TOP = 2;
    public static final float PRICE_ACTUAL_BOTTOM = 1;
    private static final float DISPLAY_SIZE = 16;
    private static final float DETAIL_GAP = 1;
    private final UIElement displayElement;
    private UIElement detailsElement;

    private MerchantItemAmountDisplay(MerchantItemInfo itemInfo,
                                      String id,
                                      Supplier<Component> normalText,
                                      Supplier<Component> originalText,
                                      Supplier<Component> actualText,
                                      Supplier<Component> rateText,
                                      boolean priceComparison) {
        setId(id);
        addClass("merchant-item-amount-display");
        layout(layout -> {
            layout.width(priceComparison ? PRICE_WIDTH : COUNT_WIDTH);
            layout.height(DISPLAY_SIZE);
            layout.flexShrink(0);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(DETAIL_GAP);
            layout.positionType(TaffyPosition.RELATIVE);
        });
        setOverflowVisible(true);

        UIElement item = new UIElement()
                .setId(id + "_item")
                .layout(layout -> {
                    layout.width(COUNT_WIDTH);
                    layout.height(DISPLAY_SIZE);
                    layout.flexShrink(0);
                    layout.positionType(TaffyPosition.RELATIVE);
                });
        // 只隐藏原版受 int 限制的堆叠数，物品或资源图片的既有渲染与提示全部保留。
        displayElement = UIElementUtil.createMerchantItemDisplay(itemInfo, true, 1)
                .setId(id + "_icon")
                .layout(layout -> {
                    layout.width(DISPLAY_SIZE);
                    layout.height(DISPLAY_SIZE);
                    layout.positionType(TaffyPosition.ABSOLUTE);
                    layout.left((COUNT_WIDTH - DISPLAY_SIZE) / 2);
                    layout.top(0);
                });
        item.addChild(displayElement);

        Label normal = createBoundLabel(id + "_count", normalText);
        normal.getTextStyle()
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.BOTTOM);
        normal.getLayout().width(COUNT_WIDTH);
        normal.getLayout().positionType(TaffyPosition.ABSOLUTE);
        normal.getLayout().left(0);
        normal.getLayout().bottom(0);
        item.addChild(normal);
        addChild(item);

        if (!priceComparison) {
            return;
        }

        Label original = createBoundLabel(id + "_original", originalText);
        original.getTextStyle()
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.BOTTOM);
        original.getLayout().width(COUNT_WIDTH);
        original.getLayout().positionType(TaffyPosition.ABSOLUTE);
        original.getLayout().left(0);
        original.getLayout().bottom(0);
        item.addChild(original);

        UIElement details = new UIElement()
                .setId(id + "_details")
                .layout(layout -> {
                    layout.width(PRICE_WIDTH - COUNT_WIDTH - DETAIL_GAP);
                    layout.height(DISPLAY_SIZE);
                    layout.flexShrink(0);
                    layout.positionType(TaffyPosition.RELATIVE);
                });
        Label actual = createBoundLabel(id + "_actual", actualText);
        actual.getTextStyle()
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.BOTTOM);
        actual.getLayout().widthPercent(100);
        actual.getLayout().positionType(TaffyPosition.ABSOLUTE);
        actual.getLayout().left(0);
        actual.getLayout().bottom(PRICE_ACTUAL_BOTTOM);

        Label rate = createBoundLabel(id + "_rate", rateText);
        rate.getTextStyle()
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.TOP);
        rate.getLayout().widthPercent(100);
        rate.getLayout().positionType(TaffyPosition.ABSOLUTE);
        rate.getLayout().left(0);
        rate.getLayout().top(PRICE_RATE_TOP);
        details.addChildren(actual, rate);
        addChild(details);
        detailsElement = details;
    }

    public static MerchantItemAmountDisplay count(MerchantItemInfo itemInfo, String id) {
        return count(itemInfo, id, () -> {
            ItemStack stack = itemInfo == null ? ItemStack.EMPTY : itemInfo.getItem();
            return stack.isEmpty() || stack.getCount() <= 1
                    ? Component.empty()
                    : Component.literal(CountTextUtil.formatCount((long) stack.getCount()));
        });
    }

    /**
     * 创建在原商品图标容器底部绘制长整型数量的组件。
     *
     * @param  itemInfo 商品的交易物品与展示配置
     * @param  id 组件的稳定标识
     * @param  countText 数量文本提供器
     * @return 新建的商品数量组件
     */
    public static MerchantItemAmountDisplay count(MerchantItemInfo itemInfo,
                                                   String id,
                                                   Supplier<Component> countText) {
        return new MerchantItemAmountDisplay(
                itemInfo,
                id,
                countText,
                Component::empty,
                Component::empty,
                Component::empty,
                false
        );
    }

    /**
     * 创建保留原商品展示并绘制促销对比信息的组件。
     *
     * @param  itemInfo 商品的交易物品与展示配置
     * @param  id 组件的稳定标识
     * @param  normalText 未发生促销时的数量文本提供器
     * @param  originalText 发生促销时的原数量文本提供器
     * @param  actualText 发生促销时的现数量文本提供器
     * @param  rateText 发生促销时的变化率文本提供器
     * @return 新建的商品价格组件
     */
    public static MerchantItemAmountDisplay price(MerchantItemInfo itemInfo,
                                                   String id,
                                                   Supplier<Component> normalText,
                                                   Supplier<Component> originalText,
                                                   Supplier<Component> actualText,
                                                   Supplier<Component> rateText) {
        return new MerchantItemAmountDisplay(
                itemInfo,
                id,
                normalText,
                originalText,
                actualText,
                rateText,
                true
        );
    }

    /**
     * 即使当前只显示数量，也保留与折扣组件相同的右侧信息区，避免相邻赠品随折扣变化移动。
     */
    public MerchantItemAmountDisplay reservePriceSpace() {
        getLayout().width(PRICE_WIDTH);
        return this;
    }

    /**
     * 保留紧邻图标的横向间距，折扣贴行上沿、现数量贴行下沿，图标垂直居中。
     */
    public MerchantItemAmountDisplay alignPriceDetailsVertically() {
        reservePriceSpace();
        getLayout().height(PRICE_HEIGHT);
        getLayout().alignItems(AlignItems.CENTER);
        if (detailsElement != null) {
            detailsElement.getLayout().height(PRICE_HEIGHT);
        }
        return this;
    }

    /**
     * 列表价格使用与货币相同的上下边距：图标居中，折扣在右上，现数量在右下。
     */
    public MerchantItemAmountDisplay alignPriceDetailsInRow() {
        alignPriceDetailsVertically();
        if (detailsElement != null) {
            detailsElement.setAllowHitTest(false);
            detailsElement.layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(PRICE_DETAIL_INSET);
                layout.top(0);
                layout.width(PRICE_WIDTH - PRICE_DETAIL_INSET * 2);
                layout.height(PRICE_HEIGHT);
            });
            detailsElement.getChildren().forEach(child -> {
                if (child instanceof Label label) {
                    label.getTextStyle().textAlignHorizontal(Horizontal.RIGHT);
                }
            });
        }
        return this;
    }

    /**
     * 网格将折扣区固定放在图标下方，商品与赠品可紧邻排列，单个图标也能真正居中。
     */
    public MerchantItemAmountDisplay reservePriceSpaceBelow() {
        getLayout().width(COUNT_WIDTH);
        getLayout().height(DISPLAY_SIZE + 14);
        if (detailsElement != null) {
            detailsElement.layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(0);
                layout.top(DISPLAY_SIZE);
                layout.width(COUNT_WIDTH);
                layout.height(14);
            });
            detailsElement.getChildren().forEach(child -> {
                if (child instanceof Label label) {
                    label.getTextStyle().textAlignHorizontal(Horizontal.CENTER);
                }
            });
        }
        return this;
    }

    /**
     * 在原商品展示的悬浮提示后空一行，再追加额外提示。
     *
     * <p>实际物品提示、替代渲染物品提示或资源图片名称均会保留。提示提供器返回
     * {@code null} 时不追加内容。
     *
     * @param  tooltips 额外悬浮提示提供器
     * @return 此商品数量组件
     */
    public MerchantItemAmountDisplay appendDisplayHoverTooltips(Supplier<HoverTooltips> tooltips) {
        displayElement.addEventListener(UIEvents.TICK, event -> {
            HoverTooltips additional = tooltips.get();
            if (additional == null || additional.tooltipTexts().isEmpty()) {
                event.currentElement.getStyle().tooltips(Tooltips.empty());
                return;
            }
            var lines = new ArrayList<Component>();
            lines.add(Component.empty());
            lines.addAll(additional.tooltipTexts());
            event.currentElement.getStyle().tooltips(Tooltips.of(lines));
        });
        return this;
    }

    public MerchantItemAmountDisplay withGiftBadge() {
        if (hasClass("shop-gift-item")) {
            return this;
        }
        addClass("shop-gift-item");
        Label badge = (Label) new Label()
                .setText(Component.translatable("viscript_shop.ui.promotion.gift_badge")
                        .withStyle(ChatFormatting.GOLD))
                .textStyle(style -> style
                        .fontSize(AMOUNT_FONT_SIZE)
                        .textAlignHorizontal(Horizontal.LEFT)
                        .textAlignVertical(Vertical.TOP)
                        .adaptiveWidth(false))
                .layout(layout -> {
                    layout.width(COUNT_WIDTH);
                    layout.height(AMOUNT_FONT_SIZE + 1);
                    layout.positionType(TaffyPosition.ABSOLUTE);
                    layout.left(0);
                    layout.top(0);
                });
        badge.setId(getId() + "_gift");
        badge.setAllowHitTest(false);
        displayElement.getParent().addChild(badge);
        return this;
    }

    private static Label createBoundLabel(String id, Supplier<Component> text) {
        Label label = (Label) new Label()
                .setText(text.get())
                .textStyle(style -> style
                        .textAlignVertical(Vertical.BOTTOM)
                        .fontSize(AMOUNT_FONT_SIZE)
                        .adaptiveWidth(false))
                .layout(layout -> layout.height(AMOUNT_FONT_SIZE + 1));
        label.setId(id);
        label.setAllowHitTest(false);
        label.bindDataSource(SupplierDataSource.of(text));
        return label;
    }
}
