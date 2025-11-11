package com.viscriptshop.network;

import com.viscriptshop.ViscriptShop;
import com.viscriptshop.network.s2c.OpenShopEditor;
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
        registrar.commonToClient(OpenShopEditor.TYPE, OpenShopEditor.CODEC, OpenShopEditor::execute);

        //c2s
    }
}
