package com.viscriptshop.compat;

import com.viscriptshop.event.CommonEventsPostJS;
import com.viscriptshop.event.ViScriptShopEventsJS;
import com.viscriptshop.util.ViScriptShopClientUtil;
import com.viscriptshop.util.ViScriptShopServerUtil;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraftforge.common.MinecraftForge;

public class ViScriptShopJSPlugin extends KubeJSPlugin {

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(CommonEventsPostJS.class);
    }

    @Override
    public void registerEvents() {
        ViScriptShopEventsJS.GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        ScriptType type = event.getType();
        if (type.isClient()) {
            event.add("ViScriptShopUtil", ViScriptShopClientUtil.class);
        } else if (type.isServer()) {
            event.add("ViScriptShopUtil", ViScriptShopServerUtil.class);
        }
    }
}
