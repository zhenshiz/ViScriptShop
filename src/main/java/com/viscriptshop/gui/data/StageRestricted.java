package com.viscriptshop.gui.data;

import com.viscript_lib.stage.StageMatchMode;

/** 保留商店阶段字段和枚举类型，访问判断统一由 VSL 处理。 */
public interface StageRestricted extends com.viscript_lib.stage.StageRestricted<MerchantFlagGroup> {
    MerchantFlagGroup.GroupMatchMode getFlagGroupMode();
    void setFlagGroupMode(MerchantFlagGroup.GroupMatchMode mode);

    @Override
    default StageMatchMode getStageGroupMode() {
        var mode = getFlagGroupMode();
        return mode == null ? StageMatchMode.OR : StageMatchMode.valueOf(mode.name());
    }

    @Override
    default void setStageGroupMode(StageMatchMode mode) {
        setFlagGroupMode(MerchantFlagGroup.GroupMatchMode.valueOf(mode.name()));
    }
}
