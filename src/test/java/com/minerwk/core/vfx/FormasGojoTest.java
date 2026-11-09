package com.minerwk.core.vfx;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.Vetor;
import org.junit.jupiter.api.Test;

class FormasGojoTest {

    @Test
    void opacidadeSempreEntreZeroEUm() {
        for (int dur : new int[]{20, 30, 120, 160}) {
            for (double t = -5; t <= dur + 5; t += 0.5) {
                double o = PerfilVfxGojo.opacidade(t, dur);
                assertTrue(o >= 0 && o <= 1, "opacidade " + o + " em t=" + t);
            }
            assertEquals(0, PerfilVfxGojo.opacidade(-1, dur));
            assertEquals(0, PerfilVfxGojo.opacidade(dur, dur), 1e-9);
            assertEquals(1, PerfilVfxGojo.opacidade(dur / 2.0, dur), 1e-9);
        }
    }

    @Test
    void perfilReduzidoTemMenosDetalhe() {
        var completo = PerfilVfxGojo.detalhe(false);
        var reduzido = PerfilVfxGojo.detalhe(true);
        assertTrue(reduzido.faixas() < completo.faixas());
        assertTrue(reduzido.passos() < completo.passos());
        assertTrue(reduzido.detritos() < completo.detritos());
        assertTrue(reduzido.estrelas() < completo.estrelas());
    }

    @Test
    void dominioSoPorDentroDoRaio12() {
        assertEquals(12, PerfilVfxGojo.DOMINIO_RAIO);
        double aberto = PerfilVfxGojo.DOMINIO_ABERTURA + 30;
        assertTrue(PerfilVfxGojo.presencaDominio(aberto, 3) > 0.99, "dentro");
        assertEquals(0, PerfilVfxGojo.presencaDominio(aberto, 20), 1e-9, "fora");
        double borda = PerfilVfxGojo.presencaDominio(aberto, 11.5);
        assertTrue(borda > 0 && borda < 1, "borda suave: " + borda);
        assertEquals(0, PerfilVfxGojo.presencaDominio(PerfilVfxGojo.DOMINIO_DURACAO, 3), 1e-9, "acabou");
        assertTrue(VazioInfinitoGojo.interior(5, 12));
        assertFalse(VazioInfinitoGojo.interior(11.8, 12));
    }

    @Test
    void fusaoDoRoxoTerminaNoPrimeiroPontoDaMassa() {
        for (int carga : new int[]{30, 80}) {
            assertEquals(0, CoroaRoxoGojo.separacao(carga, carga), 1e-9);
            assertTrue(CoroaRoxoGojo.separacao(0, carga) > 3);
        }
        Vetor olhos = new Vetor(5, 64, 5);
        Vetor dir = new Vetor(0, 0, 1);
        assertEquals(TrajetoriaGojo.roxo(olhos, dir, 2, 0), CoroaRoxoGojo.centroFusao(olhos, dir, 2));
    }

    @Test
    void cargaDoVermelhoCresceEExplosaoAbre() {
        assertTrue(DescargaVermelhoGojo.raioCarga(0) < DescargaVermelhoGojo.raioCarga(30));
        assertTrue(DescargaVermelhoGojo.raioOnda(10) > DescargaVermelhoGojo.raioOnda(1));
        assertTrue(DescargaVermelhoGojo.alphaOnda(20) < DescargaVermelhoGojo.alphaOnda(1));
    }

    @Test
    void detritosDoAzulVaoParaONucleo() {
        for (int i = 0; i < 5; i++) {
            double longe = PolimentoGojo.detrito(i, 0).comprimento();
            Vetor depois = PolimentoGojo.detrito(i, 1);
            assertTrue(Double.isFinite(depois.comprimento()));
            assertTrue(longe <= 6.5);
        }
        assertTrue(FluxoAzulGojo.fluxo(0, 0.95, 0).comprimento() < FluxoAzulGojo.fluxo(0, 0.05, 0).comprimento());
    }
}
