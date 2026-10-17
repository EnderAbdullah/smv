package com.enderabdullah.structuremover.client;

import com.enderabdullah.structuremover.StructureMoverMod;
import com.enderabdullah.structuremover.entity.VehicleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class VehicleRenderer extends EntityRenderer<VehicleEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(StructureMoverMod.MOD_ID, "textures/block/vehicle_assembler.png");

    public VehicleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(VehicleEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()) - entity.getBaseYaw();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        for (VehicleEntity.Part part : entity.getParts()) {
            poseStack.pushPose();
            poseStack.translate(part.offset().getX(), part.offset().getY(), part.offset().getZ());
            dispatcher.renderSingleBlock(part.state(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(VehicleEntity entity) {
        return TEXTURE;
    }
}
