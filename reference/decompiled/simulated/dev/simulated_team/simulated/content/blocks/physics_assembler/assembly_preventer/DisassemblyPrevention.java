package dev.simulated_team.simulated.content.blocks.physics_assembler.assembly_preventer;

import com.simibubi.create.content.contraptions.AssemblyException;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.mixin_interface.assembly_preventer.PrimaryAssemblerExtension;
import dev.simulated_team.simulated.service.SimConfigService;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class DisassemblyPrevention {
   private static final LangBuilder ERR = SimLang.builder().translate("prevent_disassembly", new Object[0]);

   public static boolean checkSubLevelForPrimary(Level level, BlockPos toCheck) throws AssemblyException {
      if ((Boolean)SimConfigService.INSTANCE.server().assembly.primaryDisassembly.get() && !(level == null & toCheck == null)) {
         if (Sable.HELPER.getContaining(level, toCheck) instanceof ServerSubLevel ssl) {
            BlockPos primary = ((PrimaryAssemblerExtension)ssl).simulated$getPrimaryAssembler();
            if (primary != null
               && level.getBlockEntity(primary) instanceof PhysicsAssemblerBlockEntity psbe
               && (!toCheck.equals(primary) || !psbe.isPrimaryAssembler())) {
               throw new AssemblyException(ERR.component());
            }
         }

         return true;
      } else {
         return true;
      }
   }
}
