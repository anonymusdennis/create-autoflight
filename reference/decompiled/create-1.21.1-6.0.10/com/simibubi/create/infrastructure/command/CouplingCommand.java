package com.simibubi.create.infrastructure.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.simibubi.create.AllAttachmentTypes;
import com.simibubi.create.content.contraptions.minecart.CouplingHandler;
import com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import net.createmod.catnip.data.Iterate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public class CouplingCommand {
   public static final SimpleCommandExceptionType ONLY_MINECARTS_ALLOWED = new SimpleCommandExceptionType(Component.literal("Only Minecarts can be coupled"));
   public static final SimpleCommandExceptionType SAME_DIMENSION = new SimpleCommandExceptionType(
      Component.literal("Minecarts have to be in the same Dimension")
   );
   public static final DynamicCommandExceptionType TWO_CARTS = new DynamicCommandExceptionType(
      a -> Component.literal("Your selector targeted " + a + " entities. You can only couple 2 Minecarts at a time.")
   );

   public static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("coupling").requires(cs -> cs.hasPermission(2)))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("add")
                        .then(
                           Commands.argument("cart1", EntityArgument.entity())
                              .then(
                                 Commands.argument("cart2", EntityArgument.entity())
                                    .executes(
                                       ctx -> {
                                          Entity cart1 = EntityArgument.getEntity(ctx, "cart1");
                                          if (!(cart1 instanceof AbstractMinecart)) {
                                             throw ONLY_MINECARTS_ALLOWED.create();
                                          } else {
                                             Entity cart2 = EntityArgument.getEntity(ctx, "cart2");
                                             if (!(cart2 instanceof AbstractMinecart)) {
                                                throw ONLY_MINECARTS_ALLOWED.create();
                                             } else if (!cart1.getCommandSenderWorld().equals(cart2.getCommandSenderWorld())) {
                                                throw SAME_DIMENSION.create();
                                             } else {
                                                Entity source = ((CommandSourceStack)ctx.getSource()).getEntity();
                                                CouplingHandler.tryToCoupleCarts(
                                                   source instanceof Player ? (Player)source : null,
                                                   cart1.getCommandSenderWorld(),
                                                   cart1.getId(),
                                                   cart2.getId()
                                                );
                                                return 1;
                                             }
                                          }
                                       }
                                    )
                              )
                        ))
                     .then(
                        Commands.argument("carts", EntityArgument.entities())
                           .executes(
                              ctx -> {
                                 Collection<? extends Entity> entities = EntityArgument.getEntities(ctx, "carts");
                                 if (entities.size() != 2) {
                                    throw TWO_CARTS.create(entities.size());
                                 } else {
                                    ArrayList<? extends Entity> eList = Lists.newArrayList(entities);
                                    Entity cart1 = eList.get(0);
                                    if (!(cart1 instanceof AbstractMinecart)) {
                                       throw ONLY_MINECARTS_ALLOWED.create();
                                    } else {
                                       Entity cart2 = eList.get(1);
                                       if (!(cart2 instanceof AbstractMinecart)) {
                                          throw ONLY_MINECARTS_ALLOWED.create();
                                       } else if (!cart1.getCommandSenderWorld().equals(cart2.getCommandSenderWorld())) {
                                          throw SAME_DIMENSION.create();
                                       } else {
                                          Entity source = ((CommandSourceStack)ctx.getSource()).getEntity();
                                          CouplingHandler.tryToCoupleCarts(
                                             source instanceof Player ? (Player)source : null, cart1.getCommandSenderWorld(), cart1.getId(), cart2.getId()
                                          );
                                          return 1;
                                       }
                                    }
                                 }
                              }
                           )
                     )
               ))
            .then(
               Commands.literal("remove")
                  .then(Commands.argument("cart1", EntityArgument.entity()).then(Commands.argument("cart2", EntityArgument.entity()).executes(ctx -> {
                     Entity cart1 = EntityArgument.getEntity(ctx, "cart1");
                     if (!(cart1 instanceof AbstractMinecart)) {
                        throw ONLY_MINECARTS_ALLOWED.create();
                     } else {
                        Entity cart2 = EntityArgument.getEntity(ctx, "cart2");
                        if (!(cart2 instanceof AbstractMinecart)) {
                           throw ONLY_MINECARTS_ALLOWED.create();
                        } else {
                           MinecartController cart1Capability = (MinecartController)cart1.getData(AllAttachmentTypes.MINECART_CONTROLLER);
                           if (cart1Capability == MinecartController.EMPTY) {
                              ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Minecart has no Couplings Attached"), true);
                              return 0;
                           } else {
                              int cart1Couplings = (cart1Capability.isConnectedToCoupling() ? 1 : 0) + (cart1Capability.isLeadingCoupling() ? 1 : 0);
                              if (cart1Couplings == 0) {
                                 ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Minecart has no Couplings Attached"), true);
                                 return 0;
                              } else {
                                 for (boolean bool : Iterate.trueAndFalse) {
                                    UUID coupledCart = cart1Capability.getCoupledCart(bool);
                                    if (coupledCart != null && coupledCart == cart2.getUUID()) {
                                       MinecartController cart2Controller = CapabilityMinecartController.getIfPresent(
                                          cart1.getCommandSenderWorld(), coupledCart
                                       );
                                       if (cart2Controller == null) {
                                          return 0;
                                       }

                                       cart1Capability.removeConnection(bool);
                                       cart2Controller.removeConnection(!bool);
                                       return 1;
                                    }
                                 }

                                 ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("The specified Carts are not coupled"), true);
                                 return 0;
                              }
                           }
                        }
                     }
                  })))
            ))
         .then(Commands.literal("removeAll").then(Commands.argument("cart", EntityArgument.entity()).executes(ctx -> {
            Entity cart = EntityArgument.getEntity(ctx, "cart");
            if (!(cart instanceof AbstractMinecart)) {
               throw ONLY_MINECARTS_ALLOWED.create();
            } else {
               MinecartController capability = (MinecartController)cart.getData(AllAttachmentTypes.MINECART_CONTROLLER);
               if (capability == MinecartController.EMPTY) {
                  ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Minecart has no Couplings Attached"), true);
                  return 0;
               } else {
                  int couplings = (capability.isConnectedToCoupling() ? 1 : 0) + (capability.isLeadingCoupling() ? 1 : 0);
                  if (couplings == 0) {
                     ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Minecart has no Couplings Attached"), true);
                     return 0;
                  } else {
                     capability.decouple();
                     ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Removed " + couplings + " couplings from the Minecart"), true);
                     return couplings;
                  }
               }
            }
         })));
   }
}
