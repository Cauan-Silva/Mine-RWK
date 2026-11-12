package com.minerwk.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.vfx.FormasIchigo;
import com.minerwk.core.vfx.GetsugaIchigo;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ZangetsuBossTest {
    @Test
    void temDuzentosESessentaDeVidaEQuinhentosDeXp() {
        assertEquals(260, ZangetsuBoss.VIDA);
        assertEquals(500, ZangetsuBoss.XP_VITORIA);
    }

    @Test
    void osGolpesTemIdsUnicosECobremQualquerDistanciaDaArena() {
        Set<String> ids = new HashSet<>();
        for (Golpe g : ZangetsuBoss.GOLPES) assertTrue(ids.add(g.id()));
        for (Golpe g : ZangetsuBoss.GOLPES_DA_MASCARA) ids.add(g.id());
        assertEquals(6, ids.size());
        for (int fase = 1; fase <= 2; fase++) {
            for (double d = 0; d <= 20; d += .5) {
                double distancia = d;
                assertTrue(ZangetsuBoss.golpes(fase).stream().anyMatch(g -> g.alcanca(distancia)),
                        "fase " + fase + ", nenhum golpe a " + d);
            }
        }
    }

    @Test
    void aMascaraVemAbaixoDeQuarentaPorCentoETrocaOGetsugaPelaSequencia() {
        assertEquals(1, ZangetsuBoss.fase(1));
        assertEquals(1, ZangetsuBoss.fase(.41));
        assertEquals(2, ZangetsuBoss.fase(.4));
        assertTrue(ZangetsuBoss.golpes(1).contains(ZangetsuBoss.GETSUGA));
        assertFalse(ZangetsuBoss.golpes(2).contains(ZangetsuBoss.GETSUGA));
        assertTrue(ZangetsuBoss.golpes(2).contains(ZangetsuBoss.GETSUGA_3));
        assertTrue(ZangetsuBoss.ritmo(2) < ZangetsuBoss.ritmo(1));
        assertTrue(ZangetsuBoss.PASSO_DA_MASCARA > ZangetsuBoss.PASSO_NORMAL);
        assertTrue(ZangetsuBoss.MASCARA_APARECE < ZangetsuBoss.TROCA_DE_FASE);
    }

    @Test
    void oGetsugaSaiUmaVezEASequenciaTresVezes() {
        assertEquals(1, saidas(ZangetsuBoss.GETSUGA));
        assertEquals(3, saidas(ZangetsuBoss.GETSUGA_3));
        assertTrue(ZangetsuBoss.getsugaSai(ZangetsuBoss.GETSUGA_3, 0));
        assertTrue(ZangetsuBoss.getsugaSai(ZangetsuBoss.GETSUGA_3, 24));
        assertFalse(ZangetsuBoss.getsugaSai(ZangetsuBoss.GETSUGA_3, 36));
        // A linha do segundo e do terceiro aparece 6 ticks antes de cada um; o primeiro tem o aviso do golpe.
        int miras = 0;
        for (int t = 0; t < ZangetsuBoss.GETSUGA_3.janela(); t++) {
            if (ZangetsuBoss.getsugaMira(ZangetsuBoss.GETSUGA_3, t)) {
                miras++;
                assertTrue(ZangetsuBoss.getsugaSai(ZangetsuBoss.GETSUGA_3, t + ZangetsuBoss.MIRA_DO_GETSUGA));
            }
        }
        assertEquals(2, miras);
        assertFalse(ZangetsuBoss.getsugaMira(ZangetsuBoss.GETSUGA, 6));
    }

    private static int saidas(Golpe g) {
        int n = 0;
        for (int t = 0; t < g.janela(); t++) if (ZangetsuBoss.getsugaSai(g, t)) n++;
        return n;
    }

    @Test
    void aMeiaLuaDoBossVoaComoOVisualDoGetsugaNegro() {
        // A janela cobre o voo inteiro: a meia-lua some quando o golpe fecha.
        int voo = (int) Math.ceil(ZangetsuBoss.ALCANCE_GETSUGA / ZangetsuBoss.PASSO_GETSUGA);
        assertEquals(GetsugaIchigo.voo(true), voo);
        assertEquals(voo, ZangetsuBoss.GETSUGA.janela());
        assertEquals(voo, ZangetsuBoss.ENTRE_GETSUGAS);
        assertEquals(3 * voo, ZangetsuBoss.GETSUGA_3.janela());
    }

    @Test
    void aEspadaArremessadaVaiEVoltaDentroDaJanela() {
        Golpe g = ZangetsuBoss.ARREMESSO;
        assertEquals(FormasIchigo.ARREMESSO, g.janela());
        double longe = 0;
        for (int t = 0; t < g.janela(); t++) longe = Math.max(longe, ZangetsuBoss.espadaEm(t, 10));
        assertEquals(10, longe, 1e-9);
        assertEquals(0, ZangetsuBoss.espadaEm(g.janela() - 1, 10), 1e-9, "no fim da janela está de volta à mão");
        assertEquals(2.5, ZangetsuBoss.espadaEm(7, 2.5), 1e-9, "com parede perto ela para antes");
    }

    @Test
    void osDanosDeixamQuemTemVinteDeVidaSobreviverAUmGolpe() {
        for (float dano : new float[] {ZangetsuBoss.DANO_CORTE, ZangetsuBoss.DANO_GETSUGA, ZangetsuBoss.DANO_SHUNPO,
                ZangetsuBoss.DANO_ARREMESSO, ZangetsuBoss.DANO_CERO}) {
            assertTrue(dano > 0 && dano < 20);
        }
        assertTrue(ZangetsuBoss.golpeMortal(8, 8));
        assertFalse(ZangetsuBoss.golpeMortal(8.5f, 8));
    }

    @Test
    void oShunpoSurgeAntesDeAJanelaAbrirEOCeroTravaAMira() {
        assertTrue(ZangetsuBoss.TICK_DE_SURGIR < ZangetsuBoss.SHUNPO.aviso());
        assertTrue(ZangetsuBoss.SHUNPO.aviso() - ZangetsuBoss.TICK_DE_SURGIR >= 8, "sobra tempo para reagir à sombra");
        assertTrue(ZangetsuBoss.CERO_TRAVA < ZangetsuBoss.CERO.aviso());
    }

    @Test
    void aLutaAcabaSemOInvocadorOuNoTempoLimite() {
        assertFalse(ZangetsuBoss.acabou(true, 100));
        assertTrue(ZangetsuBoss.acabou(false, 100));
        assertTrue(ZangetsuBoss.acabou(true, ZangetsuBoss.TEMPO_LIMITE));
    }

    @Test
    void aCutsceneDaMascaraCortaQuandoEleSeLevanta() {
        assertEquals(ZangetsuBoss.QUEDA, CutsceneMascara.LEVANTA);
        assertEquals(CutsceneMascara.LEVANTA + CutsceneMascara.DEPOIS, CutsceneMascara.COREOGRAFIA.duracao());
    }
}
