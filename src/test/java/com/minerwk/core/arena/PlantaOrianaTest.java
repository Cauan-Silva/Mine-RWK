package com.minerwk.core.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.arena.PlantaEscola.Bloco;
import com.minerwk.core.arena.PlantaEscola.Material;
import com.minerwk.core.boss.RagnarokBoss;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlantaOrianaTest {
    private static final List<Bloco> CENARIO = PlantaOriana.cenario();

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
        assertTrue(CENARIO.size() < 60000, "blocos demais: " + CENARIO.size());
        assertEquals(CENARIO.size(), porPosicao().size(), "nenhuma posição repetida");
    }

    @Test
    void oCampoInteiroTemChaoEOMeioFicaLivreParaUmBossDeSeteBlocos() {
        Map<String, Material> mapa = porPosicao();
        for (int dx = -CaixaArena.MEIA; dx <= CaixaArena.MEIA; dx++) {
            for (int dz = -CaixaArena.MEIA; dz <= CaixaArena.MEIA; dz++) {
                assertNotNull(mapa.get(dx + ",-1," + dz), "buraco no chão em " + dx + ", " + dz);
            }
        }
        // Do jogador (16 ao sul) até bem depois de onde o boss entra, 30 blocos para cada lado, nada no caminho.
        for (int dx = -30; dx <= 30; dx++) {
            for (int dz = -30; dz <= 30; dz++) {
                for (int dy = 0; dy <= (int) RagnarokBoss.ALTURA + RagnarokBoss.ALTURA_DO_VOO; dy++) {
                    assertNull(mapa.get(dx + "," + dy + "," + dz), "obstáculo em " + dx + ", " + dy + ", " + dz);
                }
            }
        }
        assertEquals(Material.ANDESITO_POLIDO, mapa.get("0,-1,16"), "o jogador entra na estrada");
        assertEquals(Material.GRAMA, mapa.get("10,-1,0"));
    }

    @Test
    void aMuralhaFechaONorteEOPortalFicaNoCeu() {
        Map<String, Material> mapa = porPosicao();
        for (int dx = -CaixaArena.MEIA; dx <= CaixaArena.MEIA; dx++) {
            assertNotNull(mapa.get(dx + ",6," + PlantaOriana.MURALHA_DE), "a muralha não tem brecha em " + dx);
        }
        assertEquals(Material.MADEIRA_ESCURA, mapa.get("0,2," + (PlantaOriana.MURALHA_ATE - 1)), "o portão fechado");
        assertEquals(Material.PORTAL_NEGRO, mapa.get("0," + PlantaOriana.PORTAL_DY + "," + PlantaOriana.PORTAL_DZ));
        assertEquals(Material.PORTAL_ROXO,
                mapa.get((PlantaOriana.PORTAL_RAIO - 1) + "," + PlantaOriana.PORTAL_DY + "," + PlantaOriana.PORTAL_DZ));
        assertTrue(PlantaOriana.PORTAL_DY - PlantaOriana.PORTAL_RAIO > PlantaOriana.TORRE_TOPO + 1, "acima das torres");
    }
}
