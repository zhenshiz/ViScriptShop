package com.viscriptshop.network.s2c;

import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.util.ViScriptShopUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record OpenShopUIPayload(ShopInfo shopInfo, String title) implements CustomPacketPayload {
    public static final Type<OpenShopUIPayload> TYPE = new Type<>(ViscriptShop.id("open_shop_ui"));
    public static final StreamCodec<FriendlyByteBuf, OpenShopUIPayload> CODEC = StreamCodec.composite(
            ShopInfo.STREAM_CODEC,
            OpenShopUIPayload::shopInfo,
            ByteBufCodecs.STRING_UTF8,
            OpenShopUIPayload::title,
            OpenShopUIPayload::new
    );


    public static void execute(OpenShopUIPayload payload, IPayloadContext context) {
        ViScriptShopUtil.clientOpenShop(payload.shopInfo(), payload.title());
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
