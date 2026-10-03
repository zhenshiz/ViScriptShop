package com.viscriptshop.gui.layout;

import com.viscriptshop.Config;
import net.minecraft.client.Minecraft;

public final class ShopUiScale {
    private ShopUiScale() {
    }

    public static float basePixelScale() {
        var window = Minecraft.getInstance().getWindow();
        return Math.min(window.getWidth() / 640f, window.getHeight() / 360f) * 1.25f;
    }

    public static float contentScale() {
        var window = Minecraft.getInstance().getWindow();
        float requested = Config.shopContentScale.get().floatValue();
        if (window.getWidth() <= 0 || window.getHeight() <= 0) return requested;
        float base = basePixelScale();
        boolean gray = Config.shopUiTheme.get() == Config.ShopUiTheme.GRAY_CAT_WORKSHOP;
        float referenceWidth = gray
                ? Math.min(window.getWidth() * 0.9f, window.getHeight() * 0.91f * GrayCatShopUiLayout.SHELL_ASPECT)
                : window.getWidth() * 0.9f;
        float frame = (gray ? 16 : 6) * base;
        float sideColumns = (referenceWidth - frame) * 47 / 102;
        // 大倍率优先保证三栏完整留在窗口内；宽屏可使用更大的实际倍率。
        float fit = (window.getWidth() * 0.98f - sideColumns - frame)
                / (ShopColumnsLayout.LIST_COLUMN_WIDTH * base);
        return Math.min(requested, fit);
    }
}
