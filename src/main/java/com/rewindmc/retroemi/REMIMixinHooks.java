package com.rewindmc.retroemi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.runtime.EmiSidebars;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.search.EmiSearch;
import net.xylose.emi.inject_interface.EMICraftingManager;
import net.xylose.emi.inject_interface.EMIGuiContainerCreative;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.util.SyntheticIdentifier;

import java.util.List;

import static org.lwjgl.opengl.GL11.glColor4f;

public class REMIMixinHooks {
	static Minecraft minecraft = Minecraft.getMinecraft();

	//GuiContainer
	public static void renderBackground(int par1, int par2) {
		EmiDrawContext context = EmiDrawContext.instance();
		EmiScreenManager.drawBackground(context, par1, par2, minecraft.timer.renderPartialTicks);
	}

	public static void renderForegroundPre(int par1, int par2, Minecraft mc) {
		GuiContainer screen = (GuiContainer) mc.currentScreen;
		EmiDrawContext context = EmiDrawContext.instance();
		context.push();
		context.matrices().translate(-((EMIGuiContainerCreative) screen).getGuiLeft(), -((EMIGuiContainerCreative) screen).getGuiTop(), 0.0);
		EmiScreenManager.render(context, par1, par2, minecraft.timer.renderPartialTicks);
		context.pop();
	}

	public static void renderForegroundPost(int par1, int par2, Minecraft mc) {
		GuiContainer screen = (GuiContainer) mc.currentScreen;
		EmiDrawContext context = EmiDrawContext.instance();
		context.push();
		context.matrices().translate(-((EMIGuiContainerCreative) screen).getGuiLeft(), -((EMIGuiContainerCreative) screen).getGuiTop(), 0.0);
		EmiScreenManager.drawForeground(context, par1, par2, minecraft.timer.renderPartialTicks);
		context.pop();
	}

	public static void drawSlot(Slot slot) {
		EmiDrawContext context = EmiDrawContext.instance();
		if (EmiScreenManager.search.highlight) {
			EmiSearch.CompiledQuery query = EmiSearch.compiledQuery;
			if (query != null && !query.test(EmiStack.of(slot.getStack()))) {
				context.push();
				context.matrices().translate(0, 0, 300);
				context.fill(slot.xDisplayPosition - 1, slot.yDisplayPosition - 1, 18, 18, 0x77000000);
				context.pop();
			}
		}
	}
	//SlotCrafting
	public static void onCrafting(EntityPlayer thePlayer, IInventory craftMatrix) {
		World world = thePlayer.worldObj;
		if (world.isRemote) {
			try {
				InventoryCrafting inv = (InventoryCrafting) craftMatrix;
				Minecraft client = Minecraft.getMinecraft();
				List<IRecipe> list = ((EMICraftingManager) CraftingManager.getInstance()).getRecipes();
				for (var r : list) {
					if (r.matches(inv, client.theWorld)) {
						ResourceLocation id = new SyntheticIdentifier(r);
						EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(id);
						if (recipe != null) {
							EmiSidebars.craft(recipe);
							return;
						}
					}
				}
			} catch (Throwable t) {}
		}
	}

	//FontRenderer
	public static int applyCustomFormatCodes(FontRenderer subject, String str, boolean shadow, int i) {
		if (str.charAt(i+1) == 'x') {
			int next = str.indexOf(String.valueOf('\u00a7') + "x", i+1);
			if (next != -1) {
				String s = str.substring(i+1, next);
				int color = Integer.parseInt(s.replace(String.valueOf('\u00a7'), "").substring(1), 16);
				if (shadow) {
					color = (color & 16579836) >> 2 | color & -16777216;
				}
				subject.textColor = color;
				glColor4f((color >> 16) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, subject.alpha);
				i += s.length()+1;
			}
		}
		return i;
	}

}
