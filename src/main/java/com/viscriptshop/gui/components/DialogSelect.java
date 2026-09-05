package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptshop.gui.components.theme.ShopButton;
import com.viscriptshop.gui.components.theme.ShopScrollerView;
import com.viscriptshop.gui.components.theme.ShopTheme;
import com.viscriptshop.network.c2s.C2SPayload;
import dev.vfyjxf.taffy.style.AlignContent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Map;

/** 快捷商店选择弹窗，与商店界面共享客户端主题。 */
public class DialogSelect extends Dialog {
    public Selector<String> selector;
    private final ShopTheme theme = ShopTheme.current();
    private final Button confirmButton;

    public DialogSelect() {
        super();
        setId("shop_selector");
        addClass(theme.styleClass());
        overlay.setId("shop_selector_panel");
        overlay.layout(layout -> {
            layout.width(224);
            layout.maxWidthPercent(90);
            layout.paddingAll(6);
            layout.gapAll(4);
        }).style(style -> style.backgroundTexture(theme.dialogPanel()));
        titleBar.setId("shop_selector_title");
        titleBar.layout(layout -> {
            layout.height(22);
            layout.justifyContent(AlignContent.CENTER);
            layout.paddingAll(4);
        }).style(style -> style.backgroundTexture(theme.titleHeader()));
        setTitle("viscript_shop.ui.quick_open.title");
        contentContainer.layout(layout -> {
            layout.paddingAll(4);
            layout.minHeight(28);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        buttonContainer.layout(layout -> {
            layout.paddingAll(4);
            layout.gapAll(6);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY));

        confirmButton = ShopButton.buying(theme).setText("ldlib.gui.tips.confirm")
                .setOnClick(event -> {
                    if (selector != null && selector.getValue() != null && !selector.getValue().isBlank()) {
                        RPCPacketDistributor.rpcToServer(C2SPayload.OPEN_SHOP_UI_C2S, selector.getValue(), "", "");
                    }
                });
        confirmButton.setId("shop_selector_confirm");
        confirmButton.setActive(false);
        Button cancelButton = ShopButton.other(theme).setText("ldlib.gui.tips.cancel")
                .setOnClick(event -> {
                    if (Minecraft.getInstance().screen != null) Minecraft.getInstance().screen.onClose();
                });
        cancelButton.setId("shop_selector_cancel");
        for (Button button : new Button[]{confirmButton, cancelButton}) {
            button.layout(layout -> {
                layout.width(46);
                layout.height(18);
                layout.flexShrink(0);
            });
            addButton(button);
        }
        contentContainer.addChild(statusLabel("viscript_shop.ui.shop_list_loading"));
        RPCPacketDistributor.rpcToServer(C2SPayload.GET_SHOP_INFO_C2S);
    }

    public void reload(Map<String, String> map) {
        String selected = selector == null ? null : selector.getValue();
        if (selector != null) {
            selector.hide();
            removeExternalElement(selector.dialog);
        }
        selector = null;
        contentContainer.clearAllChildren();
        confirmButton.setActive(!map.isEmpty());
        if (map.isEmpty()) {
            contentContainer.addChild(statusLabel("viscript_shop.ui.no_shop"));
            return;
        }

        selector = new Selector<>();
        selector.setId("shop_selector_options");
        selector.dialog.setId("shop_selector_dropdown");
        selector.dialog.getStyle().backgroundTexture(theme.selectorPopup());
        selector.getStyle().backgroundTexture(theme.searchField());
        selector.selectorStyle(style -> style.focusOverlay(IGuiTexture.EMPTY).maxItemCount(6).scrollerViewHeight(112));
        ShopScrollerView.applyTheme(selector.scrollerView, theme);
        selector.buttonIcon.getLayout().width(12);
        selector.buttonIcon.getLayout().height(12);
        selector.layout(layout -> {
            layout.widthPercent(100);
            layout.height(20);
        });
        selector.setCandidateUIProvider(value -> candidate(value == null ? null : map.get(value), value));
        var paths = map.keySet().stream().sorted().toList();
        selector.setCandidates(paths);
        selector.setValue(selected != null && map.containsKey(selected) ? selected : paths.getFirst(), false);
        contentContainer.addChild(selector);
        addExternalElement(selector.dialog);
    }

    private UIElement candidate(String name, String path) {
        Component title = Component.translatable(name == null || name.isBlank() ? "viscript_shop.ui.title" : name);
        Label label = (Label) new Label().setText(title).textStyle(style -> style
                        .textAlignVertical(Vertical.CENTER).adaptiveWidth(false).textWrap(TextWrap.HIDE))
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.height(12);
                });
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(18);
            layout.paddingHorizontal(3);
            layout.justifyContent(AlignContent.CENTER);
        }).addChild(label);
        row.getStyle().tooltips(title, Component.literal(path == null ? "" : path));
        return row;
    }

    private Label statusLabel(String key) {
        Label label = (Label) new Label().setText(key).textStyle(style -> style
                .textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).adaptiveWidth(false));
        label.setId("shop_selector_status");
        label.layout(layout -> {
            layout.widthPercent(100);
            layout.height(18);
        });
        return label;
    }
}
