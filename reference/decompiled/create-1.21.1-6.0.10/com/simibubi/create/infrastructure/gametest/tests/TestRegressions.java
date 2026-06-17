package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.level.block.Blocks;

@GameTestGroup(
   path = "regressions"
)
public class TestRegressions {
   @GameTest(
      template = "issue9615_efficient_deployers",
      timeoutTicks = 200
   )
   public static void issue9615_efficientDeployers(CreateGameTestHelper helper) {
      BlockPos lever = new BlockPos(2, 5, 0);
      BlockPos goal = new BlockPos(1, 3, 4);
      helper.unpowerLever(lever);
      helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.LIME_STAINED_GLASS, goal));
   }
}
