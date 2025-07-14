package dev.emi.emi.mixin.accessor;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.SlotCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SlotCrafting.class)
public interface SlotCraftingAccessor {
    @Accessor("craftMatrix")
    IInventory getCraftMatrix();
}
