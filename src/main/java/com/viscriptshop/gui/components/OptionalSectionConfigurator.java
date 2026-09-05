package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 表示需要显式启用后才能展开编辑的配置分组。
 *
 * <p>关闭分组时保留内部配置数据，但分组始终折叠且不能展开。重新启用后，调用方可以继续
 * 编辑先前保留的内容。标题、折叠箭头和复选框均使用 LDLib2 原生组件。
 */
public class OptionalSectionConfigurator extends ConfiguratorGroup {
    private final Supplier<Boolean> enabledSupplier;
    private final Consumer<Boolean> enabledUpdater;
    private final Toggle enabledToggle;
    private boolean enabled;

    /**
     * 创建一个默认折叠的可选配置分组。
     *
     * @param  name 分组标题的翻译键
     * @param  enabledSupplier 当前启用状态的获取器
     * @param  enabledUpdater 启用状态发生变化时的更新器
     */
    public OptionalSectionConfigurator(String name,
                                       Supplier<Boolean> enabledSupplier,
                                       Consumer<Boolean> enabledUpdater) {
        super(name, true);
        this.enabledSupplier = enabledSupplier;
        this.enabledUpdater = enabledUpdater;
        this.enabledToggle = new Toggle().noText();
        this.enabledToggle.setOn(Boolean.TRUE.equals(enabledSupplier.get()), false);
        this.enabledToggle.setOnToggleChanged(this::updateEnabledActively);
        this.enabledToggle.layout(layout -> {
            layout.width(14);
            layout.height(14);
        });
        lineContainer.addChildAt(enabledToggle, 1);
        addEventListener(UIEvents.TICK, event -> updateEnabledPassively());
        applyEnabledState(Boolean.TRUE.equals(enabledSupplier.get()));
    }

    /**
     * 设置启用复选框的稳定元素标识。
     *
     * @param  id 供样式表或自动化测试查询的元素标识
     * @return 此配置分组
     */
    public OptionalSectionConfigurator setToggleId(String id) {
        enabledToggle.setId(id + "_control");
        enabledToggle.toggleButton.setId(id);
        return this;
    }

    /**
     * 返回此分组当前是否允许编辑。
     *
     * @return 启用时返回 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    protected void onLineContainerClick(UIEvent event) {
        if (event.target == enabledToggle || enabledToggle.isAncestorOf(event.target)) {
            return;
        }
        if (enabled) {
            super.onLineContainerClick(event);
        }
    }

    private void updateEnabledActively(boolean value) {
        enabledUpdater.accept(value);
        applyEnabledState(value);
        notifyChanges();
    }

    private void updateEnabledPassively() {
        boolean current = Boolean.TRUE.equals(enabledSupplier.get());
        if (current == enabled) {
            return;
        }
        enabledToggle.setOn(current, false);
        applyEnabledState(current);
    }

    private void applyEnabledState(boolean value) {
        enabled = value;
        setCanCollapse(value);
        if (!value) {
            setCollapse(true);
        }
    }
}
