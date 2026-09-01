package com.viscriptshop.compat;

import com.viscriptshop.ViscriptShop;
import com.viscriptshop.compat.ftbquests.FtbQuestsComPat;
import net.neoforged.api.distmarker.Dist;

/**
 * 按已加载模组与运行端初始化 VSS 的可选兼容模块。
 */
public class ModComPat {
    /**
     * 初始化当前运行环境可用的兼容模块。
     *
     * @param dist 当前物理运行端
     */
    public static void init(Dist dist) {
        if (ViscriptShop.isFtbQuestsLoaded()) {
            FtbQuestsComPat.init(dist);
        }
        if (dist == Dist.CLIENT) {
            if (ViscriptShop.isFtbLibraryLoaded()) {
                FtbLibraryComPat.init();
            }
        }
    }
}
