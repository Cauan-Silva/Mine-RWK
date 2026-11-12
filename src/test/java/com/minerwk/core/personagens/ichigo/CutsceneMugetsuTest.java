package com.minerwk.core.personagens.ichigo;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minerwk.core.camera.Coreografia.Quadro;
import com.minerwk.core.personagens.gojo.CutsceneRoxo200.Palavra;
import com.minerwk.core.vfx.MugetsuIchigo;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** A cutscene do Mugetsu: os planos acompanham a transformação, o silêncio e o corte. */
class CutsceneMugetsuTest {

    @Test
    void aCutsceneTemOIdDoGolpeECobreACargaEOVeu() {
        assertEquals(Ichigo.MUGETSU, CutsceneMugetsu.ID, "o golpe pede a cutscene pelo próprio id");
        int fim = CutsceneMugetsu.COREOGRAFIA.duracao();
        assertTrue(fim >= MugetsuIchigo.CARGA + MugetsuIchigo.TICKS, "mostra o véu avançar inteiro: " + fim);
        assertTrue(fim <= MugetsuIchigo.CARGA + MugetsuIchigo.TICKS + MugetsuIchigo.RESTO, "e devolve a câmera antes de o véu sumir");
    }

    @Test
    void aTransformacaoEOCorteSaoCortesSecos() {
        // Um tick antes do corte de câmera o plano ainda é o anterior; no tick dele, pula.
        for (int tick : new int[] {MugetsuIchigo.TRANSFORMA, CutsceneMugetsu.PLANO_DO_VEU}) {
            Quadro antes = CutsceneMugetsu.COREOGRAFIA.quadro(tick - .5), depois = CutsceneMugetsu.COREOGRAFIA.quadro(tick);
            assertTrue(antes.posicao().distancia(depois.posicao()) > 2, "corte seco no tick " + tick);
        }
        assertTrue(CutsceneMugetsu.PLANO_DO_VEU > MugetsuIchigo.CARGA && CutsceneMugetsu.PLANO_DO_VEU <= MugetsuIchigo.CARGA + 4,
                "o plano aberto entra logo depois do corte da lâmina");
    }

    @Test
    void noSilencioACameraEstaNoRostoENoCorteAtrasDoOmbro() {
        Quadro silencio = CutsceneMugetsu.COREOGRAFIA.quadro(MugetsuIchigo.SILENCIO);
        assertTrue(silencio.posicao().z() > 0 && silencio.posicao().comprimento() < 1.6, "close de frente: " + silencio.posicao());
        assertTrue(silencio.fov() < .85, "fechado");
        Quadro corte = CutsceneMugetsu.COREOGRAFIA.quadro(MugetsuIchigo.CARGA);
        assertTrue(corte.posicao().z() < 0, "atrás dele, vendo para onde o corte vai");
        assertTrue(corte.alvo().z() > 3, "olhando para a frente");
    }

    @Test
    void oPlanoAbertoVeOVeuDeLadoEDoAlto() {
        Quadro aberto = CutsceneMugetsu.COREOGRAFIA.quadro(CutsceneMugetsu.PLANO_DO_VEU + 10);
        assertTrue(Math.abs(aberto.posicao().x()) > MugetsuIchigo.MEIA_LARGURA + 2, "fora da faixa do véu: de dentro não se vê nada");
        assertTrue(aberto.posicao().y() > 3, "do alto");
        assertTrue(aberto.alvo().z() > 10, "olhando para o caminho do véu");
    }

    @Test
    void oNomeDoGolpeApareceNoSilencioAntesDoCorte() throws Exception {
        assertEquals(2, CutsceneMugetsu.PALAVRAS.size());
        Palavra nome = CutsceneMugetsu.PALAVRAS.get(1);
        assertTrue(nome.inicio() >= MugetsuIchigo.SILENCIO, "Mugetsu aparece quando tudo se cala");
        assertTrue(nome.fim() <= MugetsuIchigo.CARGA + 6);
        Palavra antes = CutsceneMugetsu.PALAVRAS.get(0);
        assertTrue(antes.inicio() >= MugetsuIchigo.TRANSFORMA && antes.fim() <= nome.inicio(), "uma palavra de cada vez");
        for (String lingua : new String[] {"pt_br", "en_us"}) {
            JsonObject textos;
            try (Reader r = Files.newBufferedReader(Path.of("src/main/resources/assets/minerwk/lang/" + lingua + ".json"), StandardCharsets.UTF_8)) {
                textos = JsonParser.parseReader(r).getAsJsonObject();
            }
            for (Palavra p : CutsceneMugetsu.PALAVRAS) {
                assertTrue(textos.has(p.chave()) && textos.has(p.chave() + ".jp"), lingua + ": " + p.chave());
            }
            assertEquals("無月", textos.get(nome.chave() + ".jp").getAsString());
        }
    }
}
