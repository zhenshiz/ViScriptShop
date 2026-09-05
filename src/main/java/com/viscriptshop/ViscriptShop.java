package com.viscriptshop;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.mojang.logging.LogUtils;
import com.viscript_lib.gui.editor.ViScriptEditorWindow;
import com.viscriptshop.compat.ModComPat;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.data.*;
import com.viscriptshop.promotion.PromotionRegistries;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

@Mod(ViscriptShop.MOD_ID)
public class ViscriptShop {
    public static final String MOD_ID = "viscript_shop";
    public static final Logger LOGGER = LogUtils.getLogger();
    @Setter
    @Getter
    private static ShopSavedData shopSavedData;

    public ViscriptShop(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        PromotionRegistries.init();
        ShopRegistries.ATTACHMENT_TYPES.register(modEventBus);
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
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.CONFIG_SPEC, String.format("%s_config.toml", MOD_ID));
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG_SPEC, String.format("%s_client.toml", MOD_ID));
        if (dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String formattedMod(String path) {
        return ("%s:" + path).formatted(MOD_ID);
    }

    public static boolean isPresentResource(ResourceLocation resourceLocation) {
        return Minecraft.getInstance().getResourceManager().getResource(resourceLocation).isPresent();
    }

    //精妙背包
    public static boolean isSophisticatedBackpacksLoaded() {
        return isModLoaded("sophisticatedbackpacks");
    }

    //超越维度
    public static boolean isBeyondDimensionsLoaded() {
        return isModLoaded("beyonddimensions");
    }

    // JEI 兼容
    public static boolean isJEILoaded() {
        return isModLoaded("jei");
    }

    // Just Enough Characters 兼容
    public static boolean isJECharactersLoaded() {
        return isModLoaded("jecharacters");
    }

    // FTB Library 兼容
    public static boolean isFtbLibraryLoaded() {
        return isModLoaded("ftblibrary");
    }

    /**
     * 检查当前运行环境是否安装了 FTB Quests。
     *
     * @return 安装 FTB Quests 时返回 {@code true}
     */
    public static boolean isFtbQuestsLoaded() {
        return isModLoaded("ftbquests");
    }

    // Lightman's Currency 兼容
    public static boolean isLightmansCurrencyLoaded() {
        return isModLoaded("lightmanscurrency");
    }

    //汇流来世
    public static boolean isConfluenceLoaded() {
        return isModLoaded("confluence");
    }

    // Magic Coins 兼容
    public static boolean isMagicCoinsLoaded() {
        return isModLoaded("magic_coins");
    }

    private static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
