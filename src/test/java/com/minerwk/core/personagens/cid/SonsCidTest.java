package com.minerwk.core.personagens.cid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minerwk.core.personagens.SonsDosVisuais;
import com.minerwk.core.personagens.gojo.SonsGojo;
import com.minerwk.core.personagens.ichigo.SonsIchigo;
import com.minerwk.core.vfx.FormasCid;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Confere os sons gerados por {@code ferramentas/gerar_sfx_cid.py} contra o kit: cada som existe com as
 * variações, está no {@code sounds.json} com legenda, sai do visual certo e os marcos dele batem com os
 * ticks do Java. Mudou um tempo no Java, mudar no gerador e gerar de novo.
 */
class SonsCidTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/minerwk");
    private static final Path MANIFESTO = Path.of("ferramentas/sfx_cid_manifesto.json");
    private static final int VARIACOES = 3;

    private static JsonObject ler(Path p) throws IOException {
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static int marco(JsonObject manifesto, String som, String nome) {
        return manifesto.getAsJsonObject(som).getAsJsonObject("marcos").get(nome).getAsInt();
    }

    @Test
    void manifestoTemExatamenteOsSonsDoKit() throws IOException {
        Set<String> bases = new TreeSet<>();
        for (String arquivo : ler(MANIFESTO).keySet()) bases.add(arquivo.replaceAll("_[23]$", ""));
        assertEquals(new TreeSet<>(SonsCid.TODOS), bases, "gerador e SonsCid com a mesma lista");
    }

    @Test
    void idsNaoRepetemNemBatemComOsDosOutros() {
        assertEquals(SonsCid.TODOS.size(), new HashSet<>(SonsCid.TODOS).size());
        for (String id : SonsCid.TODOS) {
            assertTrue(id.startsWith("cid_"), id);
            assertFalse(SonsGojo.TODOS.contains(id) || SonsIchigo.TODOS.contains(id), id);
        }
    }

    @Test
    void cadaSomTemVariacoesEntradaNoSoundsJsonELegendaNasDuasLinguas() throws IOException {
        JsonObject sons = ler(ASSETS.resolve("sounds.json"));
        JsonObject pt = ler(ASSETS.resolve("lang/pt_br.json")), en = ler(ASSETS.resolve("lang/en_us.json"));
        for (String som : SonsCid.TODOS) {
            assertTrue(sons.has(som), som + " no sounds.json");
            assertEquals(VARIACOES, sons.getAsJsonObject(som).getAsJsonArray("sounds").size(), som);
            assertEquals("subtitles.minerwk." + som, sons.getAsJsonObject(som).get("subtitle").getAsString());
            assertTrue(pt.has("subtitles.minerwk." + som) && en.has("subtitles.minerwk." + som), "legenda de " + som);
            for (int v = 1; v <= VARIACOES; v++) {
                Path ogg = ASSETS.resolve("sounds/cid/" + som + (v == 1 ? "" : "_" + v) + ".ogg");
                assertTrue(Files.size(ogg) > 1000, ogg + " existe e tem áudio");
            }
        }
    }

    @Test
    void cadaVisualDoKitTemOSeuSomEOClienteOAcha() {
        // Os visuais que o kit manda: os quatro golpes (a Prisão manda dois) e o traje.
        Set<String> usados = new HashSet<>();
        for (String tipo : new String[] {Cid.Redemoinho.VISUAL, Cid.PosImagem.VISUAL, Cid.Prisao.VISUAL, Cid.Prisao.CORRENTES,
                Cid.OVERDRIVE, Cid.TRAJE}) {
            usados.add(SonsCid.doVisual(tipo).orElseThrow());
            assertEquals(SonsCid.doVisual(tipo), SonsDosVisuais.doVisual(tipo), tipo);
        }
        assertEquals(new HashSet<>(SonsCid.TODOS), usados, "nenhum som gerado à toa");
        assertTrue(SonsCid.doVisual("cid_nada").isEmpty());
        assertTrue(SonsCid.doVisual("gojo_azul").isEmpty(), "visual de outro personagem");
        // Os outros personagens continuam com os seus.
        assertEquals(SonsGojo.AZUL, SonsDosVisuais.doVisual("gojo_azul").orElseThrow());
        assertEquals(SonsIchigo.ONDA, SonsDosVisuais.doVisual("ichigo_onda").orElseThrow());
    }

    @Test
    void soOZumbidoDoOverdriveParaComOVisual() {
        assertTrue(SonsCid.cortaComOVisual(Cid.OVERDRIVE), "desligou com G: o zumbido para");
        assertTrue(SonsDosVisuais.cortaComOVisual(Cid.OVERDRIVE));
        assertFalse(SonsCid.cortaComOVisual(Cid.Prisao.VISUAL), "o slime que bate cedo ainda soa o arremesso");
        assertFalse(SonsCid.cortaComOVisual(Cid.Redemoinho.VISUAL));
        assertFalse(SonsCid.cortaComOVisual(Cid.Prisao.CORRENTES));
        assertFalse(SonsDosVisuais.cortaComOVisual("ichigo_onda"));
        assertTrue(SonsDosVisuais.antesDoFim(Cid.OVERDRIVE).isEmpty(), "nenhum som do Cid toca no fim de um visual");
    }

    @Test
    void marcosDosSonsBatemComOsTicksDoJava() throws IOException {
        JsonObject m = ler(MANIFESTO);
        assertEquals(Cid.Redemoinho.ENTRE_CORTES, marco(m, SonsCid.REDEMOINHO, "entre_cortes"), "um corte a cada 5 ticks");
        assertEquals(FormasCid.VIDA_DO_CORTE, marco(m, SonsCid.REDEMOINHO, "vida_do_corte"));
        assertEquals(FormasCid.REDEMOINHO, marco(m, SonsCid.REDEMOINHO, "fim"));
        assertEquals(FormasCid.RISCOS, marco(m, SonsCid.POS_IMAGEM, "fim"));
        assertEquals((int) Math.ceil(Cid.Prisao.ALCANCE / Cid.Prisao.PASSO), marco(m, SonsCid.SLIME, "fim"), "o voo inteiro do slime");
        assertEquals(FormasCid.FECHAR, marco(m, SonsCid.CORRENTES, "fecha"), "o estalo cai quando elas fecham");
        assertEquals(FormasCid.SOLTAR, marco(m, SonsCid.CORRENTES, "soltar"), "soltam junto com o visual, que some nesses ticks");
        assertEquals(Cid.Prisao.PRESO, marco(m, SonsCid.CORRENTES, "solta"));
        assertEquals(FormasCid.ESTOURO, marco(m, SonsCid.OVERDRIVE, "estouro"));
        assertEquals(Cid.Overdrive.DURACAO, marco(m, SonsCid.OVERDRIVE, "fim"), "o zumbido dura o Overdrive");
        assertEquals(FormasCid.TRAJE, marco(m, SonsCid.TRAJE, "fim"));
        for (String som : SonsCid.TODOS) {
            JsonObject s = m.getAsJsonObject(som);
            assertFalse(s.getAsJsonObject("marcos").isEmpty(), som + " tem ao menos um marco conferido");
            for (String nome : s.getAsJsonObject("marcos").keySet()) {
                assertTrue(marco(m, som, nome) <= s.get("ticks").getAsInt(), som + "." + nome + " cabe no som");
            }
        }
    }
}
