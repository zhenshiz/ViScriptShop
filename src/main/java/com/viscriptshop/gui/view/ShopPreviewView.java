package com.viscriptshop.gui.view;

import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.viscript_lib.gui.components.DraggableUI;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.util.MoneyUtil;
import com.viscriptshop.util.UIElementUtil;
import dev.vfyjxf.taffy.style.*;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ShopPreviewView extends View {
    public final ShopEditor editor;
    public final UIElement head = new UIElement();
    public final ScrollerView scrollerView = new ScrollerView();
    private CategoryInfo selectedCategory = null;

    // 剪贴板：用于跨分类复制/剪切/粘贴商品
    private static MerchantClipboard clipboard = null;

    private DraggableUI<MerchantInfo> draggableMerchants = null;
    private CategoryInfo lastRenderedCategory = null;
    private CategoryInfo.ShopType lastRenderedShopType = null;
    private int lastRenderedSignature = 0;

    public ShopPreviewView(ShopEditor editor) {
        super("viscript_shop.editor.view.shopPreview");
        this.editor = editor;
        this.head.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.widthPercent(100);
            layout.height(15);
            layout.gapAll(5);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));
        head.setDisplay(TaffyDisplay.NONE);
        UIElement addButton = new Button().setText("viscript_shop.editor.add.merchant").setOnClick(event -> {
            MerchantInfo merchantInfo = new MerchantInfo();
            selectedCategory.getMerchants().add(merchantInfo);
            editor.inspectMerchant(merchantInfo, selectedCategory.getShopType());
        }).layout(layout -> {
            layout.heightPercent(100);
        });
        UIElement setTradeTypeButton = new Button().setText("viscript_shop.editor.setTradeType").setOnClick(event -> {
            showTradeTypeDialog();
        }).layout(layout -> {
            layout.heightPercent(100);
        });
        UIElement pasteButton = new Button().setText("viscript_shop.button.paste").setOnClick(event -> {
            pasteMerchant(-1);
        }).layout(layout -> {
            layout.heightPercent(100);
        });
        head.addChildren(addButton, setTradeTypeButton, pasteButton).addEventListener(UIEvents.TICK, event -> {
            if (selectedCategory == null) {
                setTradeTypeButton.setDisplay(TaffyDisplay.NONE);
            } else {
                setTradeTypeButton.setDisplay(selectedCategory.getShopType().equals(CategoryInfo.ShopType.CURRENCY) ? TaffyDisplay.FLEX : TaffyDisplay.NONE);
            }
        });

        this.scrollerView.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        this.scrollerView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(0);
            layout.gapAll(0);
        });

        this.addChildren(head, scrollerView);
    }

    public void loadView() {
        this.scrollerView.viewContainer.addEventListener(UIEvents.TICK, event -> tickReloadMerchants());
    }

    private void tickReloadMerchants() {
        selectedCategory = editor.categoryView.getSelectedCategory();

        if (selectedCategory == null || !(editor.getCurrentProject() instanceof Shop)) {
            head.setDisplay(TaffyDisplay.NONE);
            scrollerView.clearAllScrollViewChildren();
            draggableMerchants = null;
            lastRenderedCategory = null;
            lastRenderedShopType = null;
            lastRenderedSignature = 0;
            return;
        }

        head.setDisplay(TaffyDisplay.FLEX);

        CategoryInfo.ShopType shopType = selectedCategory.getShopType();
        int signature = computeSignature(selectedCategory);

        boolean dragging = draggableMerchants != null && draggableMerchants.isDragging();
        boolean needsRebuild = !dragging && (
                selectedCategory != lastRenderedCategory ||
                        shopType != lastRenderedShopType ||
                        signature != lastRenderedSignature
        );

        if (needsRebuild) {
            rebuildMerchantsUI();
            lastRenderedCategory = selectedCategory;
            lastRenderedShopType = shopType;
            lastRenderedSignature = signature;
        }
    }

    private void rebuildMerchantsUI() {
        scrollerView.clearAllScrollViewChildren();

        List<MerchantInfo> merchants = selectedCategory.getMerchants();

        draggableMerchants = new DraggableUI<>(merchants, newOrder -> {
            selectedCategory.setMerchants(newOrder);
            lastRenderedSignature = computeSignatureFromMerchantList(selectedCategory.getShopType(), newOrder);
        });

        draggableMerchants.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
            layout.paddingAll(5);
            layout.gapAll(5);
        });

        for (MerchantInfo merchantInfo : merchants) {
            MerchantCard card = createMerchantCard(merchantInfo);
            card.root.addEventListener(UIEvents.MOUSE_DOWN, event -> handleMerchantMouseDown(event, merchantInfo));
            draggableMerchants.addSortableCard(merchantInfo, card.root, card.dragHandle);
        }

        scrollerView.addScrollViewChild(draggableMerchants);
    }

    private MerchantCard createMerchantCard(MerchantInfo merchantInfo) {
        UIElement merchant;
        UIElement dragHandle;
        switch (selectedCategory.getShopType()) {
            case ITEM_FOR_ITEM -> {
                merchant = new UIElement().layout(layout -> {
                    layout.width(100);
                    layout.gapAll(5);
                    layout.marginLeft(5);
                    layout.paddingAll(3);
                    layout.justifyContent(AlignContent.CENTER);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.alignItems(AlignItems.CENTER);
                });
                merchant.getStyle().backgroundTexture(Sprites.RECT_SOLID);

                dragHandle = createDragHandle();

                UIElement itemASlot = UIElementUtil.createMerchantItemDisplay(
                                merchantInfo.getItemAInfo(),
                                true
                        )
                        .addEventListener(UIEvents.MOUSE_DOWN, event -> handleMerchantMouseDown(event, merchantInfo));
                UIElement itemBSlot = UIElementUtil.createMerchantItemDisplay(
                                merchantInfo.getItemBInfo(),
                                true
                        )
                        .addEventListener(UIEvents.MOUSE_DOWN, event -> handleMerchantMouseDown(event, merchantInfo));
                UIElement resultItemSlot = UIElementUtil.createMerchantItemDisplay(
                                merchantInfo.getItemResultInfo(),
                                true
                        )
                        .addEventListener(UIEvents.MOUSE_DOWN, event -> handleMerchantMouseDown(event, merchantInfo));

                merchant.addChildren(dragHandle, itemASlot, itemBSlot,
                        new UIElement().style(style -> style.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR_S_LIGHT)).layout(layout -> {
                            layout.width(6);
                            layout.height(6);
                            layout.marginAll(5);
                        }),
                        resultItemSlot
                );
                return new MerchantCard(merchant, dragHandle);
            }
            case CURRENCY -> {
                merchant = new UIElement().layout(layout -> {
                    layout.width(55);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.alignItems(AlignItems.CENTER);
                    layout.justifyContent(AlignContent.CENTER);
                    layout.paddingAll(5);
                });
                merchant.getStyle().backgroundTexture(Sprites.RECT_SOLID);

                UIElement itemSlot = UIElementUtil.createMerchantItemDisplay(
                                merchantInfo.getItemResultInfo(),
                                true
                        )
                        .layout(layout -> {
                            layout.width(30);
                            layout.height(30);
                        });

                MerchantInfo.TradeType tradeType = merchantInfo.getTradeType();
                String tradeText = tradeType.getSerializedName();

                Button tradeLabel = (Button) new Button()
                        .setText(tradeText)
                        .textStyle(style -> style
                                .textAlignHorizontal(Horizontal.CENTER)
                        ).layout(layout -> {
                            layout.widthPercent(100);
                            layout.marginTop(5);
                            layout.marginBottom(10);
                        });

                Label priceLabel = (Label) new Label()
                        .setText(Component.literal("◎" + MoneyUtil.formatCompact(merchantInfo.getMoney())))
                        .textStyle(style -> style
                                .textColor(0xFFFFAA00)
                                .textAlignHorizontal(Horizontal.CENTER)
                        ).layout(layout -> {
                            layout.widthPercent(100);
                        });

                dragHandle = createDragHandleForColumn();

                // 纵向布局，拖拽句柄放在顶部
                merchant.addChildren(dragHandle, itemSlot, tradeLabel, priceLabel);

                return new MerchantCard(merchant, dragHandle);
            }
            default -> {
                merchant = new UIElement();
                dragHandle = new UIElement();
                return new MerchantCard(merchant, dragHandle);
            }
        }
    }

    private UIElement createDragHandle() {
        return new UIElement().layout(layout -> {
            layout.width(10);
            layout.heightPercent(100);
        }).style(style -> {
            style.backgroundTexture(Icons.ARROW_UP_DOWN);
        });
    }

    private UIElement createDragHandleForColumn() {
        return new UIElement().layout(layout -> {
            layout.width(15);
            layout.height(10);
            layout.alignSelf(AlignItems.CENTER);
        }).style(style -> {
            style.backgroundTexture(Icons.ARROW_LEFT_RIGHT);
        });
    }

    public void removeMerchant(int index) {
        selectedCategory.getMerchants().remove(index);
    }

    private void handleMerchantMouseDown(UIEvent event, MerchantInfo merchantInfo) {
        if (event.button == 0) {
            editor.inspectMerchant(merchantInfo, selectedCategory.getShopType());
            event.stopPropagation();
            return;
        }

        showMerchantMenuTab(event, merchantInfo);
    }

    private void showMerchantMenuTab(UIEvent event, MerchantInfo merchantInfo) {
        int index = findMerchantIndexByIdentity(merchantInfo);
        if (index < 0) return;

        if (event.button == 1) {
            UIElement clickedElement = event.currentElement;
            float posX = clickedElement.getPositionX();
            float posY = clickedElement.getPositionY() + clickedElement.getSizeHeight();

            TreeBuilder.Menu merchantMenu = TreeBuilder.Menu.start()
                    .leaf("viscript_shop.button.update", () -> {
                        editor.inspectMerchant(merchantInfo, selectedCategory.getShopType());
                    })
                    .leaf("viscript_shop.button.copy", () -> {
                        copyMerchant(merchantInfo, index);
                    })
                    .leaf("viscript_shop.button.cut", () -> {
                        cutMerchant(merchantInfo, index);
                    })
                    .leaf("viscript_shop.button.paste", () -> {
                        pasteMerchant(index + 1);
                    })
                    .leaf("viscript_shop.button.delete", () -> {
                        Dialog.showCheckBox("viscript_shop.button.delete", "viscript_shop.dialog.delete_merchant.info", (result) -> {
                            if (result) removeMerchant(index);
                        }).show(editor);
                    });

            UIElementUtil.openMenu(posX, posY, merchantMenu, this);
            event.stopPropagation();
        }
    }

    /**
     * 复制商品到剪贴板
     */
    private void copyMerchant(MerchantInfo merchantInfo, int index) {
        clipboard = new MerchantClipboard(merchantInfo, selectedCategory, index, false);
        Dialog.showNotification("viscript_shop.editor.copy.success", 1.5f).show(editor);
    }

    /**
     * 剪切商品到剪贴板
     */
    private void cutMerchant(MerchantInfo merchantInfo, int index) {
        clipboard = new MerchantClipboard(merchantInfo, selectedCategory, index, true);
        Dialog.showNotification("viscript_shop.editor.cut.success", 1.5f).show(editor);
    }

    /**
     * 从剪贴板粘贴商品
     */
    private void pasteMerchant(int insertIndex) {
        if (clipboard == null) return;

        // 检查类型兼容性
        if (clipboard.getSourceType() != selectedCategory.getShopType()) {
            Dialog.showNotification("viscript_shop.editor.paste.type_mismatch", 2f).show(editor);
            return;
        }

        MerchantInfo newMerchant = clipboard.merchantInfo().copy();

        if (insertIndex >= 0 && insertIndex <= selectedCategory.getMerchants().size()) {
            selectedCategory.getMerchants().add(insertIndex, newMerchant);
        } else {
            selectedCategory.getMerchants().add(newMerchant);
        }

        // 如果是剪切操作，从原分类删除原商品
        if (clipboard.isCut()) {
            CategoryInfo sourceCategory = clipboard.sourceCategory();
            int sourceIndex = clipboard.sourceIndex();

            // 如果在同一个分类中粘贴，需要调整索引
            if (sourceCategory == selectedCategory && sourceIndex < insertIndex) {
                sourceIndex++;
            }

            // 检查索引是否有效
            if (sourceIndex >= 0 && sourceIndex < sourceCategory.getMerchants().size()) {
                sourceCategory.getMerchants().remove(sourceIndex);
            }

            // 清空剪贴板
            clipboard = null;
        }
    }

    /**
     * 商品剪贴板
     * 用于存储复制/剪切的商品信息
     */
    private record MerchantClipboard(MerchantInfo merchantInfo, CategoryInfo sourceCategory, int sourceIndex,
                                     boolean isCut) {
        public CategoryInfo.ShopType getSourceType() {
            return sourceCategory.getShopType();
        }

    }

    /**
     * 显示设置交易类型的对话框
     */
    private void showTradeTypeDialog() {
        var dialog = new Dialog();
        dialog.setTitle("viscript_shop.editor.setTradeType");

        dialog.addContent(new Label()
                .textStyle(textStyle -> textStyle.textWrap(TextWrap.WRAP).adaptiveHeight(true))
                .setText("viscript_shop.dialog.setTradeType.info")
                .layout(layout -> layout.widthPercent(100)));

        dialog.addButton(new Button()
                .setOnClick(e -> {
                    setAllTradeType(MerchantInfo.TradeType.BUY);
                    dialog.close();
                })
                .setText("viscript_shop.data.merchant.tradeType.buy"));

        dialog.addButton(new Button()
                .setOnClick(e -> {
                    setAllTradeType(MerchantInfo.TradeType.SELL);
                    dialog.close();
                })
                .setText("viscript_shop.data.merchant.tradeType.sell"));

        dialog.addButton(new Button()
                .setOnClick(e -> dialog.close())
                .setText("ldlib.gui.tips.cancel"));

        dialog.show(editor);
    }

    /**
     * 设置当前分类下所有商品的交易类型
     */
    private void setAllTradeType(MerchantInfo.TradeType tradeType) {
        selectedCategory.getMerchants().forEach(merchant -> {
            merchant.setTradeType(tradeType);
        });
        Dialog.showNotification(
                Component.translatable("viscript_shop.editor.setTradeType.success",
                        Component.translatable(tradeType.getSerializedName())).getString(),
                1.5f
        ).show(editor);
    }

    private int findMerchantIndexByIdentity(MerchantInfo target) {
        if (selectedCategory == null) return -1;
        var merchants = selectedCategory.getMerchants();
        for (int i = 0; i < merchants.size(); i++) {
            if (merchants.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    private int computeSignature(CategoryInfo category) {
        return computeSignatureFromMerchantList(category.getShopType(), category.getMerchants());
    }

    private int computeSignatureFromMerchantList(CategoryInfo.ShopType shopType, List<MerchantInfo> merchants) {
        int sig = 1;
        sig = 31 * sig + (shopType == null ? 0 : shopType.ordinal());

        for (MerchantInfo m : merchants) {
            sig = 31 * sig + System.identityHashCode(m);
            sig = 31 * sig + m.hashCode();
        }
        return sig;
    }

    private record MerchantCard(UIElement root, UIElement dragHandle) {
    }

}
