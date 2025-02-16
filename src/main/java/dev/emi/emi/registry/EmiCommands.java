package dev.emi.emi.registry;

import dev.emi.emi.network.CommandS2CPacket;
import dev.emi.emi.network.EmiNetwork;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EmiCommands extends CommandBase {
	public static final byte VIEW_RECIPE = 0x01;
	public static final byte VIEW_TREE = 0x02;
	public static final byte TREE_GOAL = 0x11;
	public static final byte TREE_RESOLUTION = 0x12;

	@Override
	public String getCommandName() {
		return "emi";
	}

	@Override
	public String getCommandUsage(ICommandSender var1) {
		return "commands.emi.usage";
	}

	//TODO Implement when recipe changes are made
	@Override
	public void processCommand(ICommandSender sender, String[] userInputStrings) {
		if (userInputStrings.length > 2) {
			EntityPlayerMP player = getPlayer(sender, userInputStrings[0]);
			if (userInputStrings[0].equals("view")) {
				if (userInputStrings[1].equals("recipe")) {
					if (userInputStrings[2].isEmpty()) {
						throw new WrongUsageException("commands.emi.id", new Object[0]);
					} else {
						ResourceLocation id = new ResourceLocation(userInputStrings[2]);
						send(player, VIEW_RECIPE, id);
					}
				}
				else if (userInputStrings[1].equals("tree")) {
					send(player, VIEW_TREE, null);
				}
			}
			else if (userInputStrings[0].equals("tree")) {
				if (userInputStrings[1].equals("goal")) {
					if (userInputStrings[2].isEmpty()) {
						throw new WrongUsageException("commands.emi.id", new Object[0]);
					}
					else {
						ResourceLocation id = new ResourceLocation(userInputStrings[2]);
						send(player, TREE_GOAL, id);
					}
				}
				else if (userInputStrings[1].equals("resolution")) {
					if (userInputStrings[2].isEmpty()) {
						throw new WrongUsageException("commands.emi.id", new Object[0]);
					}
					else {
						ResourceLocation id = new ResourceLocation(userInputStrings[2]);
						send(player, TREE_RESOLUTION, id);
					}
				}
			}
		}
		else {
			throw new WrongUsageException("commands.emi.usage", new Object[0]);
		}
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] userInputStrings) {
		if (userInputStrings.length == 1) {
			return getListOfStringsMatchingLastWord(userInputStrings, "view", "tree");
		}
		if (userInputStrings.length == 2) {
			if (userInputStrings[0].equals("view")) {
				return getListOfStringsMatchingLastWord(userInputStrings, "recipe", "tree");
			}
			else if (userInputStrings[0].equals("tree")) {
				return getListOfStringsMatchingLastWord(userInputStrings, "goal", "resolution");
			}
		}
		if (userInputStrings.length == 3) {
			if (!userInputStrings[1].equals("tree")) {
				//List<EmiRecipe> recipeList = EmiApi.getRecipeManager().getRecipes();
				//return getListOfStringsMatchingLastWord(userInputStrings, "");
			}
		}
		return null;
	}

	private static void send(EntityPlayerMP player, byte type, @Nullable ResourceLocation id) {
		EmiNetwork.sendToClient(player, new CommandS2CPacket(type, id));
	}
}
