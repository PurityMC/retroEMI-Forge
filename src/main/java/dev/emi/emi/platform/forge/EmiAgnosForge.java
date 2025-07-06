package dev.emi.emi.platform.forge;

import com.mojang.blaze3d.systems.RenderSystem;
import com.rewindmc.retroemi.InputPair;
import com.rewindmc.retroemi.Prototype;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.ModMetadata;
import dev.emi.emi.*;
import dev.emi.emi.api.EmiPluginRegistry;
import dev.emi.emi.api.stack.FluidEmiStack;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tag.ItemKey;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.Type;

import com.google.common.collect.Lists;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameData;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.recipe.EmiBrewingRecipe;
import dev.emi.emi.registry.EmiPluginContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.text.WordUtils;
import com.rewindmc.retroemi.EmiMultiPlugin;
import com.rewindmc.retroemi.NamedEmiPlugin;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.Text;
import net.minecraft.util.SyntheticIdentifier;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL;

public class EmiAgnosForge extends EmiAgnos {
	static {
		EmiAgnos.delegate = new EmiAgnosForge();
	}

	@Override
	protected boolean isForgeAgnos() {
		return true;
	}


	public static void poke() {
	}

    @Override
    protected String getModNameAgnos(String namespace) {
        if (namespace.equals("c")) {
            return "Common";
        }
        Optional<? extends ModContainer> container = Optional.ofNullable(Loader.instance().getIndexedModList().get(namespace));
        if (container.isPresent()) {
            return container.get().getName();
        }
        container = Optional.ofNullable(Loader.instance().getIndexedModList().get(namespace.replace('_', '-')));
        if (container.isPresent()) {
            return container.get().getName();
        }
        return WordUtils.capitalizeFully(namespace.replace('_', ' '));
    }

	@Override
	protected Path getConfigDirectoryAgnos() {
		return Loader.instance().getConfigDir().toPath();
	}

	@Override
	protected boolean isDevelopmentEnvironmentAgnos() {
		return (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
	}

	@Override
	protected boolean isModLoadedAgnos(String id) {
		return Loader.isModLoaded(id);
	}

    @Override
    protected List<String> getAllModNamesAgnos(String id) {
        List<String> modNames = new ArrayList<>();
        for (ModContainer container : Loader.instance().getActiveModList()) {
            if (container.getModId().equals(id)) {
                modNames.add(container.getMetadata().name);
            }
        }
        return modNames;
    }

    @Override
    protected List<String> getAllModAuthorsAgnos(String id) {
        List<String> authors = new ArrayList<>();
        for (ModContainer container : Loader.instance().getActiveModList()) {
            if (container.getModId().equals(id)) {
                for (String author : container.getMetadata().authorList) {
                    if (!authors.contains(author)) {
                        authors.add(author);
                    }
                }
            }
        }
        return authors;
    }

    @Override
    protected List<EmiPluginContainer> getPluginsAgnos() {
        return EmiPluginRegistry.getPlugins();
    }

//    protected List<EmiPluginContainer> getPluginsAgnos() {
//        List<EmiPluginContainer> containers = Lists.newArrayList();
//        for (ModContainer mod : Loader.instance().getActiveModList()) {
//            ModMetadata metadata = mod.getMetadata();
//            if (metadata.containsKey("emiPlugin")) {
//                String pluginClassName = (String) metadata.get("emiPlugin");
//                try {
//                    Class<?> clazz = Class.forName(pluginClassName);
//                    if (EmiPlugin.class.isAssignableFrom(clazz)) {
//                        @SuppressWarnings("unchecked")
//                        Class<? extends EmiPlugin> pluginClass = (Class<? extends EmiPlugin>) clazz;
//                        EmiPlugin plugin = pluginClass.newInstance();
//                        containers.add(new EmiPluginContainer(plugin, mod.getModId()));
//                    } else {
//                        EmiLog.error("EmiEntrypoint " + pluginClassName + " does not implement EmiPlugin");
//                    }
//                } catch (Throwable t) {
//                    EmiLog.error("Exception constructing entrypoint:", t);
//                }
//            }
//        }
//        return containers;
//    }

	private Stream<EmiPluginContainer> createPlugin(String clazzName, String id) {
		try {
			var clazz = Class.forName(clazzName);
			if (!EmiPlugin.class.isAssignableFrom(clazz) && !EmiMultiPlugin.class.isAssignableFrom(clazz)) {
				EmiLog.warn("Registered emi entrypoint for nilmod {} does not implement EmiPlugin");
				return null;
			}
			if (!Runnable.class.isAssignableFrom(clazz)) {
				EmiLog.warn("Registered emi entrypoint for nilmod {} does not implement Runnable (this is required for NilLoader entrypoint compliance)");
				return null;
			}
			var inst = clazz.getConstructor().newInstance();
			Stream<EmiPlugin> stream = inst instanceof EmiPlugin ep ? Stream.of(ep) : Stream.empty();
			if (inst instanceof EmiMultiPlugin emp) stream = Stream.concat(stream, emp.getChildPlugins());
			return stream.map(ep -> new EmiPluginContainer(ep, ep instanceof NamedEmiPlugin n ? id + "/" + n.getName() : id));
		} catch (Throwable t) {
			EmiLog.warn("Unexpected error while attempting to create plugin for nilmod {}");
			return null;
		}
	}

	@Override
	protected void addBrewingRecipesAgnos(EmiRegistry registry) {
		var tebs = new TileEntityBrewingStand();
		var ingredience = ((Set<String>) Item.itemRegistry.getKeys())
            .stream()
            .map(name -> (Item) Item.itemRegistry.getObject(name))
            .filter(i -> i != null && ((Item) i).isPotionIngredient(new ItemStack((Item) i)))
            .collect(Collectors.toList());

        Map<InputPair, Prototype> recipes = new HashMap<>();
		IntList potions = new IntArrayList();
		potions.add(0); // water bottle
		IntSet seenPotions = new IntLinkedOpenHashSet();

		while (!potions.isEmpty()) {
			int[] iter = potions.toIntArray();
			potions.clear();
			for (int potion : iter) {
				seenPotions.add(potion);
				for (Object obj : ingredience) {
                    Item ing = (Item) obj;
					int result = tebs.func_145936_c(potion, new ItemStack(ing));
					List<PotionEffect> inputEffects = Items.potionitem.getEffects(potion);
					List<PotionEffect> resultEffects = Items.potionitem.getEffects(result);
					if (((potion <= 0 || inputEffects != resultEffects) &&
							(inputEffects == null || !inputEffects.equals(resultEffects) && resultEffects != null)) ||
							(!ItemPotion.isSplash(potion) && ItemPotion.isSplash(result))) {
						if (potion != result && !seenPotions.contains(result)) {
							potions.add(result);
							recipes.put(new InputPair(new Prototype(ing), new Prototype(Items.potionitem, potion)), new Prototype(Items.potionitem, result));
						}
					}
				}
			}
		}

		for (Map.Entry<InputPair, Prototype> en : recipes.entrySet()) {
			InputPair i = en.getKey();
			registry.addRecipe(new EmiBrewingRecipe(EmiStack.of(i.potion()), EmiStack.of(i.ingredient()), EmiStack.of(en.getValue()),
					new ResourceLocation("brewing", "/" + SyntheticIdentifier.describe(i.potion()) + "/" + SyntheticIdentifier.describe(i.ingredient()) + "/" +
							SyntheticIdentifier.describe(en.getValue()))));
		}
		// Vanilla potion entries have different meta from brewable potions (!)
		// Remove all those uncraftable potions from the index

		registry.removeEmiStacks(
				es -> es.getItemStack() != null && es.getItemStack().getItem() == Items.potionitem && !seenPotions.contains(es.getItemStack().getItemDamage()));

		// We just did an exhaustive search and determined every legitimately obtainable potion
		// So let's just cram those into the index where they're supposed to go.
		// Sort them into something resembling a logical order, though!
		List<EmiStack> sorted = recipes.values().stream().filter(p -> p.meta() != 0).distinct().sorted((b, a) -> {
			int i = Boolean.compare(ItemPotion.isSplash(a.meta()), ItemPotion.isSplash(b.meta()));
			if (i != 0) {
				return i;
			}
			List<PotionEffect> effA = ((List<PotionEffect>) ((ItemPotion) a.getItem()).getEffects(a.toStack()));
			List<PotionEffect> effB = ((List<PotionEffect>) ((ItemPotion) b.getItem()).getEffects(b.toStack()));
			return listCompare(effA, effB, Comparator.comparingInt(PotionEffect::getPotionID).thenComparingInt(PotionEffect::getAmplifier).thenComparingInt(PotionEffect::getDuration));
		}).map(EmiStack::of).collect(Collectors.toList());
		EmiStack prev = EmiStack.of(new Prototype(Items.potionitem, 0));
		for (EmiStack potion : sorted) {
			registry.addEmiStackAfter(potion, prev);
		}
	}

	private static <T> int listCompare(List<T> a, List<T> b, Comparator<? super T> cmp) {
		Objects.requireNonNull(cmp);
		if (a == b) {
			return 0;
		}
		if (a == null || b == null) {
			return a == null ? -1 : 1;
		}

		int length = Math.min(a.size(), b.size());
		for (int i = 0; i < length; i++) {
			T oa = a.get(i);
			T ob = b.get(i);
			if (oa != ob) {
				// Null-value comparison is deferred to the comparator
				int v = cmp.compare(oa, ob);
				if (v != 0) {
					return v;
				}
			}
		}

		return a.size() - b.size();
	}

//	@SuppressWarnings("RedundantCast")
//	@Override
//	protected List<TooltipComponent> getItemTooltipAgnos(ItemStack stack) {
//		if (MinecraftServerEMI.getIsServer()) {
//			String var5 = stack.getDisplayName();
//
//			if (stack.hasDisplayName())
//			{
//				var5 = EnumChatFormatting.ITALIC + var5 + EnumChatFormatting.RESET;
//			}
//			return Collections.singletonList(TooltipComponent.of(Text.literal(var5)));
//		}
//		else {
//			// I SWEAR TO GOD DON'T YOU FUCKING TOUCH THIS CAST
//			EntityPlayer player = (EntityPlayer) (Object) Minecraft.getMinecraft().thePlayer;
//			while (player == null) {
//				// THE CLASSLOADER IS A LIE
//				player = (EntityPlayer) (Object) Minecraft.getMinecraft().thePlayer;
//				try {
//					Thread.sleep(5);
//				}
//				catch (InterruptedException e) {
//					throw new RuntimeException(e);
//				}
//			}
//			List<String> tip = stack.getTooltip(player, Minecraft.getMinecraft().gameSettings.advancedItemTooltips, (Slot) null);
//			for (int i = 0; i < tip.size(); i++) {
//				tip.set(i, "§" + (i == 0 ? Integer.toHexString(stack.getRarity().rarityColor) : "7") + tip.get(i));
//			}
//			return tip.stream().map(Text::literal).map(TooltipComponent::of).collect(Collectors.toList());
//		}
//	}

	@Override
	protected List<TooltipComponent> getItemTooltipAgnos(ItemStack stack) {
		List<String> tip = stack.getTooltip(Minecraft.getMinecraft().thePlayer, Minecraft.getMinecraft().gameSettings.advancedItemTooltips);
		for (int i = 0; i < tip.size(); i++) {
			tip.set(i, "§" + (i == 0 ? stack.getRarity().rarityColor.getFormattingCode() : "7") + tip.get(i));
		}
		return tip.stream()
				.map(Text::literal).map(TooltipComponent::of)
                .collect(java.util.stream.Collectors.toList());
	}

    @Override
    protected Text getFluidNameAgnos(Fluid fluid, NBTTagCompound nbt) {
        return Text.literal(new FluidStack(fluid, 1000, nbt).getLocalizedName());
    }

	@Override
	protected List<Text> getFluidTooltipAgnos(Fluid fluid, NBTTagCompound nbt) {
		return Collections.singletonList(getFluidNameAgnos(fluid, nbt));
	}

    @Override
    protected boolean isFloatyFluidAgnos(FluidEmiStack stack) {
        FluidStack fs = new FluidStack(stack.getKeyOfType(Fluid.class), 1000, stack.getNbt());
        return fs.getFluid().getDensity() <= 0;
    }

    @Override
    protected void renderFluidAgnos(FluidEmiStack stack, MatrixStack matrices, int x, int y, float delta, int xOff, int yOff, int width, int height) {
        FluidStack fs = new FluidStack(stack.getKeyOfType(Fluid.class), 1000, stack.getNbt());
        RenderSystem.setShaderTexture(0, TextureMap.locationBlocksTexture);
        Minecraft.getMinecraft().currentScreen.drawTexturedModelRectFromIcon(x, y, fs.getFluid().getIcon(), 16, 16);
    }

    @Override
    protected EmiStack createFluidStackAgnos(Object object) {
        if (object instanceof FluidStack f) {
            return EmiStack.of(f.getFluid(), f.tag, f.amount);
        }
        return EmiStack.EMPTY;
    }

    @Override
	protected boolean canBatchAgnos(ItemStack stack) {
		return false;
	}

    @Override
    protected Map<ItemKey, Integer> getFuelMapAgnos() {
        Map<ItemKey, Integer> fuelMap = new HashMap<>();
        for (Object obj : ItemPotion.itemRegistry) {
            Item item = (Item) obj;
            List<ItemStack> stacks = new ArrayList<>();
            item.getSubItems(item, CreativeTabs.tabMisc, stacks);
            for (ItemStack stack : stacks) {
                int time = TileEntityFurnace.getItemBurnTime(stack);
                if (time > 0) {
                    fuelMap.put(ItemKey.of(stack), time);
                }
            }
        }
        return fuelMap;
    }
}
