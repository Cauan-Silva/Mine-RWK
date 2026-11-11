package com.minerwk.core.arena;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.arena.PlantaEscola.Bloco;
import com.minerwk.core.arena.PlantaEscola.Material;
import com.minerwk.core.kit.Vetor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlantaEscolaTest {
    private static String chave(int dx, int dy, int dz) {
        return dx + "," + dy + "," + dz;
    }

    private static Map<String, Material> mapa(List<Bloco> blocos) {
        Map<String, Material> m = new HashMap<>();
        for (Bloco b : blocos) m.put(chave(b.dx(), b.dy(), b.dz()), b.material());
        return m;
    }

    @Test
    void plataformaTem61Por61ComQuatroCamadas() {
        List<Bloco> blocos = PlantaEscola.plataforma();
        assertEquals(61 * 61 * 4, blocos.size());
        for (Bloco b : blocos) {
            assertTrue(Math.abs(b.dx()) <= 30 && Math.abs(b.dz()) <= 30, "fora do pátio: " + b);
            assertTrue(b.dy() >= -4 && b.dy() <= -1, "fora das camadas: " + b);
            assertEquals(b.dy() == -1 ? Material.TIJOLO_PEDRA : Material.PEDRA, b.material());
        }
    }

    @Test
    void plataformaNaoRepetePosicao() {
        Set<String> vistas = new HashSet<>();
        for (Bloco b : PlantaEscola.plataforma()) assertTrue(vistas.add(chave(b.dx(), b.dy(), b.dz())), "repetida: " + b);
    }

    @Test
    void cenarioCabeNaCaixaSemRepetirEMontaRapido() {
        List<Bloco> blocos = PlantaEscola.cenario();
        Set<String> vistas = new HashSet<>();
        for (Bloco b : blocos) {
            assertTrue(Math.abs(b.dx()) <= CaixaArena.MEIA && Math.abs(b.dz()) <= CaixaArena.MEIA, "fora da caixa: " + b);
            assertTrue(b.dy() >= CaixaArena.Y_FUNDO - CaixaArena.Y_PATIO && b.dy() < CaixaArena.Y_TETO - CaixaArena.Y_PATIO,
                    "fora da altura da caixa: " + b);
            assertTrue(vistas.add(chave(b.dx(), b.dy(), b.dz())), "repetida: " + b);
        }
        // 4000 blocos por tick: até 10 ticks de montagem.
        assertTrue(blocos.size() <= 40000, "blocos demais: " + blocos.size());
    }

    @Test
    void patioTemPisoInteiroEQuatroCamadas() {
        Map<String, Material> m = mapa(PlantaEscola.cenario());
        for (int dx = -30; dx <= 30; dx++) {
            for (int dz = -30; dz <= 30; dz++) {
                assertNotNull(m.get(chave(dx, -1, dz)), "sem piso em " + dx + "," + dz);
                assertNotEquals(Material.LAJE_PEDRA, m.get(chave(dx, -1, dz)), "piso de meia altura em " + dx + "," + dz);
                for (int dy = -4; dy <= -2; dy++) assertEquals(Material.PEDRA, m.get(chave(dx, dy, dz)));
            }
        }
        assertEquals(Material.ANDESITO_POLIDO, m.get(chave(0, -1, 0)), "a faixa central é de andesito polido");
        assertEquals(Material.TIJOLO_PEDRA, m.get(chave(15, -1, 5)), "o resto do piso é de tijolo de pedra");
    }

    @Test
    void patioFicaLivreParaALuta() {
        Map<String, Material> m = mapa(PlantaEscola.cenario());
        int pilares = 0;
        for (int dx = -30; dx <= 30; dx++) {
            for (int dz = -30; dz <= 30; dz++) {
                for (int dy = 0; dy <= 6; dy++) {
                    if (m.get(chave(dx, dy, dz)) == null) continue;
                    // Só os quatro pilares dos dois torii do caminho ficam dentro do pátio.
                    assertTrue(Math.abs(dx) == 4 && Math.abs(dz) == 12, "obstáculo no pátio em " + chave(dx, dy, dz));
                    pilares++;
                }
            }
        }
        assertEquals(4 * 7, pilares, "quatro pilares de 7 blocos");
        // O vão dos torii do caminho: 7 de largura e 7 de altura livres.
        for (int dz : new int[] {-12, 12}) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = 0; dy <= 6; dy++) assertNull(m.get(chave(dx, dy, dz)));
            }
            assertNotNull(m.get(chave(0, 7, dz)), "a viga do torii fica acima do vão");
        }
    }

    @Test
    void entradasTemChaoEEspaco() {
        Map<String, Material> m = mapa(PlantaEscola.cenario());
        CaixaArena caixa = CaixaArena.daVaga(0);
        for (Vetor p : new Vetor[] {caixa.entradaJogador(), caixa.entradaToji(), caixa.centro()}) {
            int dx = (int) Math.floor(p.x()) - caixa.centroX(), dz = (int) Math.floor(p.z()) - caixa.centroZ();
            assertNotNull(m.get(chave(dx, -1, dz)), "sem chão na entrada " + dx + "," + dz);
            for (int dy = 0; dy <= 2; dy++) assertNull(m.get(chave(dx, dy, dz)), "entrada obstruída em " + chave(dx, dy, dz));
        }
    }

    @Test
    void escadariaSobeOitoBlocosAteOPatamarComOToriiGrande() {
        Map<String, Material> m = mapa(PlantaEscola.cenario());
        for (int k = 0; k < 8; k++) {
            for (int dx = -10; dx <= 10; dx++) {
                assertEquals(Material.ESCADA_PEDRA, m.get(chave(dx, k, -31 - k)), "degrau " + k + " em dx " + dx);
                assertNull(m.get(chave(dx, k + 1, -31 - k)), "nada em cima do degrau " + k);
                assertNotNull(m.get(chave(dx, k - 1, -31 - k)), "degrau " + k + " sem apoio");
            }
        }
        for (int dz : new int[] {-39, -40}) {
            for (int dx = -10; dx <= 10; dx++) assertNotNull(m.get(chave(dx, 7, dz)), "patamar em " + dx + "," + dz);
        }
        // Torii grande no patamar: vão de 9 de largura e 9 de altura.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = 8; dy <= 16; dy++) assertNull(m.get(chave(dx, dy, -40)), "vão do torii grande em " + chave(dx, dy, -40));
        }
        assertEquals(Material.MADEIRA_VERMELHA, m.get(chave(5, 8, -40)));
        assertEquals(Material.MADEIRA_VERMELHA, m.get(chave(-5, 16, -40)));
        assertEquals(Material.MADEIRA_ESCURA, m.get(chave(0, 18, -40)), "a viga de cima é escura");
    }

    @Test
    void bordasTemMuroLanternasEDozeArvores() {
        List<Bloco> blocos = PlantaEscola.cenario();
        Map<String, Material> m = mapa(blocos);
        assertEquals(Material.MURO, m.get(chave(31, 0, 0)));
        assertEquals(Material.MURO, m.get(chave(-31, 1, 10)));
        assertEquals(Material.MURO, m.get(chave(7, 0, 31)));
        assertTrue(blocos.stream().filter(b -> b.material() == Material.LANTERNA).count() >= 20, "lanternas sobre o muro");
        // Cada árvore tem um tronco apoiado no chão da faixa de fora.
        long troncosNoChao = blocos.stream().filter(b -> b.material() == Material.TRONCO && b.dy() == 0).count();
        assertEquals(12, troncosNoChao);
        for (Bloco b : blocos) {
            if (b.material() == Material.TRONCO || b.material() == Material.FOLHA) {
                assertTrue(Math.max(Math.abs(b.dx()), Math.abs(b.dz())) >= 32, "árvore dentro do pátio: " + b);
            }
            if (b.material() == Material.LANTERNA) {
                assertEquals(Material.MURO, m.get(chave(b.dx(), b.dy() - 1, b.dz())), "lanterna sem muro embaixo: " + b);
            }
        }
    }

    /** Escreve a planta em texto para o script de prévia ({@code ferramentas/previa_arena.py}). */
    @Test
    void despejaAPlantaParaAPrevia() throws IOException {
        StringBuilder s = new StringBuilder();
        for (Bloco b : PlantaEscola.cenario()) {
            s.append(b.dx()).append(' ').append(b.dy()).append(' ').append(b.dz()).append(' ').append(b.material()).append('\n');
        }
        Path saida = Path.of("build", "arena.txt");
        Files.createDirectories(saida.getParent());
        Files.writeString(saida, s);
        assertTrue(Files.size(saida) > 0);
    }
}
