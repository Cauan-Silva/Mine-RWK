package com.minerwk.core.traje;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minerwk.core.traje.TrajeGojo.Lado;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class TrajeGojoTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/minerwk");

    @Test
    void soOGojoTemOTrajeDoGojo() {
        assertEquals("gojo", TrajeGojo.de("gojo").orElseThrow());
        assertTrue(TrajeGojo.de("teste").isEmpty());
        assertTrue(TrajeGojo.de("").isEmpty(), "quem não escolheu personagem não tem traje");
    }

    @Test
    void vendaSaiNoDominioENoRInteiro() {
        assertFalse(TrajeGojo.vendaFora(List.of()), "normalmente a venda fica no rosto");
        for (String golpe : List.of("gojo_azul", "gojo_vermelho", "gojo_vermelho_carga", "gojo_infinito", "gojo_roxo",
                "gojo_roxo_carga", "gojo_azul_marca")) {
            assertFalse(TrajeGojo.vendaFora(List.of(golpe)), golpe + " não tira a venda");
        }
        for (String golpe : List.of("gojo_vazio_infinito", "gojo_roxo_200_carga", "gojo_roxo_maximo",
                "gojo_roxo_maximo_esferas", "gojo_roxo_explosao")) {
            assertTrue(TrajeGojo.vendaFora(List.of("gojo_azul", golpe)), golpe + " tira a venda");
        }
    }

    @Test
    void barraParadaQuaseNaoMexe() {
        for (Lado lado : Lado.values()) {
            for (int linha = 0; linha < 2; linha++) {
                for (double t = 0; t < 40; t += 3) {
                    assertTrue(Math.abs(TrajeGojo.balanco(lado, linha, 0, 0, 0, t)) <= .1, "só respira");
                }
            }
        }
    }

    @Test
    void correndoABarraDeTrasAbreEADaFrenteCola() {
        assertTrue(TrajeGojo.balanco(Lado.TRAS, 0, .25, 0, 0, 0) > .3, "o vento da corrida levanta a barra de trás");
        assertTrue(TrajeGojo.balanco(Lado.FRENTE, 0, .25, 0, 0, 0) <= .05, "a da frente cola na perna");
        assertTrue(TrajeGojo.balanco(Lado.ESQUERDA, 0, 0, .25, 0, 0) > TrajeGojo.balanco(Lado.DIREITA, 0, 0, .25, 0, 0),
                "andando para a direita, abre a barra da esquerda");
    }

    @Test
    void caindoTodasAsBarrasSobemEOBalancoTemLimite() {
        for (Lado lado : Lado.values()) {
            assertTrue(TrajeGojo.balanco(lado, 0, 0, 0, -.6, 0) > .3, "caindo, a barra sobe");
            for (int linha = 0; linha < 2; linha++) {
                double a = TrajeGojo.balanco(lado, linha, 5, 5, -5, 7);
                assertTrue(a <= TrajeGojo.BALANCO_MAX && a >= -TrajeGojo.BALANCO_MAX, "nunca dobra demais");
            }
        }
    }

    @Test
    void nomesDosOssosDaBarraBatemComOModelo() throws IOException {
        Map<String, JsonObject> ossos = ossos();
        int barras = 0;
        for (Lado lado : Lado.values()) {
            for (int coluna = 0; coluna < lado.colunas(); coluna++) {
                for (int linha = 0; linha < 2; linha++) {
                    assertTrue(ossos.containsKey(TrajeGojo.ossoDaBarra(lado, coluna, linha)),
                            TrajeGojo.ossoDaBarra(lado, coluna, linha));
                    barras++;
                }
            }
        }
        assertEquals(20, barras);
    }

    @Test
    void modeloTemVendaEOlhosNaCabecaESemOculos() throws IOException {
        Map<String, JsonObject> ossos = ossos();
        assertEquals("armorHead", ossos.get(TrajeGojo.OSSO_VENDA).get("parent").getAsString());
        assertEquals("armorHead", ossos.get(TrajeGojo.OSSO_OLHOS).get("parent").getAsString());
        assertTrue(ossos.get(TrajeGojo.OSSO_VENDA).getAsJsonArray("cubes").size() >= 1);
        assertEquals(2, ossos.get(TrajeGojo.OSSO_OLHOS).getAsJsonArray("cubes").size(), "um cubo por olho");
        JsonElement cabeca = ossos.get("armorHead").get("cubes");
        assertTrue(cabeca == null || cabeca.getAsJsonArray().isEmpty(), "os óculos do Minezin saíram; sem cabelo");
    }

    @Test
    void texturaEMascaraDeBrilhoTemOTamanhoDoModeloESoOsOlhosBrilham() throws IOException {
        JsonObject desc = geometria().getAsJsonObject("description");
        BufferedImage textura = ImageIO.read(ASSETS.resolve("textures/traje/gojo.png").toFile());
        BufferedImage brilho = ImageIO.read(ASSETS.resolve("textures/traje/gojo_glowmask.png").toFile());
        assertEquals(desc.get("texture_width").getAsInt(), textura.getWidth());
        assertEquals(desc.get("texture_height").getAsInt(), textura.getHeight());
        assertEquals(textura.getWidth(), brilho.getWidth());
        assertEquals(textura.getHeight(), brilho.getHeight());
        int acesos = 0;
        for (int x = 0; x < brilho.getWidth(); x++) {
            for (int y = 0; y < brilho.getHeight(); y++) {
                if ((brilho.getRGB(x, y) >>> 24) > 0) acesos++;
            }
        }
        assertTrue(acesos > 0 && acesos <= 16, "só os poucos pixels dos olhos brilham: " + acesos);
    }

    private static JsonObject geometria() throws IOException {
        try (Reader r = Files.newBufferedReader(ASSETS.resolve("geo/traje/gojo.geo.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        }
    }

    private static Map<String, JsonObject> ossos() throws IOException {
        Map<String, JsonObject> mapa = new HashMap<>();
        JsonArray lista = geometria().getAsJsonArray("bones");
        for (JsonElement o : lista) mapa.put(o.getAsJsonObject().get("name").getAsString(), o.getAsJsonObject());
        return mapa;
    }
}
