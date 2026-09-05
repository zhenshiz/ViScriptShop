package com.viscriptshop.promotion;

import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 提供一次商品促销条件判定所需的只读上下文。
 *
 * <p>内置条件只读取玩家状态，但扩展条件仍可读取所属商店、分类、商品和本次购买数量。
 * 这些对象只用于判定，不允许条件修改报价或商店数据。
 *
 * @param player 当前玩家；无玩家的编辑器预览允许为 {@code null}
 * @param shopLocation 当前商店文件位置
 * @param shopInfo 当前商店数据
 * @param categoryInfo 当前商品所属分类
 * @param merchantInfo 当前商品
 * @param purchaseQuantity 当前商品的购买倍数
 */
public record PromotionContext(
        @Nullable Player player,
        String shopLocation,
        ShopInfo shopInfo,
        CategoryInfo categoryInfo,
        MerchantInfo merchantInfo,
        int purchaseQuantity
) {
    public PromotionContext {
        shopLocation = shopLocation == null ? "" : shopLocation;
        purchaseQuantity = Math.max(0, purchaseQuantity);
    }
}
