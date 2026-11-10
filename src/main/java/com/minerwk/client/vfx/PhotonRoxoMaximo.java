package com.minerwk.client.vfx;

import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.RoxoMaximo;
import com.minerwk.core.vfx.FusaoRoxoGojo;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * As camadas do Photon2 no R (Vazio Roxo máximo): partículas em quantidade e o brilho HDR (bloom) que o
 * desenho à mão de {@link RenderVisuaisGojo} não tem. As formas exatas (esferas, dança, núcleo) ficam lá.
 */
public final class PhotonRoxoMaximo {
    private PhotonRoxoMaximo() {
    }

    private static Vec3 vec(Vetor v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    /** O núcleo nasceu: sucção de ar e poeira para dentro e um halo HDR pulsando até o silêncio. */
    public static void carga(Level nivel, Vetor centro) {
        Vec3 c = vec(centro);
        int ate = RoxoMaximo.TICKS_FUSAO - RoxoMaximo.TICKS_SILENCIO;
        // Sucção: nasce numa casca de 7 blocos e corre para o centro, com rastro.
        FxCodigo.emissor(ate).taxa(90).esfera(7, .05f).velocidade(-7, -5).vida(18, 22).tamanho(.12f)
                .cor(0xFFC8A0FF).rastro(.35f).textura(FxCodigo.CIRCULO, .9f, .4f, 1.6f, FxCodigo.ADITIVO).tocar(nivel, c);
        // Poeira grossa sendo puxada, mais lenta e sem brilho.
        FxCodigo.emissor(ate).taxa(25).esfera(9, .1f).velocidade(-5, -3).vida(30, 40).tamanho(.9f)
                .cor(0xA0907088).textura(FxCodigo.FUMACA, 0, 0, 0, FxCodigo.TRANSLUCIDO).tocar(nivel, c);
        // Halo HDR: o bloom em volta do núcleo.
        FxCodigo.emissor(ate).rajada(0, 1).vida(ate, ate).velocidade(0, 0).tamanho(7)
                .cor(0xFFB070FF).textura(FxCodigo.CIRCULO, 1.1f, .35f, 2.2f, FxCodigo.ADITIVO).tocar(nivel, c);
    }

    /** O silêncio: um clarão branco HDR que cresce no ponto do núcleo. */
    public static void silencio(Level nivel, Vetor centro) {
        FxCodigo.emissor(RoxoMaximo.TICKS_SILENCIO + 2).rajada(0, 1).vida(RoxoMaximo.TICKS_SILENCIO + 2, RoxoMaximo.TICKS_SILENCIO + 2)
                .velocidade(0, 0).tamanho(10).cor(0xFFFFFFFF).textura(FxCodigo.CIRCULO, 2.5f, 2.2f, 3f, FxCodigo.ADITIVO)
                .tocar(nivel, vec(centro));
    }

    /**
     * O estouro: clarão HDR enorme, faíscas roxas com rastro, detritos com gravidade que batem no chão,
     * anel de poeira correndo pelo chão, fumaça da cratera e partículas roxas pairando.
     */
    public static void explosao(Level nivel, Vetor centro, double raio) {
        Vec3 c = vec(centro);
        float r = (float) raio;
        FxCodigo.emissor(10).rajada(0, 1).vida(10, 10).velocidade(0, 0).tamanho(r * 1.4f)
                .cor(0xFFFFFFFF).textura(FxCodigo.CIRCULO, 3f, 2.4f, 4f, FxCodigo.ADITIVO).tocar(nivel, c);
        // Fitas de energia saindo em raios.
        FxCodigo.emissor(20).rajada(0, 70).esfera(1, 1).velocidade(45, 70).vida(14, 20).tamanho(.25f)
                .cor(0xFFC890FF).rastro(.5f).textura(FxCodigo.CIRCULO, 1.4f, .5f, 2.6f, FxCodigo.ADITIVO).tocar(nivel, c);
        // Mais duas levas de fitas enquanto a casca cresce.
        FxCodigo.emissor(40).rajada(12, 50).esfera(r * .3f, 1).velocidade(30, 50).vida(16, 24).tamanho(.22f)
                .cor(0xFFE0C8FF).rastro(.5f).textura(FxCodigo.CIRCULO, 1.6f, .8f, 2.6f, FxCodigo.ADITIVO).tocar(nivel, c);
        FxCodigo.emissor(40).rajada(28, 40).esfera(r * .5f, 1).velocidade(20, 35).vida(18, 26).tamanho(.2f)
                .cor(0xFFC890FF).rastro(.5f).textura(FxCodigo.CIRCULO, 1.4f, .5f, 2.6f, FxCodigo.ADITIVO).tocar(nivel, c);
        // Faíscas roxas caindo em chuva depois do estouro.
        FxCodigo.emissor(90).taxa(30).esfera(r * .8f, 1).velocidade(1, 4).vida(40, 70).tamanho(.14f)
                .cor(0xFFD0A0FF).gravidade(.35f, true).rastro(.3f)
                .textura(FxCodigo.CIRCULO, 1.2f, .5f, 2.2f, FxCodigo.ADITIVO).tocar(nivel, c);
        // Detritos: pedaços escuros voando para fora e caindo.
        FxCodigo.emissor(80).rajada(0, 90).esfera(r * .3f, 1).velocidade(12, 28).vida(50, 80).tamanho(.35f)
                .cor(0xFF3A302C).gravidade(1.2f, true).textura(FxCodigo.CIRCULO, 0, 0, 0, FxCodigo.TRANSLUCIDO).tocar(nivel, c);
        // Onda de choque no chão: poeira correndo até o raio do anel.
        Vec3 chao = c.add(0, -Math.min(raio * .5, 20), 0);
        float vel = (float) (FusaoRoxoGojo.RAIO_CHOQUE / (RoxoMaximo.TICKS_EXPLOSAO / 20.0));
        FxCodigo.emissor(40).rajada(0, 160).circulo(2, 0).velocidade(vel * .8f, vel).vida(30, 40).tamanho(2.5f)
                .cor(0xC0B0A0B0).textura(FxCodigo.FUMACA, 0, 0, 0, FxCodigo.TRANSLUCIDO).tocar(nivel, chao);
        // Partículas roxas pairando no ar.
        FxCodigo.emissor(20).rajada(0, 120).esfera(r, 1).velocidade(.1f, .5f).vida(80, 120).tamanho(.18f)
                .cor(0xFFB070FF).textura(FxCodigo.CIRCULO, .8f, .3f, 1.6f, FxCodigo.ADITIVO).tocar(nivel, c);
    }

    /** A fumaça da cratera, por 5 s. Só sai depois de a explosão crescer, para não cobrir a casca. */
    public static void fumaca(Level nivel, Vetor centro, double raio) {
        float r = (float) raio;
        Vec3 chao = vec(centro).add(0, -Math.min(raio * .5, 20) + 2, 0);
        FxCodigo.emissor(100).taxa(20).esfera(r * .6f, 1).velocidade(.5f, 1.5f).vida(60, 90).tamanho(4)
                .cor(0xB0A898A8).textura(FxCodigo.FUMACA, 0, 0, 0, FxCodigo.TRANSLUCIDO).tocar(nivel, chao);
    }

    /** Uma camada da casca de distorção (fresnel, linha no terreno e o ar ondulando atrás), com o raio do tick. */
    public static void casca(Level nivel, Vetor centro, double raio) {
        FxCodigo.emissor(4).rajada(0, 1).vida(4, 4).velocidade(0, 0).tamanho((float) raio * 2)
                .cor(0xFFFFFFFF).esferaComShader("casca_roxo").tocar(nivel, vec(centro));
    }
}
