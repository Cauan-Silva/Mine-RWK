package com.minerwk.core.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.arena.PlantaEscola.Bloco;
import com.minerwk.core.arena.PlantaEscola.Material;
import com.minerwk.core.boss.RagnarokBoss;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
            assertNotNull(mapa.get(dx + ",3," + PlantaOriana.MURALHA_DE), "a muralha não tem brecha em " + dx);
        }
        assertNotNull(mapa.get("0,2," + (PlantaOriana.MURALHA_ATE - 1)), "o portão fechado");
        assertEquals(Material.PORTAL_NEGRO, mapa.get("0," + PlantaOriana.PORTAL_DY + "," + PlantaOriana.PORTAL_DZ));
        assertEquals(Material.PORTAL_ROXO,
                mapa.get((PlantaOriana.PORTAL_RAIO - 1) + "," + PlantaOriana.PORTAL_DY + "," + PlantaOriana.PORTAL_DZ));
        assertTrue(PlantaOriana.PORTAL_DY - PlantaOriana.PORTAL_RAIO > PlantaOriana.TORRE_TOPO + 1, "acima das torres");
    }

    private static boolean naTorre(int dx) {
        for (int x : PlantaOriana.TORRES) if (Math.abs(dx - x) <= PlantaOriana.MEIA_TORRE) return true;
        return false;
    }

    @Test
    void aMuralhaNaoDeixaPassar() {
        Map<String, Material> mapa = porPosicao();
        for (int dx = -CaixaArena.MEIA; dx <= CaixaArena.MEIA; dx++) {
            boolean fechada = false;
            for (int dz = PlantaOriana.MURALHA_DE; dz <= PlantaOriana.MURALHA_ATE && !fechada; dz++) {
                boolean cheia = true;
                for (int dy = 0; dy <= 3; dy++) cheia &= mapa.get(dx + "," + dy + "," + dz) != null;
                fechada = cheia;
            }
            assertTrue(fechada, "dá para passar pela muralha em dx " + dx);
        }
    }

    @Test
    void aMuralhaTemBrechasEmV() {
        Map<String, Material> mapa = porPosicao();
        for (int x : PlantaOriana.BRECHAS) {
            assertEquals(Material.BARREIRA, mapa.get(x + "," + PlantaOriana.MURALHA_TOPO + "," + PlantaOriana.MURALHA_DE),
                    "a camada de trás da brecha é uma barreira invisível");
            for (int dz = PlantaOriana.MURALHA_DE; dz <= PlantaOriana.MURALHA_ATE; dz++) {
                if (dz > PlantaOriana.MURALHA_DE) {
                    assertNull(mapa.get(x + "," + PlantaOriana.MURALHA_TOPO + "," + dz), "a brecha em " + x + " não abre no topo");
                }
                assertNotNull(mapa.get(x + "," + (PlantaOriana.FUNDO_DA_BRECHA - 1) + "," + dz), "a brecha em " + x + " desce demais");
            }
            assertNotNull(mapa.get((x + PlantaOriana.MEIA_BRECHA + 1) + "," + PlantaOriana.MURALHA_TOPO + "," + PlantaOriana.MURALHA_ATE),
                    "a brecha em " + x + " é larga demais");
        }
    }

    @Test
    void aMuralhaEstaRachada() {
        int total = 0, gastos = 0, queimados = 0;
        for (Bloco b : CENARIO) {
            if (b.dz() < PlantaOriana.MURALHA_DE || b.dz() > PlantaOriana.MURALHA_ATE || b.dy() < 0 || naTorre(b.dx())) continue;
            total++;
            if (b.material() == Material.TIJOLO_RACHADO || b.material() == Material.TIJOLO_MUSGO) gastos++;
            if (b.material() == Material.PEDRA_NEGRA && b.dz() == PlantaOriana.MURALHA_ATE) queimados++;
        }
        double parte = gastos / (double) total;
        assertTrue(parte >= .15 && parte <= .45, "rachados e com musgo: " + parte);
        assertTrue(queimados >= 20, "marcas de queimado na face de dentro: " + queimados);
    }

    @Test
    void aTorreDestruidaPerdeuOTelhado() {
        boolean fogo = false;
        for (Bloco b : CENARIO) {
            if (Math.abs(b.dx() - PlantaOriana.TORRE_DESTRUIDA) > PlantaOriana.MEIA_TORRE) continue;
            if (b.dz() < PlantaOriana.MURALHA_DE - PlantaOriana.MEIA_TORRE || b.dz() > PlantaOriana.MURALHA_ATE + PlantaOriana.MEIA_TORRE) continue;
            assertTrue(b.material() != Material.TELHA, "a torre destruída ainda tem telhado: " + b);
            fogo |= b.material() == Material.FOGO && b.dy() > PlantaOriana.TORRE_TOPO - 8;
        }
        assertTrue(fogo, "a torre destruída pega fogo em cima");
        for (int x : PlantaOriana.TORRES) {
            if (x == PlantaOriana.TORRE_DESTRUIDA) continue;
            assertTrue(CENARIO.stream().anyMatch(b -> b.material() == Material.LA_VERMELHA
                    && Math.abs(b.dx() - x) <= PlantaOriana.MEIA_TORRE + 1), "a torre de " + x + " sem estandarte");
        }
    }

    @Test
    void oPortaoArrombadoContinuaFechado() {
        Map<String, Material> mapa = porPosicao();
        boolean folha = false;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = PlantaOriana.MURALHA_ATE + 1; dz <= PlantaOriana.MURALHA_ATE + 3; dz++) {
                folha |= mapa.get(dx + ",0," + dz) == Material.MADEIRA_ESCURA;
            }
        }
        assertTrue(folha, "a folha do portão caída à frente do vão");
        for (int dx = -PlantaOriana.MEIA_ESTRADA; dx <= PlantaOriana.MEIA_ESTRADA; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                assertNotNull(mapa.get(dx + "," + dy + "," + (PlantaOriana.MURALHA_ATE - 1)), "a barricada tem buraco em " + dx + ", " + dy);
            }
        }
    }

    @Test
    void oFogoNaoSeEspalha() {
        Map<String, Material> mapa = porPosicao();
        int noCampo = 0;
        for (Bloco f : CENARIO) {
            if (f.material() != Material.FOGO) continue;
            assertEquals(Material.NETHERRACK, mapa.get(f.dx() + "," + (f.dy() - 1) + "," + f.dz()), "fogo sem netherrack embaixo: " + f);
            for (Bloco b : CENARIO) {
                if (!PlantaOriana.INFLAMAVEIS.contains(b.material())) continue;
                int dy = b.dy() - f.dy();
                assertTrue(Math.abs(b.dx() - f.dx()) > 3 || Math.abs(b.dz() - f.dz()) > 3 || dy < -1 || dy > 5,
                        "o fogo em " + f + " pega em " + b);
            }
            if (f.dz() > PlantaOriana.MURALHA_ATE + 3) noCampo++;
        }
        assertTrue(noCampo >= 8 && noCampo <= 16, "focos de fogo no campo: " + noCampo);
    }

    @Test
    void oCampoTemCraterasEDestrocos() {
        int brasas = 0, queimado = 0, fogueiras = 0;
        for (Bloco b : CENARIO) {
            if (b.dy() == -1 && b.material() == Material.BRASA) brasas++;
            if (b.dy() == -1 && (b.material() == Material.TERRA_QUEIMADA || b.material() == Material.PEDRA_NEGRA)) queimado++;
            if (b.material() == Material.FOGUEIRA) fogueiras++;
            if ((b.material() == Material.FENO || b.material() == Material.BARRIL) && b.dy() >= 0) {
                assertTrue(Math.abs(b.dx()) > 30 || Math.abs(b.dz()) > 30, "destroço no meio livre: " + b);
            }
        }
        assertTrue(brasas >= 4, "brasas no chão: " + brasas);
        // Pedido do autor (13/11): a brasa machuca quem pisa, então fica só nas crateras das beiradas.
        assertTrue(CENARIO.stream().noneMatch(b -> b.material() == Material.BRASA && Math.abs(b.dx()) <= 30 && Math.abs(b.dz()) <= 30),
                "brasa no meio livre");
        assertTrue(queimado >= 60, "chão queimado: " + queimado);
        assertTrue(fogueiras >= 2, "fogueiras: " + fogueiras);
        assertTrue(CENARIO.stream().anyMatch(b -> b.material() == Material.FENO), "feno espalhado");
        assertTrue(CENARIO.stream().anyMatch(b -> b.material() == Material.BARRIL), "barris espalhados");
        long queimadas = CENARIO.stream().filter(b -> b.material() == Material.TRONCO_QUEIMADO && b.dy() == 0).count();
        assertTrue(queimadas >= 6, "árvores queimadas: " + queimadas);
        long verdes = CENARIO.stream().filter(b -> b.material() == Material.TRONCO && b.dy() == 0 && b.dz() > 0).count();
        assertTrue(verdes >= 2, "árvores ainda verdes ao sul: " + verdes);
        assertTrue(CENARIO.stream().noneMatch(b -> b.material() == Material.TRONCO && b.dz() <= 0), "ao norte, só árvores queimadas");
    }

    /** Escreve a planta em texto para o script de prévia: {@code python ferramentas/previa_arena.py oriana}. */
    @Test
    void despejaAPlantaParaAPrevia() throws IOException {
        StringBuilder s = new StringBuilder();
        for (Bloco b : CENARIO) {
            s.append(b.dx()).append(' ').append(b.dy()).append(' ').append(b.dz()).append(' ').append(b.material()).append('\n');
        }
        Path saida = Path.of("build", "arena_oriana.txt");
        Files.createDirectories(saida.getParent());
        Files.writeString(saida, s);
        assertTrue(Files.size(saida) > 0);
    }

    /** O que o jogador atravessa: o ar e as chamas. */
    private static boolean livre(Map<String, Material> mapa, int x, int y, int z) {
        Material m = mapa.get(x + "," + y + "," + z);
        return m == null || m == Material.FOGO;
    }

    /** Onde dá para ficar em pé: chão firme embaixo e duas casas livres. */
    private static boolean emPe(Map<String, Material> mapa, int x, int y, int z) {
        return !livre(mapa, x, y - 1, z) && livre(mapa, x, y, z) && livre(mapa, x, y + 1, z);
    }

    /**
     * Andando e pulando a partir da entrada do jogador, ninguém chega atrás da muralha. Sobe um bloco por vez (nunca
     * em cima de muro ou cerca, que são mais altos), e desce de qualquer altura.
     */
    @Test
    void ninguemChegaAtrasDaMuralhaAndandoEPulando() {
        Map<String, Material> mapa = porPosicao();
        ArrayDeque<int[]> fila = new ArrayDeque<>();
        Set<String> visto = new HashSet<>();
        fila.add(new int[] {0, 0, 16});
        visto.add("0,0,16");
        int[][] passos = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!fila.isEmpty()) {
            int[] c = fila.poll();
            assertTrue(c[2] > PlantaOriana.MURALHA_DE, "o jogador chega atrás da muralha em " + c[0] + ", " + c[1] + ", " + c[2]);
            for (int[] p : passos) {
                int x = c[0] + p[0], z = c[2] + p[1];
                if (Math.abs(x) > CaixaArena.MEIA || Math.abs(z) > CaixaArena.MEIA) continue;
                // No mesmo nível, um acima (com a cabeça livre antes de pular) ou caindo até o chão.
                int y = -1;
                if (emPe(mapa, x, c[1], z)) {
                    y = c[1];
                } else if (emPe(mapa, x, c[1] + 1, z) && livre(mapa, c[0], c[1] + 2, c[2])) {
                    Material degrau = mapa.get(x + "," + c[1] + "," + z);
                    if (degrau != Material.MURO && degrau != Material.CERCA_ESCURA) y = c[1] + 1;
                } else if (livre(mapa, x, c[1], z) && livre(mapa, x, c[1] + 1, z)) {
                    for (int d = c[1] - 1; d >= -1; d--) {
                        if (emPe(mapa, x, d, z)) {
                            y = d;
                            break;
                        }
                    }
                }
                if (y >= 0 && visto.add(x + "," + y + "," + z)) fila.add(new int[] {x, y, z});
            }
        }
    }
}
