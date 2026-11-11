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

    private static final int P = PlantaEscola.MEIO_PATIO_ESCOLA;

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
        // 4000 blocos por tick: até 20 ticks de montagem.
        assertTrue(blocos.size() <= 80000, "blocos demais: " + blocos.size());
    }

    @Test
    void aCaixaInteiraTemChaoSemBuracos() {
        Map<String, Material> m = mapa(PlantaEscola.cenario());
        for (int dx = -CaixaArena.MEIA; dx <= CaixaArena.MEIA; dx++) {
            for (int dz = -CaixaArena.MEIA; dz <= CaixaArena.MEIA; dz++) {
                int topo = PlantaEscola.chao(dz);
                assertNotNull(m.get(chave(dx, topo, dz)), "buraco no chão em " + dx + "," + dz);
                assertNotNull(m.get(chave(dx, topo - 1, dz)), "chão sem apoio em " + dx + "," + dz);
            }
        }
    }

    @Test
    void patioTemPisoInteiroEQuatroCamadas() {
        Map<String, Material> m = mapa(PlantaEscola.cenario());
        for (int dx = -P; dx <= P; dx++) {
            for (int dz = -P; dz <= P; dz++) {
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
        for (int dx = -P + 1; dx <= P - 1; dx++) {
            for (int dz = -P + 1; dz <= P - 1; dz++) {
                for (int dy = 0; dy <= 6; dy++) {
                    if (m.get(chave(dx, dy, dz)) == null) continue;
                    // Só os dois pilares do torii do caminho ficam dentro do pátio.
                    assertTrue(Math.abs(dx) == PlantaEscola.PILAR_DO_CAMINHO && dz == PlantaEscola.TORII_DO_CAMINHO,
                            "obstáculo no pátio em " + chave(dx, dy, dz));
                    pilares++;
                }
            }
        }
        assertEquals(2 * 7, pilares, "dois pilares de 7 blocos");
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = 0; dy <= 6; dy++) assertNull(m.get(chave(dx, dy, PlantaEscola.TORII_DO_CAMINHO)));
        }
        assertNotNull(m.get(chave(0, 7, PlantaEscola.TORII_DO_CAMINHO)), "a viga do torii fica acima do vão");
    }

    @Test
    void entradasTemChaoEEspaco() {
        Map<String, Material> m = mapa(PlantaEscola.cenario());
        CaixaArena caixa = CaixaArena.daVaga(0);
        for (Vetor p : new Vetor[] {caixa.entradaJogador(), caixa.entradaToji(), caixa.centro()}) {
            int dx = (int) Math.floor(p.x()) - caixa.centroX(), dz = (int) Math.floor(p.z()) - caixa.centroZ();
            assertTrue(Math.max(Math.abs(dx), Math.abs(dz)) < P, "entrada fora do pátio: " + dx + "," + dz);
            assertNotNull(m.get(chave(dx, -1, dz)), "sem chão na entrada " + dx + "," + dz);
            for (int dy = 0; dy <= 2; dy++) assertNull(m.get(chave(dx, dy, dz)), "entrada obstruída em " + chave(dx, dy, dz));
        }
    }

    @Test
    void escadariaDesceParaOSulComOsToriiEmFila() {
        Map<String, Material> m = mapa(PlantaEscola.cenario());
        for (int k = 0; k < PlantaEscola.DEGRAUS; k++) {
            int degrau = PlantaEscola.ESCADA_EM + 2 * k, patamar = degrau + 1;
            for (int dx = -PlantaEscola.MEIA_ESCADA; dx <= PlantaEscola.MEIA_ESCADA; dx++) {
                assertEquals(Material.ESCADA_PEDRA, m.get(chave(dx, -1 - k, degrau)), "degrau " + k + " em dx " + dx);
                assertEquals(Material.TIJOLO_PEDRA, m.get(chave(dx, -2 - k, patamar)), "patamar " + k + " em dx " + dx);
                // Dá para andar: quatro blocos livres acima de cada fileira.
                for (int dy = 0; dy <= 3; dy++) {
                    assertNull(m.get(chave(dx, -k + dy, degrau)), "degrau " + k + " obstruído");
                    assertNull(m.get(chave(dx, -1 - k + dy, patamar)), "patamar " + k + " obstruído");
                }
            }
        }
        for (int k : PlantaEscola.TORII_DA_ESCADA) {
            int dz = PlantaEscola.ESCADA_EM + 1 + 2 * k;
            for (int lado : new int[] {-1, 1}) {
                int dx = lado * PlantaEscola.PILAR_DA_ESCADA;
                assertEquals(Material.MADEIRA_VERMELHA, m.get(chave(dx, -1 - k, dz)), "pilar do torii do degrau " + k);
                assertNotNull(m.get(chave(dx, -2 - k, dz)), "pilar sem chão no degrau " + k);
            }
            assertEquals(Material.MADEIRA_ESCURA, m.get(chave(0, -1 - k + 7, dz)), "viga do torii do degrau " + k);
        }
        // O torii grande na cabeça da escadaria: vão de 11 de largura e 9 de altura.
        int dz = PlantaEscola.TORII_GRANDE_EM;
        for (int dx = -PlantaEscola.PILAR_GRANDE + 1; dx < PlantaEscola.PILAR_GRANDE; dx++) {
            for (int dy = 0; dy < PlantaEscola.VAO_GRANDE; dy++) assertNull(m.get(chave(dx, dy, dz)), "vão do torii grande");
        }
        assertEquals(Material.MADEIRA_VERMELHA, m.get(chave(PlantaEscola.PILAR_GRANDE, 0, dz)));
        assertEquals(Material.MADEIRA_ESCURA, m.get(chave(0, PlantaEscola.VAO_GRANDE + 1, dz)), "a viga de cima é escura");
    }

    @Test
    void prediosSaoOcosTemPortaParaOPatioETelhadoFechado() {
        Map<String, Material> m = mapa(PlantaEscola.cenario());
        for (PlantaEscola.Predio p : PlantaEscola.PREDIOS) {
            int mx = (p.x0() + p.x1()) / 2, mz = (p.z0() + p.z1()) / 2;
            int topo = p.podio() + p.parede() - 1;
            // Por dentro: assoalho e ar até a viga.
            for (int dx = p.x0() + 1; dx < p.x1(); dx++) {
                for (int dz = p.z0() + 1; dz < p.z1(); dz++) {
                    assertEquals(Material.ASSOALHO, m.get(chave(dx, p.podio() - 1, dz)), "assoalho em " + dx + "," + dz);
                    for (int dy = p.podio() + 1; dy <= topo; dy++) assertNull(m.get(chave(dx, dy, dz)), "prédio não é oco");
                    // Nenhuma coluna aberta para o céu.
                    boolean coberto = false;
                    for (int dy = topo + 1; dy <= topo + 16; dy++) coberto |= m.get(chave(dx, dy, dz)) != null;
                    assertTrue(coberto, "telhado furado em " + dx + "," + dz);
                }
            }
            // A porta: 5 de largura e 4 de altura, do lado do pátio.
            for (int i = -2; i <= 2; i++) {
                for (int dy = p.podio(); dy < p.podio() + 4; dy++) {
                    String c = p.porta() == 'S' ? chave(mx + i, dy, p.z1())
                            : chave(p.porta() == 'O' ? p.x0() : p.x1(), dy, mz + i);
                    assertNull(m.get(c), "porta fechada em " + c);
                }
            }
            // Parede branca com pilar escuro no canto, e telha no beiral.
            assertEquals(Material.PILAR, m.get(chave(p.x0(), p.podio(), p.z0())));
            assertEquals(Material.PAREDE_BRANCA, m.get(chave(p.x0() + 1, p.podio(), p.z0())));
            assertNotNull(m.get(chave(p.x0() - PlantaEscola.Predio.BEIRAL, topo + 1, mz)), "sem beiral");
        }
        // O salão tem o segundo telhado: mais alto que os pavilhões.
        int alturaSalao = 0, alturaPavilhao = 0;
        for (int dy = 0; dy < 40; dy++) {
            if (m.get(chave(0, dy, -33)) != null) alturaSalao = dy;
            if (m.get(chave(33, dy, 0)) != null) alturaPavilhao = dy;
        }
        assertTrue(alturaSalao >= alturaPavilhao + 3, "salão " + alturaSalao + " x pavilhão " + alturaPavilhao);
    }

    @Test
    void florestaCercaAEscolaSemEntrarNoPatioNemNosPredios() {
        List<Bloco> blocos = PlantaEscola.cenario();
        Map<String, Material> m = mapa(blocos);
        int arvores = 0;
        for (Bloco b : blocos) {
            if (b.material() != Material.TRONCO && b.material() != Material.FOLHA) continue;
            assertTrue(Math.max(Math.abs(b.dx()), Math.abs(b.dz())) > 24 || b.dz() > P, "árvore no pátio ou na calçada: " + b);
            assertFalse(Math.abs(b.dx()) <= PlantaEscola.PILAR_DA_ESCADA + 2 && b.dz() > P, "árvore na escadaria: " + b);
            for (PlantaEscola.Predio p : PlantaEscola.PREDIOS) {
                int folga = PlantaEscola.Predio.BEIRAL;
                assertFalse(b.dx() >= p.x0() - folga && b.dx() <= p.x1() + folga && b.dz() >= p.z0() - folga
                        && b.dz() <= p.z1() + folga, "árvore dentro de um prédio: " + b);
            }
            // O pé de cada tronco fica em cima do chão.
            if (b.material() == Material.TRONCO && b.dy() == PlantaEscola.chao(b.dz()) + 1) {
                assertEquals(Material.GRAMA, m.get(chave(b.dx(), b.dy() - 1, b.dz())), "árvore fora da grama: " + b);
                arvores++;
            }
        }
        assertTrue(arvores >= 60, "floresta rala: " + arvores + " árvores");
    }

    @Test
    void lanternasTemApoio() {
        List<Bloco> blocos = PlantaEscola.cenario();
        Map<String, Material> m = mapa(blocos);
        int lanternas = 0;
        for (Bloco b : blocos) {
            if (b.material() != Material.LANTERNA) continue;
            assertNotNull(m.get(chave(b.dx(), b.dy() - 1, b.dz())), "lanterna sem apoio: " + b);
            lanternas++;
        }
        assertTrue(lanternas >= 14 + 12, "lanternas do pátio e de dentro dos prédios: " + lanternas);
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
