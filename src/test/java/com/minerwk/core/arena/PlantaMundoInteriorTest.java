package com.minerwk.core.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.arena.PlantaEscola.Bloco;
import com.minerwk.core.arena.PlantaEscola.Material;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlantaMundoInteriorTest {
    private static final List<Bloco> CENARIO = PlantaMundoInterior.cenario();

    private static Map<String, Material> porPosicao() {
        Map<String, Material> mapa = new HashMap<>();
        for (Bloco b : CENARIO) mapa.put(b.dx() + "," + b.dy() + "," + b.dz(), b.material());
        return mapa;
    }

    @Test
    void tudoCabeNaCaixaENaoPesaNaMontagem() {
        int fundo = CaixaArena.Y_FUNDO - CaixaArena.Y_PATIO, teto = CaixaArena.Y_TETO - CaixaArena.Y_PATIO;
        for (Bloco b : CENARIO) {
            assertTrue(Math.abs(b.dx()) <= CaixaArena.MEIA && Math.abs(b.dz()) <= CaixaArena.MEIA, "fora da caixa: " + b);
            assertTrue(b.dy() >= fundo && b.dy() < teto, "fora da altura: " + b);
        }
        // A montagem põe 4000 blocos por tick e o jogador espera no máximo 80.
        assertTrue(CENARIO.size() < 60000, "blocos demais: " + CENARIO.size());
        assertEquals(CENARIO.size(), porPosicao().size(), "nenhuma posição repetida");
    }

    @Test
    void haChaoSobOsPesDosDoisNaEntradaENoPredioInteiro() {
        Map<String, Material> mapa = porPosicao();
        // O jogador entra 16 blocos ao sul do centro e o boss, no centro.
        for (int dz : new int[] {0, 16}) {
            assertNotNull(mapa.get("0,-1," + dz), "sem chão em dz " + dz);
            assertNull(mapa.get("0,0," + dz), "os pés ficam livres");
            assertNull(mapa.get("0,1," + dz));
        }
        for (int dx = -PlantaMundoInterior.MEIO_PREDIO; dx <= PlantaMundoInterior.MEIO_PREDIO; dx++) {
            for (int dz = -CaixaArena.MEIA; dz <= CaixaArena.MEIA; dz++) {
                assertNotNull(mapa.get(dx + ",-1," + dz), "buraco na fachada em " + dx + ", " + dz);
            }
        }
    }

    @Test
    void aFachadaEUmaGradeDeConcretoEVidroComFundoEscuro() {
        Map<String, Material> mapa = porPosicao();
        long vidros = CENARIO.stream().filter(b -> b.dy() == -1 && b.material() == Material.VIDRO_AZUL).count();
        long vigas = CENARIO.stream().filter(b -> b.dy() == -1 && b.material() == Material.CONCRETO_CLARO).count();
        assertTrue(vidros > vigas, "mais janela do que viga: " + vidros + " e " + vigas);
        for (Bloco b : CENARIO) {
            if (b.dy() == -1 && b.material() == Material.VIDRO_AZUL) {
                assertEquals(Material.FUNDO_ESCURO, mapa.get(b.dx() + ",-2," + b.dz()));
            }
        }
    }

    @Test
    void osOutrosPrediosFicamForaDoCampoDaLuta() {
        for (Bloco b : CENARIO) {
            if (Math.abs(b.dx()) < PlantaMundoInterior.MEIO_PREDIO && b.dz() > PlantaMundoInterior.CRUZADO_ATE) {
                assertTrue(b.dy() < 0, "bloco no ar sobre o campo: " + b);
            }
        }
        assertTrue(PlantaMundoInterior.ALTO_TOPO > 0 && PlantaMundoInterior.BAIXO_TOPO < -1);
        assertTrue(PlantaMundoInterior.CRUZADO_BASE > PlantaMundoInterior.ALTO_TOPO, "o atravessado passa por cima do alto");
    }
}
