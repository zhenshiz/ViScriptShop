package com.viscriptshop.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.compat.jei.ShopJeiClient;
import com.viscriptshop.compat.jei.ShopRecipeCategory;
import com.viscriptshop.compat.jei.ShopTransferHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
@mezz.jei.api.JeiPlugin
public class JeiHelper implements IModPlugin {
    private static IJeiRuntime jeiRuntime;

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ViscriptShop.id("jei_helper");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ShopRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        List<Class<? extends AbstractContainerMenu>> supportedContainers =
                List.of(CraftingMenu.class, InventoryMenu.class, ChestMenu.class, CreativeModeInventoryScreen.ItemPickerMenu.class);
        for (var container : supportedContainers) {
            registration.addRecipeTransferHandler(new ShopTransferHandler(container), ShopRecipeCategory.TYPE);
        }
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        JeiHelper.jeiRuntime = jeiRuntime;
        ShopJeiClient.runtimeAvailable(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiHelper.jeiRuntime = null;
        ShopJeiClient.runtimeUnavailable();
    }

    public static Optional<IJeiRuntime> getJeiRuntime() {
        return Optional.ofNullable(jeiRuntime);
    }

    public static void showRecipes(ItemStack itemStack) {
        JeiHelper.getJeiRuntime().ifPresent(jeiRuntime -> {
            jeiRuntime.getIngredientManager().getIngredientTypeChecked(itemStack)
                    .ifPresent(type -> {
                        jeiRuntime.getRecipesGui().show(
                                jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, type, itemStack)
                        );
                    });
        });
    }

    public static void showUses(ItemStack itemStack) {
        JeiHelper.getJeiRuntime().ifPresent(jeiRuntime -> {
            jeiRuntime.getIngredientManager().getIngredientTypeChecked(itemStack)
                    .ifPresent(type -> {
                        jeiRuntime.getRecipesGui().show(
                                jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.INPUT, type, itemStack)
                        );
                    });
        });
    }

    public static boolean handleRecipeLookupKey(ItemStack itemStack, int keyCode, int scanCode) {
        if (itemStack.isEmpty()) {
            return false;
        }
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        return getJeiRuntime().map(runtime -> {
            var keyMappings = runtime.getKeyMappings();
            if (keyMappings.getShowRecipe().isActiveAndMatches(key)) {
                showRecipes(itemStack);
                return true;
            }
            if (keyMappings.getShowUses().isActiveAndMatches(key)) {
                showUses(itemStack);
                return true;
            }
            return false;
        }).orElse(false);
    }
}
