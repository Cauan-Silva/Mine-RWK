package com.minerwk.core.personagens.cid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minerwk.core.vfx.FormasCid;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Os visuais e as animações do Cid acompanham os tempos do kit. */
class FormasCidTest {
    @Test
    void osCortesDoVisualSaoOsDoGolpe() {
        assertEquals(Cid.Redemoinho.ENTRE_CORTES, FormasCid.ENTRE_CORTES);
        assertEquals((Cid.Redemoinho.DURACAO + Cid.Redemoinho.ENTRE_CORTES - 1) / Cid.Redemoinho.ENTRE_CORTES, FormasCid.CORTES);
        assertTrue(FormasCid.corte(.5) > .5, "o arco aparece no tick do corte");
        // O visual dura até o último arco sumir, e o golpe manda essa duração.
        assertEquals((FormasCid.CORTES - 1) * FormasCid.ENTRE_CORTES + FormasCid.VIDA_DO_CORTE, FormasCid.REDEMOINHO);
        assertTrue(FormasCid.REDEMOINHO > Cid.Redemoinho.DURACAO);
        assertEquals(0, FormasCid.corte(-1));
        assertEquals(0, FormasCid.corte(FormasCid.VIDA_DO_CORTE));
        assertTrue(FormasCid.angulo(FormasCid.VIDA_DO_CORTE) > Math.PI * 1.5, "cada corte dá quase uma volta");
        assertTrue(FormasCid.angulo(2) > FormasCid.angulo(1));
    }

    @Test
    void oVultoDuraOQueOChamarizDura() {
        assertEquals(1, FormasCid.vulto(0, Cid.PosImagem.IMAGEM));
        assertEquals(1, FormasCid.vulto(Cid.PosImagem.IMAGEM / 2.0, Cid.PosImagem.IMAGEM));
        assertTrue(FormasCid.vulto(Cid.PosImagem.IMAGEM - 1, Cid.PosImagem.IMAGEM) < .2);
        assertEquals(0, FormasCid.vulto(Cid.PosImagem.IMAGEM, Cid.PosImagem.IMAGEM));
        assertEquals(Cid.PosImagem.ALCANCE, FormasCid.CAMINHO);
    }

    @Test
    void oSlimeDesenhadoAndaComOSlimeDoServidor() {
        assertEquals(Cid.Prisao.PASSO, FormasCid.PASSO_DO_SLIME);
        assertEquals(Cid.Prisao.PASSO * 5, FormasCid.voo(5));
        assertEquals(0, FormasCid.voo(-3));
        // Quem erra: o slime para no alcance e some ali, não segue voando.
        assertEquals(Cid.Prisao.ALCANCE, FormasCid.ALCANCE_DO_SLIME);
        assertEquals(Cid.Prisao.ALCANCE, FormasCid.voo(40));
        assertEquals(1, FormasCid.slime(3));
        assertEquals(0, FormasCid.slime(Cid.Prisao.ALCANCE / Cid.Prisao.PASSO + 1));
    }

    @Test
    void asCorrentesSeguramEnquantoOAlvoEstaPreso() {
        int preso = Cid.Prisao.PRESO;
        assertTrue(FormasCid.correntes(0, preso) > 0);
        assertEquals(1, FormasCid.correntes(FormasCid.FECHAR, preso));
        assertEquals(1, FormasCid.correntes(preso - FormasCid.SOLTAR, preso));
        assertTrue(FormasCid.correntes(preso - 1, preso) < .3);
        assertEquals(0, FormasCid.correntes(preso, preso));
    }

    @Test
    void aAuraCobreOOverdriveEOsFiosSobemEmCiclo() {
        int d = Cid.Overdrive.DURACAO;
        assertEquals(1, FormasCid.aura(d / 2.0, d));
        assertEquals(0, FormasCid.aura(d, d));
        assertTrue(FormasCid.raioDoEstouro(FormasCid.ESTOURO) > FormasCid.raioDoEstouro(1));
        for (int k = 0; k < FormasCid.FIOS; k++) {
            for (int t = 0; t < 60; t++) {
                double f = FormasCid.fio(k, t);
                assertTrue(f >= 0 && f < 1, k + " " + t);
            }
        }
    }

    @Test
    void oCorteAtomicoDesenhadoChegaAondeODanoChega() {
        assertEquals(Cid.EspadaAtomica.ALCANCE, FormasCid.ALCANCE_DO_CORTE);
        assertEquals(Cid.EspadaAtomica.ARCO, FormasCid.ARCO_DO_CORTE);
        assertEquals(FormasCid.CARGA_DA_ESPADA, Cid.criar().habilidades().get(com.minerwk.core.kit.Tecla.V).numeros().cargaMax());
        assertEquals(0, FormasCid.cargaDaEspada(0));
        assertEquals(1, FormasCid.cargaDaEspada(FormasCid.CARGA_DA_ESPADA));
        assertTrue(FormasCid.raioDoCorte(0) < 2, "sai da lâmina");
        assertEquals(FormasCid.ALCANCE_DO_CORTE, FormasCid.raioDoCorte(FormasCid.VOO_DO_CORTE), 1e-9);
        assertEquals(FormasCid.ALCANCE_DO_CORTE, FormasCid.raioDoCorte(40), 1e-9, "não passa do alcance");
        assertEquals(1, FormasCid.corteAtomico(FormasCid.VOO_DO_CORTE));
        assertEquals(0, FormasCid.corteAtomico(FormasCid.CORTE_ATOMICO));
        assertTrue(FormasCid.VOO_DO_CORTE < FormasCid.CORTE_ATOMICO);
    }

    @Test
    void oCirculoEAColunaDoRecoveryTemORaioDaCura() {
        assertEquals(Cid.RecoveryAtomic.RAIO, FormasCid.RAIO_DO_RECOVERY);
        assertEquals(FormasCid.CARGA_DO_RECOVERY, Cid.criar().habilidades().get(com.minerwk.core.kit.Tecla.R).agachado().numeros().cargaMax());
        assertTrue(FormasCid.circulo(0) < 1);
        assertEquals(FormasCid.RAIO_DO_RECOVERY, FormasCid.circulo(FormasCid.CARGA_DO_RECOVERY), 1e-9, "a carga mostra até onde a cura vai");
        assertEquals(FormasCid.RAIO_DO_RECOVERY, FormasCid.onda(FormasCid.ONDA), 1e-9);
        assertEquals(0, FormasCid.onda(0));
        assertEquals(1, FormasCid.coluna(FormasCid.COLUNA / 2.0));
        assertEquals(0, FormasCid.coluna(FormasCid.COLUNA));
        assertTrue(FormasCid.ONDA < FormasCid.COLUNA);
    }

    @Test
    void cadaGolpeTemAnimacaoQueCobreACargaEODisparo() throws Exception {
        JsonObject anim = JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/assets/minerwk/player_animations/cid.json"))).getAsJsonObject().getAsJsonObject("animations");
        int[][] tempos = {{2, Cid.Redemoinho.DURACAO}, {1, 1}, {3, 2}, {2, 1},
                {FormasCid.CARGA_DA_ESPADA, 6}, {FormasCid.CARGA_DO_RECOVERY, 10},
                // O R: os socos (a animação recomeça no espaço) e as três palavras até depois do Atomic.
                {AtomicFinal.PASSAGEM, AtomicFinal.ENTRE_SOCOS}, {AtomicFinal.TICKS_DA_FALA, 10}};
        String[] ids = {"cid_redemoinho", "cid_pos_imagem", "cid_prisao", Cid.OVERDRIVE, Cid.ESPADA_ATOMICA, Cid.RECOVERY,
                Cid.Atomic.ANIM_DOS_SOCOS, Cid.Atomic.ANIM_DA_FALA};
        for (int i = 0; i < ids.length; i++) {
            assertTrue(anim.has(ids[i]), "falta a animação " + ids[i]);
            double minimo = (tempos[i][0] + tempos[i][1]) / 20.0;
            assertTrue(anim.getAsJsonObject(ids[i]).get("animation_length").getAsDouble() >= minimo - 1e-9, ids[i]);
        }
        assertEquals(ids.length, anim.size());
    }
}
