package dev.emi.emi.mixin.early.minecraft.accessor;

import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(CraftingManager.class)
public interface CraftingManagerAccessor {
    @Accessor("recipes")
    List<IRecipe> getRecipes();
}
