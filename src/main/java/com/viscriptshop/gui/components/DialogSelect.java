package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptshop.network.c2s.C2SPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class DialogSelect extends Dialog {
    public Selector<String> selector;

    public DialogSelect() {
        super();
        this.setTitle("viscript_shop.ui.quick_open.title");
        RPCPacketDistributor.rpcToServer(C2SPayload.GET_SHOP_INFO_C2S);
    }

    public void reload(Map<String, String> map) {
        //无选项值的显示
        if (map.isEmpty()) {
            Label label = (Label) new Label().setText("viscript_shop.ui.no_shop").textStyle(textStyle -> {
                textStyle.textAlignVertical(Vertical.CENTER).adaptiveWidth(true);
            }).layout(layout -> {
                layout.widthPercent(100);
            });
            this.contentContainer.addChildren(label);
        } else {
            //正常的列表显示
            selector = new Selector<>();
            selector.setValue(map.keySet().stream().findFirst().orElse(""));
            selector.setCandidates(map.keySet().stream().toList());
            selector.setCandidateUIProvider(UIElementProvider.text(value -> {
                String name = map.get(value);
                return name.isEmpty() ? Component.translatable("viscript_shop.ui.title") : Component.translatable(name);
            }));
            selector.layout(layout -> layout.widthPercent(100));
            this.contentContainer.addChildren(selector);
        }

        // 底部区域
        Button confirmButton = new Button()
                .setOnClick(event -> RPCPacketDistributor.rpcToServer(C2SPayload.OPEN_SHOP_UI_C2S, selector.getValue(), "", ""))
                .setText("ldlib.gui.tips.confirm");
        confirmButton.setVisible(!map.isEmpty());
        this.addButton(confirmButton);
        this.addButton(new Button()
                .setOnClick(event -> {
                    if (Minecraft.getInstance().screen != null) {
                        Minecraft.getInstance().screen.onClose();
                    }
                })
                .setText("ldlib.gui.tips.cancel"));
    }
}
