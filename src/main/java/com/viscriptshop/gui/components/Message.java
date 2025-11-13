package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.viscriptshop.ViscriptShop;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import java.util.concurrent.atomic.AtomicInteger;

public class Message extends UIElement {
    public final UIElement parent;
    public final UIElement icon;
    public final Label label;

    private final static int DEFAULT_TIME = 60;
    public final static SpriteTexture ERROR_ICON = SpriteTexture.of(ViscriptShop.formattedMod("textures/error.png"));
    public final static SpriteTexture INFO_ICON = SpriteTexture.of(ViscriptShop.formattedMod("textures/info.png"));
    public final static SpriteTexture SUCCESS_ICON = SpriteTexture.of(ViscriptShop.formattedMod("textures/success.png"));
    public final static SpriteTexture WARN_ICON = SpriteTexture.of(ViscriptShop.formattedMod("textures/warn.png"));

    public Message(UIElement parent) {
        this.parent = parent;
        this.layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWidth(StyleSizeLength.AUTO);
            layout.setHeightPercent(8);
            layout.setPadding(YogaEdge.ALL, 5);
            layout.setPositionPercent(YogaEdge.TOP, 3);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setAlignItems(YogaAlign.CENTER);
        }).style(style -> style.backgroundTexture(Sprites.BORDER));
        this.icon = new UIElement().layout(layout -> {
            layout.setWidth(8);
            layout.setHeight(8);
        });
        this.label = (Label) new Label().textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.CENTER);
            textStyle.textAlignVertical(Vertical.CENTER);
            textStyle.adaptiveWidth(true);
        }).layout(layout -> {
            layout.setMargin(YogaEdge.LEFT, 3);
            layout.setMargin(YogaEdge.RIGHT, 3);
        });
        this.addChildren(
                icon,
                label
        );
    }

    public static void error(String content, UIElement parent) {
        Message message = new Message(parent);
        message.label.setText(content);
        message.setIcon(ERROR_ICON);
        message.show();
    }

    public static void info(String content, UIElement parent) {
        Message message = new Message(parent);
        message.label.setText(content);
        message.setIcon(INFO_ICON);
        message.show();
    }

    public static void success(String content, UIElement parent) {
        Message message = new Message(parent);
        message.label.setText(content);
        message.setIcon(SUCCESS_ICON);
        message.show();
    }

    public static void warn(String content, UIElement parent) {
        Message message = new Message(parent);
        message.label.setText(content);
        message.setIcon(WARN_ICON);
        message.show();
    }

    private void show() {
        parent.addChild(this);
        AtomicInteger time = new AtomicInteger(DEFAULT_TIME);
        this.addEventListener(UIEvents.TICK, event -> {
            time.set(time.get() - 1);
            if (time.get() == 0) parent.removeChild(this);
        });
    }

    private void setIcon(SpriteTexture icon) {
        this.icon.getStyle().backgroundTexture(icon);
    }
}

