package com.simibubi.create.content.kinetics.chainConveyor;

import com.google.common.cache.Cache;
import com.simibubi.create.foundation.utility.RaycastHelper;
import java.util.List;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class ChainPackageInteractionHandler {
   public static boolean onUse() {
      Minecraft mc = Minecraft.getInstance();
      MutableBoolean success = new MutableBoolean(false);
      ((Cache)ChainConveyorPackage.physicsDataCache.get(mc.level))
         .asMap()
         .forEach(
            (i, data) -> {
               if (!success.booleanValue()) {
                  if (data != null && data.targetPos != null && data.beReference != null) {
                     AABB bounds = new AABB(data.targetPos, data.targetPos).move(0.0, -0.25, 0.0).expandTowards(0.0, 0.5, 0.0).inflate(0.45);
                     double range = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1.0;
                     Vec3 from = mc.player.getEyePosition();
                     Vec3 to = RaycastHelper.getTraceTarget(mc.player, range, from);
                     if (!bounds.clip(from, to).isEmpty()) {
                        ChainConveyorBlockEntity ccbe = data.beReference.get();
                        if (ccbe != null && !ccbe.isRemoved()) {
                           for (ChainConveyorPackage pckg : ccbe.getLoopingPackages()) {
                              if (pckg.netId == i) {
                                 CatnipServices.NETWORK.sendToServer(new ChainPackageInteractionPacket(ccbe.getBlockPos(), null, pckg.chainPosition, true));
                                 success.setTrue();
                                 return;
                              }
                           }

                           for (BlockPos connection : ccbe.connections) {
                              List<ChainConveyorPackage> list = ccbe.travellingPackages.get(connection);
                              if (list != null) {
                                 for (ChainConveyorPackage pckgx : list) {
                                    if (pckgx.netId == i) {
                                       CatnipServices.NETWORK
                                          .sendToServer(new ChainPackageInteractionPacket(ccbe.getBlockPos(), connection, pckgx.chainPosition, true));
                                       success.setTrue();
                                       return;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         );
      return success.booleanValue();
   }
}
