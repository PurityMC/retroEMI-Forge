package dev.emi.emi.recipe;

import dev.emi.emi.EmiUtil;
import dev.emi.emi.VanillaPlugin;
import dev.emi.emi.api.recipe.EmiIngredientRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiResolutionRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.TagEmiIngredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.tag.TagKey;

import java.util.List;

public class EmiTagRecipe extends EmiIngredientRecipe {
	private final List<EmiStack> stacks;
	private final EmiIngredient ingredient;
	public final TagKey<?> key;

	public EmiTagRecipe(TagKey<?> key) {
		this.key = key;
		this.ingredient = new TagEmiIngredient(key, 1);
		this.stacks = ingredient.getEmiStacks();
	}

	@Override
	protected EmiIngredient getIngredient() {
		return ingredient;
	}

	@Override
	protected List<EmiStack> getStacks() {
		return stacks;
	}

	@Override
	protected EmiRecipe getRecipeContext(EmiStack stack, int offset) {
		return new EmiResolutionRecipe(ingredient, stack);
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return VanillaPlugin.TAG;
	}

	@Override
	public ResourceLocation getId() {
		return new ResourceLocation("emi", "tag/" + key.getFlavor() + "/" + EmiUtil.subId(key.id()));
	}
}
