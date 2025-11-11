package com.viscriptshop.network.s2c;

import com.viscriptshop.ViscriptShop;
import com.viscriptshop.util.ViScriptShopUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record OpenShopEditor() implements CustomPacketPayload {
    public static final Type<OpenShopEditor> TYPE = new Type<>(ViscriptShop.id("open_shop_editor"));
    public static final StreamCodec<FriendlyByteBuf, OpenShopEditor> CODEC = StreamCodec.ofMember(
            OpenShopEditor::write,
            OpenShopEditor::new
    );

    public OpenShopEditor(FriendlyByteBuf friendlyByteBuf) {
        this();
    }

    private void write(FriendlyByteBuf buf) {
    }


    public static void execute(OpenShopEditor payload, IPayloadContext context) {
        ViScriptShopUtil.clientOpenNpcEditor();
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
