package com.viscriptshop.gui;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.components.PlayerHeadElement;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.network.c2s.BuyMerchantPayload;
import com.viscriptshop.util.UIElementUtil;
import com.viscriptshop.util.ViScriptShopUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.appliedenergistics.yoga.*;

import java.util.List;

public class ShopUI extends UIElement {
    public ShopInfo shopInfo;
    @Getter
    @Setter
    private String search = "";
    private ScrollerView merchantsView;

    public ShopUI(ShopInfo shopInfo, String title) {
        this.shopInfo = shopInfo;
        this.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setAlignItems(YogaAlign.CENTER);
        });
        UIElement root = new UIElement();
        root.layout((layout) -> {
            layout.setWidthPercent(75);
            layout.setHeightPercent(70);
            layout.setFlexDirection(YogaFlexDirection.COLUMN);
        });
        root.getStyle().backgroundTexture(Sprites.BORDER);
        this.addChildren(root);
        UIElement head = new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(12);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setJustifyContent(YogaJustify.SPACE_BETWEEN);
            layout.setAlignItems(YogaAlign.CENTER);
        });
        SearchComponent<Item> searchComponent = UIElementUtil.createItemSearchComponentConfigurator("", this::getSearch, this::setSearch).searchComponent;
        searchComponent.layout(layout -> {
            layout.setFlex(5);
            layout.setMargin(YogaEdge.TOP, 5);
            layout.setMargin(YogaEdge.LEFT, 5);
        }).addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            reloadMerchants();
        });
        Label titleLabel = (Label) new Label().setText(title)
                .textStyle(textStyle -> textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).adaptiveWidth(true))
                .layout(layout -> {
                    layout.setMargin(YogaEdge.TOP, 5);
                    layout.setFlex(14);
                });
        head.addChildren(searchComponent, titleLabel, new UIElement().layout(layout -> {
            layout.setFlex(5);
            layout.setHeightPercent(12);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setAlignItems(YogaAlign.FLEX_END);
        }).addChild(new PlayerHeadElement()));

        createMerchants();
        root.addChildren(head, this.merchantsView);
    }

    public void reloadMerchants() {
        merchantsView.clearAllScrollViewChildren();

        // 重新添加所有商品
        for (int i = 0; i < shopInfo.getMerchants().size(); i++) {
            MerchantInfo merchantInfo = shopInfo.getMerchants().get(i);
            if (merchantInfo.getStage() > shopInfo.getStage()) continue;
            if (!merchantInfo.getItemResult().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(this.search))) && !this.search.isEmpty() && !this.search.equals(Items.AIR.toString())) {
                continue;
            }
            merchantsView.addScrollViewChild(createMerchant(merchantInfo));
        }
    }

    public UIElement createMerchant(MerchantInfo merchantInfo) {
        UIElement merchant = new UIElement().layout(layout -> {
            layout.setWidthPercent(29);
            layout.setGap(YogaGutter.ALL, 5);
            layout.setMargin(YogaEdge.LEFT, 5);
            layout.setMargin(YogaEdge.TOP, 2);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setAlignItems(YogaAlign.CENTER);
        });

        ItemStack itemA = merchantInfo.getItemA();
        ItemSlot itemASlot = UIElementUtil.createItemSlot(itemA, false);
        ItemStack itemB = merchantInfo.getItemB();
        ItemSlot itemBSlot = UIElementUtil.createItemSlot(itemB, false);
        ItemSlot resultItemSlot = (ItemSlot) UIElementUtil.createItemSlot(merchantInfo.getItemResult(), true).addEventListener(UIEvents.CLICK, event -> {
            //购买
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                ItemStack itemStackA = merchantInfo.getItemA();
                if (!itemStackA.isEmpty() && getItemForPlayerCount(itemStackA) < itemStackA.getCount()) {
                    Message.error(Component.translatable("viscript_shop.message.notEnoughItem", itemStackA.getItem().getDescription().getString()).getString(), this);
                    return;
                }
                ItemStack itemStackB = merchantInfo.getItemB();
                if (!itemStackB.isEmpty() && getItemForPlayerCount(itemStackB) < itemStackB.getCount()) {
                    Message.error(Component.translatable("viscript_shop.message.notEnoughItem", itemStackB.getItem().getDescription().getString()).getString(), this);
                    return;
                }
                player.connection.send(new BuyMerchantPayload(merchantInfo));
                Message.success(Component.translatable("viscript_shop.message.buySuccess", merchantInfo.getItemResult().getItem().getDescription().getString()).getString(), this);
            }
        });

        UIElement itemInfo = new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeight(13);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setGap(YogaGutter.ALL, 5);
        });
        int count;
        if (!itemA.isEmpty()) {
            count = getItemForPlayerCount(itemA);
            Label itemAText = (Label) new Label().setText(getCountText(count))
                    .textStyle(textStyle -> textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER))
                    .layout(layout -> {
                        layout.setWidth(18);
                        layout.setHeight(13);
                    })
                    .addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                        event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(Component.translatable("viscript_shop.ui.have").getString() + getItemForPlayerCount(itemA))), null, null, null);
                    });
            itemInfo.addChild(itemAText);
        }
        if (!itemB.isEmpty()) {
            count = getItemForPlayerCount(itemB);
            Label itemBText = (Label) new Label().setText(getCountText(count))
                    .textStyle(textStyle -> textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER))
                    .layout(layout -> {
                        layout.setWidth(18);
                        layout.setHeight(13);
                    })
                    .addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                        event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(Component.translatable("viscript_shop.ui.have").getString() + getItemForPlayerCount(itemB))), null, null, null);
                    });
            itemInfo.addChild(itemBText);
        }

        merchant.addChildren(new UIElement().layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.COLUMN);
                }).addChildren(
                        //商品
                        new UIElement().layout(layout -> {
                            layout.setFlexDirection(YogaFlexDirection.ROW);
                            layout.setGap(YogaGutter.ALL, 5);
                        }).addChildren(
                                itemASlot, itemBSlot,
                                new UIElement().style(style -> style.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR_S_LIGHT)).layout(layout -> {
                                    layout.setWidth(6);
                                    layout.setHeight(6);
                                    layout.setMargin(YogaEdge.ALL, 5);
                                }),
                                resultItemSlot
                        ),
                        //数据
                        itemInfo
                )
        );
        return merchant;
    }

    private void createMerchants() {
        this.merchantsView = new ScrollerView();
        merchantsView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(88);
        });

        merchantsView.viewContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWrap(YogaWrap.WRAP);
            layout.setGap(YogaGutter.ALL, 5);
        });

        reloadMerchants();
    }

    private int getItemForPlayerCount(ItemStack item) {
        LocalPlayer player = Minecraft.getInstance().player;
        int count = 0;
        if (player != null) {
            //背包该物品数量
            count += ViScriptShopUtil.removeItem(player, item, 0);
        }
        return count;
    }

    private String getCountText(int count) {
        return count <= 99 ? count + "" : "99+";
    }
}
