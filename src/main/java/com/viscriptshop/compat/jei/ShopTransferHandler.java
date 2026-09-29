package com.viscriptshop.compat.jei;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptshop.gui.data.ShopDisplayEntry;
import com.viscriptshop.network.c2s.ShopJeiC2SPayload;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.library.transfer.RecipeTransferErrorTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ShopTransferHandler implements IRecipeTransferHandler<AbstractContainerMenu, ShopDisplayEntry> {
    final Class containerClass;

    public ShopTransferHandler(Class<? extends AbstractContainerMenu> containerClass) {
        this.containerClass = containerClass;
    }

    @Override
    public @NotNull Class<AbstractContainerMenu> getContainerClass() {return containerClass;}

    @Override
    public @NotNull Optional<MenuType<AbstractContainerMenu>> getMenuType() {return Optional.empty();}

    @Override
    public @NotNull RecipeType<ShopDisplayEntry> getRecipeType() {return ShopRecipeCategory.TYPE;}

    @Override
    public @Nullable IRecipeTransferError transferRecipe(AbstractContainerMenu menu, ShopDisplayEntry entry, IRecipeSlotsView iRecipeSlotsView, Player player, boolean b, boolean b1) {
        if (b1 && ShopJeiClient.isCurrent(entry)) {
            if (entry.isLocked()) return new RecipeTransferErrorTooltip(Component.translatable("viscript_shop.jei.stage.locked"));
            RPCPacketDistributor.rpcToServer(ShopJeiC2SPayload.OPEN,
                    entry.getShopLocation(), entry.getCategoryId(), entry.getMerchantId());
        }
        return null;
    }
}
