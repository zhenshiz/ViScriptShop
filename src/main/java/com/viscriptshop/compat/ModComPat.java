package com.viscriptshop.compat;

import com.viscriptshop.ViscriptShop;
import com.viscriptshop.compat.ftbquests.FtbQuestsComPat;
import net.minecraftforge.api.distmarker.Dist;

public class ModComPat {
    public static void init(Dist dist) {
        if (ViscriptShop.isFtbQuestsLoaded()) {
            FtbQuestsComPat.init(dist);
        }
        if (dist.isClient()) {
            if (ViscriptShop.isFtbLibraryLoaded()) {
                FtbLibraryComPat.init();
            }
        }
    }
}
