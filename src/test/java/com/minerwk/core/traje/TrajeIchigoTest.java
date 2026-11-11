package com.minerwk.core.traje;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.kit.Forma;
import com.minerwk.core.personagens.ichigo.Reiatsu;
import com.minerwk.core.traje.tecido.MolaManto;
import com.minerwk.core.traje.tecido.TecidoEspada;
import com.minerwk.core.traje.tecido.TecidoIchigo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Os trajes do Ichigo: qual vale em cada forma, e os ossos que o jogo mexe existem nos modelos gerados. */
class TrajeIchigoTest {
    private static String modelo(String traje) throws Exception {
        return Files.readString(Path.of("src/main/resources/assets/minerwk/geo/traje/" + traje + ".geo.json"));
    }

    private static boolean tem(String geo, String osso) {
        return geo.contains("\"name\": \"" + osso + "\"");
    }

    @Test
    void cadaFormaTemOSeuTraje() {
        assertEquals(Optional.of("ichigo"), Trajes.de("ichigo", Forma.BASE));
        assertEquals(Optional.of("ichigo_bankai"), Trajes.de("ichigo", Reiatsu.BANKAI));
        assertEquals(Optional.of("gojo"), Trajes.de("gojo", Forma.BASE));
        assertEquals(Optional.empty(), Trajes.de("teste", Forma.BASE));
        assertEquals(Optional.empty(), Trajes.de("", Forma.BASE));
    }

    @Test
    void aEspadaEstaNosDoisModelosPresaAoBracoDireito() throws Exception {
        for (String traje : new String[] {TrajeIchigo.SHIKAI, TrajeIchigo.BANKAI}) {
            String geo = modelo(traje);
            assertTrue(tem(geo, TrajeIchigo.OSSO_ARMA), traje);
            assertTrue(java.util.regex.Pattern.compile("\"name\": \"arma\",\\s*\"parent\": \"armorRightArm\"").matcher(geo).find(),
                    traje + ": a arma segue o braço direito");
        }
    }

    @Test
    void faixaECorrenteDaEspadaSaoOssosDeTecido() throws Exception {
        String shikai = modelo(TrajeIchigo.SHIKAI);
        String bankai = modelo(TrajeIchigo.BANKAI);
        for (String osso : new String[] {"arma_cloth", "arma_cloth_1", "arma_cloth_2", "arma_cloth_3"}) {
            assertTrue(tem(shikai, osso), osso);
            assertTrue(TecidoEspada.segmento(osso) >= 0, osso);
        }
        for (String osso : new String[] {"arma_chain", "arma_chain_1", "arma_chain_2", "arma_chain_3"}) {
            assertTrue(tem(bankai, osso), osso);
            assertTrue(TecidoEspada.segmento(osso) >= 0, osso);
        }
        assertEquals(0, TecidoEspada.segmento("arma_cloth"));
        assertEquals(3, TecidoEspada.segmento("arma_chain_3"));
        assertEquals(-1, TecidoEspada.segmento("arma_root"));
        assertEquals(-1, TecidoEspada.segmento("arma_lamina_2"));
    }

    @Test
    void mangasDoShikaiExistemESeMexemComOMovimento() throws Exception {
        String shikai = modelo(TrajeIchigo.SHIKAI);
        TecidoIchigo tecido = new TecidoIchigo();
        for (int i = 0; i < 40; i++) tecido.tick(new MolaManto.Entrada(.25, 0, 0, 0, true), .2, -.2);
        for (String osso : TrajeIchigo.MANGAS_DO_SHIKAI) {
            assertTrue(tem(shikai, osso), osso);
            MolaManto.Pose p = tecido.pose(osso, 1);
            assertTrue(Math.abs(p.x()) > .01 && Math.abs(p.x()) <= .5, osso + " deveria balançar, x " + p.x());
        }
    }

    @Test
    void faixaDaEspadaNuncaFicaParadaENaoPassaDoLimite() {
        TecidoEspada espada = new TecidoEspada();
        double maior = 0;
        for (int i = 0; i < 200; i++) {
            espada.tick(i < 100 ? .2 : 0, 0, 0, i % 20 == 0 ? .4 : 0, 0, 0, 0, true);
            MolaManto.Pose p = espada.pose("arma_cloth_3", 1);
            assertTrue(Math.abs(p.x()) <= 1.3 && Math.abs(p.y()) <= .6 && Math.abs(p.z()) <= 1.3);
            if (i > 150) maior = Math.max(maior, Math.abs(p.x()) + Math.abs(p.z()));
        }
        assertTrue(maior > .02, "parada, a ponta ainda balança: " + maior);
        espada.tick(Double.NaN, 0, 0, 0, 0, 0, 0, true);
        MolaManto.Pose p = espada.pose("arma_cloth", 0);
        assertTrue(Double.isFinite(p.x() + p.y() + p.z()), "entrada inválida zera em vez de espalhar NaN");
    }
}
