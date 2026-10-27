package com.viscriptshop.gui.components;

import com.viscriptshop.gui.data.MerchantFlagGroup;
import com.viscriptshop.gui.data.StageRestricted;

/** 使用 VSL 阶段编辑器，并保留商店原有的条件组存档类型。 */
public class StageRestrictionConfigurator extends com.viscript_lib.gui.configurator.StageRestrictionConfigurator<MerchantFlagGroup> {
    public StageRestrictionConfigurator(StageRestricted restriction) {
        super(restriction, MerchantFlagGroup::new);
    }
}
