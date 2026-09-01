package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.components.theme.ShopTheme;
import dev.vfyjxf.taffy.style.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.concurrent.atomic.AtomicInteger;

public class Message extends UIElement {
    public final UIElement parent;
    public final UIElement icon;
    public final Label label;

    private final static int DEFAULT_TIME = 60;
    public final static SpriteTexture ERROR_ICON = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/error.png"));
    public final static SpriteTexture INFO_ICON = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/info.png"));
    public final static SpriteTexture SUCCESS_ICON = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/success.png"));
    public final static SpriteTexture WARN_ICON = SpriteTexture.of(ViscriptShop.formattedMod("textures/icons/warn.png"));

    public Message(UIElement parent) {
        this.parent = parent;
        ShopTheme theme = ShopTheme.current();
        this.setId("shop_message");
        this.addClasses("shop-message", theme.styleClass());
        this.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.flexDirection(FlexDirection.ROW);
            layout.setWidth(TaffyDimension.AUTO);
            layout.heightPercent(8);
            layout.paddingAll(5);
            layout.top(10);
            layout.justifyContent(AlignContent.CENTER);
            layout.alignItems(AlignItems.CENTER);
        }).style(style -> style.backgroundTexture(theme.messageBackground()));
        this.icon = new UIElement().layout(layout -> {
            layout.width(8);
            layout.height(8);
        });
        this.label = (Label) new Label().textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.CENTER);
            textStyle.textAlignVertical(Vertical.CENTER);
            textStyle.adaptiveWidth(true);
        }).layout(layout -> {
            layout.marginLeft(3);
            layout.marginRight(3);
        });
        this.label.setId("shop_message_text");
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

    public static void send(Type type, String content, UIElement parent) {
        switch (type) {
            case ERROR -> error(content, parent);
            case INFO -> info(content, parent);
            case SUCCESS -> success(content, parent);
            case WARN -> warn(content, parent);
        }
    }

    private void show() {
        parent.addChild(this);
        AtomicInteger time = new AtomicInteger(DEFAULT_TIME);
        this.addEventListener(UIEvents.TICK, event -> {
            time.set(time.get() - 1);
            if (time.get() == 20) {
                this.animation()
                        .duration(1)
                        .ease(Eases.QUAD_IN_OUT)
                        .style(PropertyRegistry.OPACITY, 0f)
                        .start();
            }
            if (time.get() == 0) parent.removeChild(this);
        });
    }

    private void setIcon(SpriteTexture icon) {
        this.icon.getStyle().backgroundTexture(icon);
    }

    public enum Type {
        ERROR,
        INFO,
        SUCCESS,
        WARN
    }
}
