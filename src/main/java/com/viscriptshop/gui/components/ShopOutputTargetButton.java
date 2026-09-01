package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.viscript_lib.register.IContainerHelper;
import com.viscriptshop.gui.components.theme.ShopTheme;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * 使用商店主题切换按钮外观显示当前物品输出目标。
 */
public final class ShopOutputTargetButton extends Button {
    private final UIElement icon = new UIElement().layout(layout -> {
        layout.widthPercent(100);
        layout.heightPercent(100);
    });

    @Getter
    private IContainerHelper target;

    /**
     * 创建使用指定商店主题贴图的输出目标切换按钮。
     *
     * @param theme 当前商店主题
     */
    public ShopOutputTargetButton(ShopTheme theme) {
        noText();
        layout(layout -> layout.paddingAll(0));
        buttonStyle(style -> style
                .baseTexture(theme.toggleBase())
                .hoverTexture(theme.toggleHover())
                .pressedTexture(theme.toggleHover()));
        addChild(icon);
    }

    /**
     * 更新按钮当前显示的输出目标、图标和悬浮提示。
     *
     * @param target 新的输出目标
     * @return 此按钮
     */
    public ShopOutputTargetButton setTarget(IContainerHelper target) {
        this.target = Objects.requireNonNull(target);
        icon.style(style -> style.backgroundTexture(
                new ItemStackTexture(target.getItemOutputIcon()).scale(0.65f)
        ));
        style(style -> style.tooltips(Component.translatable(target.getItemOutputTranslationKey())));
        return this;
    }
}
