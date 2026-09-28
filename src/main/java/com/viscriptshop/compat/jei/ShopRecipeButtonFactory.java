package com.viscriptshop.compat.jei;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptshop.gui.data.ShopDisplayEntry;
import com.viscriptshop.network.c2s.ShopJeiC2SPayload;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.buttons.IButtonState;
import mezz.jei.api.gui.buttons.IIconButtonController;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.recipe.advanced.IRecipeButtonControllerFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** 在 JEI 收藏按钮旁添加商店跳转按钮，不直接执行购买。 */
public final class ShopRecipeButtonFactory implements IRecipeButtonControllerFactory {
    private final IDrawable icon;

    public ShopRecipeButtonFactory(IDrawable icon) {
        this.icon = icon;
    }

    @Override
    public <T> @Nullable IIconButtonController createButtonController(IRecipeLayoutDrawable<T> layout) {
        if (!(layout.getRecipe() instanceof ShopDisplayEntry entry)
                || !layout.getRecipeCategory().getRecipeType().equals(ShopRecipeCategory.TYPE)) return null;
        return new IIconButtonController() {
            @Override
            public void initState(IButtonState state) {
                state.setIcon(icon);
                updateState(state);
            }

            @Override
            public void updateState(IButtonState state) {
                state.setActive(Minecraft.getInstance().player != null && ShopJeiClient.isCurrent(entry) && !entry.isLocked());
            }

            @Override
            public boolean onPress(IJeiUserInput input) {
                if (!input.isSimulate() && ShopJeiClient.isCurrent(entry) && !entry.isLocked()) {
                    RPCPacketDistributor.rpcToServer(ShopJeiC2SPayload.OPEN,
                            entry.getShopLocation(), entry.getCategoryId(), entry.getMerchantId());
                }
                return true;
            }

            @Override
            public void getTooltips(ITooltipBuilder tooltip) {
                if (entry.isLocked()) tooltip.addAll(entry.getStageTooltips());
                else tooltip.add(Component.translatable("viscript_shop.jei.open"));
            }
        };
    }
}
