package com.viscriptshop.gui.configurator.accessor;

import com.lowdragmc.lowdraglib2.configurator.accessors.TypesAccessor;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.viscriptshop.gui.configurator.configurator.ItemStackConfigurator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "item", registry = "ldlib2:configurator_accessor")
public class ItemStackAccessor extends TypesAccessor<ItemStack> {

    public ItemStackAccessor() {
        super(ItemStack.class);
    }

    @Override
    public ItemStack defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return BuiltInRegistries.ITEM.get(ResourceLocation.parse(field.getAnnotation(DefaultValue.class).stringValue()[0])).getDefaultInstance();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Configurator create(String name, Supplier<ItemStack> supplier, Consumer<ItemStack> consumer, boolean forceUpdate, Field field, Object owner) {
        return new ItemStackConfigurator(name, supplier, consumer, defaultValue(field, ItemStack.class), forceUpdate);
    }
}
