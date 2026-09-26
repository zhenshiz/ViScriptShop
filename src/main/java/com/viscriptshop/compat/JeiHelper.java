package com.viscriptshop.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.viscriptshop.ViscriptShop;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

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
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        JeiHelper.jeiRuntime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiHelper.jeiRuntime = null;
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
