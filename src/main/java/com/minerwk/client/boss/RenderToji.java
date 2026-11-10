package com.minerwk.client.boss;

import com.minerwk.MineRWK;
import com.minerwk.boss.TojiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Desenha o Toji com o modelo, a textura e as animações que vieram do Minezin. */
public final class RenderToji extends GeoEntityRenderer<TojiEntity> {
    private static final ResourceLocation MODELO = MineRWK.id("geo/boss/toji.geo.json");
    private static final ResourceLocation TEXTURA = MineRWK.id("textures/boss/toji.png");
    private static final ResourceLocation ANIMACOES = MineRWK.id("animations/boss/toji.animation.json");

    public RenderToji(EntityRendererProvider.Context contexto) {
        super(contexto, new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(TojiEntity toji) {
                return MODELO;
            }

            @Override
            public ResourceLocation getTextureResource(TojiEntity toji) {
                return TEXTURA;
            }

            @Override
            public ResourceLocation getAnimationResource(TojiEntity toji) {
                return ANIMACOES;
            }
        });
        shadowRadius = .55f;
        // A lâmina e os olhos brilham pelo toji_glowmask.png.
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            public void render(com.mojang.blaze3d.vertex.PoseStack pilha, TojiEntity toji,
                               software.bernie.geckolib.cache.object.BakedGeoModel modelo,
                               net.minecraft.client.renderer.RenderType tipo,
                               net.minecraft.client.renderer.MultiBufferSource buffers,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer, float parcial, int luz, int sobreposicao) {
                if (!toji.isInvisible()) super.render(pilha, toji, modelo, tipo, buffers, buffer, parcial, luz, sobreposicao);
            }
        });
    }

    /** No Desaparecer ele some inteiro, com a sombra e o brilho da lâmina; a animação continua correndo. */
    @Override
    protected float getShadowRadius(TojiEntity toji) {
        return toji.isInvisible() ? 0 : super.getShadowRadius(toji);
    }

    /** Morre com a animação "morte", sem tombar de lado. */
    @Override
    protected float getDeathMaxRotation(TojiEntity toji) {
        return 0;
    }
}
