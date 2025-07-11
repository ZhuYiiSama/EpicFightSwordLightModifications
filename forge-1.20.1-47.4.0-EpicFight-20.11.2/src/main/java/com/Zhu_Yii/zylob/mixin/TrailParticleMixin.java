package com.Zhu_Yii.zylob.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sun.misc.Unsafe;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.client.particle.AbstractTrailParticle;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.List;

@Mixin(AbstractTrailParticle.class)
public abstract class TrailParticleMixin{

    @Unique
    private static final int EMISSIVE_LIGHT_VALUE = 0xE000EA; // 最大发光值

    @Mutable
    @Final
    @Shadow

    protected final TrailInfo trailInfo;

    // 修改粒子渲染类型为发光类型
    @Unique
    public ParticleRenderType getRenderType() {
        return EMISSIVE_RENDER_TYPE;
    }

    @Unique
    private static final ParticleRenderType EMISSIVE_RENDER_TYPE = new ParticleRenderType() {
        @Override
        public void begin(com.mojang.blaze3d.vertex.BufferBuilder builder, net.minecraft.client.renderer.texture.TextureManager textureManager) {
            // 使用统一的渲染设置（非光影状态下的设置）
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(
                    GlStateManager.SourceFactor.SRC_COLOR,
                    GlStateManager.DestFactor.SRC_ALPHA
            );
            RenderSystem.disableCull();

            // 绑定粒子纹理 - 使用原版粒子纹理
            textureManager.bindForSetup(new ResourceLocation("textures/particle/swing_trail.png"));

            // 开始渲染缓冲区
            builder.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(com.mojang.blaze3d.vertex.Tesselator tesselator) {
            tesselator.end();

            // 恢复默认状态
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "EMISSIVE_TRAIL_PARTICLE";
        }
    };

    @Unique
    protected int getLightColor(float partialTick) {
        // 总是使用最大光照值，使粒子发光
        return 0xA000A0;
    }
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;vertex(DDD)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private VertexConsumer enhanceVertexColor(
            VertexConsumer vertexConsumer,
            double x, double y, double z
    ) {
        // 颜色增强系数 (增强颜色但不改变透明度)
        float colorBoost = 1.5f;
        float alphaBoost = 1.2f;

        float r = Math.min(trailInfo.rCol() * colorBoost, 1.0f);
        float g = Math.min(trailInfo.gCol() * colorBoost, 1.0f);
        float b = Math.min(trailInfo.bCol() * colorBoost, 1.0f);
        float a = Math.min(alphaBoost, 1.0f);

        return vertexConsumer
                .vertex(x, y, z)
                .color(r, g, b, a)
                .uv2(EMISSIVE_LIGHT_VALUE);
    }




    protected TrailParticleMixin(ClientLevel level, Object entitypatch, Joint joint, StaticAnimation animation, TrailInfo trailInfo, SpriteSet spriteSet, List<AbstractTrailParticle.TrailEdge> visibleTrailEdges, List<AbstractTrailParticle.TrailEdge> invisibleTrailEdges) {
        this.trailInfo = trailInfo;
    }

    // 添加这个方法确保粒子渲染时使用正确的光照值
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;uv2(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private VertexConsumer forceEmissiveLight(VertexConsumer vertexConsumer, int light) {
        // 总是使用最大光照值
        return vertexConsumer.uv2(EMISSIVE_LIGHT_VALUE);
    }
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V",
            at = @At("HEAD")
    )
    private void preRender(CallbackInfo ci) {
        // 禁用背面剔除，确保粒子始终可见
        RenderSystem.disableCull();

        // 启用深度测试但禁用深度写入
        RenderSystem.depthMask(false);

        // 设置加性混合模式增强发光效果
        RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.SRC_ALPHA
        );
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V",
            at = @At("HEAD")
    )
    private void enhanceRendering(CallbackInfo ci) {
        RenderSystem.disableCull();
        RenderSystem.depthMask(true);

        RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_COLOR,
                GlStateManager.DestFactor.SRC_ALPHA
        );
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void forceEmissiveRendering(CallbackInfo ci) {
        // 尝试通过 Oculus API 强制发光
        if (isOculusShadersEnabled()) {
            try {
                Class<?> oculusClass = Class.forName("net.coderbot.iris.pipeline.WorldRenderingPipeline");
                Object pipeline = oculusClass.getMethod("getInstance").invoke(null);
                if (pipeline != null) {
                    Method setEmissive = pipeline.getClass().getMethod("setEmissive", boolean.class);
                    setEmissive.invoke(pipeline, true);

                    // 设置特殊发光参数
                    Method setCustomParameter = pipeline.getClass().getMethod("setCustomParameter", int.class, float.class);
                    setCustomParameter.invoke(pipeline, 1, 5f);
                    setCustomParameter.invoke(pipeline, 2, 0.5f);
                }
            } catch (Exception e) {
                System.out.println("[Zylob] Failed to set emissive via Oculus API: " + e.getMessage());
            }
        }
    }
    @Unique
    private static boolean isOculusShadersEnabled() {
        try {
            Class<?> irisClass = Class.forName("net.coderbot.iris.Iris");
            Method isShaderPackInUse = irisClass.getMethod("isShaderPackInUse");
            return (Boolean) isShaderPackInUse.invoke(null);
        } catch (Exception e) {
            return false;
        }
    }



    //减少粒子效果持续时间
//    @Inject(
//            method = "tick",
//            at = @At("HEAD")
//    )
//    private void reduceParticleLifetime(CallbackInfo ci) {
//        AbstractTrailParticle<?> particle = (AbstractTrailParticle<?>) (Object) this;
//        particle.setLifetime(Math.max(particle.getLifetime()-1,6));
//    }
}
