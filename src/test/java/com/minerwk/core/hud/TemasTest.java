package com.minerwk.core.hud;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.Forma;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.personagens.cid.Cid;
import com.minerwk.core.personagens.gojo.Gojo;
import com.minerwk.core.personagens.ichigo.Ichigo;
import com.minerwk.core.personagens.ichigo.Reiatsu;
import java.util.List;
import org.junit.jupiter.api.Test;

class TemasTest {

    @Test
    void gojoTemMolduraLisa() {
        TemaUi t = Temas.tema("gojo", Forma.BASE);
        assertEquals(TemaUi.Moldura.LISA, t.moldura());
        assertEquals("gojo", t.fundoArvore());
    }

    @Test
    void ichigoTrocaDeTemaNoHollow() {
        TemaUi base = Temas.tema("ichigo", Forma.BASE);
        assertEquals(TemaUi.Moldura.CORTADA, base.moldura());
        assertEquals(base, Temas.tema("ichigo", Reiatsu.BANKAI));
        TemaUi hollow = Temas.tema("ichigo", Reiatsu.HOLLOW);
        assertNotEquals(base.corPrincipal(), hollow.corPrincipal());
        assertEquals("ichigo_hollow", hollow.fundoArvore());
    }

    @Test
    void cidTemMolduraOrnada() {
        assertEquals(TemaUi.Moldura.ORNADA, Temas.tema("cid", Forma.BASE).moldura());
    }

    @Test
    void semTemaUsaOGenerico() {
        assertSame(Temas.GENERICO, Temas.tema("asta", Forma.BASE));
        assertEquals("generico", Temas.GENERICO.fundoArvore());
    }

    @Test
    void formaDesconhecidaUsaOTemaBase() {
        assertEquals(Temas.tema("ichigo", Forma.BASE), Temas.tema("ichigo", "xyz"));
    }

    @Test
    void todoPersonagemJogavelTemTema() {
        for (Personagem p : List.of(Gojo.criar(), Ichigo.criar(), Cid.criar())) {
            assertTrue(Temas.temTema(p.id()), p.id());
        }
    }
}
