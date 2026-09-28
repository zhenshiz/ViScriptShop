package com.viscriptshop.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.compat.JeiHelper;
import com.viscriptshop.compat.jei.ShopRecipeCategory;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.data.*;
import com.viscriptshop.promotion.PromotionRule;
import com.viscriptshop.promotion.condition.PromotionConditionEntry;
import com.viscriptshop.promotion.condition.StageFlagCondition;
import com.viscriptshop.util.ShopHelper;
import com.viscriptshop.util.ViScriptShopClientUtil;
import com.viscriptshop.util.ViScriptShopServerUtil;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.gui.recipes.IRecipeLayoutWithButtons;
import mezz.jei.gui.recipes.RecipesGui;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 验证真实 JEI 打开刷新、阶段折扣、锁定提示及原生按钮精确跳转。 */
@LDLRegisterClient(name = "shop_jei_stage", group = ViscriptShop.MOD_ID, registry = UIScenario.REGISTRY,
        modID = "jei", environment = RegistrationEnvironment.DEV_ONLY)
public final class ShopJeiStageScenario implements UIScenario {
    private static final String LOCATION = "__uitest_jei_stage";
    private static final String FLAG = "__uitest_jei_unlock";
    private static final String PACK_KEY = "example.pack.custom_unlock_message";

    @Override
    public void configure(ScenarioOptions o) {
        o.guiScale(2).defaultTimeoutMs(20000).scenarioTimeoutMs(180000);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.server("write isolated server shop fixtures", sc -> {
            ViScriptShopServerUtil.removeStageFlag(sc.player(), FLAG);
            for (boolean hidden : List.of(false, true)) {
                var file = file(hidden);
                if (Files.exists(file)) throw new IllegalStateException("Fixture already exists: " + file);
                try {
                    Files.createDirectories(file.getParent());
                    NbtIo.writeCompressed(Shop.serializeRuntimeNBT(sc.server().registryAccess(), fixture(hidden)), file);
                    sc.put(hidden ? "hiddenCreated" : "shownCreated", true);
                } catch (java.io.IOException e) { throw new RuntimeException(e); }
            }
            ViScriptShopServerUtil.serverOpenShop(sc.player(), LOCATION);
        }).awaitModularUI().awaitElement("#shop_merchant_lock_1")
          .step("capture actual shop hover text", c -> {
              c.put("merchantHints", hints(c, "#shop_merchant_lock_1"));
              c.put("categoryHints", hints(c, "#shop_category_2"));
              c.check("custom literal text shown in shop", hints(c, "#shop_merchant_lock_1").contains("Complete the trial to unlock"));
              c.check("custom translation key resolved in shop", hints(c, "#shop_merchant_lock_1")
                      .contains(Component.translatable("viscript_shop.jei.title").getString()));
          }).server("open barter category to capture generated conditions", sc ->
                  ViScriptShopServerUtil.serverOpenShop(sc.player(), LOCATION, "barter", null))
          .awaitElement("#shop_merchant_lock_0")
          .step("capture generated shop lock text", c -> c.put("defaultHints", hints(c, "#shop_merchant_lock_0")))
          .closeScreen().waitUntil("JEI runtime ready", c -> JeiHelper.getJeiRuntime().isPresent())
          .step("open JEI to request server catalogue", c -> runtime().getRecipesGui().showTypes(List.of(RecipeTypes.CRAFTING)))
          .waitUntil("locked offers arrive", c -> entries().size() == 4)
          .step("check locked state and shared messages", c -> {
              var target = entry("target");
              c.check("merchant locked before stage", target.isLocked());
              c.check("money has no premature discount", target.getMoney() == 100, 100, target.getMoney());
              c.check("barter has no premature discount", entry("barter").getItemACount() == 8);
              c.check("category restriction locks offer", entry("category").isLocked());
              c.check("HIDDEN shop excluded while locked", entries().stream().noneMatch(e -> e.getShopLocation().endsWith("_hidden")));
              c.check("JEI merchant text equals shop text after scope/status headers", strings(target.getStageTooltips().subList(2, target.getStageTooltips().size())).equals(c.get("merchantHints")));
              var category = entry("category");
              c.check("JEI category text equals shop text after scope/status headers", strings(category.getStageTooltips().subList(2, category.getStageTooltips().size())).equals(c.get("categoryHints")));
              var barter = entry("barter");
              c.check("JEI generated conditions equal shop conditions", strings(barter.getStageTooltips().subList(2, barter.getStageTooltips().size())).equals(c.get("defaultHints")));
              c.check("resource-pack key remains translatable across server sync", target.getStageTooltips().stream()
                      .anyMatch(t -> t.getContents() instanceof TranslatableContents tc && tc.getKey().equals(PACK_KEY)));
              c.put("craftingCount", runtime().getRecipeManager().createRecipeLookup(RecipeTypes.CRAFTING).get().count());
              showTarget("target");
          }).waitUntil("locked layout visible", c -> layout(c, "target") != null)
          .step("hover JEI lock", c -> {
              var rect = layout(c, "target").getRecipeLayout().getRect();
              c.input().moveTo(rect.getX() + 198, rect.getY() + 32);
          }).screenshot("locked_jei")
          .step("press disabled native plus", c -> pressPlus(c, "target"))
          .step("release disabled native plus", c -> releasePlus(c))
          .ticks(5).check("locked plus cannot open shop", c -> c.screen() instanceof RecipesGui)
          .server("unlock player's stage", sc -> sc.check("flag newly granted", ViScriptShopServerUtil.addStageFlag(sc.player(), FLAG)))
          .waitUntil("stage sync reaches client", c -> ViScriptShopClientUtil.getStageFlags(c.player()).contains(FLAG))
          .step("record refresh boundary", c -> {
              c.check("open page waits until next open", entry("target").isLocked());
          }).closeScreen()
          .step("reopen JEI", c -> runtime().getRecipesGui().showTypes(List.of(ShopRecipeCategory.TYPE)))
          .waitUntil("new player prices and unlock arrive", c -> entries().size() == 5 && !entry("target").isLocked() && entry("target").getMoney() == 75)
          .waitUntil("visible JEI layout rebuilt", c -> {
              var hit = layout(c, "target");
              return hit != null && !((ShopDisplayEntry) hit.getRecipeLayout().getRecipe()).isLocked();
          }).step("verify refreshed player display", c -> {
              c.check("currency discount is 100 to 75", entry("target").getMoney() == 75);
              c.check("barter discount is 8 to 4", entry("barter").getItemACount() == 4);
              c.check("all stage restricted offers now unlocked", entries().stream().noneMatch(ShopDisplayEntry::isLocked));
              c.check("unlocked offers no longer display lock tooltip", entries().stream().allMatch(e -> e.getStageTooltips().isEmpty()));
              c.check("HIDDEN offer appears after unlock", entries().stream().anyMatch(e -> e.getShopLocation().endsWith("_hidden")));
              c.check("no duplicate offers after refresh", entries().stream().map(e -> e.getShopLocation() + e.getMerchantId()).distinct().count() == 5);
              c.check("crafting recipes unchanged", c.<Long>get("craftingCount") == runtime().getRecipeManager().createRecipeLookup(RecipeTypes.CRAFTING).get().count());
          }).screenshot("unlocked_jei")
          .step("focus exact target for jump", c -> showTarget("target"))
          .waitUntil("target shown", c -> layout(c, "target") != null)
          .step("press native plus", c -> pressPlus(c, "target"))
          .step("release native plus", c -> releasePlus(c))
          .awaitModularUI().awaitElement("#shop_merchant_list_1")
          .step("verify exact destination and actual shop price", c -> {
              var shop = c.all("*").stream().map(ElementRef::element).filter(ShopUI.class::isInstance).map(ShopUI.class::cast).findFirst().orElseThrow();
              c.check("correct category", shop.getSelectedCategory().getId().equals("money"));
              c.check("exact ordinal search instead of duplicate item search", !shop.isSearchMode() && shop.getSearchId().equals("2"));
              c.check("only target offer visible", shop.merchantsView.viewContainer.getChildren().size() == 1);
              c.check("lock removed in shop", !c.exists("#shop_merchant_lock_1"));
              c.check("shop price also shows 75", c.el("#shop_merchant_price_1_actual").text().equals("75"), "75", c.el("#shop_merchant_price_1_actual").text());
          }).screenshot("jump_to_exact_discounted_offer")
          .closeScreen().step("open unlocked category offer", c -> showTarget("category"))
          .waitUntil("category target shown", c -> layout(c, "category") != null)
          .step("press category native plus", c -> pressPlus(c, "category"))
          .step("release category native plus", c -> releasePlus(c))
          .awaitModularUI().awaitElement("#shop_merchant_list_0")
          .check("unlocked category is selectable", c -> c.all("*").stream().map(ElementRef::element).filter(ShopUI.class::isInstance)
                  .map(ShopUI.class::cast).anyMatch(ui -> ui.getSelectedCategory().getId().equals("restricted")))
          .closeScreen().server("revoke stage", sc -> ViScriptShopServerUtil.removeStageFlag(sc.player(), FLAG))
          .waitUntil("revocation synced", c -> !ViScriptShopClientUtil.getStageFlags(c.player()).contains(FLAG))
          .step("reopen after revocation", c -> runtime().getRecipesGui().showTypes(List.of(ShopRecipeCategory.TYPE)))
          .waitUntil("locked catalogue restored", c -> entries().size() == 4 && entry("target").isLocked() && entry("target").getMoney() == 100)
          .check("barter full price restored", c -> entry("barter").getItemACount() == 8)
          .check("custom lock text restored", c -> strings(entry("target").getStageTooltips()).containsAll(c.<List<String>>get("merchantHints")))
          .teardown("close screen", c -> c.mc().setScreen(null))
          .teardownServer("remove owned fixture files and stage", sc -> {
              ViScriptShopServerUtil.removeStageFlag(sc.player(), FLAG);
              for (boolean hidden : List.of(false, true)) {
                  if (Boolean.TRUE.equals(sc.get(hidden ? "hiddenCreated" : "shownCreated"))) {
                      try { Files.deleteIfExists(file(hidden)); } catch (java.io.IOException e) { throw new RuntimeException(e); }
                      ViScriptShopServerUtil.reloadOpenShop(LOCATION + (hidden ? "_hidden" : ""));
                  }
              }
          });
    }

    private static Path file(boolean hidden) {
        return LDLib2.getAssetsDir().toPath().resolve(ShopHelper.SHOP_PATH).resolve(LOCATION + (hidden ? "_hidden" : "") + Shop.SUFFIX);
    }

    private static ShopInfo fixture(boolean hidden) {
        var shop = new ShopInfo();
        shop.setName(hidden ? "Hidden Stage Shop" : "JEI Stage Test");
        if (hidden) shop.setLockedMerchantVisibility(ShopInfo.LockedMerchantVisibility.HIDDEN);
        var money = new CategoryInfo();
        money.setId("money"); money.setName("Currency"); money.setShopType(CategoryInfo.ShopType.CURRENCY);
        var target = merchant(hidden ? "hidden" : "target", Items.DIAMOND);
        target.setMoney(100); restrict(target, List.of("Complete the trial to unlock", "viscript_shop.jei.title", PACK_KEY));
        discount(target, PromotionRule.Target.MONEY_COST, 25);
        if (!hidden) money.getMerchants().add(merchant("decoy", Items.DIAMOND));
        money.getMerchants().add(target); shop.getCategoryInfos().add(money);
        if (!hidden) {
            var barter = new CategoryInfo(); barter.setId("barter"); barter.setName("Barter");
            var offer = merchant("barter", Items.EMERALD); offer.setItemA(new ItemStack(Items.GOLD_INGOT, 8));
            restrict(offer, List.of()); discount(offer, PromotionRule.Target.ITEM_A, 50);
            barter.getMerchants().add(offer); shop.getCategoryInfos().add(barter);
            var category = new CategoryInfo(); category.setId("restricted"); category.setName("Stage category");
            restrict(category, List.of("Unlock the category trial"));
            var catOffer = merchant("category", Items.DIAMOND_SWORD); catOffer.setItemA(new ItemStack(Items.EMERALD));
            category.getMerchants().add(catOffer); shop.getCategoryInfos().add(category);
        }
        return shop;
    }

    private static MerchantInfo merchant(String id, net.minecraft.world.item.Item item) {
        var m = new MerchantInfo(); m.setId(id); m.setItemResult(new ItemStack(item)); return m;
    }

    private static void restrict(StageRestricted target, List<String> messages) {
        target.setStageRestrictionEnabled(true);
        target.setFlagGroups(new ArrayList<>(List.of(new MerchantFlagGroup(MerchantFlagGroup.MatchMode.AND, List.of(FLAG)))));
        target.setLockMessages(new ArrayList<>(messages));
    }

    private static void discount(MerchantInfo merchant, PromotionRule.Target target, double percentage) {
        var rule = new PromotionRule(); rule.setTarget(target); rule.setPercentage(percentage);
        var condition = new PromotionConditionEntry(StageFlagCondition.ID);
        ((StageFlagCondition) condition.getCondition()).setFlag(FLAG);
        rule.getConditions().add(condition); merchant.setPromotionEnabled(true); merchant.getPromotionRules().add(rule);
    }

    private static IJeiRuntime runtime() { return JeiHelper.getJeiRuntime().orElseThrow(); }
    private static List<ShopDisplayEntry> entries() {
        return runtime().getRecipeManager().createRecipeLookup(ShopRecipeCategory.TYPE).get()
                .filter(e -> e.getShopLocation().startsWith(LOCATION)).toList();
    }
    private static ShopDisplayEntry entry(String id) {
        return entries().stream().filter(e -> e.getMerchantId().equals(id)).findFirst().orElseThrow();
    }
    private static void showTarget(String id) {
        runtime().getRecipesGui().showRecipes(runtime().getRecipeManager().getRecipeCategory(ShopRecipeCategory.TYPE), List.of(entry(id)), List.of());
    }
    private static List<String> hints(TestContext c, String selector) {
        return strings(c.el(selector).element().collectHoverTooltips().tooltipTexts());
    }
    private static List<String> strings(List<Component> text) { return text.stream().map(Component::getString).toList(); }

    private static IRecipeLayoutWithButtons<?> layout(TestContext c, String id) {
        if (!(c.screen() instanceof RecipesGui gui)) return null;
        for (int y = 0; y < gui.height; y += 4) {
            for (int x = 0; x < gui.width; x += 4) {
                var hit = gui.getRecipeLayoutUnderMouse(x, y);
                if (hit.isPresent() && hit.get().getRecipeLayout().getRecipe() instanceof ShopDisplayEntry e
                        && e.getShopLocation().equals(LOCATION) && e.getMerchantId().equals(id)) return hit.get();
            }
        }
        return null;
    }

    private static void releasePlus(TestContext c) {
        c.input().mouseUp(c.<Float>get("plusX"), c.<Float>get("plusY"), 0);
    }

    private static void pressPlus(TestContext c, String id) {
        var hit = layout(c, id);
        if (hit == null) throw new IllegalStateException("Recipe not visible: " + id);
        var recipe = hit.getRecipeLayout();
        var rect = recipe.getRect(); var button = recipe.getSideButtonArea(1);
        float x = rect.getX() + button.getX() + button.getWidth() / 2f;
        float y = rect.getY() + button.getY() + button.getHeight() / 2f;
        c.put("plusX", x); c.put("plusY", y);
        c.input().moveTo(x, y);
        c.input().mouseDown(x, y, 0);
    }
}
