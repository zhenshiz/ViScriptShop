package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.configurator.ConfiguratorParser;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * 为手工分组的表单创建 LDLib2 字段配置器。
 */
public final class ConfiguratorFieldHelper {
    private ConfiguratorFieldHelper() {
    }

    /**
     * 将指定字段的原生 LDLib2 配置器添加到分组末尾。
     *
     * @param  group 接收字段配置器的分组
     * @param  owner 持有配置字段的对象
     * @param  fieldName 声明字段的名称
     * @return 新增的字段配置器
     * @throws IllegalArgumentException 字段不存在或没有生成配置器
     */
    public static Configurator addField(ConfiguratorGroup group, Object owner, String fieldName) {
        try {
            Field field = owner.getClass().getDeclaredField(fieldName);
            int previousSize = group.getConfigurators().size();
            ConfiguratorParser.createFieldConfigurator(
                    field,
                    group,
                    field.getDeclaringClass(),
                    new HashMap<>(),
                    owner
            );
            if (group.getConfigurators().size() <= previousSize) {
                throw new IllegalArgumentException("未能为配置字段创建组件：" + fieldName);
            }
            return group.getConfigurators().getLast();
        } catch (NoSuchFieldException exception) {
            throw new IllegalArgumentException("配置字段不存在：" + fieldName, exception);
        }
    }
}
