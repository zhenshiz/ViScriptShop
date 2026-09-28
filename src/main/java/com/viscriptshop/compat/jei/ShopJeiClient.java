package com.viscriptshop.compat.jei;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptshop.gui.data.ShopDisplayEntry;
import com.viscriptshop.network.c2s.ShopJeiC2SPayload;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** 管理当前连接的商店快照，仅增量更新 VSS 类别中的可见条目。 */
public final class ShopJeiClient {
    private static final Map<CompoundTag, ShopDisplayEntry> REGISTERED = new LinkedHashMap<>();
    private static List<ShopDisplayEntry> snapshot = List.of();
    private static List<ShopDisplayEntry> visible = List.of();
    private static final List<ShopDisplayEntry> incoming = new ArrayList<>();
    private static IJeiRuntime runtime;
    private static long revision = -1;
    private static int nextBatch;
    private static long generation;
    private static boolean refreshingPage;
    private static boolean requestPending;
    private static boolean requestAgain;

    private ShopJeiClient() {
    }

    private static void request() {
        if (Minecraft.getInstance().getConnection() != null) {
            if (requestPending) {
                requestAgain = true;
                return;
            }
            requestPending = true;
            RPCPacketDistributor.rpcToServer(ShopJeiC2SPayload.REFRESH);
        }
    }

    public static void onScreenOpening(net.minecraft.client.gui.screens.Screen screen) {
        if (!refreshingPage && runtime != null && screen == runtime.getRecipesGui()) request();
    }

    public static void runtimeAvailable(IJeiRuntime value) {
        runtime = value;
        apply();
        request();
    }

    public static void runtimeUnavailable() {
        runtime = null;
        REGISTERED.clear();
        visible = List.of();
    }

    public static void receive(long version, int batch, boolean last, List<ShopDisplayEntry> entries) {
        if (Minecraft.getInstance().getConnection() == null || version < revision) return;
        if (batch == 0 && version > revision) {
            revision = version;
            incoming.clear();
            nextBatch = 0;
        }
        if (version != revision || batch != nextBatch) return;
        incoming.addAll(entries);
        nextBatch++;
        if (last) {
            snapshot = List.copyOf(incoming);
            incoming.clear();
            apply();
            requestPending = false;
            if (requestAgain) {
                requestAgain = false;
                request();
            }
        }
    }

    private static void apply() {
        if (runtime == null) return;
        var manager = runtime.getRecipeManager();
        var next = new ArrayList<ShopDisplayEntry>();
        var added = new ArrayList<ShopDisplayEntry>();
        for (var entry : snapshot) {
            CompoundTag key = entry.serializeNBT(Platform.getFrozenRegistry());
            var existing = REGISTERED.get(key);
            if (existing == null) {
                REGISTERED.put(key, entry);
                added.add(entry);
                existing = entry;
            }
            next.add(existing);
        }
        var nextSet = new HashSet<>(next);
        var previousSet = new HashSet<>(visible);
        var addedSet = new HashSet<>(added);
        var removed = visible.stream().filter(entry -> !nextSet.contains(entry)).toList();
        var restored = next.stream().filter(entry -> !previousSet.contains(entry) && !addedSet.contains(entry)).toList();
        if (!removed.isEmpty()) manager.hideRecipes(ShopRecipeCategory.TYPE, removed);
        if (!added.isEmpty()) manager.addRecipes(ShopRecipeCategory.TYPE, added);
        if (!restored.isEmpty()) manager.unhideRecipes(ShopRecipeCategory.TYPE, restored);
        visible = List.copyOf(next);
        if (!removed.isEmpty() || !added.isEmpty() || !restored.isEmpty()) generation++;
    }

    static long generation() {
        return generation;
    }

    static void refreshOpenPage(IFocusGroup focuses) {
        var minecraft = Minecraft.getInstance();
        if (refreshingPage || runtime == null || minecraft.screen != runtime.getRecipesGui()) return;
        refreshingPage = true;
        minecraft.tell(() -> {
            try {
                if (runtime == null || minecraft.screen != runtime.getRecipesGui()) return;
                var matching = runtime.getRecipeManager().createRecipeLookup(ShopRecipeCategory.TYPE)
                        .limitFocus(focuses.getAllFocuses()).get().findAny();
                if (matching.isEmpty()) minecraft.screen.onClose();
                else if (focuses.isEmpty()) runtime.getRecipesGui().showTypes(List.of(ShopRecipeCategory.TYPE));
                else runtime.getRecipesGui().show(focuses.getAllFocuses());
            } finally {
                refreshingPage = false;
            }
        });
    }

    public static boolean isCurrent(ShopDisplayEntry entry) {
        return visible.contains(entry);
    }

    public static void clear() {
        snapshot = List.of();
        apply();
        // JEI 会保留隐藏条目；同一个 runtime 内复用它们，避免重连后重复注册。
        incoming.clear();
        revision = -1;
        nextBatch = 0;
        requestPending = false;
        requestAgain = false;
    }
}
