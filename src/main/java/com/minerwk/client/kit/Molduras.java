package com.minerwk.client.kit;

import com.minerwk.core.hud.TemaUi;
import java.util.HashMap;
import java.util.Map;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/** A moldura dos quadros no estilo do tema, e os ícones gerados ({@code textures/gui/icones}), da HUD e da árvore. */
public final class Molduras {
    private static final Map<String, Boolean> EXISTE = new HashMap<>();

    private Molduras() {
    }

    /** Moldura em volta do quadro {@code (x, y, lado)}, por fora dele, na {@code cor} (a borda do tema ou um destaque). */
    public static void desenhar(GuiGraphics g, int x, int y, int lado, TemaUi tema, int cor) {
        int x0 = x - 1, y0 = y - 1, x1 = x + lado + 1, y1 = y + lado + 1;
        switch (tema.moldura()) {
            case LISA -> borda(g, x0, y0, x1, y1, cor, 0);
            case CORTADA -> {
                // Sem os pixels dos cantos, mais um risco diagonal em lâmina no canto de cima à direita.
                borda(g, x0, y0, x1, y1, cor, 1);
                for (int k = 0; k < 4; k++) g.fill(x1 - 4 + k, y0 - 2 + k, x1 - 2 + k, y0 + k, tema.corPrincipal());
            }
            case ORNADA -> {
                borda(g, x0, y0, x1, y1, cor, 0);
                int o = tema.corSecundaria();
                // Um L de 3 px em cada canto, um pixel por fora.
                g.fill(x0 - 1, y0 - 1, x0 + 3, y0, o);
                g.fill(x0 - 1, y0 - 1, x0, y0 + 3, o);
                g.fill(x1 - 3, y0 - 1, x1 + 1, y0, o);
                g.fill(x1, y0 - 1, x1 + 1, y0 + 3, o);
                g.fill(x0 - 1, y1, x0 + 3, y1 + 1, o);
                g.fill(x0 - 1, y1 - 3, x0, y1 + 1, o);
                g.fill(x1 - 3, y1, x1 + 1, y1 + 1, o);
                g.fill(x1, y1 - 3, x1 + 1, y1 + 1, o);
            }
        }
    }

    /** Uma borda de 1 px no retângulo, sem {@code corte} pixels em cada canto. */
    private static void borda(GuiGraphics g, int x0, int y0, int x1, int y1, int cor, int corte) {
        g.fill(x0 + corte, y0, x1 - corte, y0 + 1, cor);
        g.fill(x0 + corte, y1 - 1, x1 - corte, y1, cor);
        g.fill(x0, y0 + corte, x0 + 1, y1 - corte, cor);
        g.fill(x1 - 1, y0 + corte, x1, y1 - corte, cor);
    }

    public static ResourceLocation icone(String nome) {
        return ResourceLocation.fromNamespaceAndPath("minerwk", "textures/gui/icones/" + nome + ".png");
    }

    /** Se o ícone foi gerado; sem ele, quem desenha cai para a letra. */
    public static boolean temIcone(String nome) {
        return EXISTE.computeIfAbsent(nome,
                n -> Minecraft.getInstance().getResourceManager().getResource(icone(n)).isPresent());
    }

    /** Recarregar os recursos (F3+T) esquece quais ícones existem: um PNG novo ou removido vale na hora. */
    public static void aoRegistrarRecarga(RegisterClientReloadListenersEvent evento) {
        evento.registerReloadListener((ResourceManagerReloadListener) recursos -> EXISTE.clear());
    }

    /** O ícone de 32x32 desenhado em {@code lado} x {@code lado}, com blend (o fill de antes o deixa desligado). */
    public static void icone(GuiGraphics g, String nome, int x, int y, int lado) {
        RenderSystem.enableBlend();
        g.blit(icone(nome), x, y, lado, lado, 0, 0, 32, 32, 32, 32);
    }
}
