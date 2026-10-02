package com.viscriptshop.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.mrcrayfish.backpacked.BackpackHelper;
import com.viscript_lib.util.item.ItemOutputTargets;
import com.viscriptshop.ShopRegistries;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.components.ShopOutputTargetButton;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 验证商店输出按钮通过真实网络购买流程向已装备的 Backpacked 背包发货。
 *
 * <p>仅在安装 Backpacked 的开发环境注册，在测试存档中验证目标同步、重新打开界面
 * 和未装备背包时的扣款保护。
 */
@LDLRegisterClient(name = "vss_backpacked_output", group = ViscriptShop.MOD_ID,
        registry = UIScenario.REGISTRY, modID = "backpacked",
        environment = RegistrationEnvironment.DEV_ONLY)
public final class BackpackedOutputScenario implements UIScenario {
    private static final String SHOP = "__uitest_backpacked.vss";
    private static final String BUTTON = "#shop_output_target_button";

    @Override
    public void configure(ScenarioOptions options) {
        options.requiresWorld(true).defaultTimeoutMs(15_000).tags("shop", "backpacked");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        scenario.server("prepare currency shop and equipped backpack", context -> {
                    var player = context.player();
                    context.put("originalMoney", player.getData(ShopRegistries.MONEY));
                    context.put("originalBackpack", BackpackHelper.getBackpackStack(player, 0).copy());
                    player.setData(ShopRegistries.MONEY, new ShopRegistries.Money());
                    ViScriptShopServerUtil.setMoney(player, 100);
                    context.check("equip Backpacked backpack", BackpackHelper.setBackpackStack(player,
                            new ItemStack(BuiltInRegistries.ITEM.get(
                                    ResourceLocation.fromNamespaceAndPath("backpacked", "backpack"))), 0));
                    ViScriptShopServerUtil.setShopInfo(SHOP, createShop());
                })
                .waitForSync("initial player balance", context -> ViScriptShopServerUtil.getMoney(context.player()),
                        context -> context.requirePlayer().getData(ShopRegistries.MONEY).getMoney())
                .step("clear cached cart", context -> ShopHelper.clearCache())
                .server("open shop through server", context ->
                        ViScriptShopServerUtil.serverOpenShop(context.player(), SHOP))
                .awaitModularUI()
                .awaitElement(BUTTON)
                .check("initial target is player inventory", context -> ItemOutputTargets.PLAYER_INVENTORY.equals(
                        context.el(BUTTON).as(ShopOutputTargetButton.class).getTarget().name()))
                .click(BUTTON)
                .check("button selects Backpacked", context -> ItemOutputTargets.BACKPACKED.equals(
                        context.el(BUTTON).as(ShopOutputTargetButton.class).getTarget().name()))
                .waitUntilServer("server stores Backpacked selection", context -> ItemOutputTargets.BACKPACKED.equals(
                        context.player().getData(ShopRegistries.MONEY).getOutputTargetId()))
                .waitForSync("output preference reaches client",
                        context -> context.player().getData(ShopRegistries.MONEY).getOutputTargetId(),
                        context -> context.requirePlayer().getData(ShopRegistries.MONEY).getOutputTargetId())
                .hover(BUTTON)
                .screenshot("backpacked_output_selected")
                .click("#shop_merchant_add_0")
                .click("#shop_buy_button")
                .waitUntilServer("purchase reaches equipped backpack", context ->
                        ItemOutputTargets.resolve(ItemOutputTargets.BACKPACKED)
                                .getItemStackCount(context.player(), new ItemStack(Items.DIAMOND)) == 3)
                .checkServer("purchase deducted exactly two currency", context ->
                        ViScriptShopServerUtil.getMoney(context.player()) == 98)
                .checkServer("purchase did not enter player inventory", context ->
                        context.player().getInventory().countItem(Items.DIAMOND) == 0)
                .closeScreen()
                .server("reopen shop", context -> ViScriptShopServerUtil.serverOpenShop(context.player(), SHOP))
                .awaitModularUI()
                .awaitElement(BUTTON)
                .check("reopened button remembers Backpacked", context -> ItemOutputTargets.BACKPACKED.equals(
                        context.el(BUTTON).as(ShopOutputTargetButton.class).getTarget().name()))
                .server("unequip backpack", context ->
                        BackpackHelper.setBackpackStack(context.player(), ItemStack.EMPTY, 0))
                .click("#shop_merchant_add_0")
                .click("#shop_buy_button")
                .waitUntil("unavailable backpack message arrives", context -> context.exists("#shop_message_text")
                        && context.el("#shop_message_text").text().equals(Component.translatable(
                        "viscript_shop.message.output_target.unavailable", Component.translatable(
                                "viscript_lib.item_output_target.backpacked_unavailable")).getString()))
                .checkServer("unavailable target did not charge currency", context ->
                        ViScriptShopServerUtil.getMoney(context.player()) == 98)
                .checkServer("unavailable target did not send to another inventory", context ->
                        context.player().getInventory().countItem(Items.DIAMOND) == 0)
                .screenshot("backpacked_not_equipped")
                .teardown("clear client cart", context -> {
                    context.mc().setScreen(null);
                    ShopHelper.clearCache();
                })
                .teardownServer("restore player and remove shop fixture", context -> {
                    if (context.get("originalMoney") instanceof ShopRegistries.Money money) {
                        context.player().setData(ShopRegistries.MONEY, money);
                    }
                    if (context.get("originalBackpack") instanceof ItemStack stack) {
                        BackpackHelper.setBackpackStack(context.player(), stack, 0);
                    }
                    ViscriptShop.getShopSavedData().resetShopInfo(SHOP);
                });
    }

    /**
     * 创建每份花费两单位货币、获得三颗钻石的测试商店。
     *
     * @return 独立的商店数据
     */
    private static ShopInfo createShop() {
        MerchantInfo merchant = new MerchantInfo();
        merchant.setId("diamonds");
        merchant.setMoney(2);
        merchant.getItemResultInfo().setItem(new ItemStack(Items.DIAMOND, 3));
        CategoryInfo category = new CategoryInfo();
        category.setId("currency");
        category.setName("Currency");
        category.setShopType(CategoryInfo.ShopType.CURRENCY);
        category.getMerchants().add(merchant);
        ShopInfo shop = new ShopInfo();
        shop.setName("Backpacked Output Test");
        shop.getCategoryInfos().add(category);
        return shop;
    }
}
