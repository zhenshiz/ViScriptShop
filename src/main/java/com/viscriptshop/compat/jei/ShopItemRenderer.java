package com.viscriptshop.compat.jei;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.viscriptshop.gui.data.MerchantItemDisplay;
import com.viscriptshop.gui.data.MerchantItemInfo;
import com.viscript_lib.util.CountTextUtil;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** 自定义图标只改变显示，JEI 的物品索引仍使用实际交易物品。 */
final class ShopItemRenderer implements IIngredientRenderer<ItemStack> {
    private final MerchantItemDisplay display;
    private final int count;
    private final int baseCount;
    private final boolean gift;

    ShopItemRenderer(MerchantItemInfo item, int count) {
        this(item, count, false);
    }

    ShopItemRenderer(MerchantItemInfo item, int count, boolean gift) {
        this.display = item.getDisplay();
        this.baseCount = item.getItem().getCount();
        this.count = count;
        this.gift = gift;
    }

    @Override
    public void render(GuiGraphics graphics, ItemStack ingredient) {
        ItemStack shown = ingredient;
        if (display.resolvedRenderMode() == MerchantItemDisplay.RenderMode.ITEM_RENDER) {
            shown = display.getRenderItem().copyWithCount(ingredient.getCount());
        }
        ResourceLocation resource = display.resolvedRenderMode() == MerchantItemDisplay.RenderMode.RESOURCE
                ? ResourceLocation.tryParse(display.getResourcePath()) : null;
        if (resource != null) SpriteTexture.of(resource).draw(graphics, 0, 0, 0, 0, 16, 16, 0);
        else graphics.renderItem(shown, 0, 0);
        var font = Minecraft.getInstance().font;
        if (resource == null) graphics.renderItemDecorations(font, shown.copyWithCount(1), 0, 0);
        drawAmount(graphics, CountTextUtil.formatCount(count), 0, 0, count != baseCount);
        if (gift) {
            graphics.pose().pushPose();
            graphics.pose().translate(-2, -2, 200);
            graphics.pose().scale(6f / 9f, 6f / 9f, 1);
            graphics.drawString(font, Component.translatable("viscript_shop.ui.promotion.gift_badge")
                    .withStyle(ChatFormatting.GOLD), 0, 0, 0xFFFFFFFF, true);
            graphics.pose().popPose();
        }
    }

    static void drawAmount(GuiGraphics graphics, String amount, int x, int y, boolean changed) {
        var font = Minecraft.getInstance().font;
        float scale = Math.min(5f / 9f, 28f / Math.max(1, font.width(amount)));
        graphics.pose().pushPose();
        graphics.pose().translate(x + 18, y + 16 - font.lineHeight * scale, 200);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, amount, -font.width(amount), 0, changed ? 0xFF80FF80 : 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    @Override
    @SuppressWarnings("removal")
    public List<Component> getTooltip(ItemStack ingredient, TooltipFlag flag) {
        var minecraft = Minecraft.getInstance();
        return getTooltip(ingredient, Item.TooltipContext.of(minecraft.level), minecraft.player, flag);
    }

    @Override
    public List<Component> getTooltip(ItemStack ingredient, Item.TooltipContext context, @Nullable Player player, TooltipFlag flag) {
        var tooltip = new ArrayList<>(ingredient.getTooltipLines(context, player, flag));
        if (display.resolvedRenderMode() == MerchantItemDisplay.RenderMode.RESOURCE && !display.getResourceName().isBlank()) {
            tooltip.addFirst(Component.literal(display.getResourceName()));
        }
        tooltip.add(Component.translatable("viscript_shop.jei.item_count", String.format(java.util.Locale.ROOT, "%,d", count)));
        if (count != baseCount) tooltip.add(Component.translatable("viscript_shop.ui.promotion.price_compare",
                String.format(java.util.Locale.ROOT, "%,d", baseCount), String.format(java.util.Locale.ROOT, "%,d", count)));
        return tooltip;
    }
}
