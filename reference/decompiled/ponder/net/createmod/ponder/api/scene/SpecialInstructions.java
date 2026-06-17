package net.createmod.ponder.api.scene;

import java.util.function.Supplier;
import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public interface SpecialInstructions {
   ElementLink<ParrotElement> createBirb(Vec3 var1, Supplier<? extends ParrotPose> var2);

   void changeBirbPose(ElementLink<ParrotElement> var1, Supplier<? extends ParrotPose> var2);

   void movePointOfInterest(Vec3 var1);

   void movePointOfInterest(BlockPos var1);

   void rotateParrot(ElementLink<ParrotElement> var1, double var2, double var4, double var6, int var8);

   void moveParrot(ElementLink<ParrotElement> var1, Vec3 var2, int var3);

   ElementLink<MinecartElement> createCart(Vec3 var1, float var2, MinecartElement.MinecartConstructor var3);

   void rotateCart(ElementLink<MinecartElement> var1, float var2, int var3);

   void moveCart(ElementLink<MinecartElement> var1, Vec3 var2, int var3);

   <T extends AnimatedSceneElement> void hideElement(ElementLink<T> var1, Direction var2);
}
