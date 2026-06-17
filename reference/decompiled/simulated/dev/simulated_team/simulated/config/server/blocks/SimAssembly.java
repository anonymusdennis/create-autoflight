package dev.simulated_team.simulated.config.server.blocks;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ConfigBase.ConfigBool;
import net.createmod.catnip.config.ConfigBase.ConfigFloat;
import net.createmod.catnip.config.ConfigBase.ConfigInt;

public class SimAssembly extends ConfigBase {
   public final ConfigInt maxBlocksMoved = this.i(128000, 1, "maxBlocksMoved", new String[]{SimAssembly.Comments.maxBlocksMoved});
   public final ConfigInt honeyGlueRange = this.i(48, 1, Integer.MAX_VALUE, "honeyGlueRange", new String[]{SimAssembly.Comments.honeyGlueRange});
   public final ConfigFloat mergingGlueRange = this.f(4.0F, 0.0F, Float.MAX_VALUE, "mergingGlueRange", new String[]{SimAssembly.Comments.mergingGlueRange});
   public final ConfigInt maxDisassemblyTicks = this.i(20, 5, "maxDisassemblyTicks", new String[]{SimAssembly.Comments.maxDisassemblyTicks});
   public final ConfigFloat disassemblyDegreeTolerance = this.f(
      4.0F, 0.0F, "disassemblyDegreeTolerance", new String[]{SimAssembly.Comments.disassemblyDegreeTolerance}
   );
   public final ConfigFloat disassemblyMaxVelocity = this.f(5.0F, 0.0F, "disassemblyMaxVelocity", new String[]{SimAssembly.Comments.disassemblyMaxVelocity});
   public final ConfigFloat disassemblyMaxAngularVelocity = this.f(
      (float) (Math.PI / 2), 0.0F, "disassemblyMaxAngularVelocity", new String[]{SimAssembly.Comments.disassemblyMaxAngularVelocity}
   );
   public final ConfigBool disallowMidAirDisassembly = this.b(true, "disallowMidAirDisassembly", new String[]{SimAssembly.Comments.disallowMidAirDisassembly});
   public final ConfigBool primaryDisassembly = this.b(
      false,
      "Primary Disassembly",
      new String[]{
         "Whether only the original Physics Assembler can disassemble the Sub-Level it assembled",
         "Disabling allows *ALL* Physics Assemblers to disassemble any Sub-Level"
      }
   );

   public String getName() {
      return "assembly";
   }

   private static class Comments {
      static String honeyGlueRange = "Maximum range in blocks which honey glue may initially be placed";
      static String mergingGlueRange = "Maximum range in blocks which merging glue may be placed by items such as slime balls";
      static String maxBlocksMoved = "Maximum amount of blocks in a structure assemble-able by Physics Assemblers, Swivel Bearings, or other means.";
      static String maxDisassemblyTicks = "The amount of ticks that disassembly alignment is allowed to take before failing.";
      static String disassemblyDegreeTolerance = "The maximum amount of degrees a Simulated Contraption is allowed to be tilted to fully disassemble";
      static String disassemblyMaxVelocity = "The maximum velocity a Simulated Contraption is allowed to disassemble at in m/s";
      static String disassemblyMaxAngularVelocity = "The maximum angular velocity a Simulated Contraption is allowed to disassemble at in rad/s";
      static String disallowMidAirDisassembly = "Disallow disassembly of Simulated Contraptions in mid-air, requiring them to be within a few chunk sections of terrain";
   }
}
