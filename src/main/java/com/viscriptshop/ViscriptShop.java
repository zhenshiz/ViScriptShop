package com.viscriptshop;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.mojang.logging.LogUtils;
import com.viscript_lib.gui.editor.ViScriptEditorWindow;
import com.viscriptshop.compat.ModComPat;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.data.ShopSavedData;
import com.viscriptshop.promotion.PromotionRegistries;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(ViscriptShop.MOD_ID)
public class ViscriptShop {
    public static final String MOD_ID = "viscript_shop";
    public static final Logger LOGGER = LogUtils.getLogger();
    @Setter @Getter
    private static ShopSavedData shopSavedData;

    public ViscriptShop() {
        PromotionRegistries.init();
        ShopRegistries.ATTACHMENT_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        var dist = FMLEnvironment.dist;
        ModComPat.init(dist);
        PlayerUIMenuType.register(ShopEditor.SHOP_ID, ignored -> player -> {
            if (player.level().isClientSide) {
                ModularUI modularUI = new ModularUI(UI.of(ViScriptEditorWindow.open(ShopEditor.SHOP_ID, ShopEditor::new)
                        .setMinimizedBoundsPercent(0, 0, 79, 100)))
                        .shouldCloseOnKeyInventory(false);
                if (!Platform.isDevEnv()) {
                    modularUI.shouldCloseOnEsc(false);
                }
                return modularUI;
            }
            return new ModularUI(UI.empty());
        });
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.COMMON, Config.CONFIG_SPEC, String.format("%s_config.toml", MOD_ID));
        if (dist.isClient()) {
            context.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG_SPEC, String.format("%s_client.toml", MOD_ID));
        }
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static String formattedMod(String path) {
        return ("%s:" + path).formatted(MOD_ID);
    }

    public static boolean isPresentResource(ResourceLocation resourceLocation) {
        return Minecraft.getInstance().getResourceManager().getResource(resourceLocation).isPresent();
    }

    //jei
    public static boolean isJEILoaded() {return isModLoaded("jei");}

    //FtbLibrary
    public static boolean isFtbLibraryLoaded() {return isModLoaded("ftblibrary");}

    public static boolean isFtbQuestsLoaded() {return isModLoaded("ftbquests");}

    private static boolean isModLoaded(String modId) {return ModList.get().isLoaded(modId);}
}
