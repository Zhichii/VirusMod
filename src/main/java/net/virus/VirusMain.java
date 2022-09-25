package net.virus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.mixin.object.builder.DefaultAttributeRegistryAccessor;
import net.minecraft.block.*;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.BlockSoundGroup;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.virus.entities.Virus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.virus.entities.Helicopter.helicopter;
import static net.virus.entities.Virus.virus_spawn_egg;

public class VirusMain implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("virus");

	public static class FaintEffect extends StatusEffect {

		public FaintEffect() {
			super(StatusEffectCategory.HARMFUL, 0xFFFFFF);
		}

		@Override
		public boolean canApplyUpdateEffect(int duration, int amplifier) {
			return true;
		}

		@Override
		public void applyUpdateEffect(LivingEntity entity, int amplifier) {
			entity.setMovementSpeed(0.0F);
		}

	}

	public class MaskMaterial implements ArmorMaterial {
		private static final int[] BASE_DURABILITY = new int[] {0, 0, 0, 4};
		private static final int[] PROTECTION_VALUES = new int[] {12, 0, 0, 0};

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
		public Ingredient getRepairIngredient() { return Ingredient.ofItems(Items.WHITE_WOOL); }

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
					return VoxelShapes.cuboid(0.0f, 0.0f, 0.5f, 1.0f, 1.0f, 1.1f);
				case SOUTH:
					return VoxelShapes.cuboid(0.0f, 0.0f, -0.1f, 1.0f, 1.0f, 0.5f);
				case EAST:
					return VoxelShapes.cuboid(-0.1f, 0.0f, 0.0f, 0.5f, 1.0f, 1.0f);
				case WEST:
					return VoxelShapes.cuboid(0.5f, 0.0f, 0.0f, 1.1f, 1.0f, 1.0f);
			}
			return VoxelShapes.fullCube();
		}

		@Override
		public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
			world.setBlockState(pos, tap.getStateWithProperties(state));
			return ActionResult.SUCCESS;
		}

		public BlockState getPlacementState(ItemPlacementContext context) {
			return this.getDefaultState().with(Properties.HORIZONTAL_FACING, context.getPlayerFacing());
		}

		@Override
		public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
			world.spawnEntity(new ItemEntity((World)world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(this.asItem())));
		}

		@Override
		public Item asItem() {
			return i_tap;
		}
	}

	public class WaterTap extends Tap {

		public WaterTap(Settings settings) {
			super(settings);
			setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
		}

		@Override
		public Item asItem() {
			return i_water_tap;
		}

	}

	public class LavaTap extends Tap {

		public LavaTap(Settings settings) {
			super(settings);
			setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
		}

		public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
			world.setBlockState(pos, tap.getStateWithProperties(state));
			world.playSound(player, pos, SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_LAVA, SoundCategory.BLOCKS, 200f, 1f);
			player.damage(DamageSource.ON_FIRE, 1);
			return ActionResult.SUCCESS;
		}

		@Override
		public Item asItem() {
			return i_lava_tap;
		}

	}

	public class SlowFaller extends Item {

		public SlowFaller(Settings settings) {
			super(settings);
		}

		public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
			if ( selected || (slot == EquipmentSlot.OFFHAND.getEntitySlotId()) ) {
				if (entity instanceof PlayerEntity) {
					((PlayerEntity)entity).addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 2));
				}
			}
		}

	}

	public Block.Settings tap_settings = Block.Settings.of(Material.METAL, MapColor.IRON_GRAY).requiresTool().strength(5.0F, 6.0F).sounds(BlockSoundGroup.METAL);
	public Tap tap = new Tap(tap_settings);
	public WaterTap water_tap = new WaterTap(tap_settings);
	public LavaTap lava_tap = new LavaTap(tap_settings);
	public Item.Settings i_tap_settings = new Item.Settings().group(ItemGroup.DECORATIONS);
	public BlockItem i_tap = new BlockItem(tap, i_tap_settings);
	public BlockItem i_water_tap = new BlockItem(water_tap, i_tap_settings);
	public BlockItem i_lava_tap = new BlockItem(lava_tap, i_tap_settings);
	public ArmorMaterial maskMaterial = new MaskMaterial();
	public Item mask = new ArmorItem(maskMaterial, EquipmentSlot.HEAD, new Item.Settings().group(ItemGroup.COMBAT));
	public SlowFaller slow_faller = new SlowFaller(new Item.Settings().group(ItemGroup.TOOLS));
	public static StatusEffect faint = new FaintEffect();
	public Potion p_faint = new Potion("alcohol", new StatusEffectInstance(faint, 60, 1, true, true, true));

	public void reg() {
		DefaultAttributeRegistryAccessor.getRegistry().put(Virus.VIRUS, Virus.VirusEntity.createMobAttributes().add(EntityAttributes.GENERIC_ATTACK_DAMAGE).build());
		Registry.register(Registry.BLOCK, new Identifier("virus", "tap"), tap);
		Registry.register(Registry.BLOCK, new Identifier("virus", "water_tap"), water_tap);
		Registry.register(Registry.BLOCK, new Identifier("virus", "lava_tap"), lava_tap);
		Registry.register(Registry.ITEM, new Identifier("virus", "tap"), i_tap);
		Registry.register(Registry.ITEM, new Identifier("virus", "water_tap"), i_water_tap);
		Registry.register(Registry.ITEM, new Identifier("virus", "lava_tap"), i_lava_tap);
		Registry.register(Registry.ITEM, new Identifier("virus", "mask"), mask);
		Registry.register(Registry.ITEM, new Identifier("virus", "slow_faller"), slow_faller);
		Registry.register(Registry.ITEM, new Identifier("virus", "virus_spawn_egg"), virus_spawn_egg);
		Registry.register(Registry.ITEM, new Identifier("virus", "helicopter"), helicopter);
		Registry.register(Registry.STATUS_EFFECT, new Identifier("virus", "faint"), faint);
		Registry.register(Registry.POTION, new Identifier("virus", "alcohol"), p_faint);
	}

	@Override
	public void onInitialize() {
		reg();
		LOGGER.info("mod: test");
	}
}
