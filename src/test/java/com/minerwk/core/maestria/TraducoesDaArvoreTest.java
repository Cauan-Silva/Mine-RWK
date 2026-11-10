package com.minerwk.core.maestria;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.personagens.gojo.ArvoreGojo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TraducoesDaArvoreTest {
    @Test
    void todoNoDoGojoTemNomeEDescricaoNasDuasLinguas() throws IOException {
        for (String lingua : new String[] {"pt_br", "en_us"}) {
            String json = Files.readString(Path.of("src/main/resources/assets/minerwk/lang/" + lingua + ".json"),
                    StandardCharsets.UTF_8);
            for (No no : ArvoreGojo.ARVORE.nos()) {
                String chave = "\"no.minerwk." + no.id();
                assertTrue(json.contains(chave + "\""), lingua + ": falta o nome de " + no.id());
                assertTrue(json.contains(chave + ".desc\""), lingua + ": falta a descrição de " + no.id());
            }
        }
    }
}
