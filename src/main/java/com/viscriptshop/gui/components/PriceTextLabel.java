package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

/** 金额保留原来的布局占位，但只有实际单行文字区域参与鼠标命中。 */
public final class PriceTextLabel extends Label {
    @Override
    public boolean isIntersectWithPoint(double localX, double localY) {
        if (getText().getString().isEmpty() || !super.isIntersectWithPoint(localX, localY)) return false;
        float height = getTextStyle().fontSize();
        float width = getFont().width(getText()) * height / getFont().lineHeight;
        float x = getContentX();
        float y = getContentY();
        // 与 TextElement 的单行文字绘制对齐规则保持一致。
        x += switch (getTextStyle().textAlignHorizontal()) {
            case LEFT -> 0;
            case CENTER -> width > getContentWidth() ? 0 : (getContentWidth() - width) / 2;
            case RIGHT -> getContentWidth() - width;
        };
        y += switch (getTextStyle().textAlignVertical()) {
            case TOP -> 0;
            case CENTER -> (getContentHeight() - height) / 2;
            case BOTTOM -> getContentHeight() - height;
        };
        return isMouseOverRect(x, y, width, height, localX, localY);
    }
}
