package net.virus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import net.virus.entities.Helicopter;
import net.virus.entities.Virus;

@Environment(EnvType.CLIENT)
public class VirusClient implements ClientModInitializer {
    public static final EntityModelLayer MODEL_VIRUS_LAYER = new EntityModelLayer(new Identifier("virus", "virus"), "main");
    public static final EntityModelLayer MODEL_HELICOPTER_LAYER = new EntityModelLayer(new Identifier("virus", "helicopter"), "main");

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(Virus.VIRUS, (context) -> {
            return new Virus.VirusEntityRenderer(context);
        });

        EntityModelLayerRegistry.registerModelLayer(MODEL_VIRUS_LAYER, Virus.VirusEntityModel::getTexturedModelData);



        EntityRendererRegistry.INSTANCE.register(Helicopter.HELICOPTER, (context) -> {
            return new Helicopter.HelicopterEntityRenderer(context);
        });

        EntityModelLayerRegistry.registerModelLayer(MODEL_HELICOPTER_LAYER, Helicopter.HelicopterEntityModel::getTexturedModelData);
    }
}