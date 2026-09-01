package com.viscriptshop.gui.components;

import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;

/**
 * 保留旧名称的阶段限制编辑器兼容入口。
 *
 * @deprecated 使用 {@link StageRestrictionConfigurator} 代替
 */
@Deprecated
public class MerchantFlagGroupsConfigurator extends StageRestrictionConfigurator {

    /**
     * 创建商品阶段条件编辑器。
     *
     * @param  merchant 需要编辑阶段条件的商品
     */
    public MerchantFlagGroupsConfigurator(MerchantInfo merchant) {
        super(merchant);
    }

    /**
     * 创建分类阶段条件编辑器。
     *
     * @param  category 需要编辑阶段条件的分类
     */
    public MerchantFlagGroupsConfigurator(CategoryInfo category) {
        super(category);
    }
}
