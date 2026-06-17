package net.createmod.ponder.foundation;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.createmod.catnip.math.Pointing;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.DebugInstructions;
import net.createmod.ponder.api.scene.EffectInstructions;
import net.createmod.ponder.api.scene.OverlayInstructions;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.api.scene.SpecialInstructions;
import net.createmod.ponder.api.scene.WorldInstructions;
import net.createmod.ponder.foundation.element.ElementLinkImpl;
import net.createmod.ponder.foundation.element.EntityElementImpl;
import net.createmod.ponder.foundation.element.InputWindowElement;
import net.createmod.ponder.foundation.element.MinecartElementImpl;
import net.createmod.ponder.foundation.element.ParrotElementImpl;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.createmod.ponder.foundation.instruction.AnimateMinecartInstruction;
import net.createmod.ponder.foundation.instruction.AnimateParrotInstruction;
import net.createmod.ponder.foundation.instruction.AnimateWorldSectionInstruction;
import net.createmod.ponder.foundation.instruction.BlockEntityDataInstruction;
import net.createmod.ponder.foundation.instruction.ChaseAABBInstruction;
import net.createmod.ponder.foundation.instruction.CreateMinecartInstruction;
import net.createmod.ponder.foundation.instruction.CreateParrotInstruction;
import net.createmod.ponder.foundation.instruction.DelayInstruction;
import net.createmod.ponder.foundation.instruction.DisplayWorldSectionInstruction;
import net.createmod.ponder.foundation.instruction.EmitParticlesInstruction;
import net.createmod.ponder.foundation.instruction.FadeOutOfSceneInstruction;
import net.createmod.ponder.foundation.instruction.HighlightValueBoxInstruction;
import net.createmod.ponder.foundation.instruction.KeyframeInstruction;
import net.createmod.ponder.foundation.instruction.LineInstruction;
import net.createmod.ponder.foundation.instruction.MarkAsFinishedInstruction;
import net.createmod.ponder.foundation.instruction.MovePoiInstruction;
import net.createmod.ponder.foundation.instruction.OutlineSelectionInstruction;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.instruction.ReplaceBlocksInstruction;
import net.createmod.ponder.foundation.instruction.RotateSceneInstruction;
import net.createmod.ponder.foundation.instruction.ShowInputInstruction;
import net.createmod.ponder.foundation.instruction.TextInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class PonderSceneBuilder implements SceneBuilder {
   private final OverlayInstructions overlay;
   private final WorldInstructions world;
   private final DebugInstructions debug;
   private final EffectInstructions effects;
   private final SpecialInstructions special;
   protected final PonderScene scene;

   public PonderSceneBuilder(PonderScene ponderScene) {
      this.scene = ponderScene;
      this.overlay = new PonderSceneBuilder.PonderOverlayInstructions();
      this.special = new PonderSceneBuilder.PonderSpecialInstructions();
      this.world = new PonderSceneBuilder.PonderWorldInstructions();
      this.debug = new PonderSceneBuilder.PonderDebugInstructions();
      this.effects = new PonderSceneBuilder.PonderEffectInstructions();
   }

   @Override
   public OverlayInstructions overlay() {
      return this.overlay;
   }

   @Override
   public WorldInstructions world() {
      return this.world;
   }

   @Override
   public DebugInstructions debug() {
      return this.debug;
   }

   @Override
   public EffectInstructions effects() {
      return this.effects;
   }

   @Override
   public SpecialInstructions special() {
      return this.special;
   }

   @Override
   public PonderScene getScene() {
      return this.scene;
   }

   @Override
   public void title(String sceneId, String title) {
      this.scene.sceneId = ResourceLocation.fromNamespaceAndPath(this.scene.getNamespace(), sceneId);
      this.scene.localization.registerSpecific(this.scene.sceneId, "header", title);
   }

   @Override
   public void configureBasePlate(int xOffset, int zOffset, int basePlateSize) {
      this.scene.basePlateOffsetX = xOffset;
      this.scene.basePlateOffsetZ = zOffset;
      this.scene.basePlateSize = basePlateSize;
   }

   @Override
   public void scaleSceneView(float factor) {
      this.scene.scaleFactor = factor;
   }

   @Override
   public void removeShadow() {
      this.scene.hidePlatformShadow = true;
   }

   @Override
   public void setSceneOffsetY(float yOffset) {
      this.scene.yOffset = yOffset;
   }

   @Override
   public void showBasePlate() {
      this.world
         .showSection(
            this.scene
               .getSceneBuildingUtil()
               .select()
               .cuboid(
                  new BlockPos(this.scene.getBasePlateOffsetX(), 0, this.scene.getBasePlateOffsetZ()),
                  new Vec3i(this.scene.getBasePlateSize() - 1, 0, this.scene.getBasePlateSize() - 1)
               ),
            Direction.UP
         );
   }

   @Override
   public void addInstruction(PonderInstruction instruction) {
      this.scene.schedule.add(instruction);
   }

   @Override
   public void addInstruction(Consumer<PonderScene> callback) {
      this.addInstruction(PonderInstruction.simple(callback));
   }

   @Override
   public void idle(int ticks) {
      this.addInstruction(new DelayInstruction(ticks));
   }

   @Override
   public void idleSeconds(int seconds) {
      this.idle(seconds * 20);
   }

   @Override
   public void markAsFinished() {
      this.addInstruction(new MarkAsFinishedInstruction());
   }

   @Override
   public void setNextUpEnabled(boolean isEnabled) {
      this.addInstruction(scene -> scene.setNextUpEnabled(isEnabled));
   }

   @Override
   public void rotateCameraY(float degrees) {
      this.addInstruction(new RotateSceneInstruction(0.0F, degrees, true));
   }

   @Override
   public void addKeyframe() {
      this.addInstruction(KeyframeInstruction.IMMEDIATE);
   }

   @Override
   public void addLazyKeyframe() {
      this.addInstruction(KeyframeInstruction.DELAYED);
   }

   public class PonderDebugInstructions implements DebugInstructions {
      @Override
      public void debugSchematic() {
         PonderSceneBuilder.this.addInstruction(scene -> scene.addElement(new WorldSectionElementImpl(scene.getSceneBuildingUtil().select().everywhere())));
      }

      @Override
      public void addInstructionInstance(PonderInstruction instruction) {
         PonderSceneBuilder.this.addInstruction(instruction);
      }

      @Override
      public void enqueueCallback(Consumer<PonderScene> callback) {
         PonderSceneBuilder.this.addInstruction(callback);
      }
   }

   public class PonderEffectInstructions implements EffectInstructions {
      @Override
      public void emitParticles(Vec3 location, ParticleEmitter emitter, float amountPerCycle, int cycles) {
         PonderSceneBuilder.this.addInstruction(new EmitParticlesInstruction(location, emitter, amountPerCycle, cycles));
      }

      @Override
      public <T extends ParticleOptions> ParticleEmitter simpleParticleEmitter(T data, Vec3 motion) {
         return (w, x, y, z) -> w.addParticle(data, x, y, z, motion.x, motion.y, motion.z);
      }

      @Override
      public <T extends ParticleOptions> ParticleEmitter particleEmitterWithinBlockSpace(T data, Vec3 motion) {
         return (w, x, y, z) -> w.addParticle(
               data,
               Math.floor(x) + (double)Ponder.RANDOM.nextFloat(),
               Math.floor(y) + (double)Ponder.RANDOM.nextFloat(),
               Math.floor(z) + (double)Ponder.RANDOM.nextFloat(),
               motion.x,
               motion.y,
               motion.z
            );
      }

      @Override
      public void indicateRedstone(BlockPos pos) {
         this.createRedstoneParticles(pos, 16711680, 10);
      }

      @Override
      public void indicateSuccess(BlockPos pos) {
         this.createRedstoneParticles(pos, 8454058, 10);
      }

      @Override
      public void createRedstoneParticles(BlockPos pos, int color, int amount) {
         Vector3f rgb = new Color(color).asVectorF();
         PonderSceneBuilder.this.addInstruction(
            new EmitParticlesInstruction(
               VecHelper.getCenterOf(pos),
               PonderSceneBuilder.this.effects().particleEmitterWithinBlockSpace(new DustParticleOptions(rgb, 1.0F), Vec3.ZERO),
               (float)amount,
               2
            )
         );
      }
   }

   public class PonderOverlayInstructions implements OverlayInstructions {
      @Override
      public TextElementBuilder showText(int duration) {
         TextWindowElement textWindowElement = new TextWindowElement();
         PonderSceneBuilder.this.addInstruction(new TextInstruction(textWindowElement, duration));
         return textWindowElement.builder(PonderSceneBuilder.this.scene);
      }

      @Override
      public TextElementBuilder showOutlineWithText(Selection selection, int duration) {
         TextWindowElement textWindowElement = new TextWindowElement();
         PonderSceneBuilder.this.addInstruction(new TextInstruction(textWindowElement, duration, selection));
         return textWindowElement.builder(PonderSceneBuilder.this.scene).pointAt(selection.getCenter());
      }

      @Override
      public InputElementBuilder showControls(Vec3 sceneSpace, Pointing direction, int duration) {
         InputWindowElement inputWindowElement = new InputWindowElement(sceneSpace, direction);
         PonderSceneBuilder.this.addInstruction(new ShowInputInstruction(inputWindowElement, duration));
         return inputWindowElement.builder();
      }

      @Override
      public void chaseBoundingBoxOutline(PonderPalette color, Object slot, AABB boundingBox, int duration) {
         PonderSceneBuilder.this.addInstruction(new ChaseAABBInstruction(color, slot, boundingBox, duration));
      }

      @Override
      public void showCenteredScrollInput(BlockPos pos, Direction side, int duration) {
         this.showScrollInput(PonderSceneBuilder.this.scene.getSceneBuildingUtil().vector().blockSurface(pos, side), side, duration);
      }

      @Override
      public void showScrollInput(Vec3 location, Direction side, int duration) {
         Axis axis = side.getAxis();
         float s = 0.0625F;
         float q = 0.25F;
         Vec3 expands = new Vec3(axis == Axis.X ? (double)s : (double)q, axis == Axis.Y ? (double)s : (double)q, axis == Axis.Z ? (double)s : (double)q);
         PonderSceneBuilder.this.addInstruction(new HighlightValueBoxInstruction(location, expands, duration));
      }

      @Override
      public void showRepeaterScrollInput(BlockPos pos, int duration) {
         float s = 0.0625F;
         float q = 0.16666667F;
         Vec3 expands = new Vec3((double)q, (double)s, (double)q);
         PonderSceneBuilder.this.addInstruction(
            new HighlightValueBoxInstruction(
               PonderSceneBuilder.this.scene.getSceneBuildingUtil().vector().blockSurface(pos, Direction.DOWN).add(0.0, 0.1875, 0.0), expands, duration
            )
         );
      }

      @Override
      public void showFilterSlotInput(Vec3 location, int duration) {
         float s = 0.1F;
         Vec3 expands = new Vec3((double)s, (double)s, (double)s);
         PonderSceneBuilder.this.addInstruction(new HighlightValueBoxInstruction(location, expands, duration));
      }

      @Override
      public void showFilterSlotInput(Vec3 location, Direction side, int duration) {
         location = location.add(Vec3.atLowerCornerOf(side.getNormal()).scale(-0.0234375));
         Vec3 expands = VecHelper.axisAlingedPlaneOf(side).scale(0.0859375);
         PonderSceneBuilder.this.addInstruction(new HighlightValueBoxInstruction(location, expands, duration));
      }

      @Override
      public void showLine(PonderPalette color, Vec3 start, Vec3 end, int duration) {
         PonderSceneBuilder.this.addInstruction(new LineInstruction(color, start, end, duration, false));
      }

      @Override
      public void showBigLine(PonderPalette color, Vec3 start, Vec3 end, int duration) {
         PonderSceneBuilder.this.addInstruction(new LineInstruction(color, start, end, duration, true));
      }

      @Override
      public void showOutline(PonderPalette color, Object slot, Selection selection, int duration) {
         PonderSceneBuilder.this.addInstruction(new OutlineSelectionInstruction(color, slot, selection, duration));
      }
   }

   public class PonderSpecialInstructions implements SpecialInstructions {
      @Override
      public ElementLink<ParrotElement> createBirb(Vec3 location, Supplier<? extends ParrotPose> pose) {
         ElementLink<ParrotElement> link = new ElementLinkImpl<>(ParrotElement.class);
         ParrotElement parrot = ParrotElementImpl.create(location, pose);
         PonderSceneBuilder.this.addInstruction(new CreateParrotInstruction(10, Direction.DOWN, parrot));
         PonderSceneBuilder.this.addInstruction(scene -> scene.linkElement(parrot, link));
         return link;
      }

      @Override
      public void changeBirbPose(ElementLink<ParrotElement> birb, Supplier<? extends ParrotPose> pose) {
         PonderSceneBuilder.this.addInstruction(scene -> scene.resolveOptional(birb).ifPresent(safeBirb -> safeBirb.setPose(pose.get())));
      }

      @Override
      public void movePointOfInterest(Vec3 location) {
         PonderSceneBuilder.this.addInstruction(new MovePoiInstruction(location));
      }

      @Override
      public void movePointOfInterest(BlockPos location) {
         this.movePointOfInterest(VecHelper.getCenterOf(location));
      }

      @Override
      public void rotateParrot(ElementLink<ParrotElement> link, double xRotation, double yRotation, double zRotation, int duration) {
         PonderSceneBuilder.this.addInstruction(AnimateParrotInstruction.rotate(link, new Vec3(xRotation, yRotation, zRotation), duration));
      }

      @Override
      public void moveParrot(ElementLink<ParrotElement> link, Vec3 offset, int duration) {
         PonderSceneBuilder.this.addInstruction(AnimateParrotInstruction.move(link, offset, duration));
      }

      @Override
      public ElementLink<MinecartElement> createCart(Vec3 location, float angle, MinecartElement.MinecartConstructor type) {
         ElementLink<MinecartElement> link = new ElementLinkImpl<>(MinecartElement.class);
         MinecartElement cart = new MinecartElementImpl(location, angle, type);
         PonderSceneBuilder.this.addInstruction(new CreateMinecartInstruction(10, Direction.DOWN, cart));
         PonderSceneBuilder.this.addInstruction(scene -> scene.linkElement(cart, link));
         return link;
      }

      @Override
      public void rotateCart(ElementLink<MinecartElement> link, float yRotation, int duration) {
         PonderSceneBuilder.this.addInstruction(AnimateMinecartInstruction.rotate(link, yRotation, duration));
      }

      @Override
      public void moveCart(ElementLink<MinecartElement> link, Vec3 offset, int duration) {
         PonderSceneBuilder.this.addInstruction(AnimateMinecartInstruction.move(link, offset, duration));
      }

      @Override
      public <T extends AnimatedSceneElement> void hideElement(ElementLink<T> link, Direction direction) {
         PonderSceneBuilder.this.addInstruction(new FadeOutOfSceneInstruction<>(15, direction, link));
      }
   }

   public class PonderWorldInstructions implements WorldInstructions {
      @Override
      public Provider getHolderLookupProvider() {
         return PonderSceneBuilder.this.scene.getWorld().registryAccess();
      }

      @Override
      public void incrementBlockBreakingProgress(BlockPos pos) {
         PonderSceneBuilder.this.addInstruction(scene -> {
            PonderLevel world = scene.getWorld();
            int progress = world.getBlockBreakingProgressions().getOrDefault(pos, -1) + 1;
            if (progress == 9) {
               world.addBlockDestroyEffects(pos, world.getBlockState(pos));
               world.destroyBlock(pos, false);
               world.setBlockBreakingProgress(pos, 0);
               scene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
            } else {
               world.setBlockBreakingProgress(pos, progress + 1);
            }
         });
      }

      @Override
      public void showSection(Selection selection, Direction fadeInDirection) {
         PonderSceneBuilder.this.addInstruction(
            new DisplayWorldSectionInstruction(15, fadeInDirection, selection, PonderSceneBuilder.this.scene::getBaseWorldSection)
         );
      }

      @Override
      public void showSectionAndMerge(Selection selection, Direction fadeInDirection, ElementLink<WorldSectionElement> link) {
         PonderSceneBuilder.this.addInstruction(
            new DisplayWorldSectionInstruction(15, fadeInDirection, selection, () -> PonderSceneBuilder.this.scene.resolve(link))
         );
      }

      @Override
      public void glueBlockOnto(BlockPos position, Direction fadeInDirection, ElementLink<WorldSectionElement> link) {
         PonderSceneBuilder.this.addInstruction(
            new DisplayWorldSectionInstruction(
               15,
               fadeInDirection,
               PonderSceneBuilder.this.scene.getSceneBuildingUtil().select().position(position),
               () -> PonderSceneBuilder.this.scene.resolve(link),
               position
            )
         );
      }

      @Override
      public ElementLink<WorldSectionElement> showIndependentSection(Selection selection, Direction fadeInDirection) {
         DisplayWorldSectionInstruction instruction = new DisplayWorldSectionInstruction(15, fadeInDirection, selection, null);
         PonderSceneBuilder.this.addInstruction(instruction);
         return instruction.createLink(PonderSceneBuilder.this.scene);
      }

      @Override
      public ElementLink<WorldSectionElement> showIndependentSectionImmediately(Selection selection) {
         DisplayWorldSectionInstruction instruction = new DisplayWorldSectionInstruction(0, Direction.DOWN, selection, null);
         PonderSceneBuilder.this.addInstruction(instruction);
         return instruction.createLink(PonderSceneBuilder.this.scene);
      }

      @Override
      public void hideSection(Selection selection, Direction fadeOutDirection) {
         WorldSectionElement worldSectionElement = new WorldSectionElementImpl(selection);
         ElementLink<WorldSectionElement> elementLink = new ElementLinkImpl<>(WorldSectionElement.class);
         PonderSceneBuilder.this.addInstruction(scene -> {
            scene.getBaseWorldSection().erase(selection);
            scene.linkElement(worldSectionElement, elementLink);
            scene.addElement(worldSectionElement);
            worldSectionElement.queueRedraw();
         });
         this.hideIndependentSection(elementLink, fadeOutDirection);
      }

      @Override
      public void hideIndependentSection(ElementLink<WorldSectionElement> link, Direction fadeOutDirection) {
         PonderSceneBuilder.this.addInstruction(new FadeOutOfSceneInstruction<>(15, fadeOutDirection, link));
      }

      @Override
      public void restoreBlocks(Selection selection) {
         PonderSceneBuilder.this.addInstruction(scene -> scene.getWorld().restoreBlocks(selection));
      }

      @Override
      public ElementLink<WorldSectionElement> makeSectionIndependent(Selection selection) {
         WorldSectionElementImpl worldSectionElement = new WorldSectionElementImpl(selection);
         ElementLink<WorldSectionElement> elementLink = new ElementLinkImpl<>(WorldSectionElement.class);
         PonderSceneBuilder.this.addInstruction(scene -> {
            scene.getBaseWorldSection().erase(selection);
            scene.linkElement(worldSectionElement, elementLink);
            scene.addElement(worldSectionElement);
            worldSectionElement.queueRedraw();
            worldSectionElement.resetAnimatedTransform();
            worldSectionElement.setVisible(true);
            worldSectionElement.forceApplyFade(1.0F);
         });
         return elementLink;
      }

      @Override
      public void rotateSection(ElementLink<WorldSectionElement> link, double xRotation, double yRotation, double zRotation, int duration) {
         PonderSceneBuilder.this.addInstruction(AnimateWorldSectionInstruction.rotate(link, new Vec3(xRotation, yRotation, zRotation), duration));
      }

      @Override
      public void configureCenterOfRotation(ElementLink<WorldSectionElement> link, Vec3 anchor) {
         PonderSceneBuilder.this.addInstruction(scene -> scene.resolveOptional(link).ifPresent(safe -> safe.setCenterOfRotation(anchor)));
      }

      @Override
      public void configureStabilization(ElementLink<WorldSectionElement> link, Vec3 anchor) {
         PonderSceneBuilder.this.addInstruction(scene -> scene.resolveOptional(link).ifPresent(safe -> safe.stabilizeRotation(anchor)));
      }

      @Override
      public void moveSection(ElementLink<WorldSectionElement> link, Vec3 offset, int duration) {
         PonderSceneBuilder.this.addInstruction(AnimateWorldSectionInstruction.move(link, offset, duration));
      }

      @Override
      public void setBlocks(Selection selection, BlockState state, boolean spawnParticles) {
         PonderSceneBuilder.this.addInstruction(new ReplaceBlocksInstruction(selection, $ -> state, true, spawnParticles));
      }

      @Override
      public void destroyBlock(BlockPos pos) {
         this.setBlock(pos, Blocks.AIR.defaultBlockState(), true);
      }

      @Override
      public void setBlock(BlockPos pos, BlockState state, boolean spawnParticles) {
         this.setBlocks(PonderSceneBuilder.this.scene.getSceneBuildingUtil().select().position(pos), state, spawnParticles);
      }

      @Override
      public void replaceBlocks(Selection selection, BlockState state, boolean spawnParticles) {
         this.modifyBlocks(selection, $ -> state, spawnParticles);
      }

      @Override
      public void modifyBlock(BlockPos pos, UnaryOperator<BlockState> stateFunc, boolean spawnParticles) {
         this.modifyBlocks(PonderSceneBuilder.this.scene.getSceneBuildingUtil().select().position(pos), stateFunc, spawnParticles);
      }

      @Override
      public void cycleBlockProperty(BlockPos pos, Property<?> property) {
         this.modifyBlocks(
            PonderSceneBuilder.this.scene.getSceneBuildingUtil().select().position(pos),
            s -> s.hasProperty(property) ? (BlockState)s.cycle(property) : s,
            false
         );
      }

      @Override
      public void modifyBlocks(Selection selection, UnaryOperator<BlockState> stateFunc, boolean spawnParticles) {
         PonderSceneBuilder.this.addInstruction(new ReplaceBlocksInstruction(selection, stateFunc, false, spawnParticles));
      }

      @Override
      public void toggleRedstonePower(Selection selection) {
         this.modifyBlocks(selection, s -> {
            if (s.hasProperty(BlockStateProperties.POWER)) {
               s = (BlockState)s.setValue(BlockStateProperties.POWER, s.getValue(BlockStateProperties.POWER) == 0 ? 15 : 0);
            }

            if (s.hasProperty(BlockStateProperties.POWERED)) {
               s = (BlockState)s.cycle(BlockStateProperties.POWERED);
            }

            if (s.hasProperty(RedstoneTorchBlock.LIT)) {
               s = (BlockState)s.cycle(RedstoneTorchBlock.LIT);
            }

            return s;
         }, false);
      }

      @Override
      public <T extends Entity> void modifyEntities(Class<T> entityClass, Consumer<T> entityCallBack) {
         PonderSceneBuilder.this.addInstruction(scene -> scene.forEachWorldEntity(entityClass, entityCallBack));
      }

      @Override
      public <T extends Entity> void modifyEntitiesInside(Class<T> entityClass, Selection area, Consumer<T> entityCallBack) {
         PonderSceneBuilder.this.addInstruction(scene -> scene.forEachWorldEntity(entityClass, e -> {
               if (area.test(e.blockPosition())) {
                  entityCallBack.accept(e);
               }
            }));
      }

      @Override
      public void modifyEntity(ElementLink<EntityElement> link, Consumer<Entity> entityCallBack) {
         PonderSceneBuilder.this.addInstruction(scene -> {
            EntityElement resolve = scene.resolve(link);
            if (resolve != null) {
               resolve.ifPresent(entityCallBack);
            }
         });
      }

      @Override
      public ElementLink<EntityElement> createEntity(Function<Level, Entity> factory) {
         ElementLink<EntityElement> link = new ElementLinkImpl<>(EntityElement.class, UUID.randomUUID());
         PonderSceneBuilder.this.addInstruction(scene -> {
            PonderLevel world = scene.getWorld();
            Entity entity = factory.apply(world);
            EntityElement handle = new EntityElementImpl(entity);
            scene.addElement(handle);
            scene.linkElement(handle, link);
            world.addFreshEntity(entity);
         });
         return link;
      }

      @Override
      public ElementLink<EntityElement> createItemEntity(Vec3 location, Vec3 motion, ItemStack stack) {
         return this.createEntity(world -> {
            ItemEntity itemEntity = new ItemEntity(world, location.x, location.y, location.z, stack);
            itemEntity.setDeltaMovement(motion);
            return itemEntity;
         });
      }

      @Override
      public void modifyBlockEntityNBT(Selection selection, Class<? extends BlockEntity> beType, Consumer<CompoundTag> consumer) {
         this.modifyBlockEntityNBT(selection, beType, consumer, false);
      }

      @Override
      public <T extends BlockEntity> void modifyBlockEntity(BlockPos position, Class<T> beType, Consumer<T> consumer) {
         PonderSceneBuilder.this.addInstruction(scene -> {
            BlockEntity blockEntity = scene.getWorld().getBlockEntity(position);
            if (beType.isInstance(blockEntity)) {
               consumer.accept(beType.cast(blockEntity));
            }
         });
      }

      @Override
      public void modifyBlockEntityNBT(Selection selection, Class<? extends BlockEntity> teType, Consumer<CompoundTag> consumer, boolean reDrawBlocks) {
         PonderSceneBuilder.this.addInstruction(new BlockEntityDataInstruction(selection, teType, nbt -> {
            consumer.accept(nbt);
            return nbt;
         }, reDrawBlocks));
      }
   }
}
