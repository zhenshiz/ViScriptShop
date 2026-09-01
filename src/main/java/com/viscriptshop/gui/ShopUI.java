package com.viscriptshop.gui;

import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.*;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplate;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.viscriptshop.Config;
import com.viscriptshop.ShopRegistries;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.event.neoforge.ShopClientEvent;
import com.viscriptshop.gui.components.Message;
import com.viscriptshop.gui.components.PlayerHeadElement;
import com.viscriptshop.gui.components.SceneToggleBuilder;
import com.viscriptshop.gui.components.ShopOutputTargetButton;
import com.viscriptshop.gui.components.theme.ShopButton;
import com.viscriptshop.gui.components.theme.ShopScrollerView;
import com.viscriptshop.gui.components.theme.ShopTheme;
import com.viscriptshop.gui.data.AggregatedResources;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.gui.layout.GlassDarkShopUiLayout;
import com.viscriptshop.gui.layout.GrayCatShopUiLayout;
import com.viscriptshop.gui.layout.ShopUiElements;
import com.viscriptshop.gui.layout.ShopUiLayout;
import com.viscriptshop.network.c2s.BuyMerchantPayload;
import com.viscriptshop.network.c2s.C2SPayload;
import com.viscriptshop.network.c2s.GetItemCountC2SPayload;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.MoneyUtil;
import com.viscript_lib.util.CountTextUtil;
import com.viscript_lib.register.IContainerHelper;
import com.viscript_lib.util.item.ItemOutputTargets;
import com.viscript_lib.util.item.SimpleItemStackFilter;
import com.viscriptshop.util.UIElementUtil;
import com.viscriptshop.util.ViScriptShopClientUtil;
import dev.vfyjxf.taffy.style.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ShopUI extends UIElement {
    Minecraft minecraft = Minecraft.getInstance();
    //主题样式
    private final ShopTheme theme = ShopTheme.current();
    // 界面
    public ScrollerView categoryView = new ShopScrollerView(theme);
    public ScrollerView merchantsView = new ShopScrollerView(theme);
    public ScrollerView shoppingCarView = new ShopScrollerView(theme);
    public ScrollerView inventoryView = new ShopScrollerView(theme);
    public SearchComponent<ItemStack> searchComponent;
    private final Toggle currencyLayoutToggle;
    private final UIElement shopUiShell;
    private final ShopUiLayout shopUiLayout;

    private final IGuiTexture LIST_BACKGROUND = theme.merchantList();
    private final IGuiTexture GRID_BACKGROUND = theme.merchantGrid();
    private final SpriteTexture RIGHT_ARROW = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/right_arrow.png"));
    private final SpriteTexture LOCK = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/lock.png"));
    private final SpriteTexture COIN = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/coin.png"));
    private static final float CURRENCY_GRID_GAP = 3f;
    private static final float LOCKED_CATEGORY_OPACITY = 0.7f;

    // 数据
    //玩家身上对应物品的数量
    public List<AggregatedResources.ItemEntry> playerItems = new ArrayList<>();
    //打开的商店信息
    public ShopInfo currentShopInfo;
    //商店文件位置（用于购买后保存数据）
    private String shopLocation;
    //玩家选择的商店信息
    @Getter
    @Setter
    private CategoryInfo selectedCategory;
    @Getter
    @Setter
    private ItemStack searchItem = ItemStack.EMPTY;
    @Getter
    @Setter
    private String searchId = "";
    //当前模式 true为物品查询 false为序号查询
    @Getter
    @Setter
    private boolean searchMode = true;

    @Getter
    @Setter
    private boolean currencyGridLayout = false;
    @Getter
    private IContainerHelper selectedOutputTarget;
    private int currencyGridColumns = -1;
    private String renderedCategoryState = "";

    public ShopUI(String shopLocation, ShopInfo shopInfo, String title) {
        this(shopLocation, shopInfo, title, null, null);
    }

    public ShopUI(String shopLocation, ShopInfo shopInfo, String title, String categoryId, String merchantId) {
        this.shopLocation = shopLocation;
        this.playerItems.clear();
        this.currentShopInfo = initCurrentShopInfo(shopInfo);
        if (minecraft.player != null) {
            // 根据 categoryId 查找对应分类
            if (categoryId != null && !categoryId.isEmpty()) {
                for (CategoryInfo category : this.currentShopInfo.getCategoryInfos()) {
                    if (categoryId.equals(category.getId()) && !isCategoryLocked(category)) {
                        selectedCategory = category;
                        break;
                    }
                }
            }
            // 如果没找到指定分类，优先选择第一个已解锁分类；全部锁定时保留第一个分类用于展示提示。
            if (selectedCategory == null) {
                selectedCategory = this.currentShopInfo.getCategoryInfos().stream()
                        .filter(category -> !isCategoryLocked(category))
                        .findFirst()
                        .orElse(this.currentShopInfo.getCategoryInfos().getFirst());
            }

            // 根据 merchantId 查找对应商品的索引
            if (!isCategoryLocked(selectedCategory) && merchantId != null && !merchantId.isEmpty()) {
                for (int i = 0; i < selectedCategory.getMerchants().size(); i++) {
                    MerchantInfo merchant = selectedCategory.getMerchants().get(i);
                    if (merchantId.equals(merchant.getId())) {
                        this.searchId = String.valueOf(i + 1);
                        this.searchMode = false;
                        break;
                    }
                }
            }

            if (!isCategoryLocked(selectedCategory)) {
                RPCPacketDistributor.rpcToServer(GetItemCountC2SPayload.GET_ITEM_COUNT, currentShopInfo);
            }
        }
        this.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).addEventListener(UIEvents.TICK, event -> NeoForge.EVENT_BUS.post(new ShopClientEvent.Tick(this)));

        ShopUiElements elements = createUiElements(title);
        this.searchComponent = elements.itemSearch();
        this.currencyLayoutToggle = elements.currencyLayoutToggle();
        this.shopUiLayout = theme.isGrayCatWorkshop()
                ? new GrayCatShopUiLayout(elements.itemSearch())
                : GlassDarkShopUiLayout.INSTANCE;
        this.shopUiShell = shopUiLayout.build(theme, elements);
        this.addChild(shopUiShell);

        reloadCategoryList();
        updateCurrencyLayoutToggleState();
        reloadMerchants();
        reloadShoppingItem();
        reloadInventoryItem();
    }

    private ShopUiElements createUiElements(String title) {
        categoryView.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        }).addEventListener(UIEvents.TICK, event -> refreshCategoryListIfNeeded());
        categoryView.verticalScroller.layout(layout -> layout.marginRight(3));
        categoryView.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        categoryView.viewContainer.layout(layout -> {
            layout.gapColumn(5);
            layout.paddingAll(3);
            layout.flexDirection(FlexDirection.COLUMN);
        });

        merchantsView.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        merchantsView.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(5);
        });
        merchantsView.viewPort.addEventListener(UIEvents.LAYOUT_CHANGED, event -> updateCurrencyGridColumns());

        shoppingCarView.viewContainer.layout(layout -> {
            layout.paddingLeft(6);
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
        });
        shoppingCarView.viewPort.getLayout().paddingAll(3);
        shoppingCarView.viewPort.getStyle().backgroundTexture(IGuiTexture.EMPTY);

        inventoryView.viewContainer.layout(layout -> {
            layout.paddingLeft(6);
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
        });
        inventoryView.viewPort.getLayout().paddingAll(3);
        inventoryView.viewPort.setId("shop_consumption_panel");
        inventoryView.viewPort.getStyle().backgroundTexture(theme.consumptionPanel());

        Label categoryTitle = (Label) new Label().setText("viscript_shop.data.shop.categoryInfos");
        Label shopTitle = (Label) new Label().setText(title);
        UIElement balanceIcon = new UIElement().style(style -> style.backgroundTexture(GuiTextureGroup.of(
                theme.balanceIconBackground(),
                COIN.copy().scale(theme.balanceIconScale())
        )));
        Label balanceValue = (Label) new Label()
                .addEventListener(UIEvents.TICK, event ->
                        ((Label) event.currentElement).setText(MoneyUtil.formatCompact(
                                ViScriptShopClientUtil.getMoney(minecraft.player)
                        )));

        UIElement searchIcon = new UIElement().setId("shop_search_icon").style(style ->
                style.backgroundTexture(GuiTextureGroup.of(
                        theme.searchIconBackground(),
                        theme.searchIcon().copy().scale(theme.searchIconScale())
                ))
        );
        SearchComponent<ItemStack> itemSearch = UIElementUtil.createItemStackSearchComponentConfigurator(
                "",
                this::getSearchItem,
                search -> {
                    this.searchItem = search;
                    reloadMerchants();
                },
                getCategoryItems()
        ).searchComponent;
        itemSearch.setId("shop_item_search");
        itemSearch.getStyle().backgroundTexture(theme.searchField());
        itemSearch.searchStyle(style -> style.focusOverlay(IGuiTexture.EMPTY));
        itemSearch.setDisplay(searchMode ? TaffyDisplay.FLEX : TaffyDisplay.NONE);

        StringConfigurator idSearch = (StringConfigurator) new StringConfigurator(
                "",
                this::getSearchId,
                search -> {
                    if (search.chars().allMatch(Character::isDigit)) {
                        this.searchId = search;
                        reloadMerchants();
                    }
                },
                searchId,
                true
        ).setId("shop_id_search");
        idSearch.setDisplay(searchMode ? TaffyDisplay.NONE : TaffyDisplay.FLEX);
        idSearch.getStyle().backgroundTexture(theme.searchField());
        idSearch.textField.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        idSearch.textField.textFieldStyle(textStyle -> {
            textStyle.placeholder(Component.empty());
            textStyle.focusOverlay(IGuiTexture.EMPTY);
        });

        Toggle searchModeToggle = new SceneToggleBuilder(this::isSearchMode, this::setSearchMode)
                .icon(
                        new ItemStackTexture(Items.GRASS_BLOCK),
                        SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/id.png"))
                )
                .baseTexture(theme.toggleBase())
                .hoverTexture(theme.toggleHover())
                .build();
        searchModeToggle.setId("shop_search_mode_toggle");
        searchModeToggle.setOnToggleChanged(isOn -> {
            reloadMerchants();
            itemSearch.setDisplay(isOn ? TaffyDisplay.FLEX : TaffyDisplay.NONE);
            idSearch.setDisplay(isOn ? TaffyDisplay.NONE : TaffyDisplay.FLEX);
        });
        searchModeToggle.addEventListener(UIEvents.TICK, event -> event.target.getStyle().tooltips(
                        Component.translatable(searchMode
                                ? "viscript_shop.ui.searchMode.item"
                                : "viscript_shop.ui.searchMode.id")
                ));

        SpriteTexture gridIcon = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/grid.png"));
        SpriteTexture listIcon = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/list.png"));
        Toggle layoutToggle = new SceneToggleBuilder(
                this::isCurrencyGridLayout,
                this::setCurrencyGridLayout
        )
                .icon(gridIcon, listIcon)
                .baseTexture(theme.toggleBase())
                .hoverTexture(theme.toggleHover())
                .build();
        layoutToggle.setId("shop_currency_layout_toggle");
        layoutToggle.setOnToggleChanged(isOn -> {
            setCurrencyGridLayout(isOn);
            reloadMerchants();
        });

        UIElement playerHead = new UIElement().setId("shop_player_head").addChild(
                new PlayerHeadElement().layout(layout -> layout.marginRight(5))
        );

        ShopButton clearButton = ShopButton.other(theme);
        clearButton.setId("shop_clear_button");
        clearButton.setText("viscript_shop.button.clear").setOnClick(event -> {
                    currentShopInfo.getCategoryInfos().forEach(categoryInfo ->
                            categoryInfo.getMerchants().forEach(merchantInfo ->
                                    merchantInfo.setBuyCount(0)
                            )
                    );
                    reloadShoppingItem();
                    reloadInventoryItem();
                });
        ShopButton stashButton = ShopButton.other(theme);
        stashButton.setId("shop_stash_button");
        stashButton.setText("viscript_shop.button.ts").setOnClick(event -> {
                    ShopHelper.cacheShopInfo = this.currentShopInfo;
                    if (minecraft.screen != null) {
                        minecraft.screen.onClose();
                    }
                });
        ShopButton buyButton = ShopButton.buying(theme);
        buyButton.setId("shop_buy_button");
        buyButton.setText("viscript_shop.button.buy").setOnClick(event -> buy());

        selectedOutputTarget = minecraft.player == null
                ? ItemOutputTargets.playerInventory()
                : ItemOutputTargets.resolve(minecraft.player.getData(ShopRegistries.MONEY).getOutputTargetId());
        ShopOutputTargetButton outputTargetButton = new ShopOutputTargetButton(theme)
                .setTarget(selectedOutputTarget);
        outputTargetButton.setId("shop_output_target_button");
        outputTargetButton.setOnClick(event -> {
            selectedOutputTarget = ItemOutputTargets.next(selectedOutputTarget);
            outputTargetButton.setTarget(selectedOutputTarget);
            RPCPacketDistributor.rpcToServer(C2SPayload.SET_OUTPUT_TARGET_C2S, selectedOutputTarget.name());
        });

        Label shoppingCartTitle = (Label) new Label().setText("viscript_shop.ui.shoppingCar");
        Label consumptionTitle = (Label) new Label().setText("viscript_shop.ui.inventory");

        return new ShopUiElements(
                categoryView,
                merchantsView,
                shoppingCarView,
                inventoryView,
                categoryTitle,
                shopTitle,
                balanceIcon,
                balanceValue,
                searchIcon,
                itemSearch,
                idSearch,
                searchModeToggle,
                layoutToggle,
                playerHead,
                shoppingCartTitle,
                consumptionTitle,
                outputTargetButton,
                stashButton,
                clearButton,
                buyButton
        );
    }

    private void buy() {
        AggregatedResources gainSummary = AggregatedResources.getGainSummary(this.currentShopInfo);
        if (gainSummary.isEmpty()) {
            Message.warn("viscript_shop.message.shoppingCar.empty", this);
            return;
        }
        int maxItems = Config.maxShopUiGiveItemsPerPurchase.get();
        if (maxItems >= 0 && gainSummary.getTotalItemCount() > maxItems) {
            Message.error(Component.translatable(
                    "viscript_shop.message.buy.too_many_items",
                    maxItems
            ).getString(), this);
            return;
        }
        RPCPacketDistributor.rpcToServer(
                BuyMerchantPayload.BUY_MERCHANT,
                this.shopLocation,
                gainSummary.toPurchaseRequest(),
                selectedOutputTarget.name()
        );
    }
    private ShopInfo initCurrentShopInfo(ShopInfo shopInfo) {
        if (ShopHelper.cacheShopInfo == null) {
            return shopInfo;
        }
        copyCachedBuyCounts(ShopHelper.cacheShopInfo, shopInfo);
        return shopInfo;
    }

    private void copyCachedBuyCounts(ShopInfo cachedShopInfo, ShopInfo freshShopInfo) {
        for (CategoryInfo freshCategory : freshShopInfo.getCategoryInfos()) {
            CategoryInfo cachedCategory = cachedShopInfo.getCategoryInfos().stream()
                    .filter(category -> category.getId().equals(freshCategory.getId()))
                    .findFirst()
                    .orElse(null);
            if (cachedCategory == null || isCategoryLocked(freshCategory)) continue;

            for (MerchantInfo freshMerchant : freshCategory.getMerchants()) {
                cachedCategory.getMerchants().stream()
                        .filter(merchant -> merchant.getId().equals(freshMerchant.getId()))
                        .findFirst()
                        .ifPresent(cachedMerchant -> {
                            int buyCount = cachedMerchant.getBuyCount().intValue();
                            int stock = freshMerchant.getStock();
                            freshMerchant.setBuyCount(stock >= 0 ? Math.min(buyCount, stock) : buyCount);
                        });
            }
        }
    }

    @Override
    public void initScreen(int screenWidth, int screenHeight) {
        super.initScreen(screenWidth, screenHeight);
        Size layoutSize = getAutoGuiScaledSize(Size.of(screenWidth, screenHeight));
        shopUiLayout.initScreen(shopUiShell, layoutSize);
        applyAutoGuiScaleTransform();
    }

    public static Size getAutoGuiScaledSize(Size screenSize) {
        float scale = getAutoGuiScaleFactor();
        if (scale <= 0f) return screenSize;

        return Size.of(
                Math.max(1, Math.round(screenSize.getWidth() / scale)),
                Math.max(1, Math.round(screenSize.getHeight() / scale))
        );
    }

    private void applyAutoGuiScaleTransform() {
        float scale = getAutoGuiScaleFactor();
        // 让固定尺寸控件在任意 GUI Scale 下都保持 Auto 缩放时的视觉大小。
        transform(transform -> transform.pivot(0.5f, 0.5f).scale(scale));
    }

    private static float getAutoGuiScaleFactor() {
        Minecraft minecraft = Minecraft.getInstance();

        var window = minecraft.getWindow();
        double currentScale = window.getGuiScale();
        if (currentScale <= 0d) return 1f;

        int autoScale = window.calculateScale(0, minecraft.isEnforceUnicode());
        return Math.max(1f, (float) (autoScale / currentScale));
    }

    public void reloadCategoryList() {
        renderedCategoryState = getCategoryListState();
        categoryView.clearAllScrollViewChildren();

        for (int i = 0; i < currentShopInfo.getCategoryInfos().size(); i++) {
            CategoryInfo categoryInfo = currentShopInfo.getCategoryInfos().get(i);
            List<Component> lockReasons = getCategoryLockReasons(categoryInfo);
            boolean locked = !lockReasons.isEmpty();
            if (locked && currentShopInfo.getLockedMerchantVisibility() == ShopInfo.LockedMerchantVisibility.HIDDEN) {
                continue;
            }
            UIElement categoryUI = UIElementUtil.createCategoryUI(
                    categoryInfo,
                    !locked && categoryInfo.equals(this.selectedCategory),
                    value -> {
                        if (isCategoryLocked(value)) {
                            return;
                        }
                        setSelectedCategory(value);
                        if (minecraft.player != null) {
                            RPCPacketDistributor.rpcToServer(GetItemCountC2SPayload.GET_ITEM_COUNT, currentShopInfo);
                        }
                        reloadMerchants();
                    },
                    theme.categoryDefault(),
                    theme.categorySelected(),
                    theme.categoryEntryHeight()
            );
            categoryUI.setId("shop_category_" + i);
            if (locked) {
                categoryUI.addClass("shop-category-locked");
                setCategoryVisualOpacity(categoryUI, LOCKED_CATEGORY_OPACITY);
                categoryUI.addEventListener(UIEvents.HOVER_TOOLTIPS, event ->
                        event.hoverTooltips = new HoverTooltips(lockReasons, null, null, null));
            }
            categoryView.viewContainer.addChildren(categoryUI);
        }
    }

    private void refreshCategoryListIfNeeded() {
        if (!renderedCategoryState.equals(getCategoryListState())) {
            reloadCategoryList();
        }
    }

    private String getCategoryListState() {
        StringBuilder state = new StringBuilder()
                .append(currentShopInfo.getLockedMerchantVisibility())
                .append('|')
                .append(System.identityHashCode(selectedCategory));
        for (CategoryInfo categoryInfo : currentShopInfo.getCategoryInfos()) {
            state.append('|')
                    .append(System.identityHashCode(categoryInfo))
                    .append(':')
                    .append(isCategoryLocked(categoryInfo));
        }
        return state.toString();
    }

    private static void setCategoryVisualOpacity(UIElement categoryUI, float opacity) {
        categoryUI.select(".shop-category-icon")
                .forEach(element -> element.style(style -> style.opacity(opacity)));
        categoryUI.select(".shop-category-label")
                .forEach(element -> element.style(style -> style.opacity(opacity)));
    }

    public void reloadMerchants() {
        merchantsView.clearAllScrollViewChildren();
        updateCurrencyLayoutToggleState();
        configureMerchantsContainerLayout();
        if (selectedCategory == null || isCategoryLocked(selectedCategory)) {
            return;
        }

        // 重新添加所有商品
        for (int i = 0; i < selectedCategory.getMerchants().size(); i++) {
            MerchantInfo merchantInfo = selectedCategory.getMerchants().get(i);
            // 锁定内容显示方式：隐藏
            if (currentShopInfo.getLockedMerchantVisibility().equals(ShopInfo.LockedMerchantVisibility.HIDDEN) && isMerchantLocked(merchantInfo)) {
                continue;
            }
            //搜索筛选 物品筛选和序号筛选
            if (this.searchMode) {
                if (!this.searchItem.isEmpty()) {
                    boolean isMatch = ItemStack.isSameItemSameComponents(merchantInfo.getItemResult(), this.searchItem) ||
                            merchantInfo.getItemAMatchRule().matches(merchantInfo.getItemA(), this.searchItem) ||
                            merchantInfo.getItemBMatchRule().matches(merchantInfo.getItemB(), this.searchItem);
                    if (!isMatch) {
                        continue;
                    }
                }
            } else {
                if (!this.searchId.isEmpty()) {
                    try {
                        int targetIndex = Integer.parseInt(this.searchId);
                        if ((i + 1) != targetIndex) {
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
            if (isCurrencyGridActive()) {
                merchantsView.addScrollViewChild(createCurrencyMerchantGrid(merchantInfo, i));
            } else {
                merchantsView.addScrollViewChild(createMerchant(merchantInfo, i));
            }
        }
    }

    private boolean isCurrencyGridActive() {
        return selectedCategory != null
                && selectedCategory.getShopType() == CategoryInfo.ShopType.CURRENCY
                && currencyGridLayout;
    }

    private void updateCurrencyLayoutToggleState() {
        if (currencyLayoutToggle == null) return;

        boolean show = selectedCategory != null && selectedCategory.getShopType() == CategoryInfo.ShopType.CURRENCY;
        currencyLayoutToggle.setDisplay(show ? TaffyDisplay.FLEX : TaffyDisplay.NONE);
        currencyLayoutToggle.getStyle().tooltips(Component.translatable(currencyGridLayout ? "viscript_shop.ui.layout.grid" : "viscript_shop.ui.layout.list"));
        currencyLayoutToggle.setValue(currencyGridLayout, false);
    }

    private void configureMerchantsContainerLayout() {
        if (isCurrencyGridActive()) {
            merchantsView.viewContainer.layout(layout -> {
                layout.display(TaffyDisplay.GRID);
                layout.gridAutoFlow(GridAutoFlow.ROW);
                layout.justifyItems(AlignItems.CENTER);
                layout.alignItems(AlignItems.FLEX_START);
                layout.justifyContent(AlignContent.CENTER);
                layout.alignContent(AlignContent.FLEX_START);
                layout.gapAll(CURRENCY_GRID_GAP);
            });
            updateCurrencyGridColumns();
        } else {
            merchantsView.viewContainer.layout(layout -> {
                layout.display(TaffyDisplay.FLEX);
                layout.flexDirection(FlexDirection.COLUMN);
                layout.wrap(FlexWrap.NO_WRAP);
                layout.gapAll(5);
            });
            currencyGridColumns = -1;
        }
    }

    private void updateCurrencyGridColumns() {
        if (!isCurrencyGridActive()) return;
        if (merchantsView == null || merchantsView.viewPort == null) return;

        float available = merchantsView.viewPort.getContentWidth();
        if (available <= 1f) return;

        float cardWidth = theme.merchantGridWidth();
        int cols = Math.max(1, (int) Math.floor((available + CURRENCY_GRID_GAP) / (cardWidth + CURRENCY_GRID_GAP)));
        while (cols > 1) {
            float required = cols * cardWidth + (cols - 1) * CURRENCY_GRID_GAP;
            if (required <= available + 0.01f) break;
            cols--;
        }
        if (cols == currencyGridColumns) return;
        currencyGridColumns = cols;

        List<TrackSizingFunction> tracks = new ArrayList<>(cols);
        for (int i = 0; i < cols; i++) {
            tracks.add(TrackSizingFunction.fixed(cardWidth));
        }
        merchantsView.viewContainer.getLayout().gridTemplateColumns(new GridTemplate(tracks, List.of(), List.of()));
        merchantsView.viewContainer.markTaffyStyleDirty();
    }

    public void reloadShoppingItem() {
        shoppingCarView.clearAllScrollViewChildren();

        AggregatedResources gainSummary = AggregatedResources.getGainSummary(currentShopInfo);
        AggregatedResources costSummary = AggregatedResources.getCostSummary(currentShopInfo);
        gainSummary.getItems().forEach(itemStack -> {
            int count = itemStack.getCount();
            ItemStack displayStack = itemStack.copy();
            displayStack.setCount(1);
            Label countLabel = (Label) new Label().setText(CountTextUtil.formatCount(count))
                    .textStyle(textStyle -> {
                        textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.BOTTOM);
                        textStyle.fontSize(5);
                    })
                    .layout(layout -> {
                        layout.width(10);
                        layout.heightPercent(100);
                    })
                    .addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                        event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(String.valueOf(count))), null, null, null);
                    });
            shoppingCarView.addScrollViewChild(createItemInfoBox().addChildren(UIElementUtil.createItemSlot(displayStack, false, true), countLabel));
        });
        double netMoneyGain = MoneyUtil.subtract(gainSummary.getTotalMoney(), costSummary.getTotalMoney());
        if (netMoneyGain > 0) {
            UIElement moneyIcon = new UIElement().layout(layout -> {
                layout.width(16);
                layout.height(16);
                layout.marginLeft(2);
            }).style(style -> style.backgroundTexture(COIN));
            Label money = (Label) new Label().setText(MoneyUtil.formatCompact(netMoneyGain)).textStyle(textStyle -> {
                textStyle.textAlignVertical(Vertical.BOTTOM).adaptiveWidth(true);
                textStyle.fontSize(5);
            }).layout(layout -> {
                layout.heightPercent(100);
            }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(MoneyUtil.format(netMoneyGain))), null, null, null);
            });
            money.setId("shop_cart_money");
            shoppingCarView.addScrollViewChild(createItemInfoBox().addChildren(moneyIcon, money));
        }
    }

    public void reloadInventoryItem() {
        inventoryView.clearAllScrollViewChildren();
        AggregatedResources costSummary = AggregatedResources.getCostSummary(currentShopInfo);
        AggregatedResources gainSummary = AggregatedResources.getGainSummary(currentShopInfo);
        costSummary.getItemEntries().forEach(itemEntry -> {
            ItemStack itemStack = itemEntry.getItemStack();
            int count = itemEntry.getCount();
            ItemStack displayStack = itemStack.copy();
            displayStack.setCount(1);
            int itemCount = getItemCount(itemEntry);
            String color = itemCount >= count ? "§a" : "§c";
            Label countLabel = (Label) new Label().setText(color + CountTextUtil.formatCount(count) + "§f/" + CountTextUtil.formatCount(itemCount))
                    .textStyle(textStyle -> {
                        textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.BOTTOM);
                        textStyle.fontSize(4);
                    })
                    .layout(layout -> {
                        layout.width(10);
                        layout.heightPercent(100);
                    })
                    .addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                        event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(color + count + "§f/" + itemCount)), null, null, null);
                    });
            inventoryView.addScrollViewChild(createItemInfoBox().addChildren(UIElementUtil.createItemSlot(displayStack, false, true), countLabel));
        });
        double netMoneyCost = MoneyUtil.subtract(costSummary.getTotalMoney(), gainSummary.getTotalMoney());
        if (netMoneyCost > 0 && minecraft.player != null) {
            String color = MoneyUtil.hasEnough(ViScriptShopClientUtil.getMoney(minecraft.player),
                    netMoneyCost) ? "§a" : "§c";
            UIElement moneyIcon = new UIElement().layout(layout -> {
                layout.width(16);
                layout.height(16);
                layout.marginLeft(2);
            }).style(style -> style.backgroundTexture(COIN));
            Label money = (Label) new Label().setText(color + MoneyUtil.formatCompact(netMoneyCost)).textStyle(textStyle -> {
                textStyle.textAlignVertical(Vertical.BOTTOM).adaptiveWidth(true);
                textStyle.fontSize(5);
            }).layout(layout -> {
                layout.heightPercent(100);
            }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(color + MoneyUtil.format(netMoneyCost))), null, null, null);
            });
            money.setId("shop_cost_money");
            inventoryView.addScrollViewChild(createItemInfoBox().addChildren(moneyIcon, money));
        }

    }

    public void reloadSearchComponent() {
        Set<ItemStack> items = getCategoryItems();
        searchComponent.setSearchUI(new SearchComponent.ISearchUI<>() {
            @Override
            public @NotNull String resultText(@NotNull ItemStack value) {
                return value.isEmpty() ? "" : value.getHoverName().getString();
            }

            @Override
            public void onResultSelected(@Nullable ItemStack value) {
                searchItem = value;
                reloadMerchants();
            }

            @Override
            public void search(String word, IResultHandler<ItemStack> handler) {
                Collection<ItemStack> candidatesItems = items;

                if (candidatesItems == null) {
                    candidatesItems = BuiltInRegistries.ITEM.stream()
                            .map(ItemStack::new)
                            .toList();
                }

                for (ItemStack stack : candidatesItems) {
                    if (Thread.currentThread().isInterrupted()) return;

                    if (stack.isEmpty()) {
                        handler.acceptResult(stack);
                        continue;
                    }

                    if (SimpleItemStackFilter.matchItemSearch(stack, word)) {
                        handler.acceptResult(stack);
                    }
                }
            }
        });
    }

    public UIElement createMerchant(MerchantInfo merchantInfo, int index) {
        UIElement merchant = new UIElement().setId("shop_merchant_list_" + index).layout(layout -> {
            layout.widthPercent(100);
            layout.height(theme.merchantRowHeight());
            layout.gapAll(6);
            layout.flexDirection(FlexDirection.ROW);
            layout.paddingHorizontal(4);
            layout.alignItems(AlignItems.CENTER);
        });
        merchant.getStyle().backgroundTexture(LIST_BACKGROUND);
        List<Component> lockReasons = getMerchantLockReasons(merchantInfo);
        boolean locked = !lockReasons.isEmpty();
        Label id = (Label) new Label().setText(String.valueOf(index + 1)).textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER);
            textStyle.fontSize(6);
        }).layout(layout -> {
            layout.width(20);
            layout.heightPercent(100);
        });

        UIElement uiElement = new UIElement().layout(layout -> {
            layout.widthPercent(20);
            layout.heightPercent(100);
            layout.gapAll(5);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
        });
        UIElement rightArrowIcon = new UIElement().style(style -> style.backgroundTexture(RIGHT_ARROW)).layout(layout -> {
            layout.width(12);
            layout.height(12);
        });
        UIElement resultItemSlot = UIElementUtil.createMerchantItemDisplay(
                merchantInfo.getItemResultInfo(),
                true
        ).setId("itemResult" + index);
        resultItemSlot.getLayout().marginRight(2);

        merchant.addChildren(id);

        switch (selectedCategory.getShopType()) {
            case ITEM_FOR_ITEM -> {
                UIElement itemASlot = UIElementUtil.createMerchantItemDisplay(
                        merchantInfo.getItemAInfo(),
                        true
                ).setId("itemA" + index);
                UIElement itemBSlot = UIElementUtil.createMerchantItemDisplay(
                        merchantInfo.getItemBInfo(),
                        true
                ).setId("itemB" + index);
                uiElement.addChildren(itemASlot, itemBSlot);
                merchant.addChildren(uiElement, rightArrowIcon, resultItemSlot);
            }
            case CURRENCY -> {
                Label money = (Label) new Label().setText("◎" + MoneyUtil.formatCompact(merchantInfo.getMoney())).textStyle(textStyle -> {
                    textStyle.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).adaptiveWidth(true);
                    textStyle.fontSize(8);
                }).layout(layout -> {
                    layout.heightPercent(100);
                }).addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(MoneyUtil.format(merchantInfo.getMoney()))), null, null, null);
                });
                uiElement.getLayout().justifyContent(AlignContent.SPACE_BETWEEN);
                uiElement.getLayout().widthPercent(45);
                UIElement moneyUI = new UIElement().layout(layout -> {
                    layout.widthPercent(40);
                    layout.heightPercent(100);
                    layout.justifyContent(AlignContent.CENTER);
                    layout.alignItems(AlignItems.CENTER);
                }).addChild(money);

                UIElement itemUI = new UIElement().layout(layout -> {
                    layout.widthPercent(40);
                    layout.heightPercent(100);
                    layout.justifyContent(AlignContent.CENTER);
                    layout.alignItems(AlignItems.CENTER);
                }).addChild(resultItemSlot);
                switch (merchantInfo.getTradeType()) {
                    case BUY -> uiElement.addChildren(moneyUI, rightArrowIcon, itemUI);
                    case SELL -> uiElement.addChildren(itemUI, rightArrowIcon, moneyUI);
                }
                merchant.addChildren(uiElement);
            }
        }

        // 添加库存悬浮提示或遮罩
        int stock = merchantInfo.getStock();
        if (stock > 0) {
            // 库存 > 0：添加悬浮提示显示库存
            addStockTooltip(merchant, stock);
        } else if (stock == 0) {
            // 库存 = 0：添加半透明遮罩
            merchant.addChildren(createStockOverlay());
        }
        UIElement actionArea = new UIElement().setId("shop_merchant_action_" + index).layout(layout -> {
            layout.gapAll(1);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.FLEX_END);
            layout.heightPercent(100);
        });

        if (locked) {
            UIElement lockIcon = new UIElement()
                    .setId("shop_merchant_lock_" + index)
                    .style(style -> style.backgroundTexture(LOCK))
                    .layout(layout -> {
                        layout.width(16);
                        layout.height(16);
                    })
                    .addEventListener(UIEvents.HOVER_TOOLTIPS, event ->
                            event.hoverTooltips = new HoverTooltips(lockReasons, null, null, null));
            actionArea.addChild(lockIcon);
        } else {
            final Button[] buttonHolder = new Button[2];

            buttonHolder[0] = ShopButton.other(theme).setText("-").setOnClick(event -> {
                if ((int) merchantInfo.getBuyCount() > 0) {
                    merchantInfo.setBuyCount((int) merchantInfo.getBuyCount() - 1);
                    reloadShoppingItem();
                    reloadInventoryItem();
                    updateStockButtons(merchantInfo, buttonHolder[0], buttonHolder[1]);
                }
            });
            buttonHolder[0].setId("shop_merchant_remove_" + index);

            buttonHolder[1] = ShopButton.other(theme).setText("+").setOnClick(event -> {
                int maxCount = stock >= 0 ? stock : Integer.MAX_VALUE;
                if ((int) merchantInfo.getBuyCount() < maxCount) {
                    merchantInfo.setBuyCount(stock > 0 && event.isCtrlDown()
                            ? stock
                            : (int) merchantInfo.getBuyCount() + 1);
                    reloadShoppingItem();
                    reloadInventoryItem();
                    updateStockButtons(merchantInfo, buttonHolder[0], buttonHolder[1]);
                }
            });
            buttonHolder[1].setId("shop_merchant_add_" + index);

            if (stock > 0) {
                buttonHolder[1].addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    event.hoverTooltips = new HoverTooltips(
                            List.of(Component.translatable(
                                    "viscript_shop.tooltip.stock.quick_max",
                                    Minecraft.ON_OSX ? "Cmd" : "Ctrl"
                            )),
                            null, null, null
                    );
                });
            }

            NumberConfigurator countConfigurator = new NumberConfigurator("", merchantInfo::getBuyCount, count -> {
                merchantInfo.setBuyCount(count);
                reloadShoppingItem();
                reloadInventoryItem();
                updateStockButtons(merchantInfo, buttonHolder[0], buttonHolder[1]);
            }, 0, true);
            countConfigurator.setId("shop_merchant_count_" + index);
            countConfigurator.layout(layout -> {
                switch (selectedCategory.getShopType()) {
                    case ITEM_FOR_ITEM -> layout.width(35);
                    case CURRENCY -> layout.width(30);
                }
            });
            applyMerchantCountFieldBackground(countConfigurator);
            applyStockRestrictions(merchantInfo, countConfigurator, buttonHolder[0], buttonHolder[1]);
            actionArea.addChildren(buttonHolder[0], countConfigurator, buttonHolder[1]);
        }

        UIElement actionSpacer = new UIElement().layout(layout -> {
            layout.width(0);
            layout.height(1);
            layout.flex(1);
        });
        merchant.addChildren(actionSpacer, actionArea);

        return merchant;
    }

    public UIElement createCurrencyMerchantGrid(MerchantInfo merchantInfo, int index) {
        UIElement merchant = new UIElement()
                .setId("shop_merchant_grid_" + index)
                .addClass("shop-merchant-grid-card")
                .layout(layout -> {
                    layout.width(theme.merchantGridWidth());
                    if (theme.merchantGridHeight() > 0) {
                        layout.height(theme.merchantGridHeight());
                    }
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.alignItems(AlignItems.CENTER);
                    layout.justifyContent(AlignContent.FLEX_START);
                    layout.paddingAll(5);
                    layout.gapAll(2);
                    layout.positionType(TaffyPosition.RELATIVE);
                });
        merchant.getStyle().backgroundTexture(GRID_BACKGROUND);
        List<Component> lockReasons = getMerchantLockReasons(merchantInfo);
        boolean locked = !lockReasons.isEmpty();

        Label id = (Label) new Label().setText(String.valueOf(index + 1)).textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER);
            textStyle.fontSize(8);
        }).layout(layout -> {
            layout.widthPercent(100);
            layout.height(6);
            layout.alignSelf(AlignItems.FLEX_START);
        });

        UIElement resultItemSlot = UIElementUtil.createMerchantItemDisplay(
                        merchantInfo.getItemResultInfo(),
                        true
                )
                .setId("itemResult" + index)
                .layout(layout -> {
                    layout.width(20);
                    layout.height(20);
                });

        String tradeText = merchantInfo.getTradeType().getSerializedName();
        Label tradeLabel = (Label) new Label()
                .setText(Component.translatable(tradeText))
                .textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER).fontSize(6))
                .layout(layout -> layout.widthPercent(100));

        Label priceLabel = (Label) new Label()
                .setText(Component.literal("◎" + MoneyUtil.formatCompact(merchantInfo.getMoney())))
                .textStyle(textStyle -> textStyle
                        .textColor(0xFFFFAA00)
                        .textAlignHorizontal(Horizontal.CENTER)
                        .textAlignVertical(Vertical.CENTER)
                        .fontSize(8)
                )
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.marginTop(1);
                    layout.marginBottom(2);
                })
                .addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                    event.hoverTooltips = new HoverTooltips(List.of(Component.nullToEmpty(MoneyUtil.format(merchantInfo.getMoney()))), null, null, null);
                });

        int stock = merchantInfo.getStock();
        // 添加库存悬浮提示或遮罩
        if (stock > 0) {
            // 库存 > 0：添加悬浮提示显示库存
            addStockTooltip(merchant, stock);
        } else if (stock == 0) {
            // 库存 = 0：添加半透明遮罩
            merchant.addChildren(createStockOverlay());
        }

        UIElement body = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(3);
        }).addChildren(resultItemSlot, tradeLabel, priceLabel);

        NumberConfigurator countConfigurator;
        if (locked) {
            countConfigurator = new NumberConfigurator("", () -> 0, count -> {}, 0, true);
        } else {
            countConfigurator = new NumberConfigurator("", merchantInfo::getBuyCount, count -> {
                merchantInfo.setBuyCount(count);
                reloadShoppingItem();
                reloadInventoryItem();
            }, 0, true);
        }
        countConfigurator.setId("shop_merchant_grid_count_" + index);
        countConfigurator.layout(layout -> layout.width(28));
        applyMerchantCountFieldBackground(countConfigurator);
        if (locked) {
            countConfigurator.setRange(0, 0);
            countConfigurator.textField.setWheelDur(0);
            countConfigurator.textField.setActive(false);
        } else if (stock >= 0) {
            countConfigurator.setRange(0, stock);
            if (stock == 0) {
                countConfigurator.textField.setWheelDur(0);
                countConfigurator.textField.setActive(false);
            }
        } else {
            countConfigurator.setRange(0, Integer.MAX_VALUE);
        }

        UIElement controls = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            if (theme.isGrayCatWorkshop()) {
                layout.top(-3);
            }
        }).addChild(countConfigurator);
        merchant.addChildren(id, body, controls);
        if (locked) {
            UIElement lockIcon = new UIElement()
                    .setId("shop_merchant_grid_lock_" + index)
                    .style(style -> style.backgroundTexture(LOCK))
                    .layout(layout -> {
                        layout.width(12);
                        layout.height(12);
                        layout.positionType(TaffyPosition.ABSOLUTE);
                        layout.top(2);
                        layout.right(2);
                    })
                    .addEventListener(UIEvents.HOVER_TOOLTIPS, event ->
                            event.hoverTooltips = new HoverTooltips(lockReasons, null, null, null));
            merchant.addChild(lockIcon);
        }
        return merchant;
    }

    private void applyMerchantCountFieldBackground(NumberConfigurator countConfigurator) {
        countConfigurator.inlineContainer.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        countConfigurator.textField.getStyle().backgroundTexture(theme.searchField());
        countConfigurator.textField.textFieldStyle(style -> style.focusOverlay(IGuiTexture.EMPTY));
    }

    /**
     * 判断商品是否解锁
     *
     * @param merchantInfo 商品信息
     * @return 商品尚未满足阶段条件时返回 {@code true}
     */
    private boolean isMerchantLocked(MerchantInfo merchantInfo) {
        return !getMerchantLockReasons(merchantInfo).isEmpty();
    }

    private List<Component> getMerchantLockReasons(MerchantInfo merchantInfo) {
        if (minecraft.player == null) {
            return List.of();
        }

        return merchantInfo.getLockTooltips(ViScriptShopClientUtil.getStageFlags(minecraft.player));
    }

    /**
     * 判断分类是否因玩家阶段条件未满足而锁定。
     *
     * @param  categoryInfo 分类信息
     * @return 玩家无法访问此分类时返回 {@code true}
     */
    private boolean isCategoryLocked(CategoryInfo categoryInfo) {
        return categoryInfo != null && !categoryInfo.canAccess(getPlayerStageFlags());
    }

    /**
     * 获取分类锁定时显示的默认或自定义提示行。
     *
     * @param  categoryInfo 分类信息
     * @return 分类锁定提示；分类已解锁或玩家不存在时返回空列表
     */
    private List<Component> getCategoryLockReasons(CategoryInfo categoryInfo) {
        return categoryInfo == null ? List.of() : categoryInfo.getLockTooltips(getPlayerStageFlags());
    }

    private Collection<String> getPlayerStageFlags() {
        return minecraft.player == null ? List.of() : ViScriptShopClientUtil.getStageFlags(minecraft.player);
    }

    /**
     * 创建库存遮罩层（当库存为0时显示）
     */
    private UIElement createStockOverlay() {
        UIElement overlay = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.top(0);
            layout.left(0);
        });
        overlay.getStyle().backgroundTexture(new ColorRectTexture(0x80000000)); // 半透明黑色
        overlay.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            event.hoverTooltips = new HoverTooltips(
                    List.of(Component.translatable("viscript_shop.message.stock.out").withStyle(ChatFormatting.RED)),
                    null, null, null
            );
        });
        return overlay;
    }

    /**
     * 创建库存悬浮提示（当库存>0时显示）
     */
    private void addStockTooltip(UIElement element, int stock) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            event.hoverTooltips = new HoverTooltips(
                    List.of(Component.translatable("viscript_shop.message.stock.available", stock).withStyle(ChatFormatting.YELLOW)),
                    null, null, null
            );
        });
    }

    /**
     * 应用库存限制到输入框和按钮
     */
    private void applyStockRestrictions(MerchantInfo merchantInfo, NumberConfigurator countConfigurator, Button removeButton, Button addButton) {
        int stock = merchantInfo.getStock();

        // 库存 < 0：无限库存，不限制
        if (stock < 0) {
            countConfigurator.setRange(0, Integer.MAX_VALUE);
            return;
        }

        // 库存 = 0：禁用所有控件
        if (stock == 0) {
            countConfigurator.setRange(0, 0);
            countConfigurator.textField.setWheelDur(0);
            countConfigurator.textField.setActive(false);
            removeButton.setActive(false);
            addButton.setActive(false);
            return;
        }

        // 库存 > 0：设置范围并控制按钮状态
        countConfigurator.setRange(0, stock);

        // 根据当前购买数量更新按钮状态
        updateStockButtons(merchantInfo, removeButton, addButton);
    }

    /**
     * 更新按钮状态（根据库存和当前购买数量）
     */
    private void updateStockButtons(MerchantInfo merchantInfo, Button removeButton, Button addButton) {
        int stock = merchantInfo.getStock();
        int currentCount = (int) merchantInfo.getBuyCount();

        if (stock < 0) {
            // 无限库存，按钮始终可用
            removeButton.setActive(true);
            addButton.setActive(true);
            return;
        }

        removeButton.setActive(currentCount > 0);
        addButton.setActive(currentCount < stock);
    }

    public void setItemCount(AggregatedResources.ItemEntry itemEntry) {
        AggregatedResources.ItemEntry copy = itemEntry.copyWithCount(itemEntry.getCount());
        for (int i = 0; i < this.playerItems.size(); i++) {
            AggregatedResources.ItemEntry existing = this.playerItems.get(i);
            if (existing.canMerge(copy.getItemStack(), copy.getMatchRule())) {
                this.playerItems.set(i, copy);
                return;
            }
        }
        this.playerItems.add(copy);
    }

    public int getItemCount(AggregatedResources.ItemEntry itemEntry) {
        for (AggregatedResources.ItemEntry item : this.playerItems) {
            if (item.canMerge(itemEntry.getItemStack(), itemEntry.getMatchRule())) {
                return item.getCount();
            }
        }
        return 0;
    }

    private UIElement createItemInfoBox() {
        return new UIElement().layout(layout -> {
            layout.widthPercent(50);
            layout.height(20);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.alignItems(AlignItems.CENTER);
            layout.flexDirection(FlexDirection.ROW);
        });
    }

    public Set<ItemStack> getCategoryItems() {
        Set<ItemStack> items = new HashSet<>();
        items.add(ItemStack.EMPTY);
        if (selectedCategory == null || isCategoryLocked(selectedCategory)) {
            return items;
        }
        List<MerchantInfo> merchants = selectedCategory.getMerchants();

        for (MerchantInfo merchant : merchants) {
            if (!isMerchantLocked(merchant)) {
                if (selectedCategory.getShopType() == CategoryInfo.ShopType.ITEM_FOR_ITEM) {
                    addItemStackIfUnique(items, merchant.getItemA());
                    addItemStackIfUnique(items, merchant.getItemB());
                }
                addItemStackIfUnique(items, merchant.getItemResult());
            }
        }
        return items;
    }

    private void addItemStackIfUnique(Set<ItemStack> list, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        for (ItemStack existing : list) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                return;
            }
        }
        ItemStack displayStack = stack.copy();
        displayStack.setCount(1);

        list.add(displayStack);
    }
}
