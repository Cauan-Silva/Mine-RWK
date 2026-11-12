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
 * Confere os sons gerados por {@code ferramentas/gerar_sfx_ragnarok.py} contra {@link SonsRagnarok} e os tempos
 * da luta, e a cena da asa contra a troca de fase. Mudou um tempo no Java, mudar no gerador e gerar de novo.
 */
class SonsRagnarokTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/minerwk");
    private static final Path MANIFESTO = Path.of("ferramentas/sfx_ragnarok_manifesto.json");

    private static JsonObject ler(Path caminho) throws IOException {
        try (Reader r = Files.newBufferedReader(caminho, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    @Test
    void manifestoTemExatamenteOsSonsDoRagnarok() throws IOException {
        Set<String> bases = new TreeSet<>();
        for (String arquivo : ler(MANIFESTO).keySet()) bases.add(arquivo.replaceAll("_[23]$", ""));
        assertEquals(new TreeSet<>(SonsRagnarok.TODOS), bases, "gerador e SonsRagnarok com a mesma lista");
    }

    @Test
    void cadaSomTemArquivosEntradaNoSoundsJsonELegenda() throws IOException {
        JsonObject sons = ler(ASSETS.resolve("sounds.json"));
        JsonObject pt = ler(ASSETS.resolve("lang/pt_br.json"));
        JsonObject en = ler(ASSETS.resolve("lang/en_us.json"));
        for (String id : SonsRagnarok.TODOS) {
            assertTrue(sons.has(id), "sounds.json sem " + id);
            assertEquals(3, sons.getAsJsonObject(id).getAsJsonArray("sounds").size(), "3 variações de " + id);
            for (String arquivo : new String[] {id, id + "_2", id + "_3"}) {
                assertTrue(Files.exists(ASSETS.resolve("sounds/ragnarok/" + arquivo + ".ogg")), "falta " + arquivo + ".ogg");
            }
            assertTrue(pt.has("subtitles.minerwk." + id), "legenda pt de " + id);
            assertTrue(en.has("subtitles.minerwk." + id), "legenda en de " + id);
        }
        assertTrue(pt.has("cutscene.minerwk.ragnarok_asa.1") && en.has("cutscene.minerwk.ragnarok_asa.1.jp"));
    }

    @Test
    void aCargaDoSoproDuraOAvisoInteiro() throws IOException {
        JsonObject carga = ler(MANIFESTO).getAsJsonObject(SonsRagnarok.SOPRO_CARGA);
        assertEquals(RagnarokBoss.SOPRO.aviso(), carga.getAsJsonObject("marcos").get("feixe").getAsInt());
        assertTrue(carga.get("ticks").getAsInt() >= RagnarokBoss.SOPRO.aviso());
        assertTrue(ler(MANIFESTO).getAsJsonObject(SonsRagnarok.SOPRO).get("ticks").getAsInt() >= RagnarokBoss.SOPRO.janela(),
                "o som do feixe cobre a varredura");
    }

    @Test
    void aCenaDaAsaCabeNaTrocaDeFase() {
        assertTrue(CutsceneAsa.CORTE < CutsceneAsa.ASA_NO_CHAO && CutsceneAsa.ASA_NO_CHAO < CutsceneAsa.URRO);
        assertEquals(RagnarokBoss.ASA_CAI, CutsceneAsa.ASA_NO_CHAO, "a asa some do corpo quando chega ao chão");
        assertEquals(RagnarokBoss.TROCA_DE_FASE, CutsceneAsa.COREOGRAFIA.duracao(), 1e-9,
                "a cena acaba quando ele volta a atacar");
    }
}
