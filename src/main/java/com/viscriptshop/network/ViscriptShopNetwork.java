package com.viscriptshop.network;

import com.viscriptshop.ViscriptShop;
import com.viscriptshop.network.c2s.BuyMerchantPayload;
import com.viscriptshop.network.s2c.OpenShopEditorPayload;
import com.viscriptshop.network.s2c.OpenShopUIPayload;
import com.viscriptshop.network.s2c.ReloadShopUIPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ViscriptShop.MOD_ID)
public class ViscriptShopNetwork {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ViscriptShop.MOD_ID);
        //s2c
        registrar.commonToClient(OpenShopEditorPayload.TYPE, OpenShopEditorPayload.CODEC, OpenShopEditorPayload::execute);
        registrar.commonToClient(OpenShopUIPayload.TYPE, OpenShopUIPayload.CODEC, OpenShopUIPayload::execute);
        registrar.commonToClient(ReloadShopUIPayload.TYPE, ReloadShopUIPayload.CODEC, ReloadShopUIPayload::execute);

        //c2s
        registrar.commonToServer(BuyMerchantPayload.TYPE, BuyMerchantPayload.CODEC, BuyMerchantPayload::execute);
    }
}
