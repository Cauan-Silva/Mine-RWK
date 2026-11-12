package com.minerwk.core.hud;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.Habilidade;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.maestria.No;
import com.minerwk.core.personagens.cid.Cid;
import com.minerwk.core.personagens.gojo.Gojo;
import com.minerwk.core.personagens.ichigo.Ichigo;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Os ícones e fundos gerados por ferramentas/gerar_icones.py: nenhum golpe, recurso ou Despertar sem ícone. */
class IconesTest {
    private static final Path GUI = Path.of("src/main/resources/assets/minerwk/textures/gui");

    static List<Personagem> jogaveis() {
        return List.of(Gojo.criar(), Ichigo.criar(), Cid.criar());
    }

    /** Todos os nomes de ícone que os três personagens pedem. */
    static Set<String> nomes() {
        Set<String> nomes = new LinkedHashSet<>();
        for (Personagem p : jogaveis()) {
            for (Habilidade h : p.todasAsHabilidades()) nomes.add(h.id());
            nomes.add(p.id() + "_recurso");
            p.arvore().nos().stream().filter(n -> n.tipo() == No.Tipo.DESPERTAR).forEach(n -> nomes.add(n.id()));
        }
        for (String s : List.of("forca", "recarga", "carga", "duracao", "recurso")) nomes.add("selo_" + s);
        return nomes;
    }

    private static BufferedImage ler(Path p) throws IOException {
        return Files.exists(p) ? ImageIO.read(p.toFile()) : null;
    }

    @Test
    void todoIconeExisteCom32() throws IOException {
        List<String> faltam = new ArrayList<>();
        for (String nome : nomes()) {
            BufferedImage img = ler(GUI.resolve("icones/" + nome + ".png"));
            if (img == null) faltam.add(nome);
            else {
                assertEquals(32, img.getWidth(), nome);
                assertEquals(32, img.getHeight(), nome);
            }
        }
        assertTrue(faltam.isEmpty(), "falta o ícone " + faltam + ": rode ferramentas/gerar_icones.py");
    }

    @Test
    void osFundosExistemCom256() throws IOException {
        for (String f : List.of("gojo", "ichigo", "ichigo_hollow", "cid", "generico")) {
            BufferedImage img = ler(GUI.resolve("fundos/" + f + ".png"));
            assertNotNull(img, "falta o fundo " + f + ": rode ferramentas/gerar_icones.py");
            assertEquals(256, img.getWidth(), f);
            assertEquals(256, img.getHeight(), f);
        }
    }
}
