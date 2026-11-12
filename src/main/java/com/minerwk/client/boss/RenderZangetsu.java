package com.minerwk.client.boss;

import com.minerwk.MineRWK;
import com.minerwk.boss.ZangetsuEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Desenha o Zangetsu interior: o traje do Bankai em branco sobre um corpo próprio (gerado por
 * {@code ferramentas/gerar_boss_zangetsu.py}). A máscara Hollow é um osso escondido até ele a vestir, e a
 * espada some da mão enquanto está arremessada (quem a desenha no ar é o visual do golpe).
 */
public final class RenderZangetsu extends GeoEntityRenderer<ZangetsuEntity> {
    private static final ResourceLocation MODELO = MineRWK.id("geo/boss/zangetsu.geo.json");
    private static final ResourceLocation TEXTURA = MineRWK.id("textures/boss/zangetsu.png");
    private static final ResourceLocation ANIMACOES = MineRWK.id("animations/boss/zangetsu.animation.json");

    public RenderZangetsu(EntityRendererProvider.Context contexto) {
        super(contexto, new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(ZangetsuEntity zangetsu) {
                return MODELO;
            }

            @Override
            public ResourceLocation getTextureResource(ZangetsuEntity zangetsu) {
                return TEXTURA;
            }

            @Override
            public ResourceLocation getAnimationResource(ZangetsuEntity zangetsu) {
                return ANIMACOES;
            }

            @Override
            public void setCustomAnimations(ZangetsuEntity zangetsu, long instancia, AnimationState<ZangetsuEntity> estado) {
                super.setCustomAnimations(zangetsu, instancia, estado);
                var mascara = getAnimationProcessor().getBone("mascara");
                if (mascara != null) mascara.setHidden(!zangetsu.mascarado());
                var espada = getAnimationProcessor().getBone("arma_root");
                if (espada != null) espada.setHidden(zangetsu.semEspada());
            }
        });
        shadowRadius = .55f;
        // Os olhos brilham pelo zangetsu_glowmask.png.
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /** Morre com a animação "morte", sem tombar de lado. */
    @Override
    protected float getDeathMaxRotation(ZangetsuEntity zangetsu) {
        return 0;
    }
}
