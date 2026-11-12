package com.minerwk.core.traje;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.kit.Forma;
import com.minerwk.core.traje.tecido.MolaManto;
import com.minerwk.core.traje.tecido.TecidoIchigo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** O traje do Shadow: é o do Cid, e o modelo gerado tem a espada, a barra de pano e o capuz aberto, sem rosto pintado. */
class TrajeCidTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/minerwk");

    private static String modelo() throws Exception {
        return Files.readString(ASSETS.resolve("geo/traje/cid.geo.json"));
    }

    @Test
    void oCidVesteOTrajeDoShadow() {
        assertEquals(Optional.of("cid"), Trajes.de("cid", Forma.BASE));
        assertTrue(TrajeCid.temPano(TrajeCid.SHADOW));
        assertTrue(TrajeCid.temPano(TrajeIchigo.BANKAI));
        assertFalse(TrajeCid.temPano("gojo"));
        assertFalse(TrajeIchigo.eDoIchigo(TrajeCid.SHADOW), "o Shadow não herda a máscara nem o rastro de corte do Ichigo");
    }

    @Test
    void aEspadaEstaPresaAoBracoDireito() throws Exception {
        assertTrue(Pattern.compile("\"name\": \"arma\",\\s*\"parent\": \"armorRightArm\"").matcher(modelo()).find());
    }

    @Test
    void oModeloNaoTemOOssoDosOlhosDoGojo() throws Exception {
        // A camada do traje esconde o osso "olhos" de quem não é o Gojo sem a venda: os do Shadow são pintados no rosto.
        assertFalse(modelo().contains("\"name\": \"" + TrajeGojo.OSSO_OLHOS + "\""));
        assertFalse(modelo().contains("\"name\": \"" + TrajeGojo.OSSO_VENDA + "\""));
    }

    @Test
    void aBarraDoSobretudoEPanoQueOTecidoBalanca() throws Exception {
        TecidoIchigo tecido = new TecidoIchigo();
        for (int i = 0; i < 40; i++) tecido.tick(new MolaManto.Entrada(.25, 0, 0, 0, true));
        Matcher m = Pattern.compile("\"name\": \"(bk_[a-z0-9_]+)\"").matcher(modelo());
        int ossos = 0, mexendo = 0;
        while (m.find()) {
            ossos++;
            MolaManto.Pose p = tecido.pose(m.group(1), 1);
            assertTrue(Double.isFinite(p.x() + p.y() + p.z()), m.group(1));
            if (Math.abs(p.x()) + Math.abs(p.z()) > .005) mexendo++;
        }
        assertEquals(96, ossos, "seis colunas atrás e cinco tiras de cada lado, de seis segmentos");
        assertTrue(mexendo > ossos / 2, "andando, a maior parte da barra balança: " + mexendo);
    }

    @Test
    void oRostoEODoJogadorENadaBrilha() throws Exception {
        var textura = ImageIO.read(ASSETS.resolve("textures/traje/cid.png").toFile());
        var brilho = ImageIO.read(ASSETS.resolve("textures/traje/cid_glowmask.png").toFile());
        assertEquals(textura.getWidth(), brilho.getWidth());
        assertEquals(textura.getHeight(), brilho.getHeight());
        // O quadro do rosto fica vazio: o capuz é aberto na frente e quem aparece é a skin do jogador.
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) assertEquals(0, textura.getRGB(x, y) >>> 24, "rosto pintado em " + x + "," + y);
        }
        int acesos = 0;
        for (int y = 0; y < brilho.getHeight(); y++) {
            for (int x = 0; x < brilho.getWidth(); x++) {
                if ((brilho.getRGB(x, y) >>> 24) == 0) continue;
                acesos++;
                assertEquals(0, textura.getRGB(x, y) >>> 24, "brilho sobre o desenho em " + x + "," + y);
            }
        }
        // O GeckoLib derruba o jogo com uma máscara de brilho vazia: fica um pixel, fora do desenho.
        assertEquals(1, acesos);
    }

    @Test
    void oCapuzNaoTemCuboSobreORosto() throws Exception {
        // Só o cubo da cabeça inteira (8x8x8) cobriria o rosto: o capuz é feito de placas em volta dela.
        assertFalse(Pattern.compile("\"size\": \\[\\s*8,\\s*8,\\s*8\\s*\\]").matcher(modelo()).find());
    }
}
