package com.minerwk.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * As gravações de terceiros que os geradores de som misturam à síntese ({@code Som.amostra} em
 * {@code ferramentas/sfx_motor.py}): toda gravação em {@code art/audio/cc0} tem fonte e licença CC0 no
 * {@code fontes.json}, e cada autor aparece nos créditos.
 */
class GravacoesCc0Test {
    private static final Path PASTA = Path.of("art/audio/cc0");

    private static JsonObject fontes() throws IOException {
        try (Reader r = Files.newBufferedReader(PASTA.resolve("fontes.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    @Test
    void todaGravacaoTemFonteELicencaCc0() throws IOException {
        Set<String> arquivos = new TreeSet<>();
        try (Stream<Path> todos = Files.walk(PASTA)) {
            todos.filter(p -> p.toString().endsWith(".ogg"))
                    .forEach(p -> arquivos.add(PASTA.relativize(p).toString().replace('\\', '/')));
        }
        JsonObject fontes = fontes();
        assertFalse(arquivos.isEmpty());
        assertEquals(arquivos, new TreeSet<>(fontes.keySet()), "cada gravação no fontes.json, e nada a mais");
        for (String arquivo : arquivos) {
            JsonObject f = fontes.getAsJsonObject(arquivo);
            assertEquals("CC0 1.0", f.get("licenca").getAsString(), arquivo);
            assertTrue(f.get("origem").getAsString().startsWith("https://"), arquivo);
            assertFalse(f.get("autor").getAsString().isBlank(), arquivo);
        }
    }

    @Test
    void cadaAutorEstaNosCreditos() throws IOException {
        String creditos = Files.readString(Path.of("THIRD_PARTY_NOTICES.md"), StandardCharsets.UTF_8);
        for (String arquivo : fontes().keySet()) {
            String autor = fontes().getAsJsonObject(arquivo).get("autor").getAsString();
            assertTrue(creditos.contains(autor), "créditos sem " + autor);
        }
    }
}
