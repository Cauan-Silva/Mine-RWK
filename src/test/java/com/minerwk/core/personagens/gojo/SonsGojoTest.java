package com.minerwk.core.personagens.gojo;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minerwk.core.kit.Tecla;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Confere os sons gerados por {@code ferramentas/gerar_sfx_gojo.py} contra os golpes do kit: cada som
 * existe com as variações, está no {@code sounds.json} com legenda, e os marcos dele batem com os ticks
 * do Java. Mudou um tempo no Java, mudar no gerador e gerar de novo.
 */
class SonsGojoTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/minerwk");
    private static final Path MANIFESTO = Path.of("ferramentas/sfx_gojo_manifesto.json");
    private static final int VARIACOES = 3;

    /** Os sons da parte 1 (Z, X, C, V e G). */
    private static final List<String> SONS = SonsGojo.TODOS;

    @Test
    void manifestoTemExatamenteOsSonsDoKit() throws IOException {
        java.util.Set<String> bases = new java.util.TreeSet<>();
        for (String arquivo : ler(MANIFESTO).keySet()) bases.add(arquivo.replaceAll("_[23]$", ""));
        assertEquals(new java.util.TreeSet<>(SONS), bases, "gerador e SonsGojo com a mesma lista");
    }

    @Test
    void sonsDeFaseSaemDoVisual() {
        assertEquals(SonsGojo.AZUL, SonsGojo.doVisual("gojo_azul").orElseThrow());
        assertEquals(SonsGojo.VERMELHO_EXPLOSAO, SonsGojo.doVisual("gojo_vermelho_explosao").orElseThrow());
        assertEquals(SonsGojo.INFINITO_TIC, SonsGojo.doVisual("gojo_infinito_toque").orElseThrow());
        assertTrue(SonsGojo.doVisual("gojo_roxo_rastro").isEmpty(), "visual sem som próprio");
    }

    @Test
    void somDaCargaSaiDoVisualDaCargaEParaComEle() {
        assertEquals(SonsGojo.VERMELHO_CARGA, SonsGojo.doVisual("gojo_vermelho_carga").orElseThrow());
        assertEquals(SonsGojo.ROXO_CARGA, SonsGojo.doVisual("gojo_roxo_carga").orElseThrow());
        assertTrue(SonsGojo.cortaComOVisual("gojo_vermelho_carga"), "soltou cedo ou foi interrompido: o som para");
        assertTrue(SonsGojo.cortaComOVisual("gojo_roxo_carga"));
        assertFalse(SonsGojo.cortaComOVisual("gojo_vermelho_explosao"), "a explosão soa até o fim");
        assertFalse(SonsGojo.cortaComOVisual("gojo_azul"));
    }

    @Test
    void dominioRachaComOEstilhacoCaindoQuandoEleSome() throws IOException {
        SonsGojo.NoFim fim = SonsGojo.antesDoFim("gojo_vazio_infinito").orElseThrow();
        assertEquals(SonsGojo.DOMINIO_FIM, fim.som());
        assertEquals(marco(ler(MANIFESTO), "gojo_dominio_fim", "estilhaco"), fim.antes(),
                "o som começa antes do fim, para o estilhaço cair no tick em que o domínio some");
        assertTrue(SonsGojo.antesDoFim("gojo_azul").isEmpty());
    }

    @Test
    void cargaDoRoxoNaoTocaDuasVezes() {
        java.util.List<String> sons = new java.util.ArrayList<>();
        com.minerwk.core.kit.Contexto ctx = new com.minerwk.core.kit.Contexto() {
            @Override
            public void apresentacao(String animacao, String efeito, String som) {
                sons.add(som);
            }
        };
        Gojo.criar().naTecla(Tecla.V).orElseThrow().aoIniciarCarga(ctx);
        assertFalse(sons.contains(SonsGojo.ROXO_CARGA), "o som da carga vem do visual, não da apresentação");
    }

    private static JsonObject ler(Path arquivo) throws IOException {
        try (Reader r = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static int cargaMax(Tecla tecla) {
        return Gojo.criar().naTecla(tecla).orElseThrow().numeros().cargaMax();
    }

    @Test
    void cadaSomTemVariacoesEntradaNoSoundsJsonELegendaNasDuasLinguas() throws IOException {
        JsonObject sons = ler(ASSETS.resolve("sounds.json"));
        JsonObject pt = ler(ASSETS.resolve("lang/pt_br.json")), en = ler(ASSETS.resolve("lang/en_us.json"));
        for (String som : SONS) {
            assertTrue(sons.has(som), som + " no sounds.json");
            assertEquals(VARIACOES, sons.getAsJsonObject(som).getAsJsonArray("sounds").size(), som);
            assertEquals("subtitles.minerwk." + som, sons.getAsJsonObject(som).get("subtitle").getAsString());
            assertTrue(pt.has("subtitles.minerwk." + som) && en.has("subtitles.minerwk." + som), "legenda de " + som);
            for (int v = 1; v <= VARIACOES; v++) {
                Path ogg = ASSETS.resolve("sounds/gojo/" + som + (v == 1 ? "" : "_" + v) + ".ogg");
                assertTrue(Files.size(ogg) > 1000, ogg + " existe e tem áudio");
            }
        }
    }

    @Test
    void marcosDosSonsBatemComOsTicksDoJava() throws IOException {
        JsonObject m = ler(MANIFESTO);
        assertEquals(Gojo.CampoAzul.DURACAO, marco(m, "gojo_azul", "fim"), "o Azul soa enquanto o campo existe");
        assertEquals(cargaMax(Tecla.X), marco(m, "gojo_vermelho_carga", "cheia"), "a carga do Vermelho enche no máximo");
        assertEquals(cargaMax(Tecla.V), marco(m, "gojo_roxo_carga", "dispara"), "a carga do Roxo acaba no disparo");
        assertEquals(cargaMax(Tecla.G), marco(m, "gojo_dominio", "abertura"), "o baque do domínio cai na abertura");
        for (String som : SONS) {
            JsonObject s = m.getAsJsonObject(som);
            for (String nome : s.getAsJsonObject("marcos").keySet()) {
                assertTrue(marco(m, som, nome) <= s.get("ticks").getAsInt(), som + "." + nome + " cabe no som");
            }
        }
    }

    private static int marco(JsonObject manifesto, String som, String nome) {
        return manifesto.getAsJsonObject(som).getAsJsonObject("marcos").get(nome).getAsInt();
    }
}
