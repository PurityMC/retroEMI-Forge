package net.xylose.emi.inject_interface;

import net.minecraft.util.ResourceLocation;

public interface EmiResourceLocation {
    default int compareTo(ResourceLocation that) {
        return 0;
    }
}
