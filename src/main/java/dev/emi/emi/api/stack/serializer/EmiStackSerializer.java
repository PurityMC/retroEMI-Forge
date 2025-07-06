package dev.emi.emi.api.stack.serializer;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.JsonHelper;

public interface EmiStackSerializer<T extends EmiStack> extends EmiIngredientSerializer<T> {
	static final Pattern STACK_REGEX = Pattern.compile("^([\\w_\\-./]+):([\\w_\\-.]+):([\\w_\\-./]+)(\\{.*\\})?$");

	EmiStack create(ResourceLocation id, NBTTagCompound nbt, long amount);

//	private static <T> DynamicOps<T> withRegistryAccess(DynamicOps<T> ops) {
//		MinecraftClient instance = MinecraftClient.getInstance();
//		if (instance == null || instance.world == null) {
//			//Note: instance can be null in datagen, just fall back to a variant that doesn't have registry access
//			// as in the majority of cases this will work fine
//			return ops;
//		}
//		return instance.world.getRegistryManager().getOps(ops);
//	}

	@Override
	default EmiIngredient deserialize(JsonElement element) {
		ResourceLocation id = null;
		String nbt = null;
		long amount = 1;
		float chance = 1;
		EmiStack remainder = EmiStack.EMPTY;
		if (JsonHelper.isString(element)) {
			String s = element.getAsString();
			Matcher m = STACK_REGEX.matcher(s);
			if (m.matches()) {
				id = EmiPort.id(m.group(2), m.group(3));
				nbt = m.group(4);
			}
		} else if (element.isJsonObject()) {
			JsonObject json = element.getAsJsonObject();
			id = EmiPort.id(JsonHelper.getString(json, "id"));
			nbt = JsonHelper.getString(json, "nbt", null);
			amount = JsonHelper.getLong(json, "amount", 1);
			chance = JsonHelper.getFloat(json, "chance", 1);
			if (JsonHelper.hasElement(json, "remainder")) {
				EmiIngredient ing = EmiIngredientSerializer.getDeserialized(json.get("remainder"));
				if (ing instanceof EmiStack stack) {
					remainder = stack;
				}
			}
		}
		if (id != null) {
			try {
				NBTTagCompound nbtComp = null;
				if (nbt != null) {
					nbtComp = StringNbtReader.parse(nbt);
				}
				EmiStack stack = create(id, nbtComp, amount);
				if (chance != 1) {
					stack.setChance(chance);
				}
				if (!remainder.isEmpty()) {
					stack.setRemainder(remainder);
				}
				return stack;
			} catch (Exception e) {
				EmiLog.error("Error parsing NBT in deserialized stack", e);
				return EmiStack.EMPTY;
			}
		}
		return EmiStack.EMPTY;
	}

	@Override
	default JsonElement serialize(T stack) throws IOException {
		String nbt = null;
		if (stack.hasNbt()) {
			nbt += StringNbtReader.encode(stack.getNbt());
		}
		if (stack.getAmount() == 1 && stack.getChance() == 1 && stack.getRemainder().isEmpty()) {
			String s = getType() + ":" + stack.getId();
			if (nbt != null) {
				s += nbt;
			}
			return new JsonPrimitive(s);
		} else {
			JsonObject json = new JsonObject();
			json.addProperty("type", getType());
			json.addProperty("id", stack.getId().toString());
			if (nbt != null) {
				json.addProperty("nbt", nbt);
			}
			if (stack.getAmount() != 1) {
				json.addProperty("amount", stack.getAmount());
			}
			if (stack.getChance() != 1) {
				json.addProperty("chance", stack.getChance());
			}
			if (!stack.getRemainder().isEmpty()) {
				EmiStack remainder = stack.getRemainder();
				if (!remainder.getRemainder().isEmpty()) {
					remainder = remainder.copy().setRemainder(EmiStack.EMPTY);
				}
				if (remainder.getRemainder().isEmpty()) {
					JsonElement remainderElement = EmiIngredientSerializer.getSerialized(remainder);
					if (remainderElement != null) {
						json.add("remainder", remainderElement);
					}
				}
			}
			return json;
		}
	}
}
