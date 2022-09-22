package net.virus.entities;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Virus implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("virus");

	@Override
	public void onInitialize() {

	}

	public static class VirusEntity extends FlyingEntity implements Monster {

		private final boolean summonable = true;

		public VirusEntity(EntityType<? extends FlyingEntity> entityType, World world) {
			super(entityType, world);
		}
	}

	public static class VirusEntityModel extends EntityModel<VirusEntity> {

		private final ModelPart base;

		public VirusEntityModel(ModelPart modelPart) {
			this.base = modelPart.getChild(EntityModelPartNames.CUBE);
		}

		public static TexturedModelData getTexturedModelData() {
			ModelData modelData = new ModelData();
			ModelPartData modelPartData = modelData.getRoot();
			modelPartData.addChild(EntityModelPartNames.CUBE, ModelPartBuilder.create().uv(0, 0).cuboid(0F, 0F, 0F, 1F, 1F, 1F), ModelTransform.pivot(0F, -10F, 0F));
			return TexturedModelData.of(modelData, 1, 1);
		}

		@Override
		public void setAngles(VirusEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {}

		@Override
		public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
			ImmutableList.of(this.base).forEach((modelRenderer) -> {
				modelRenderer.render(matrices, vertices, light, overlay, red, green, blue, alpha);
			});
		}
	}

	public static EntityType<VirusEntity> VIRUS = Registry.register(
			Registry.ENTITY_TYPE,
			new Identifier("virus", "virus"),
			EntityType.Builder.create(VirusEntity::new, SpawnGroup.MONSTER).setDimensions(0.1F, 0.1F).build("virus")
	);

	public static final Item virus_spawn_egg = new SpawnEggItem(VIRUS, 0xb83dba, 0x880d8a, new Item.Settings().group(ItemGroup.MISC));

	public static class VirusEntityRenderer extends MobEntityRenderer<VirusEntity, VirusEntityModel> {

		public VirusEntityRenderer(EntityRendererFactory.Context context) {
			super(context, new VirusEntityModel(context.getPart(net.virus.VirusClient.MODEL_VIRUS_LAYER)), 0.5f);
		}

		@Override
		public Identifier getTexture(VirusEntity entity) {
			return new Identifier("virus", "textures/entity/virus.png");
		}
	}
}
