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
            "gojo_roxo_200", 60,
            "gojo_vazio_infinito", 40);

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
            assertTrue(ossos.contains("right_leg") || ossos.contains("body"), e.getKey() + " precisa mexer pernas ou o corpo");
        }
    }
}
