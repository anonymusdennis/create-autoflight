package dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create;

import java.util.EnumSet;
import net.minecraft.core.Direction;

public interface NozzleBlockEntityExtension {
   EnumSet<Direction> sable$getValidDirections();
}
