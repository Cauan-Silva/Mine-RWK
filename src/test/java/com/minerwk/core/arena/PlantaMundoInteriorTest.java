package com.minerwk.core.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.arena.PlantaEscola.Bloco;
import com.minerwk.core.arena.PlantaEscola.Material;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;
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
                Material sala = mapa.get(b.dx() + ",-2," + b.dz());
                assertTrue(sala == Material.FUNDO_ESCURO || sala == Material.JANELA_ACESA, "embaixo do vidro: " + sala);
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

    @Test
    void haJanelasAcesasEmTodosOsPredios() {
        boolean luta = false, alto = false, baixo = false, claro = false;
        for (Bloco b : CENARIO) {
            if (b.material() == Material.VIDRO_CLARO && Math.abs(b.dx()) <= PlantaMundoInterior.MEIO_PREDIO && b.dy() == -1) claro = true;
            if (b.material() != Material.JANELA_ACESA) continue;
            if (Math.abs(b.dx()) <= PlantaMundoInterior.MEIO_PREDIO && b.dy() == -2) luta = true;
            if (b.dx() >= PlantaMundoInterior.ALTO_DE) alto = true;
            if (b.dx() <= PlantaMundoInterior.BAIXO_ATE) baixo = true;
        }
        assertTrue(luta && alto && baixo, "janelas acesas na luta, no alto e no baixo: " + luta + ", " + alto + ", " + baixo);
        assertTrue(claro, "vidro claro na fachada da luta");
    }

    @Test
    void detalhesForaDoCampoDaLuta() {
        Set<Material> detalhes = EnumSet.of(Material.GRADE_FERRO, Material.PARA_RAIOS, Material.LETREIRO_VERMELHO,
                Material.LETREIRO_AMARELO, Material.ASSOALHO);
        int raios = 0, vermelhos = 0, amarelos = 0;
        for (Bloco b : CENARIO) {
            // A face do atravessado (de dz −48 a −38) fica fora do campo, mesmo acima dele.
            if (detalhes.contains(b.material()) && b.dz() > PlantaMundoInterior.CRUZADO_ATE) {
                assertTrue(Math.abs(b.dx()) > PlantaMundoInterior.MEIO_PREDIO || b.dy() < 0, "detalhe sobre o campo da luta: " + b);
            }
            if (b.material() == Material.PARA_RAIOS) raios++;
            if (b.material() == Material.LETREIRO_VERMELHO) vermelhos++;
            if (b.material() == Material.LETREIRO_AMARELO) amarelos++;
        }
        assertTrue(raios >= 4, "antenas: " + raios);
        assertTrue(vermelhos >= 2 && amarelos >= 2, "letreiros: " + vermelhos + " e " + amarelos);
    }

    @Test
    void asAntenasTemApoio() {
        Map<String, Material> mapa = porPosicao();
        for (Bloco b : CENARIO) {
            if (b.material() != Material.PARA_RAIOS) continue;
            int y = b.dy() - 1;
            assertEquals(Material.GRADE_FERRO, mapa.get(b.dx() + "," + y + "," + b.dz()), "para-raios sem antena: " + b);
            while (mapa.get(b.dx() + "," + y + "," + b.dz()) == Material.GRADE_FERRO) y--;
            assertNotNull(mapa.get(b.dx() + "," + y + "," + b.dz()), "antena solta no ar: " + b);
        }
    }

    @Test
    void aFachadaDaLutaContinuaInteira() {
        Map<String, Material> mapa = porPosicao();
        Set<Material> chao = EnumSet.of(Material.CONCRETO_CLARO, Material.VIDRO_AZUL, Material.VIDRO_CLARO);
        for (int dx = -PlantaMundoInterior.MEIO_PREDIO; dx <= PlantaMundoInterior.MEIO_PREDIO; dx++) {
            for (int dz = -CaixaArena.MEIA; dz <= CaixaArena.MEIA; dz++) {
                Material m = mapa.get(dx + ",-1," + dz);
                assertTrue(m != null && chao.contains(m), "chão da luta em " + dx + ", " + dz + ": " + m);
            }
        }
    }

    /** Escreve a planta em texto para o script de prévia: {@code python ferramentas/previa_arena.py interior}. */
    @Test
    void despejaAPlantaParaAPrevia() throws IOException {
        StringBuilder s = new StringBuilder();
        for (Bloco b : CENARIO) {
            s.append(b.dx()).append(' ').append(b.dy()).append(' ').append(b.dz()).append(' ').append(b.material()).append('\n');
        }
        Path saida = Path.of("build", "arena_interior.txt");
        Files.createDirectories(saida.getParent());
        Files.writeString(saida, s);
        assertTrue(Files.size(saida) > 0);
    }
}
