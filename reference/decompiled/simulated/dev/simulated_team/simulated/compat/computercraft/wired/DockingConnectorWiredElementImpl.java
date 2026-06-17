package dev.simulated_team.simulated.compat.computercraft.wired;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class DockingConnectorWiredElementImpl implements DockingConnectorWiredElement, WiredElement {
   private final DockingConnectorBlockEntity entity;
   private final WiredNode node;

   public DockingConnectorWiredElementImpl(DockingConnectorBlockEntity entity) {
      this.entity = entity;
      this.node = ComputerCraftAPI.createWiredNodeForElement(this);
   }

   public WiredNode getNode() {
      return this.node;
   }

   public String getSenderID() {
      return "docking_connector";
   }

   public Level getLevel() {
      return this.entity.getLevel();
   }

   public Vec3 getPosition() {
      return Vec3.atCenterOf(this.entity.getBlockPos());
   }

   @Override
   public void connect(DockingConnectorWiredElement other) {
      if (other instanceof DockingConnectorWiredElementImpl we) {
         this.getNode().connectTo(we.getNode());
      }
   }

   @Override
   public void disconnect(DockingConnectorWiredElement other) {
      if (other instanceof DockingConnectorWiredElementImpl we) {
         this.getNode().disconnectFrom(we.getNode());
      }
   }

   @Override
   public void remove() {
      this.getNode().remove();
   }
}
