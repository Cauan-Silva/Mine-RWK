package com.minerwk.core.hud;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.hud.FormaArvore.Galho;
import com.minerwk.core.hud.FormaArvore.Selo;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.maestria.No;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FormaArvoreTest {
    private static final Path ICONES = Path.of("src/main/resources/assets/minerwk/textures/gui/icones");

    @Test
    void todoNoApareceEmExatamenteUmGalhoDoLadoCerto() {
        for (Personagem p : IconesTest.jogaveis()) {
            FormaArvore forma = FormasArvore.de(p.id()).orElseThrow(() -> new AssertionError("sem forma: " + p.id()));
            Map<String, No> nos = new HashMap<>();
            p.arvore().nos().forEach(n -> nos.put(n.id(), n));
            Map<String, Integer> vezes = new HashMap<>();
            for (Galho g : forma.galhos()) {
                assertFalse(g.nos().isEmpty(), g.id());
                assertTrue(Files.exists(ICONES.resolve(g.icone() + ".png")), "ícone do galho " + g.id());
                for (String id : g.nos()) {
                    No no = nos.get(id);
                    assertNotNull(no, "o galho " + g.id() + " cita " + id + ", que não existe");
                    assertEquals(g.depoisDoDespertar(), no.depoisDoDespertar(), id + " no galho " + g.id());
                    vezes.merge(id, 1, Integer::sum);
                }
            }
            for (No no : p.arvore().nos()) {
                if (no.tipo() == No.Tipo.DESPERTAR) {
                    assertNull(vezes.get(no.id()), "o Despertar fica no tronco");
                } else {
                    assertEquals(1, vezes.getOrDefault(no.id(), 0), no.id());
                }
            }
            assertEquals(forma.galhos().size(), forma.antes().size() + forma.depois().size());
        }
    }

    @Test
    void seloPeloMapaOuPeloTipo() {
        FormaArvore cid = FormasArvore.de("cid").orElseThrow();
        Map<String, No> nos = new HashMap<>();
        IconesTest.jogaveis().stream().filter(p -> p.id().equals("cid")).findFirst().orElseThrow()
                .arvore().nos().forEach(n -> nos.put(n.id(), n));
        assertEquals(Selo.DURACAO, cid.selo(nos.get("cid_prisao_duracao")));
        assertEquals(Selo.RECURSO, cid.selo(nos.get("cid_magia_reserva")));
        assertEquals(Selo.RECARGA, cid.selo(nos.get("cid_redemoinho_recarga")));
        assertEquals(Selo.FORCA, cid.selo(nos.get("cid_redemoinho_forca")));
    }

    @Test
    void semFormaParaQuemNaoTem() {
        assertTrue(FormasArvore.de("asta").isEmpty());
    }
}
