package com.minerwk.core.vfx;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.RoxoMaximo;
import org.junit.jupiter.api.Test;

class FusaoRoxoGojoTest {
    private static final int DANCA = RoxoMaximo.TICKS_DANCA;

    @Test
    void dancaFechaAEspiralAteAsEsferasEncostarem() {
        double antes = Double.MAX_VALUE;
        for (int d = 0; d <= FusaoRoxoGojo.encosta(); d++) {
            double sep = FusaoRoxoGojo.separacao(d);
            assertTrue(sep < antes || d == 0, "a espiral só fecha");
            antes = sep;
        }
        assertEquals(2 * FusaoRoxoGojo.ORBITA, FusaoRoxoGojo.separacao(0), 1e-9);
        assertEquals(2 * FusaoRoxoGojo.RAIO_ESFERA, FusaoRoxoGojo.separacao(FusaoRoxoGojo.encosta()), 1e-9, "encostadas");
    }

    @Test
    void toqueParaDoisTicksColadasEDepoisAfundamUmaNaOutra() {
        int e = FusaoRoxoGojo.encosta();
        assertEquals(DANCA, e + FusaoRoxoGojo.TOQUE + FusaoRoxoGojo.AFUNDA);
        for (int d = e; d <= e + FusaoRoxoGojo.TOQUE; d++) {
            assertEquals(2 * FusaoRoxoGojo.RAIO_ESFERA, FusaoRoxoGojo.separacao(d), 1e-9, "paradas coladas");
        }
        assertEquals(0, FusaoRoxoGojo.separacao(DANCA), 1e-9, "somem num ponto no fim da dança");
    }

    @Test
    void duasVoltasEMeiaEmVoltaDoCentroEmLadosOpostos() {
        double voltas = (FusaoRoxoGojo.angulo(FusaoRoxoGojo.encosta()) - FusaoRoxoGojo.angulo(0)) / (2 * Math.PI);
        assertEquals(2.5, voltas, 1e-9);
        for (int d = 0; d <= DANCA; d++) {
            Vetor azul = FusaoRoxoGojo.posicao(-1, d), verm = FusaoRoxoGojo.posicao(1, d);
            assertEquals(Vetor.ZERO, azul.mais(verm), "simétricas pelo centro");
            assertEquals(FusaoRoxoGojo.separacao(d), azul.distancia(verm), 1e-9);
        }
    }

    @Test
    void comecaOndeOArcoTermina() {
        // A viagem em arco chega com o azul à esquerda e o vermelho à direita, na distância da órbita.
        assertEquals(new Vetor(-FusaoRoxoGojo.ORBITA, 0, 0), FusaoRoxoGojo.posicao(-1, 0));
        assertEquals(new Vetor(FusaoRoxoGojo.ORBITA, 0, 0), FusaoRoxoGojo.posicao(1, 0));
    }

    @Test
    void esticamEmGotaQuantoMaisPerto() {
        assertEquals(1, FusaoRoxoGojo.alongamento(2 * FusaoRoxoGojo.ORBITA), 1e-9, "longe: redondas");
        assertEquals(FusaoRoxoGojo.GOTA_MAX, FusaoRoxoGojo.alongamento(2 * FusaoRoxoGojo.RAIO_ESFERA), 1e-9);
        assertTrue(FusaoRoxoGojo.alongamento(1.5) > FusaoRoxoGojo.alongamento(2.5));
    }

    @Test
    void fileteVaiDoCianoAoCarmesimPassandoPeloVioleta() {
        assertEquals(FusaoRoxoGojo.CIANO, FusaoRoxoGojo.corFilete(0));
        assertEquals(FusaoRoxoGojo.VIOLETA, FusaoRoxoGojo.corFilete(.5));
        assertEquals(FusaoRoxoGojo.CARMESIM, FusaoRoxoGojo.corFilete(1));
    }

    @Test
    void nucleoNascePequenoCresceEEncolheNoSilencio() {
        int carga = RoxoMaximo.TICKS_FUSAO, silencio = carga - RoxoMaximo.TICKS_SILENCIO;
        assertEquals(FusaoRoxoGojo.NUCLEO_NASCE, FusaoRoxoGojo.raioNucleo(0), 1e-9);
        for (int f = 1; f <= silencio; f++) assertTrue(FusaoRoxoGojo.raioNucleo(f) > FusaoRoxoGojo.raioNucleo(f - 1));
        for (int f = silencio + 1; f <= carga; f++) assertTrue(FusaoRoxoGojo.raioNucleo(f) < FusaoRoxoGojo.raioNucleo(f - 1), "encolhe");
        assertEquals(0, FusaoRoxoGojo.silencio(silencio), 1e-9);
        assertEquals(1, FusaoRoxoGojo.silencio(carga), 1e-9);
    }

    @Test
    void pulsoAceleraEParaNoSilencio() {
        assertTrue(FusaoRoxoGojo.frequencia(30) > 2 * FusaoRoxoGojo.frequencia(0), "pulsa cada vez mais rápido");
        int silencio = RoxoMaximo.TICKS_FUSAO - RoxoMaximo.TICKS_SILENCIO;
        assertEquals(1, FusaoRoxoGojo.pulso(silencio + 2), 1e-9, "no silêncio não pulsa");
    }

    @Test
    void ondaDeChoqueCorreOChaoAte60NaExplosao() {
        assertEquals(0, FusaoRoxoGojo.raioChoque(0), 1e-9);
        assertEquals(FusaoRoxoGojo.RAIO_CHOQUE, FusaoRoxoGojo.raioChoque(RoxoMaximo.TICKS_EXPLOSAO), 1e-9);
        assertEquals(60, FusaoRoxoGojo.RAIO_CHOQUE, 1e-9);
        assertTrue(FusaoRoxoGojo.raioChoque(5) > RoxoMaximo.RAIO_EXPLOSAO * 5 / RoxoMaximo.TICKS_EXPLOSAO, "sai na frente da casca");
    }

    @Test
    void ceuEscureceRapidoEVoltaDevagar() {
        double c = 0;
        for (int i = 0; i < 20; i++) c = FusaoRoxoGojo.ceu(c, 1);
        assertTrue(c > .9, "escurece em 1 s");
        int ticks = 0;
        while (c > .05) {
            c = FusaoRoxoGojo.ceu(c, 0);
            ticks++;
        }
        assertTrue(ticks >= 80, "volta devagar (" + ticks + ")");
    }
}
