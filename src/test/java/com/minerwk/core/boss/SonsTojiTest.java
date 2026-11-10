package com.minerwk.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Confere os sons gerados por {@code ferramentas/gerar_sfx_toji.py} contra {@link SonsToji} e os tempos da
 * luta, e a cutscene do despertar contra a queda. Mudou um tempo no Java, mudar no gerador e gerar de novo.
 */
class SonsTojiTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/minerwk");
    private static final Path MANIFESTO = Path.of("ferramentas/sfx_toji_manifesto.json");

    private static JsonObject ler(Path caminho) throws IOException {
        try (Reader r = Files.newBufferedReader(caminho, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    @Test
    void manifestoTemExatamenteOsSonsDoToji() throws IOException {
        Set<String> bases = new TreeSet<>();
        for (String arquivo : ler(MANIFESTO).keySet()) bases.add(arquivo.replaceAll("_[23]$", ""));
        assertEquals(new TreeSet<>(SonsToji.TODOS), bases, "gerador e SonsToji com a mesma lista");
    }

    @Test
    void cadaSomTemArquivosEntradaNoSoundsJsonELegenda() throws IOException {
        JsonObject sons = ler(ASSETS.resolve("sounds.json"));
        JsonObject pt = ler(ASSETS.resolve("lang/pt_br.json"));
        JsonObject en = ler(ASSETS.resolve("lang/en_us.json"));
        for (String id : SonsToji.TODOS) {
            assertTrue(sons.has(id), "sounds.json sem " + id);
            assertEquals(3, sons.getAsJsonObject(id).getAsJsonArray("sounds").size(), "3 variações de " + id);
            for (String arquivo : new String[] {id, id + "_2", id + "_3"}) {
                assertTrue(Files.exists(ASSETS.resolve("sounds/toji/" + arquivo + ".ogg")), "falta " + arquivo + ".ogg");
            }
            assertTrue(pt.has("subtitles.minerwk." + id), "legenda pt de " + id);
            assertTrue(en.has("subtitles.minerwk." + id), "legenda en de " + id);
        }
    }

    @Test
    void coracaoDaQuedaVaiAteOTickDeLevantar() throws IOException {
        JsonObject queda = ler(MANIFESTO).getAsJsonObject(SonsToji.QUEDA);
        assertEquals(TojiBoss.QUEDA, queda.getAsJsonObject("marcos").get("levanta").getAsInt());
        assertTrue(queda.get("ticks").getAsInt() >= TojiBoss.QUEDA, "o som cobre a queda inteira");
    }

    @Test
    void cutsceneCobreAQuedaECortaQuandoEleSeLevanta() throws IOException {
        assertEquals(TojiBoss.QUEDA, CutsceneDespertar.LEVANTA);
        assertEquals(TojiBoss.QUEDA + CutsceneDespertar.DEPOIS, CutsceneDespertar.COREOGRAFIA.duracao());
        assertTrue(CutsceneDespertar.COREOGRAFIA.quadro(CutsceneDespertar.LEVANTA).posicao().y() < 0,
                "depois do corte a câmera olha de baixo");
        JsonObject pt = ler(ASSETS.resolve("lang/pt_br.json"));
        for (var palavra : CutsceneDespertar.PALAVRAS) {
            assertTrue(palavra.inicio() >= CutsceneDespertar.LEVANTA, "a palavra aparece quando ele se levanta");
            assertTrue(pt.has(palavra.chave()) && pt.has(palavra.chave() + ".jp"), "tradução de " + palavra.chave());
        }
    }
}
