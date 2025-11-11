package com.viscriptshop;

import com.viscriptshop.util.ViScriptShopUtil;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public class ViScriptShopJSPlugin implements KubeJSPlugin {

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("ViScriptShopUtil", ViScriptShopUtil.class);
    }
}
