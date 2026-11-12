package com.minerwk.core.personagens.ichigo;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.Habilidade;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.personagens.SonsDosVisuais;
import com.minerwk.core.personagens.gojo.SonsGojo;
import com.minerwk.core.vfx.FormasIchigo;
import com.minerwk.core.vfx.GetsugaIchigo;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Confere os sons gerados por {@code ferramentas/gerar_sfx_ichigo.py} contra o kit: cada som existe com as
 * variações, está no {@code sounds.json} com legenda, sai do visual certo e os marcos dele batem com os
 * ticks do Java. Mudou um tempo no Java, mudar no gerador e gerar de novo.
 */
class SonsIchigoTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/minerwk");
    private static final Path MANIFESTO = Path.of("ferramentas/sfx_ichigo_manifesto.json");
    private static final int VARIACOES = 3;

    @Test
    void manifestoTemExatamenteOsSonsDoKit() throws IOException {
        Set<String> bases = new TreeSet<>();
        for (String arquivo : ler(MANIFESTO).keySet()) bases.add(arquivo.replaceAll("_[23]$", ""));
        assertEquals(new TreeSet<>(SonsIchigo.TODOS), bases, "gerador e SonsIchigo com a mesma lista");
    }

    @Test
    void idsNaoRepetemNemBatemComOsDoGojo() {
        assertEquals(SonsIchigo.TODOS.size(), new HashSet<>(SonsIchigo.TODOS).size());
        for (String id : SonsIchigo.TODOS) {
            assertTrue(id.startsWith("ichigo_"), id);
            assertFalse(SonsGojo.TODOS.contains(id), id);
        }
    }

    @Test
    void cadaSomTemVariacoesEntradaNoSoundsJsonELegendaNasDuasLinguas() throws IOException {
        JsonObject sons = ler(ASSETS.resolve("sounds.json"));
        JsonObject pt = ler(ASSETS.resolve("lang/pt_br.json")), en = ler(ASSETS.resolve("lang/en_us.json"));
        for (String som : SonsIchigo.TODOS) {
            assertTrue(sons.has(som), som + " no sounds.json");
            assertEquals(VARIACOES, sons.getAsJsonObject(som).getAsJsonArray("sounds").size(), som);
            assertEquals("subtitles.minerwk." + som, sons.getAsJsonObject(som).get("subtitle").getAsString());
            assertTrue(pt.has("subtitles.minerwk." + som) && en.has("subtitles.minerwk." + som), "legenda de " + som);
            for (int v = 1; v <= VARIACOES; v++) {
                Path ogg = ASSETS.resolve("sounds/ichigo/" + som + (v == 1 ? "" : "_" + v) + ".ogg");
                assertTrue(Files.size(ogg) > 1000, ogg + " existe e tem áudio");
            }
        }
    }

    @Test
    void cadaVisualDoKitTemOSeuSom() {
        assertEquals(SonsIchigo.GETSUGA_CARGA, SonsIchigo.doVisual("ichigo_carga").orElseThrow());
        assertEquals(SonsIchigo.GETSUGA, SonsIchigo.doVisual("ichigo_getsuga").orElseThrow());
        assertEquals(SonsIchigo.GETSUGA_NEGRO, SonsIchigo.doVisual("ichigo_getsuga_negro").orElseThrow());
        assertEquals(SonsIchigo.SHUNPO, SonsIchigo.doVisual("ichigo_shunpo").orElseThrow());
        assertEquals(SonsIchigo.SHUNPO, SonsIchigo.doVisual("ichigo_shunpo_bankai").orElseThrow(), "o passo é o mesmo nas duas formas");
        assertEquals(SonsIchigo.ONDA, SonsIchigo.doVisual("ichigo_onda").orElseThrow());
        assertEquals(SonsIchigo.ARREMESSO, SonsIchigo.doVisual("ichigo_arremesso").orElseThrow());
        assertEquals(SonsIchigo.BANKAI_CARGA, SonsIchigo.doVisual("ichigo_bankai_carga").orElseThrow());
        assertEquals(SonsIchigo.BANKAI, SonsIchigo.doVisual("ichigo_bankai").orElseThrow());
        assertEquals(SonsIchigo.LAMINA, SonsIchigo.doVisual("ichigo_lamina").orElseThrow());
        assertEquals(SonsIchigo.SAIR, SonsIchigo.doVisual("ichigo_sair").orElseThrow());
        assertEquals(SonsIchigo.CORTE, SonsIchigo.doVisual("ichigo_corte").orElseThrow());
        assertEquals(SonsIchigo.MASCARA_VESTE, SonsIchigo.doVisual("ichigo_mascara").orElseThrow());
        assertEquals(SonsIchigo.MUGETSU_CARGA, SonsIchigo.doVisual("ichigo_mugetsu_carga").orElseThrow());
        assertEquals(SonsIchigo.MUGETSU, SonsIchigo.doVisual("ichigo_mugetsu").orElseThrow());
        assertEquals(SonsIchigo.PODERES_PERDIDOS, SonsIchigo.doVisual("ichigo_poderes_perdidos").orElseThrow());
        assertEquals(SonsIchigo.PODERES_VOLTAM, SonsIchigo.doVisual("ichigo_poderes_voltam").orElseThrow());
        assertTrue(SonsIchigo.doVisual("ichigo_traje").isEmpty(), "visual sem som");
        assertTrue(SonsIchigo.doVisual("gojo_azul").isEmpty(), "visual de outro personagem");
        // Todo som é usado por algum visual: nenhum fica gerado à toa.
        Set<String> usados = new HashSet<>();
        for (String tipo : List.of("ichigo_carga", "ichigo_getsuga", "ichigo_getsuga_negro", "ichigo_shunpo", "ichigo_onda",
                "ichigo_arremesso", "ichigo_bankai_carga", "ichigo_bankai", "ichigo_lamina", "ichigo_sair", "ichigo_corte",
                "ichigo_mascara", "ichigo_mugetsu_carga", "ichigo_mugetsu", "ichigo_poderes_perdidos", "ichigo_poderes_voltam")) {
            usados.add(SonsIchigo.doVisual(tipo).orElseThrow());
        }
        // A máscara quebrando é o único que não começa com um visual: toca no fim do da máscara.
        usados.add(SonsIchigo.antesDoFim("ichigo_mascara").orElseThrow().som());
        assertEquals(new HashSet<>(SonsIchigo.TODOS), usados);
    }

    @Test
    void soAsCargasEALaminaParamComOVisual() {
        assertTrue(SonsIchigo.cortaComOVisual("ichigo_carga"), "soltou o Getsuga ou foi interrompido: o som da carga para");
        assertTrue(SonsIchigo.cortaComOVisual("ichigo_bankai_carga"), "liberação cortada: o som para");
        assertTrue(SonsIchigo.cortaComOVisual("ichigo_lamina"), "saiu do Bankai: a lâmina apaga");
        assertTrue(SonsIchigo.cortaComOVisual("ichigo_mugetsu_carga"), "golpe forte na cutscene: o som da carga para");
        assertFalse(SonsIchigo.cortaComOVisual("ichigo_mugetsu"), "o corte soa até o fim");
        assertFalse(SonsIchigo.cortaComOVisual("ichigo_mascara"), "sair do Bankai não corta o som de vestir, que é curto");
        assertFalse(SonsIchigo.cortaComOVisual("ichigo_getsuga"), "a meia-lua que bate na parede ainda soa o estouro");
        assertFalse(SonsIchigo.cortaComOVisual("ichigo_bankai"));
        assertFalse(SonsIchigo.cortaComOVisual("ichigo_onda"));
    }

    @Test
    void oClienteAchaOSomDeQualquerPersonagem() {
        assertEquals(SonsGojo.AZUL, SonsDosVisuais.doVisual("gojo_azul").orElseThrow());
        assertEquals(SonsIchigo.ONDA, SonsDosVisuais.doVisual("ichigo_onda").orElseThrow());
        assertTrue(SonsDosVisuais.doVisual("nada").isEmpty());
        assertTrue(SonsDosVisuais.cortaComOVisual("gojo_vermelho_carga"));
        assertTrue(SonsDosVisuais.cortaComOVisual("ichigo_carga"));
        assertFalse(SonsDosVisuais.cortaComOVisual("gojo_azul"));
        assertFalse(SonsDosVisuais.cortaComOVisual("ichigo_onda"));
    }

    @Test
    void marcosDosSonsBatemComOsTicksDoJava() throws IOException {
        JsonObject m = ler(MANIFESTO);
        assertEquals(carga(Tecla.Z, false), marco(m, SonsIchigo.GETSUGA_CARGA, "cheia"), "a carga do Getsuga enche no máximo");
        assertEquals(GetsugaIchigo.voo(false), marco(m, SonsIchigo.GETSUGA, "voo"), "o corte voa esse tempo antes de estourar");
        assertEquals(GetsugaIchigo.duracao(false), marco(m, SonsIchigo.GETSUGA, "fim"));
        assertEquals(GetsugaIchigo.voo(true), marco(m, SonsIchigo.GETSUGA_NEGRO, "voo"));
        assertEquals(GetsugaIchigo.duracao(true), marco(m, SonsIchigo.GETSUGA_NEGRO, "fim"));
        assertEquals(FormasIchigo.SHUNPO, marco(m, SonsIchigo.SHUNPO, "fim"));
        assertEquals(FormasIchigo.ONDA, marco(m, SonsIchigo.ONDA, "fim"));
        assertEquals(FormasIchigo.ARREMESSO_IDA, marco(m, SonsIchigo.ARREMESSO, "volta"), "a espada começa a voltar");
        assertEquals(FormasIchigo.ARREMESSO, marco(m, SonsIchigo.ARREMESSO, "fim"), "e chega à mão");
        assertEquals(carga(Tecla.G, false), marco(m, SonsIchigo.BANKAI_CARGA, "libera"), "a carga acaba na liberação");
        assertEquals(FormasIchigo.BANKAI, marco(m, SonsIchigo.BANKAI, "fim"));
        assertEquals(Ichigo.LaminaCarregada.DURACAO, marco(m, SonsIchigo.LAMINA, "fim"), "a lâmina soa enquanto está carregada");
        assertEquals(FormasIchigo.SAIR, marco(m, SonsIchigo.SAIR, "fim"));
        assertEquals(FormasIchigo.CORTE, marco(m, SonsIchigo.CORTE, "fim"));
        assertEquals(Ichigo.Mugetsu.TRANSFORMA, marco(m, SonsIchigo.MUGETSU_CARGA, "transforma"), "a forma final aparece");
        assertEquals(Ichigo.Mugetsu.SILENCIO, marco(m, SonsIchigo.MUGETSU_CARGA, "silencio"), "o silêncio antes do corte");
        assertEquals(Ichigo.Mugetsu.CARGA, marco(m, SonsIchigo.MUGETSU_CARGA, "corte"));
        assertEquals(Ichigo.Veu.TICKS, marco(m, SonsIchigo.MUGETSU, "passou"), "o véu termina de avançar");
        assertEquals(Ichigo.Veu.TICKS + Ichigo.Veu.RESTO, marco(m, SonsIchigo.MUGETSU, "fim"));
        assertEquals(SonsIchigo.antesDoFim("ichigo_mascara").orElseThrow().antes(), marco(m, SonsIchigo.MASCARA_QUEBRA, "quebra"),
                "a máscara termina de quebrar quando some do rosto");
        for (String som : SonsIchigo.TODOS) {
            JsonObject s = m.getAsJsonObject(som);
            assertFalse(s.getAsJsonObject("marcos").isEmpty(), som + " tem ao menos um marco conferido");
            for (String nome : s.getAsJsonObject("marcos").keySet()) {
                assertTrue(marco(m, som, nome) <= s.get("ticks").getAsInt(), som + "." + nome + " cabe no som");
            }
        }
    }

    @Test
    void aMascaraQuebraUmPoucoAntesDeSumir() {
        SonsGojo.NoFim fim = SonsIchigo.antesDoFim("ichigo_mascara").orElseThrow();
        assertEquals(SonsIchigo.MASCARA_QUEBRA, fim.som());
        assertTrue(fim.antes() > 0 && fim.antes() < 20);
        assertTrue(SonsIchigo.antesDoFim("ichigo_lamina").isEmpty());
        // O cliente pergunta por qualquer personagem.
        assertEquals(SonsIchigo.MASCARA_QUEBRA, SonsDosVisuais.antesDoFim("ichigo_mascara").orElseThrow().som());
        assertEquals(SonsGojo.DOMINIO_FIM, SonsDosVisuais.antesDoFim("gojo_vazio_infinito").orElseThrow().som());
        assertTrue(SonsDosVisuais.antesDoFim("gojo_azul").isEmpty());
    }

    @Test
    void nenhumGolpePedeSomPelaApresentacao() {
        // O som vem sempre do visual (sai do lugar certo e para junto): pela apresentação ele tocaria duas vezes.
        List<String> sons = new ArrayList<>();
        Contexto ctx = new Contexto() {
            @Override
            public void apresentacao(String animacao, String efeito, String som) {
                sons.add(som);
            }
        };
        for (Tecla tecla : Tecla.values()) {
            Ichigo.criar().naTecla(tecla).ifPresent(h -> h.aoIniciarCarga(ctx));
        }
        assertFalse(sons.isEmpty(), "as cargas tocam a animação pela apresentação");
        for (String som : sons) assertEquals("", som);
    }

    private static int carga(Tecla tecla, boolean bankai) {
        Habilidade h = Ichigo.criar().naTecla(tecla).orElseThrow();
        return h.numeros().cargaMax();
    }

    private static int marco(JsonObject manifesto, String som, String nome) {
        return manifesto.getAsJsonObject(som).getAsJsonObject("marcos").get(nome).getAsInt();
    }

    private static JsonObject ler(Path arquivo) throws IOException {
        try (Reader r = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }
}
