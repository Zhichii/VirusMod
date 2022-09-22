package net.virus.entities;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.GameRules;
import net.minecraft.world.MobSpawnerLogic;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.virus.VirusClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class Helicopter implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("virus");

	@Override
	public void onInitialize() {

	}

	public static class HelicopterEntity extends Entity implements JumpingMount {

		private final boolean summonable = true;

		public HelicopterEntity(EntityType<? extends HelicopterEntity> entityType, World world) {
			super(entityType, world);
		}

		@Override
		protected void initDataTracker() { }

		@Override
		protected void readCustomDataFromNbt(NbtCompound nbt) { }

		@Override
		protected void writeCustomDataToNbt(NbtCompound nbt) { }

		@Override
		public Packet<?> createSpawnPacket() {
			return new EntitySpawnS2CPacket(this);
		}

		@Override
		public ActionResult interact(PlayerEntity player, Hand hand) {
			if (player.shouldCancelInteraction()) {
				return ActionResult.PASS;
			} else {
				if (!this.world.isClient) {
					return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;
				} else {
					return ActionResult.SUCCESS;
				}
			}
		}

		public boolean canBeControlledByRider() {
			return true;
		}

		public void tick() {
			if (this.isAlive()) {
				if (this.hasPassengers() && this.canBeControlledByRider()) {
					PlayerEntity livingEntity = (PlayerEntity)this.getFirstPassenger();
					this.setYaw(livingEntity.getYaw());
					this.prevYaw = this.getYaw();
					this.setPitch(livingEntity.getPitch() * 0.5F);
					this.setRotation(this.getYaw(), this.getPitch());
					float f = livingEntity.sidewaysSpeed * 0.5F;
					float u = livingEntity.upwardSpeed;
					float g = livingEntity.forwardSpeed;
					this.travel(new Vec3d(f, u, g));

					this.tryCheckBlockCollision();
				}
			}
		}

		public void travel(Vec3d movementInput) {
			if (this.isTouchingWater()) {
				this.updateVelocity(0.02F, movementInput);
				this.move(MovementType.SELF, this.getVelocity());
				this.setVelocity(this.getVelocity().multiply(0.800000011920929));
			} else if (this.isInLava()) {
				this.updateVelocity(0.02F, movementInput);
				this.move(MovementType.SELF, this.getVelocity());
				this.setVelocity(this.getVelocity().multiply(0.5));
			} else {
				float f = 0.91F;
				if (this.onGround) {
					f = this.world.getBlockState(new BlockPos(this.getX(), this.getY() - 1.0, this.getZ())).getBlock().getSlipperiness() * 0.91F;
				}

				float g = 0.16277137F / (f * f * f);
				f = 0.91F;
				if (this.onGround) {
					f = this.world.getBlockState(new BlockPos(this.getX(), this.getY() - 1.0, this.getZ())).getBlock().getSlipperiness() * 0.91F;
				}

				this.updateVelocity(this.onGround ? 0.04F * g : 0.08F, movementInput);
				this.move(MovementType.SELF, this.getVelocity());
				this.setVelocity(this.getVelocity().multiply(f));
			}
		}

		@Override
		public void setJumpStrength(int strength) { }

		@Override
		public boolean canJump() { return true; }

		@Override
		public void startJumping(int height) { }

		public void stopJumping() { }

		public boolean isCollidable() {
			return false;
		}

		public boolean isPushable() {
			return true;
		}

		protected Vec3d positionInPortal(Direction.Axis portalAxis, BlockLocating.Rectangle portalRect) {
			return LivingEntity.positionInPortal(super.positionInPortal(portalAxis, portalRect));
		}

		public double getMountedHeightOffset() {
			return -0.1;
		}

		public boolean damage(DamageSource source, float amount) {
			if (this.isInvulnerableTo(source)) {
				return false;
			} else if (!this.world.isClient && !this.isRemoved()) {
				this.scheduleVelocityUpdate();
				this.emitGameEvent(GameEvent.ENTITY_DAMAGED, source.getAttacker());
				if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
					this.dropItem(this.asItem());
					this.remove(RemovalReason.KILLED);
				}
				return true;
			}
			return true;
		}

		public void pushAwayFrom(Entity entity) {
			if (entity instanceof HelicopterEntity) {
				if (entity.getBoundingBox().minY < this.getBoundingBox().maxY) {
					super.pushAwayFrom(entity);
				}
			} else if (entity.getBoundingBox().minY <= this.getBoundingBox().minY) {
				super.pushAwayFrom(entity);
			}

		}

		public Item asItem() {
			return helicopter;
		}

		public boolean collides() {
			return !this.isRemoved();
		}

		public Direction getMovementDirection() {
			return this.getHorizontalFacing().rotateYClockwise();
		}

	}

	public static class HelicopterItem extends Item {

		public HelicopterItem(Settings settings) {
			super(settings);
		}

		public ActionResult useOnBlock(ItemUsageContext context) {
			World world = context.getWorld();
			if (!(world instanceof ServerWorld)) {
				return ActionResult.SUCCESS;
			} else {
				ItemStack itemStack = context.getStack();
				BlockPos blockPos = context.getBlockPos();
				Direction direction = context.getSide();
				BlockState blockState = world.getBlockState(blockPos);
				if (blockState.isOf(Blocks.SPAWNER)) {
					BlockEntity blockEntity = world.getBlockEntity(blockPos);
					if (blockEntity instanceof MobSpawnerBlockEntity) {
						MobSpawnerLogic mobSpawnerLogic = ((MobSpawnerBlockEntity)blockEntity).getLogic();
						EntityType<?> entityType = HELICOPTER;
						mobSpawnerLogic.setEntityId(entityType);
						blockEntity.markDirty();
						world.updateListeners(blockPos, blockState, blockState, 3);
						itemStack.decrement(1);
						return ActionResult.CONSUME;
					}
				}

				BlockPos blockPos2;
				if (blockState.getCollisionShape(world, blockPos).isEmpty()) {
					blockPos2 = blockPos;
				} else {
					blockPos2 = blockPos.offset(direction);
				}

				EntityType<?> entityType2 = HELICOPTER;
				if (entityType2.spawnFromItemStack((ServerWorld)world, itemStack, context.getPlayer(), blockPos2, SpawnReason.SPAWN_EGG, true, !Objects.equals(blockPos, blockPos2) && direction == Direction.UP) != null) {
					itemStack.decrement(1);
					world.emitGameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, blockPos);
				}

				return ActionResult.CONSUME;
			}
		}

	}

	public static class HelicopterEntityModel extends EntityModel<HelicopterEntity> {

		private final ModelPart top;
		private final ModelPart bottom;
		private final ModelPart right;
		private final ModelPart left;
		private final ModelPart back;
		private final ModelPart front;
		private final ModelPart fanZ;
		private final ModelPart fan1;
		private final ModelPart fan2;
		private final ModelPart leg1;
		private final ModelPart leg2;
		private final ModelPart foot1;
		private final ModelPart foot2;

		public HelicopterEntityModel(ModelPart modelPart) {
			this.top = modelPart.getChild("top");
			this.bottom = modelPart.getChild("bottom");
			this.right = modelPart.getChild("right");
			this.left = modelPart.getChild("left");
			this.back = modelPart.getChild("back");
			this.front = modelPart.getChild("front");
			this.fanZ = modelPart.getChild("fanZ");
			this.fan1 = modelPart.getChild("fan1");
			this.fan2 = modelPart.getChild("fan2");
			this.leg1 = modelPart.getChild("leg1");
			this.leg2 = modelPart.getChild("leg2");
			this.foot1 = modelPart.getChild("foot1");
			this.foot2 = modelPart.getChild("foot2");
		}

		public static TexturedModelData getTexturedModelData() {
			ModelData modelData = new ModelData();
			ModelPartData modelPartData = modelData.getRoot();
			modelPartData.addChild("top", ModelPartBuilder.create().uv(0, 0).cuboid(-8F, 13F, -12F, 16F, 1F, 24F), ModelTransform.pivot(0F, 0F, 0F));
			modelPartData.addChild("bottom", ModelPartBuilder.create().uv(0, 0).cuboid(-8F, -1F, -12F, 16F, 1F, 24F), ModelTransform.pivot(0F, 0F, 0F));
			modelPartData.addChild("right", ModelPartBuilder.create().uv(0, 0).cuboid(0F, 0F, 0F, 1F, 1F, 1F), ModelTransform.pivot(0F, 0F, 0F));
			modelPartData.addChild("left", ModelPartBuilder.create().uv(0, 0).cuboid(0F, 0F, 0F, 1F, 1F, 1F), ModelTransform.pivot(0F, 0F, 0F));
			modelPartData.addChild("back", ModelPartBuilder.create().uv(0, 0).cuboid(0F, 0F, 0F, 1F, 1F, 1F), ModelTransform.pivot(0F, 0F, 0F));
			modelPartData.addChild("front", ModelPartBuilder.create().uv(0, 25).cuboid(-8F, -1F, 11F, 16F, 8F, 1F), ModelTransform.pivot(0F, 0F, 0F));
			modelPartData.addChild("fanZ", ModelPartBuilder.create().uv(0, 0).cuboid(0F, 0F, 0F, 1F, 1F, 1F), ModelTransform.pivot(0F, 0F, 0F));
			modelPartData.addChild("fan1", ModelPartBuilder.create().uv(0, 0).cuboid(0F, 0F, 0F, 1F, 1F, 1F), ModelTransform.pivot(0F, 0F, 0F));
			modelPartData.addChild("fan2", ModelPartBuilder.create().uv(0, 0).cuboid(0F, 0F, 0F, 1F, 1F, 1F), ModelTransform.pivot(0F, 0F, 0F));
			modelPartData.addChild("leg1", ModelPartBuilder.create().uv(0, 0).cuboid(0F, 0F, 0F, 1F, 1F, 1F), ModelTransform.pivot(0F, 0F, 0F));
			modelPartData.addChild("leg2", ModelPartBuilder.create().uv(0, 0).cuboid(0F, 0F, 0F, 1F, 1F, 1F), ModelTransform.pivot(0F, 0F, 0F));
			modelPartData.addChild("foot1", ModelPartBuilder.create().uv(0, 0).cuboid(0F, 0F, 0F, 1F, 1F, 1F), ModelTransform.pivot(0F, 0F, 0F));
			modelPartData.addChild("foot2", ModelPartBuilder.create().uv(0, 0).cuboid(0F, 0F, 0F, 1F, 1F, 1F), ModelTransform.pivot(0F, 0F, 0F));
			return TexturedModelData.of(modelData, 128, 128);
		}

		@Override
		public void setAngles(HelicopterEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {}

		@Override
		public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
			ImmutableList.of(
					this.top,
					this.bottom,
					this.right,
					this.left,
					this.back,
					this.front,
					this.fanZ,
					this.fan1,
					this.fan2,
					this.leg1,
					this.leg2,
					this.foot1,
					this.foot2
			).forEach((modelRenderer) -> {
				modelRenderer.render(matrices, vertices, light, overlay, red, green, blue, alpha);
			});
		}
	}

	public static EntityType<HelicopterEntity> HELICOPTER = Registry.register(
			Registry.ENTITY_TYPE,
			new Identifier("virus", "helicopter"),
			EntityType.Builder.create(HelicopterEntity::new, SpawnGroup.MISC).setDimensions(0.75F, 0.75F).build("helicopter")
	);

	public static final Item helicopter = new HelicopterItem(new Item.Settings().group(ItemGroup.MISC));

	public static class HelicopterEntityRenderer extends EntityRenderer<HelicopterEntity> {


		private static final Identifier TEXTURE = new Identifier("virus", "textures/entity/helicopter.png");
		private final HelicopterEntityModel model;

		public HelicopterEntityRenderer(EntityRendererFactory.Context context) {
			super(context);
			this.model = new HelicopterEntityModel(context.getPart(VirusClient.MODEL_HELICOPTER_LAYER));
		}

		@Override
		public Identifier getTexture(HelicopterEntity entity) {
			return TEXTURE;

		}

		public void render(HelicopterEntity helicopterEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
			matrixStack.push();
			matrixStack.scale(-1.0F, -1.0F, 1.0F);
			this.model.setAngles(helicopterEntity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
			VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(this.model.getLayer(TEXTURE));
			this.model.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
			matrixStack.pop();
			super.render(helicopterEntity, f, g, matrixStack, vertexConsumerProvider, i);
		}
	}
}
