package com.viscriptshop.compat.jei;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.MerchantItemInfo;
import com.viscriptshop.gui.data.ShopDisplayEntry;
import com.viscriptshop.util.MoneyUtil;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.common.gui.elements.TextWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 使用两个成本位置、一个收益位置和末尾赠品位置展示商店交易。 */
public final class ShopRecipeCategory implements IRecipeCategory<ShopDisplayEntry> {
    public static final RecipeType<ShopDisplayEntry> TYPE = new RecipeType<>(
            ViscriptShop.id("shop_trade"), ShopDisplayEntry.class);
    private static final SpriteTexture COIN = SpriteTexture.of(ViscriptShop.id("textures/icons/coin.png"));
    private static final SpriteTexture LOCK = SpriteTexture.of(ViscriptShop.id("textures/icons/lock.png"));
    private static final int RESULT_X = 120;
    private static final int GIFT_X = 160;
    private static final int LOCK_X = 188;
    private final IDrawable arrow;
    private final IDrawable icon = new IDrawable() {
        @Override public int getWidth() { return 16; }
        @Override public int getHeight() { return 16; }
        @Override public void draw(GuiGraphics graphics, int x, int y) {
            COIN.draw(graphics, 0, 0, x, y, 16, 16, 0);
        }
    };

    public ShopRecipeCategory(IGuiHelper gui) {
        arrow = gui.getRecipeArrow();
    }

    @Override public RecipeType<ShopDisplayEntry> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("viscript_shop.jei.title"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 212; }
    @Override public int getHeight() { return 46; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ShopDisplayEntry entry, IFocusGroup focuses) {
        if (entry.isCurrency()) {
            addItem(builder, entry.getResult(), entry.isSelling() ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT,
                    entry.isSelling() ? 8 : RESULT_X, entry.getResultCount());
        } else {
            addItem(builder, entry.getItemA(), RecipeIngredientRole.INPUT, 8, entry.getItemACount());
            addItem(builder, entry.getItemB(), RecipeIngredientRole.INPUT, 48, entry.getItemBCount());
            addItem(builder, entry.getResult(), RecipeIngredientRole.OUTPUT, RESULT_X, entry.getResultCount());
        }
        if (!entry.getGift().getItem().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, GIFT_X, 24)
                    .addItemStack(entry.getGift().getItem().copyWithCount(1))
                    .setCustomRenderer(VanillaTypes.ITEM_STACK,
                            new ShopItemRenderer(entry.getGift(), entry.getGift().getItem().getCount(), true))
                    .addRichTooltipCallback((slot, tooltip) -> {
                        tooltip.add(Component.translatable("viscript_shop.ui.promotion.gift_preview",
                                entry.getGiftThreshold(), entry.getGift().getItem().getCount()));
                        if (!entry.isGiftConditionsMet()) tooltip.add(Component.translatable("viscript_shop.ui.promotion.gift_conditional"));
                    });
        }
    }

    private void addItem(IRecipeLayoutBuilder builder, MerchantItemInfo item, RecipeIngredientRole role, int x, int count) {
        if (item.getItem().isEmpty()) return;
        // JEI 只索引单个物品；完整交易数量保留在 VSS 数据和独立渲染器中。
        builder.addSlot(role, x, 24).addItemStack(item.getItem().copyWithCount(1))
                .setCustomRenderer(VanillaTypes.ITEM_STACK, new ShopItemRenderer(item, count));
    }

    @Override
    public void draw(ShopDisplayEntry entry, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        String title = entry.getShopName() + " / " + entry.getCategoryName();
        String label = font.width(title) <= 204 ? title : font.plainSubstrByWidth(title, 194) + "…";
        graphics.drawString(font, label, 4, 3, 0xFF555555, false);
        graphics.fill(4, 15, 208, 16, 0x30707070);
        arrow.draw(graphics, 82, 24);
        if (!entry.getStageTooltips().isEmpty()) {
            LOCK.draw(graphics, 0, 0, LOCK_X, 22, 20, 20, 0);
        }
        if (entry.isCurrency()) {
            int x = entry.isSelling() ? RESULT_X : 8;
            icon.draw(graphics, x, 24);
            ShopItemRenderer.drawAmount(graphics, MoneyUtil.formatCompact(entry.getMoney()), x, 24,
                    entry.getMoney() != entry.getBaseMoney());
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ShopDisplayEntry entry, IFocusGroup focuses) {
        builder.addWidget(new IRecipeWidget() {
            private long generation = ShopJeiClient.generation();

            @Override public ScreenPosition getPosition() { return new ScreenPosition(0, 0); }

            @Override public void tick() {
                if (generation != ShopJeiClient.generation()) {
                    generation = ShopJeiClient.generation();
                    ShopJeiClient.refreshOpenPage(focuses);
                }
            }
        });
        builder.addWidget(new TooltipWidget(List.of(Component.literal(entry.getShopName() + " / " + entry.getCategoryName()),
                Component.translatable("viscript_shop.jei.current_price")), 0, 0, 212, 18));
        if (!entry.getStageTooltips().isEmpty()) {
            builder.addWidget(new TooltipWidget(entry.getStageTooltips().stream().map(t -> (FormattedText) t).toList(), LOCK_X, 22, 20, 20));
        }
        if (entry.isCurrency()) {
            var tooltip = new ArrayList<FormattedText>();
            tooltip.add(Component.translatable("viscript_shop.jei.money", MoneyUtil.formatGrouped(entry.getMoney())));
            if (entry.getMoney() != entry.getBaseMoney()) {
                tooltip.add(Component.translatable("viscript_shop.ui.promotion.price_compare",
                        MoneyUtil.formatGrouped(entry.getBaseMoney()), MoneyUtil.formatGrouped(entry.getMoney())));
            }
            builder.addWidget(new TooltipWidget(tooltip, entry.isSelling() ? RESULT_X - 10 : 0, 24,
                    entry.isSelling() ? 28 : 26, 16));
        }
    }

    @Override
    public ResourceLocation getRegistryName(ShopDisplayEntry entry) {
        String key = entry.getShopLocation() + '\0' + entry.getCategoryId() + '\0' + entry.getMerchantId();
        return ViscriptShop.id("trade/" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)));
    }

    public static class TooltipWidget extends TextWidget {
        public TooltipWidget(List<FormattedText> text, int xPos, int yPos, int maxWidth, int maxHeight) {
            super(text, xPos, yPos, maxWidth, maxHeight);
        }

        @Override
        public void drawWidget(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        }
    }
}
