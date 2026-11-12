package com.minerwk.core.personagens.cid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.maestria.Melhorias;
import com.minerwk.core.maestria.No;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArvoreCidTest {
    @Test
    void aArvoreTemOnzeNosSemRepetirEODespertarNoMeio() {
        Set<String> ids = new HashSet<>();
        for (No no : ArvoreCid.ARVORE.nos()) assertTrue(ids.add(no.id()), "repetido: " + no.id());
        assertEquals(11, ids.size());
        assertTrue(ids.contains(ArvoreCid.DESPERTAR));
        assertEquals(ArvoreCid.ARVORE, Cid.criar().arvore(), "o kit do Cid usa esta árvore");
    }

    @Test
    void todoNoTemNomeEDescricaoNasDuasLinguas() throws IOException {
        for (String lingua : new String[] {"pt_br", "en_us"}) {
            String json = Files.readString(Path.of("src/main/resources/assets/minerwk/lang/" + lingua + ".json"),
                    StandardCharsets.UTF_8);
            for (No no : ArvoreCid.ARVORE.nos()) {
                String chave = "\"no.minerwk." + no.id();
                assertTrue(json.contains(chave + "\""), lingua + ": falta o nome de " + no.id());
                assertTrue(json.contains(chave + ".desc\""), lingua + ": falta a descrição de " + no.id());
            }
        }
    }

    @Test
    void aReservaEOFluxoDaMagiaVemDaArvore() {
        Magia m = new Magia();
        m.melhorias(new Melhorias(ArvoreCid.ARVORE, Map.of(ArvoreCid.MAGIA_RESERVA, 2)));
        assertEquals(Magia.MAXIMO * 1.2, m.maximo(), 1e-9);
        m.carregar(500);
        assertEquals(Magia.MAXIMO * 1.2, m.atual(), 1e-9, "enche até o máximo novo");
        m.melhorias(Melhorias.NENHUMA);
        assertEquals(Magia.MAXIMO, m.atual(), 1e-9, "sem os nós, o que passa do máximo some");
    }
}
