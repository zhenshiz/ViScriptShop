package com.viscriptshop.gui.components.theme;

import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;

public class ShopScrollerView extends ScrollerView {

    public ShopScrollerView(ShopTheme theme) {
        applyTheme(this, theme);
    }

    public static void applyTheme(ScrollerView view, ShopTheme theme) {
        view.verticalScroller(scroller -> {
            scroller.layout(layout -> layout.width(theme.scrollWidth()));
            scroller.headButton(btn -> {
                btn.layout(layout -> {
                    layout.width(theme.scrollWidth());
                    layout.height(5);
                });
                btn.buttonStyle(style -> {
                    style.baseTexture(theme.scrollHead());
                    if (theme.isGrayCatWorkshop()) {
                        style.hoverTexture(theme.scrollHead());
                        style.pressedTexture(theme.scrollHeadPressed());
                    }
                });
            });
            scroller.tailButton(btn -> {
                btn.layout(layout -> {
                    layout.width(theme.scrollWidth());
                    layout.height(5);
                });
                btn.buttonStyle(style -> {
                    style.baseTexture(theme.scrollTail());
                    if (theme.isGrayCatWorkshop()) {
                        style.hoverTexture(theme.scrollTail());
                        style.pressedTexture(theme.scrollTailPressed());
                    }
                });
            });
            scroller.scrollContainer(container -> container.style(style -> style
                    .backgroundTexture(theme.scrollTrack())
            ));
            scroller.scrollBar(bar -> bar.buttonStyle(style -> {
                style.baseTexture(theme.scrollBarBase());
                style.hoverTexture(theme.scrollBarHover());
                style.pressedTexture(theme.scrollBarPressed());
            }));
        });
    }
}
