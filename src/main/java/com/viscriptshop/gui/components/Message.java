package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.viscriptshop.ViscriptShop;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Message extends UIElement {
    public final UIElement icon;
    public final Label label;

    public final static SpriteTexture WARN_ICON = SpriteTexture.of(ViscriptShop.formattedMod("%s:textures/warn.png"));

    public Message() {
        this.layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWidth(StyleSizeLength.FIT_CONTENT);
            layout.setHeightPercent(8);
            layout.setPadding(YogaEdge.ALL, 5);
            layout.setPositionPercent(YogaEdge.TOP, 3);
            layout.setJustifyContent(YogaJustify.CENTER);
            layout.setAlignItems(YogaAlign.CENTER);
        }).style(style -> style.backgroundTexture(Sprites.BORDER));

        this.icon = new UIElement().style(style -> {
            style.backgroundTexture(WARN_ICON);
        }).layout(layout -> {
            layout.setWidth(8);
            layout.setHeight(8);
            layout.setMargin(YogaEdge.RIGHT, 3);
        });

        this.label = (Label) new Label().textStyle(textStyle -> {
            textStyle.textAlignHorizontal(Horizontal.CENTER);
        }).layout(layout -> {
            layout.setWidth(StyleSizeLength.FIT_CONTENT);
        });

        this.addChildren(
                icon,
                label
        );
    }

    public static void warn(String content, UIElement parent) {
        Message message = new Message();
        message.label.setText(content);
        parent.addChild(message);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            parent.removeChild(message);
        }, 3, TimeUnit.SECONDS);
    }
}

