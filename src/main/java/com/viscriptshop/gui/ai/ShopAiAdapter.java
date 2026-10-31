package com.viscriptshop.gui.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.viscript_lib.gui.ai.AiEditorAdapter;
import com.viscript_lib.gui.ai.AiJson;
import com.viscript_lib.gui.editor.ViScriptEditor;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.Shop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 向 AI 暴露商店、分类和商品的明确领域操作。
 *
 * <p>未声明的促销、阶段、指令及自定义组件字段保持原样。修改通过整个商店信息对象的
 * 可逆切换进入 LDLib2 历史，撤销时重新使用旧对象，使此前表单历史仍能操作原来的对象。
 */
public final class ShopAiAdapter implements AiEditorAdapter {
    @Override
    public String guide() {
        return """
                This is ViScriptShop's shop editor. Left panel: categories; middle: preview; right: shop/category/
                merchant inspector. Select a category to see its merchants; select a merchant to edit its fields.
                Supported AI operations: shop name/quick opening, add/rename/remove categories, add/update/remove
                merchants, result/cost item IDs and counts, currency price, stock, experience and BUY/SELL type.
                CURRENCY uses money (BUY means player buys, SELL means player sells). ITEM_FOR_ITEM requires at
                least one nonempty cost and a result. stock=-1 means unlimited. IDs identify entries, not names.
                Supply a unique id for new entries. Item counts are 1..64; query_registry discovers valid IDs.
                Commands, promotions, stage restrictions, custom components and runtime upload are not supported
                by this adapter; explain how to edit them manually instead of inventing operations. Existing such
                settings are preserved. read_editor returns a flat paginated list of categories and merchants.
                All edits affect only the open local project; the player uses File/Upload to publish afterwards.
                """;
    }

    @Override
    public JsonObject editSchema() {
        var root = AiJson.object();
        var operations = AiJson.property(root, "operations", "array", "One or more ordered atomic domain operations; choose batch size to fit the model's output budget.");
        operations.addProperty("minItems", 1);
        var choices = new JsonArray();
        var shop = operation("set_shop", "name", "quick_open");
        AiJson.property(shop, "quick_open", "boolean", "Whether quick opening is enabled.");
        choices.add(shop);
        var addCategory = operation("add_category", "id", "name", "shop_type");
        AiJson.required(addCategory, "op", "id", "name", "shop_type");
        choices.add(addCategory);
        var rename = operation("rename_category", "category_id", "name");
        AiJson.required(rename, "op", "category_id", "name");
        choices.add(rename);
        var removeCategory = operation("remove_category", "category_id");
        AiJson.required(removeCategory, "op", "category_id");
        choices.add(removeCategory);
        for (String name : List.of("add_merchant", "update_merchant")) {
            var merchant = operation(name, "category_id", "id", "result_item", "cost_a", "cost_b", "trade_type");
            for (String field : List.of("result_count", "cost_a_count", "cost_b_count", "stock", "xp")) {
                AiJson.property(merchant, field, "integer", "Item count 1..64; stock -1..1000000000; xp 0..1000000.");
            }
            AiJson.property(merchant, "money", "number", "Currency amount 0..1000000000000.");
            AiJson.required(merchant, name.equals("add_merchant")
                    ? new String[]{"op", "category_id", "id", "result_item"}
                    : new String[]{"op", "category_id", "id"});
            choices.add(merchant);
        }
        var removeMerchant = operation("remove_merchant", "category_id", "id");
        AiJson.required(removeMerchant, "op", "category_id", "id");
        choices.add(removeMerchant);
        var item = new JsonObject();
        item.add("oneOf", choices);
        operations.add("items", item);
        AiJson.required(root, "operations");
        return root;
    }

    private static JsonObject operation(String op, String... fields) {
        var schema = AiJson.object();
        var type = AiJson.property(schema, "op", "string", "Operation name.");
        var values = new JsonArray();
        values.add(op);
        type.add("enum", values);
        for (String field : fields) AiJson.property(schema, field, "string", switch (field) {
            case "shop_type" -> "ITEM_FOR_ITEM or CURRENCY.";
            case "trade_type" -> "BUY or SELL.";
            case "cost_a", "cost_b" -> "Registered item ID; minecraft:air clears this optional cost.";
            default -> field;
        });
        AiJson.required(schema, "op");
        return schema;
    }

    @Override
    public JsonObject describe(IProject project, int offset, int limit) {
        var shop = ((Shop) project).getShopInfo();
        var all = new JsonArray();
        for (var category : shop.getCategoryInfos()) {
            var row = new JsonObject();
            row.addProperty("kind", "category");
            row.addProperty("id", category.getId());
            row.addProperty("name", category.getName());
            row.addProperty("shop_type", category.getShopType().name());
            row.addProperty("merchant_count", category.getMerchants().size());
            all.add(row);
            for (var merchant : category.getMerchants()) all.add(merchantData(category, merchant));
        }
        var entries = new JsonArray();
        for (int i = offset; i < Math.min(all.size(), offset + limit); i++) entries.add(all.get(i));
        var result = new JsonObject();
        result.addProperty("shop_name", shop.getName());
        result.addProperty("quick_open", shop.isQuickOpening());
        result.add("entries", entries);
        result.addProperty("has_more", offset + entries.size() < all.size());
        result.addProperty("next_offset", offset + entries.size());
        return result;
    }

    private static JsonObject merchantData(CategoryInfo category, MerchantInfo merchant) {
        var row = new JsonObject();
        row.addProperty("kind", "merchant");
        row.addProperty("category_id", category.getId());
        row.addProperty("id", merchant.getId());
        row.addProperty("result_item", BuiltInRegistries.ITEM.getKey(merchant.getItemResult().getItem()).toString());
        row.addProperty("result_count", merchant.getItemResult().getCount());
        row.addProperty("cost_a", BuiltInRegistries.ITEM.getKey(merchant.getItemA().getItem()).toString());
        row.addProperty("cost_a_count", merchant.getItemA().getCount());
        row.addProperty("cost_b", BuiltInRegistries.ITEM.getKey(merchant.getItemB().getItem()).toString());
        row.addProperty("cost_b_count", merchant.getItemB().getCount());
        row.addProperty("money", merchant.getMoney());
        row.addProperty("stock", merchant.getStock());
        row.addProperty("xp", merchant.getXp());
        row.addProperty("trade_type", merchant.getTradeType().name());
        return row;
    }

    @Override
    public List<String> edit(IProject draft, JsonObject arguments) {
        AiJson.keys(arguments, "operations");
        if (!arguments.has("operations") || !arguments.get("operations").isJsonArray()) {
            throw new IllegalArgumentException("Expected operations array");
        }
        var operations = arguments.getAsJsonArray("operations");
        if (operations.isEmpty()) throw new IllegalArgumentException("Expected at least one operation");
        var changes = new ArrayList<String>();
        var shop = ((Shop) draft).getShopInfo();
        for (var raw : operations) {
            var op = raw.getAsJsonObject();
            String kind = AiJson.string(op, "op", 40);
            switch (kind) {
                case "set_shop" -> {
                    AiJson.keys(op, "op", "name", "quick_open");
                    if (op.has("name")) {
                        String name = AiJson.string(op, "name", 256);
                        changes.add(t("name", shop.getName(), name));
                        shop.setName(name);
                    }
                    if (op.has("quick_open")) {
                        boolean enabled = AiJson.bool(op, "quick_open");
                        changes.add(t("quick_open", shop.isQuickOpening(), enabled));
                        shop.setQuickOpening(enabled);
                    }
                }
                case "add_category" -> {
                    AiJson.keys(op, "op", "id", "name", "shop_type");
                    String id = id(op, "id");
                    if (shop.getCategoryInfos().stream().anyMatch(c -> c.getId().equals(id))) throw new IllegalArgumentException("Duplicate category ID");
                    var category = new CategoryInfo();
                    category.setId(id);
                    category.setName(AiJson.string(op, "name", 256));
                    category.setShopType(CategoryInfo.ShopType.valueOf(AiJson.string(op, "shop_type", 32)));
                    shop.getCategoryInfos().add(category);
                    changes.add(t("add_category", category.getName(), id, Component.translatable(category.getShopType().getName()).getString()));
                }
                case "rename_category" -> {
                    AiJson.keys(op, "op", "category_id", "name");
                    var category = category((Shop) draft, op);
                    String name = AiJson.string(op, "name", 256);
                    changes.add(t("rename_category", category.getId(), category.getName(), name));
                    category.setName(name);
                }
                case "remove_category" -> {
                    AiJson.keys(op, "op", "category_id");
                    var category = category((Shop) draft, op);
                    changes.add(t("remove_category", category.getName(), category.getMerchants().size()));
                    shop.getCategoryInfos().remove(category);
                }
                case "add_merchant", "update_merchant" -> {
                    AiJson.keys(op, "op", "category_id", "id", "result_item", "result_count", "cost_a", "cost_a_count",
                            "cost_b", "cost_b_count", "money", "stock", "xp", "trade_type");
                    var category = category((Shop) draft, op);
                    String id = id(op, "id");
                    var existing = category.getMerchants().stream().filter(m -> m.getId().equals(id)).findFirst();
                    boolean create = kind.equals("add_merchant");
                    if (create == existing.isPresent()) throw new IllegalArgumentException(create ? "Duplicate merchant ID" : "Unknown merchant ID");
                    var merchant = create ? new MerchantInfo() : existing.orElseThrow();
                    String before = create ? t("none") : merchantSummary(merchant);
                    merchant.setId(id);
                    if (create && !op.has("result_item")) throw new IllegalArgumentException("result_item is required");
                    if (op.has("result_item") || op.has("result_count")) merchant.setItemResult(item(op, "result_item", "result_count", merchant.getItemResult(), false));
                    if (op.has("cost_a") || op.has("cost_a_count")) merchant.setItemA(item(op, "cost_a", "cost_a_count", merchant.getItemA(), true));
                    if (op.has("cost_b") || op.has("cost_b_count")) merchant.setItemB(item(op, "cost_b", "cost_b_count", merchant.getItemB(), true));
                    if (op.has("money")) {
                        var value = op.get("money");
                        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected money number");
                        double money = value.getAsDouble();
                        if (!Double.isFinite(money) || money < 0 || money > 1e12) throw new IllegalArgumentException("Invalid money amount");
                        merchant.setMoney(money);
                    }
                    merchant.setStock(AiJson.integer(op, "stock", merchant.getStock(), -1, 1_000_000_000));
                    merchant.setXp(AiJson.integer(op, "xp", merchant.getXp(), 0, 1_000_000));
                    if (op.has("trade_type")) merchant.setTradeType(MerchantInfo.TradeType.valueOf(AiJson.string(op, "trade_type", 16)));
                    if (merchant.getItemResult().isEmpty()) throw new IllegalArgumentException("Result item is required");
                    if (category.getShopType() == CategoryInfo.ShopType.ITEM_FOR_ITEM && merchant.getItemA().isEmpty() && merchant.getItemB().isEmpty()) {
                        throw new IllegalArgumentException("ITEM_FOR_ITEM requires a nonempty cost");
                    }
                    if (create) category.getMerchants().add(merchant);
                    changes.add(t("merchant", id, before, merchantSummary(merchant)));
                }
                case "remove_merchant" -> {
                    AiJson.keys(op, "op", "category_id", "id");
                    var category = category((Shop) draft, op);
                    String id = id(op, "id");
                    var merchant = category.getMerchants().stream().filter(m -> m.getId().equals(id)).findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Unknown merchant ID"));
                    changes.add(t("remove_merchant", merchant.getId(), merchantSummary(merchant)));
                    category.getMerchants().remove(merchant);
                }
                default -> throw new IllegalArgumentException("Unsupported shop operation: " + kind);
            }
        }
        return changes;
    }

    private static String merchantSummary(MerchantInfo merchant) {
        return t("merchant_summary", stackSummary(merchant.getItemResult()), stackSummary(merchant.getItemA()),
                stackSummary(merchant.getItemB()), merchant.getMoney(), merchant.getStock(), merchant.getXp(),
                Component.translatable(merchant.getTradeType().getName()).getString());
    }

    private static String stackSummary(ItemStack stack) {
        return stack.isEmpty() ? t("none") : stack.getHoverName().getString() + " × " + stack.getCount()
                + " (" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + ")";
    }

    private static String t(String key, Object... args) {
        return Component.translatable("viscript_shop.ai.change." + key, args).getString();
    }

    private static String id(JsonObject args, String key) {
        String id = AiJson.string(args, key, 128);
        if (id.isBlank()) throw new IllegalArgumentException("Blank ID: " + key);
        return id;
    }

    private static CategoryInfo category(Shop shop, JsonObject args) {
        String id = id(args, "category_id");
        return shop.getShopInfo().getCategoryInfos().stream().filter(c -> c.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown category ID"));
    }

    private static ItemStack item(JsonObject args, String key, String countKey, ItemStack original, boolean allowEmpty) {
        int count = AiJson.integer(args, countKey, Math.max(1, original.getCount()), 1, 64);
        if (!args.has(key)) {
            if (original.isEmpty()) throw new IllegalArgumentException("Item ID required: " + key);
            return original.copyWithCount(count);
        }
        String text = AiJson.string(args, key, 256);
        var id = text.contains(":") ? ResourceLocation.tryParse(text) : null;
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalArgumentException("Unknown item ID: " + text);
        var item = BuiltInRegistries.ITEM.get(id);
        var stack = original.is(item) ? original.copyWithCount(count) : new ItemStack(item, count);
        if (stack.isEmpty() && !allowEmpty) throw new IllegalArgumentException("Result cannot be air");
        return stack;
    }

    @Override
    public EditAction createAction(ViScriptEditor editor, IProject draft) {
        var target = (Shop) editor.getCurrentProject();
        var before = target.getShopInfo();
        var after = ((Shop) draft).getShopInfo();
        return EditAction.of(() -> {
            target.shopInfo = after;
            try { refresh(editor); } catch (RuntimeException exception) { target.shopInfo = before; refresh(editor); throw exception; }
        }, () -> {
            target.shopInfo = before;
            refresh(editor);
        });
    }

    @Override
    public void setPreviewMode(ViScriptEditor editor, boolean previewing) {
        ((ShopEditor) editor).setAiPreviewing(previewing);
    }

    @Override
    public void refresh(ViScriptEditor editor) {
        if (editor instanceof ShopEditor shopEditor && editor.getCurrentProject() instanceof Shop shop) {
            shopEditor.shopInspectorView.refreshAfterHistory(shop.getShopInfo());
            shopEditor.categoryView.loadView();
            shopEditor.shopPreviewView.loadView();
        }
    }
}
