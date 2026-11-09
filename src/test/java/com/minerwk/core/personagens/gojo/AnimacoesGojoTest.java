package com.minerwk.core.personagens.gojo;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minerwk.core.kit.Habilidade;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Confere o arquivo de animações do Gojo contra os golpes do kit. */
class AnimacoesGojoTest {
    private static final Path ARQUIVO = Path.of("src/main/resources/assets/minerwk/player_animations/gojo.json");
    /** Ossos que a Player Animation Library conhece no modelo do jogador. */
    private static final Set<String> OSSOS = Set.of("head", "body", "torso", "right_arm", "left_arm",
            "right_leg", "left_leg", "right_item", "left_item");
    /** Quanto dura o golpe depois da carga, em ticks (o visual principal de cada um). */
    private static final Map<String, Integer> DEPOIS_DA_CARGA = Map.of(
            "gojo_azul", 60,
            "gojo_vermelho", 30,
            "gojo_infinito", 20,
            "gojo_roxo", 40,
            // O R só cobre o avanço; o uppercut e o resto têm animações próprias (abaixo).
            "gojo_roxo_200", RoxoMaximo.AVANCO_MAX,
            "gojo_vazio_infinito", 40);

    /** Golpes que se usam andando: Z, X, C e V. */
    private static final List<String> ANDAM = List.of("gojo_azul", "gojo_vermelho", "gojo_infinito", "gojo_roxo");

    @Test
    void oRTemAnimacaoDoUppercutEDoCasoSemAlvoAteOFimDaExplosao() throws IOException {
        JsonObject animacoes = animacoes();
        int aposSoco = RoxoMaximo.fim(0);
        int semAlvo = RoxoMaximo.TICKS_ESFERAS_SEM_ALVO + RoxoMaximo.TICKS_FUSAO + RoxoMaximo.TICKS_EXPLOSAO;
        assertTrue(animacoes.getAsJsonObject(Gojo.ANIM_R_UPPERCUT).get("animation_length").getAsDouble() * 20 >= aposSoco);
        assertTrue(animacoes.getAsJsonObject(Gojo.ANIM_R_SEM_ALVO).get("animation_length").getAsDouble() * 20 >= semAlvo);
    }

    @Test
    void descidaDoREmLoopSemBody() throws IOException {
        JsonObject descida = animacoes().getAsJsonObject(Gojo.ANIM_R_DESCIDA);
        assertNotNull(descida, "falta a animação de descida");
        assertTrue(descida.get("loop").getAsBoolean(), "a descida repete até ele pousar");
    }

    /** Quadros-chave de um osso entre {@code de} e {@code ate} segundos. */
    private static long quadros(String anim, String osso, double de, double ate) throws IOException {
        return animacoes().getAsJsonObject(anim).getAsJsonObject("bones").getAsJsonObject(osso)
                .getAsJsonObject("rotation").keySet().stream()
                .mapToDouble(Double::parseDouble).filter(t -> t >= de && t <= ate).count();
    }

    @Test
    void encantamentoENoArSeMexemSemParar() throws IOException {
        // Quatro gestos com transição e tremor: muitos quadros nos braços durante os 4 s.
        assertTrue(quadros("gojo_roxo_200", "right_arm", 0, 4) >= 20, "encantamento com mais gestos no braço direito");
        assertTrue(quadros("gojo_roxo_200", "left_arm", 0, 4) >= 20, "e no esquerdo");
        assertTrue(quadros("gojo_roxo_200", "torso", 0, 4) >= 10, "o corpo respira e balança");
        // No ar, do fim do uppercut até a explosão, as pernas e o tronco flutuam.
        assertTrue(quadros(Gojo.ANIM_R_UPPERCUT, "right_leg", .6, 1.75) >= 4, "perna flutuando");
        assertTrue(quadros(Gojo.ANIM_R_UPPERCUT, "torso", 1.75, 2.55) >= 3, "reação à explosão");
    }

    private static JsonObject animacoes() throws IOException {
        try (Reader r = Files.newBufferedReader(ARQUIVO, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("animations");
        }
    }

    @Test
    void cadaGolpeTemAnimacaoComOMesmoIdQueCobreACargaEOGolpe() throws IOException {
        JsonObject animacoes = animacoes();
        for (Habilidade h : Gojo.criar().habilidades().values()) {
            assertTrue(animacoes.has(h.id()), "falta a animação " + h.id());
            double segundos = animacoes.getAsJsonObject(h.id()).get("animation_length").getAsDouble();
            int minimo = h.numeros().cargaMax() + DEPOIS_DA_CARGA.get(h.id());
            assertTrue(segundos * 20 >= minimo - 1e-6, h.id() + " dura " + segundos + " s, precisa de " + minimo + " ticks");
        }
    }

    @Test
    void todaAnimacaoMexeOCorpoInteiroComOssosQueExistem() throws IOException {
        for (Map.Entry<String, com.google.gson.JsonElement> e : animacoes().entrySet()) {
            Set<String> ossos = e.getValue().getAsJsonObject().getAsJsonObject("bones").keySet();
            for (String osso : ossos) assertTrue(OSSOS.contains(osso), e.getKey() + ": osso desconhecido " + osso);
            assertTrue(ossos.containsAll(Set.of("head", "torso", "right_arm", "left_arm")),
                    e.getKey() + " precisa mexer cabeça, tronco e os dois braços");
            if (e.getKey().startsWith("gojo_roxo_200") || e.getKey().startsWith("gojo_roxo_maximo")) {
                // O Gojo sobe de verdade no R: o corpo do modelo não sobe de novo por cima.
                assertFalse(ossos.contains("body"), e.getKey() + " não pode mexer o body");
                continue;
            }
            boolean andando = ANDAM.stream().anyMatch(e.getKey()::startsWith);
            if (andando) {
                // Usados andando: as pernas e o corpo ficam com a passada normal do jogo.
                for (String livre : List.of("right_leg", "left_leg", "body")) {
                    assertFalse(ossos.contains(livre), e.getKey() + " não pode prender " + livre);
                }
            } else {
                assertTrue(ossos.contains("right_leg"), e.getKey() + " precisa mexer as pernas");
            }
        }
    }
}
