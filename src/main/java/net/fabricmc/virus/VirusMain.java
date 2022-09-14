package net.fabricmc.virus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirusMain implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("virus");

	public class FallDownEffect extends StatusEffect {

		public FallDownEffect() {
			super(
					StatusEffectCategory.HARMFUL, // whether beneficial or harmful for entities
					0xFFFFFF); // color in RGB
		}

		// This method is called every tick to check whether it should apply the status effect or not
		@Override
		public boolean canApplyUpdateEffect(int duration, int amplifier) {
			// In our case, we just make it return true so that it applies the status effect every tick.
			return true;
		}

		// This method is called when it applies the status effect. We implement custom functionality here.
		@Override
		public void applyUpdateEffect(LivingEntity entity, int amplifier) {
			if (entity instanceof PlayerEntity) {
				((PlayerEntity) entity).setMovementSpeed(0.1f);
			}
		}

	}

	public class MaskMaterial implements ArmorMaterial {
		private static final int[] BASE_DURABILITY = new int[] {0, 0, 0, 0};
		private static final int[] PROTECTION_VALUES = new int[] {1, 0, 0, 0};

		@Override
		public int getDurability(EquipmentSlot slot) {
			return BASE_DURABILITY[slot.getEntitySlotId()];
		}

		@Override
		public int getProtectionAmount(EquipmentSlot slot) {
			return PROTECTION_VALUES[slot.getEntitySlotId()];
		}

		@Override
		public int getEnchantability() {
			return 0;
		}

		@Override
		public SoundEvent getEquipSound() {
			return SoundEvents.ITEM_ARMOR_EQUIP_LEATHER;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.ofItems();
		}

		@Override
		public String getName() {
			return "mask";
		}

		@Override
		public float getToughness() {
			return 0F;
		}

		@Override
		public float getKnockbackResistance() {
			return 0F;
		}
	}

	public class Tap extends Block {

		public Tap(Settings settings) {
			super(settings);
			setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
		}

		protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
			stateManager.add(Properties.HORIZONTAL_FACING);
		}

		@Override
		public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
			switch(state.get(Properties.HORIZONTAL_FACING)) {
				case NORTH:
					return VoxelShapes.cuboid(0.0f, 0.0f, 0.5f, 1.0f, 1.0f, 1.0f);
				case SOUTH:
					return VoxelShapes.cuboid(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.5f);
				case EAST:
					return VoxelShapes.cuboid(0.0f, 0.0f, 0.0f, 0.5f, 1.0f, 1.0f);
				case WEST:
					return VoxelShapes.cuboid(0.5f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
			}
			return VoxelShapes.fullCube();
		}

		@Override
		public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {

			return ActionResult.SUCCESS;
		}

		public BlockState getPlacementState(ItemPlacementContext context) {
			return this.getDefaultState().with(Properties.HORIZONTAL_FACING, context.getPlayerFacing());
		}
	}

	public class WaterTap extends Tap {
		public WaterTap(Settings settings) {
			super(settings);
			setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
		}
		public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
			world.playSound(player, pos, SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_WATER, SoundCategory.BLOCKS, 200f, 1f);
			return ActionResult.SUCCESS;
		}
	}

	public class LavaTap extends Tap {
		public LavaTap(Settings settings) {
			super(settings);
			setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
		}
		public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
			world.playSound(player, pos, SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_LAVA, SoundCategory.BLOCKS, 200f, 1f);
			player.damage(DamageSource.ON_FIRE, 1);
			return ActionResult.SUCCESS;
		}
	}

	public void reg() {
		Tap tap = new Tap(FabricBlockSettings.of(Material.METAL).hardness(0.5f));
		WaterTap water_tap = new WaterTap(FabricBlockSettings.of(Material.METAL).hardness(0.5f));
		LavaTap lava_tap = new LavaTap(FabricBlockSettings.of(Material.METAL).hardness(0.5f));
		BlockItem i_tap = new BlockItem(tap, new Item.Settings().group(ItemGroup.DECORATIONS));
		BlockItem i_water_tap = new BlockItem(water_tap, new Item.Settings().group(ItemGroup.DECORATIONS));
		BlockItem i_lava_tap = new BlockItem(lava_tap, new Item.Settings().group(ItemGroup.DECORATIONS));
		ArmorMaterial maskMaterial = new MaskMaterial();
		Item i_mask = new ArmorItem(maskMaterial, EquipmentSlot.HEAD, new Item.Settings().group(ItemGroup.COMBAT));
		Potion i_alcohol = new Potion("alcohol", new StatusEffectInstance(new FallDownEffect(), 12));
		Registry.register(Registry.BLOCK, new Identifier("virus", "tap"), tap);
		Registry.register(Registry.BLOCK, new Identifier("virus", "water_tap"), water_tap);
		Registry.register(Registry.BLOCK, new Identifier("virus", "lava_tap"), lava_tap);
		Registry.register(Registry.ITEM, new Identifier("virus", "tap"), i_tap);
		Registry.register(Registry.ITEM, new Identifier("virus", "water_tap"), i_water_tap);
		Registry.register(Registry.ITEM, new Identifier("virus", "lava_tap"), i_lava_tap);
		Registry.register(Registry.ITEM, new Identifier("virus", "mask"), i_mask);
		Registry.register(Registry.POTION, new Identifier("virus", "alcohol"), i_alcohol);
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		reg();
		LOGGER.info("mod: test");
	}
}