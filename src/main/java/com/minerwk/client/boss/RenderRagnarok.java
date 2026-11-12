package com.minerwk.client.boss;

import com.minerwk.MineRWK;
import com.minerwk.boss.RagnarokEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Desenha o Ragnarok: o demônio de sete blocos, preto-azulado, com os membros cor de sangue, as asas e as
 * brasas pelo corpo (modelo, textura e animações gerados por {@code ferramentas/gerar_boss_ragnarok.py}). A
 * asa esquerda é um osso que some quando o Shadow a corta.
 */
public final class RenderRagnarok extends GeoEntityRenderer<RagnarokEntity> {
    private static final ResourceLocation MODELO = MineRWK.id("geo/boss/ragnarok.geo.json");
    private static final ResourceLocation TEXTURA = MineRWK.id("textures/boss/ragnarok.png");
    private static final ResourceLocation ANIMACOES = MineRWK.id("animations/boss/ragnarok.animation.json");

    public RenderRagnarok(EntityRendererProvider.Context contexto) {
        super(contexto, new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(RagnarokEntity ragnarok) {
                return MODELO;
            }

            @Override
            public ResourceLocation getTextureResource(RagnarokEntity ragnarok) {
                return TEXTURA;
            }

            @Override
            public ResourceLocation getAnimationResource(RagnarokEntity ragnarok) {
                return ANIMACOES;
            }

            @Override
            public void setCustomAnimations(RagnarokEntity ragnarok, long instancia, AnimationState<RagnarokEntity> estado) {
                super.setCustomAnimations(ragnarok, instancia, estado);
                var asa = getAnimationProcessor().getBone("asaEsquerda");
                if (asa != null) asa.setHidden(ragnarok.semAsa());
            }
        });
        shadowRadius = 2.2f;
        // Os olhos, a boca e as brasas brilham pelo ragnarok_glowmask.png.
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /** Morre com a animação "morte", sem tombar de lado. */
    @Override
    protected float getDeathMaxRotation(RagnarokEntity ragnarok) {
        return 0;
    }
}
