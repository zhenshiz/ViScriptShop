package com.viscriptshop.network.s2c;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUIScreen;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopUI;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ReloadShopUIPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ReloadShopUIPayload> TYPE = new CustomPacketPayload.Type<>(ViscriptShop.id("reload_shop_ui"));
    public static final StreamCodec<FriendlyByteBuf, ReloadShopUIPayload> CODEC = StreamCodec.ofMember(
            ReloadShopUIPayload::write,
            ReloadShopUIPayload::new
    );

    public ReloadShopUIPayload(FriendlyByteBuf friendlyByteBuf) {
        this();
    }

    private void write(FriendlyByteBuf buf) {
    }


    public static void execute(ReloadShopUIPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ModularUIScreen screen && screen.modularUI.ui.rootElement instanceof ShopUI shopUI) {
            shopUI.reloadMerchants();
        }
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
