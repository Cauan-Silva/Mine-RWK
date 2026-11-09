package com.minerwk.client.kit;

import com.minerwk.rede.EscolherPersonagem;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tela simples de escolha do personagem (a versão com retratos vem no 1.4).
 * Recebe a lista do servidor como {@code "id|nome|obra"}.
 */
public final class TelaEscolha extends Screen {
    private static final int LARGURA = 220;
    private static final int ALTURA = 20;
    private static final int ESPACO = 24;

    private final List<String[]> personagens;
    private String[] escolhido;

    public TelaEscolha(List<String> lista) {
        super(Component.translatable("tela.minerwk.escolha"));
        this.personagens = lista.stream().map(s -> s.split("\\|", 3)).filter(p -> p.length == 3).toList();
    }

    @Override
    protected void init() {
        int x = (width - LARGURA) / 2;
        if (escolhido == null) {
            int y = Math.max(40, (height - personagens.size() * ESPACO) / 2);
            for (String[] p : personagens) {
                addRenderableWidget(Button.builder(Component.literal(p[1] + " — " + p[2]), b -> confirmar(p))
                        .bounds(x, y, LARGURA, ALTURA).build());
                y += ESPACO;
            }
        } else {
            int y = height / 2;
            addRenderableWidget(Button.builder(Component.translatable("tela.minerwk.escolha.sim"), b -> enviar())
                    .bounds(x, y, LARGURA / 2 - 4, ALTURA).build());
            addRenderableWidget(Button.builder(Component.translatable("tela.minerwk.escolha.nao"), b -> confirmar(null))
                    .bounds(x + LARGURA / 2 + 4, y, LARGURA / 2 - 4, ALTURA).build());
        }
    }

    private void confirmar(String[] personagem) {
        escolhido = personagem;
        rebuildWidgets();
    }

    private void enviar() {
        PacketDistributor.sendToServer(new EscolherPersonagem(escolhido[0]));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float parcial) {
        super.render(g, mouseX, mouseY, parcial);
        if (escolhido == null) {
            g.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        } else {
            g.drawCenteredString(font, Component.translatable("tela.minerwk.escolha.confirmar", escolhido[1]),
                    width / 2, height / 2 - 30, 0xFFFFFF);
            g.drawCenteredString(font, Component.translatable("tela.minerwk.escolha.definitiva"),
                    width / 2, height / 2 - 18, 0xFFAA55);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
