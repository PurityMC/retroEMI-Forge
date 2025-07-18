package dev.emi.emi.mixinsupport;

import com.github.bsideup.jabel.Desugar;
import com.google.common.collect.Maps;
import dev.emi.emi.mixinsupport.annotation.AdditionalField;
import dev.emi.emi.mixinsupport.annotation.Extends;
import dev.emi.emi.mixinsupport.annotation.InvokeTarget;
import dev.emi.emi.mixinsupport.annotation.StripConstructors;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.lib.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.util.Annotations;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EmiMixinPlugin implements IMixinConfigPlugin {
	private static final String MIXIN_PLACEHOLDER = MixinPlaceholder.class.getName().replace(".", "/");

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

    @Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		processClassAnnotations(targetClassName, targetClass, mixinClassName, mixinInfo);
		EmiMixinTransformation.relinkTransforms(targetClass);
		processMethodAnnotations(targetClassName, targetClass, mixinClassName, mixinInfo);
	}

	private void processClassAnnotations(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		//RemapperChain remapper = MixinEnvironment.getCurrentEnvironment().getRemappers();
		EmiMixinTransformation.applyTransform(targetClass);
		AnnotationNode extendsAnnot = Annotations.getInvisible(targetClass, Extends.class);
		if (extendsAnnot != null) {
			targetClass.superName = Annotations.getValue(extendsAnnot, "value", targetClass.superName);
		}
		AnnotationNode stripConstructors = Annotations.getInvisible(targetClass, StripConstructors.class);
		if (stripConstructors != null) {
			for (int i = 0; i < targetClass.methods.size(); i++) {
				if (targetClass.methods.get(i).name.equals("<init>")) {
					targetClass.methods.remove(i--);
				}
			}
		}
	}

	private void processMethodAnnotations(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		//RemapperChain remapper = MixinEnvironment.getCurrentEnvironment().getRemappers();
		String thisOwner = targetClassName.replace(".", "/");
		ClassNode mixinClass = mixinInfo.getClassNode(0);
		Map<String, InvokeTargetInfo> targets = Maps.newHashMap();
		for (MethodNode method : mixinClass.methods) {
			AnnotationNode invokeTarget = Annotations.getInvisible(method, InvokeTarget.class);
			if (invokeTarget != null) {
				String owner = Annotations.getValue(invokeTarget, "owner", targetClassName);
				owner = switch(owner) {
					case "this" -> thisOwner;
					case "super" -> targetClass.superName;
					default -> owner;
				};
				String name = Annotations.getValue(invokeTarget, "name", method.name);
				String desc = Annotations.getValue(invokeTarget, "desc", method.desc);
				int type = switch (Annotations.getValue(invokeTarget, "type", "")) {
					case "VIRTUAL" -> Opcodes.INVOKEVIRTUAL;
					case "SPECIAL" -> Opcodes.INVOKESPECIAL;
					case "STATIC" -> Opcodes.INVOKESTATIC;
					case "INTERFACE" -> Opcodes.INVOKEINTERFACE;
					case "NEW" -> Opcodes.NEW;
					default -> -1;
				};
				targets.put(method.name + method.desc, new InvokeTargetInfo(owner, name, type, desc));
			}
			AnnotationNode additionalField = Annotations.getInvisible(method, AdditionalField.class);
			if (additionalField != null) {
				String value = Annotations.getValue(additionalField, "value", "");
				String ret = method.desc.split("\\)")[1];
				targetClass.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC, value, ret, null, null));
				MethodNode clinit = null;
				for (MethodNode mn : targetClass.methods) {
					if ("<clinit>".equals(mn.name)) {
						clinit = mn;
					}
				}
				if (clinit == null) {
					clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
					clinit.instructions.add(new LabelNode());
					clinit.instructions.add(new InsnNode(Opcodes.RETURN));
					targetClass.methods.add(clinit);
				}
				clinit.instructions.insertBefore(clinit.instructions.getLast(), new FieldInsnNode(Opcodes.GETSTATIC,
					Annotations.getValue(additionalField, "owner", ""), Annotations.getValue(additionalField, "name", ""), ret));
				clinit.instructions.insertBefore(clinit.instructions.getLast(), new FieldInsnNode(Opcodes.PUTSTATIC, thisOwner, value, ret));
			}
		}
		for (int i = 0; i < targetClass.methods.size(); i++) {
			MethodNode method = targetClass.methods.get(i);
			if (Annotations.getInvisible(method, InvokeTarget.class) != null || Annotations.getInvisible(method, AdditionalField.class) != null) {
				targetClass.methods.remove(i--);
			}
		}
		for (MethodNode method : targetClass.methods) {
			FieldInsnNode lastNewDup = null;
			for (int i = 0; i < method.instructions.size(); i++) {
				AbstractInsnNode node = method.instructions.get(i);
				if (node instanceof MethodInsnNode min && thisOwner.equals(min.owner)) {
					String desc = min.name + min.desc;
					if (targets.containsKey(desc)) {
						InvokeTargetInfo info = targets.get(desc);
						int type = info.type;
						if (type == -1) {
							if (info.name.equals("<init>")) {
								type = Opcodes.INVOKESPECIAL;
							} else {
								type = min.getOpcode();
							}
						}
						if (type == Opcodes.NEW) {
							if (lastNewDup != null) {
								method.instructions.insertBefore(lastNewDup, new TypeInsnNode(Opcodes.NEW, info.owner));
								method.instructions.insertBefore(lastNewDup, new InsnNode(Opcodes.DUP));
								method.instructions.remove(lastNewDup);
								lastNewDup = null;
								i += 2;
							}
							type = Opcodes.INVOKESPECIAL;
						}
						method.instructions.set(min, new MethodInsnNode(type, info.owner, info.name, info.desc));
						if (info.name.equals("<init>")) {
						}
					}
				} else if (node instanceof FieldInsnNode field) {
					if (MIXIN_PLACEHOLDER.equals(field.owner) && field.name.equals("NEW_DUP")) {
						lastNewDup = field;
					}
				}
			}
		}
	}

    @Desugar
	private static record InvokeTargetInfo(String owner, String name, int type, String desc) {
	}
}
