package com.minerwk.core.arena;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlantaEscolaTest {

    @Test
    void plataformaTem61Por61ComQuatroCamadas() {
        List<PlantaEscola.Bloco> blocos = PlantaEscola.plataforma();
        assertEquals(61 * 61 * 4, blocos.size());
        for (PlantaEscola.Bloco b : blocos) {
            assertTrue(Math.abs(b.dx()) <= 30 && Math.abs(b.dz()) <= 30, "fora do pátio: " + b);
            assertTrue(b.dy() >= -4 && b.dy() <= -1, "fora das camadas: " + b);
            assertEquals(b.dy() == -1 ? PlantaEscola.Material.TIJOLO_PEDRA : PlantaEscola.Material.PEDRA, b.material());
        }
    }

    @Test
    void plataformaNaoRepetePosicao() {
        Set<String> vistas = new HashSet<>();
        for (PlantaEscola.Bloco b : PlantaEscola.plataforma()) {
            assertTrue(vistas.add(b.dx() + "," + b.dy() + "," + b.dz()), "repetida: " + b);
        }
    }
}
