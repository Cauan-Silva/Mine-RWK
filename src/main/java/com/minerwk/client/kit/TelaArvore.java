package com.minerwk.client.kit;

import com.minerwk.core.hud.LayoutArvore;
import com.minerwk.core.hud.LayoutEscolha.Retangulo;
import com.minerwk.core.hud.LogicaHud;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.maestria.Arvore;
import com.minerwk.core.maestria.No;
import com.minerwk.rede.ComprarNo;
import com.minerwk.rede.SincronizarKit;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tela da árvore de habilidades do personagem atual: os nós de antes do despertar em cima, o Despertar no meio
 * e os de depois embaixo. Clicar num nó pede a compra ao servidor, que é quem decide; a tela só mostra o estado
 * que o servidor mandou.
 */
public final class TelaArvore extends Screen {
    private static final int FUNDO = 0xC0101018;
    private static final int FUNDO_TRANCADO = 0xC0080808;
    private static final int COR_PODE = 0xFFE8B84A;
    private static final int COR_CHEIO = 0xFF5AD27A;
    private static final int COR_APAGADO = 0xFF4A4A55;
    private static final int COR_DESPERTAR = 0xFF9A6BFF;
    private static final int TEXTO = 0xFFFFFFFF;
    private static final int TEXTO_APAGADO = 0xFF8A8A95;

    private final Personagem personagem;
    private final List<No> nos = new ArrayList<>();
    private LayoutArvore layout;
    private int rolagem;

    public TelaArvore(Personagem personagem) {
        super(Component.translatable("tela.minerwk.arvore"));
        this.personagem = personagem;
        // Na ordem do layout: antes do despertar, o Despertar e depois.
        List<No> todos = personagem.arvore().nos();
        todos.stream().filter(n -> !n.depoisDoDespertar() && n.tipo() != No.Tipo.DESPERTAR).forEach(nos::add);
        todos.stream().filter(n -> n.tipo() == No.Tipo.DESPERTAR).findFirst().ifPresent(nos::add);
        todos.stream().filter(No::depoisDoDespertar).forEach(nos::add);
    }

    /** Abre a árvore do personagem atual, se ele tem uma. */
    public static void abrir() {
        Personagens.porId(RecepcaoKit.estado().personagem())
                .filter(p -> p.arvore().nos().stream().anyMatch(n -> n.tipo() == No.Tipo.DESPERTAR))
                .ifPresent(p -> Minecraft.getInstance().setScreen(new TelaArvore(p)));
    }

    @Override
    protected void init() {
        int antes = (int) nos.stream().filter(n -> !n.depoisDoDespertar() && n.tipo() != No.Tipo.DESPERTAR).count();
        layout = new LayoutArvore(width, height, antes, nos.size() - antes - 1);
        rolagem = Mth.clamp(rolagem, 0, layout.rolagemMaxima());
    }

    private static int nivelDe(SincronizarKit s, No no) {
        return Math.min(no.niveis(), s.nos().getOrDefault(no.id(), 0));
    }

    private Arvore.Resultado estadoDe(SincronizarKit s, No no) {
        return personagem.arvore().podeComprar(s.nos(), no.id(), s.nivel(), s.despertou());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float parcial) {
        super.render(g, mouseX, mouseY, parcial);
        SincronizarKit s = RecepcaoKit.estado();
        // O personagem mudou (comando ou saída do mundo) com a tela aberta: ela não vale mais.
        if (!s.personagem().equals(personagem.id())) {
            onClose();
            return;
        }
        g.enableScissor(0, LayoutArvore.TOPO - 2, width, height);
        for (int i = 0; i < nos.size(); i++) desenharNo(g, s, nos.get(i), layout.celula(i), i == layout.indiceDoDespertar());
        g.disableScissor();
        if (layout.rolagemMaxima() > 0) {
            // Barra de rolagem à direita, só quando a árvore não cabe na altura.
            int trilho = height - LayoutArvore.TOPO - LayoutArvore.MARGEM;
            int barra = Math.max(10, trilho * trilho / (trilho + layout.rolagemMaxima()));
            int y = LayoutArvore.TOPO + (trilho - barra) * rolagem / layout.rolagemMaxima();
            g.fill(width - 5, LayoutArvore.TOPO, width - 3, LayoutArvore.TOPO + trilho, 0x40FFFFFF);
            g.fill(width - 5, y, width - 3, y + barra, 0xC0FFFFFF);
        }
        desenharCabecalho(g, s);
        int sob = layout.indiceEm(mouseX, mouseY, rolagem);
        if (sob >= 0) g.renderComponentTooltip(font, dica(s, nos.get(sob)), mouseX, mouseY);
    }

    private void desenharCabecalho(GuiGraphics g, SincronizarKit s) {
        g.drawCenteredString(font, Component.translatable("tela.minerwk.arvore.titulo", personagem.nome()), width / 2, 8, TEXTO);
        int livres = personagem.arvore().pontosLivres(s.nos(), s.nivel());
        Component linha = Component.translatable("tela.minerwk.arvore.maestria", s.nivel(), livres);
        g.drawCenteredString(font, linha, width / 2, 20, COR_PODE);
        int l = Math.min(200, width - 2 * LayoutArvore.MARGEM);
        int x = (width - l) / 2;
        int cheio = (int) Math.round(l * LogicaHud.fracaoXp(s.xpNoNivel(), s.xpProximo()));
        g.fill(x - 1, 32, x + l + 1, 36, 0xAA000000);
        g.fill(x, 33, x + cheio, 35, COR_PODE);
    }

    private void desenharNo(GuiGraphics g, SincronizarKit s, No no, Retangulo r, boolean despertar) {
        int y = r.y() - rolagem;
        int nivel = nivelDe(s, no);
        Arvore.Resultado estado = estadoDe(s, no);
        boolean cheio = estado == Arvore.Resultado.NIVEL_MAXIMO;
        boolean trancado = estado == Arvore.Resultado.NAO_DESPERTOU || estado == Arvore.Resultado.MAESTRIA_BAIXA;
        int borda = cheio ? COR_CHEIO : estado == Arvore.Resultado.OK ? (despertar ? COR_DESPERTAR : COR_PODE) : COR_APAGADO;
        g.fill(r.x() - 1, y - 1, r.x() + r.largura() + 1, y + r.altura() + 1, borda);
        g.fill(r.x(), y, r.x() + r.largura(), y + r.altura(), trancado ? FUNDO_TRANCADO : FUNDO);
        int corTexto = trancado && nivel == 0 ? TEXTO_APAGADO : TEXTO;
        String nome = cortar(Component.translatable("no.minerwk." + no.id()).getString(), r.largura() - 6);
        g.drawString(font, nome, r.x() + 3, y + 3, corTexto, false);
        String direita = despertar ? "" : nivel + "/" + no.niveis();
        g.drawString(font, direita, r.x() + r.largura() - 3 - font.width(direita), y + 15, cheio ? COR_CHEIO : corTexto, false);
        g.drawString(font, cortar(rodape(s, no, estado).getString(), r.largura() - 8 - font.width(direita)),
                r.x() + 3, y + 15, estado == Arvore.Resultado.OK ? COR_PODE : TEXTO_APAGADO, false);
    }

    /** A linha de baixo do nó: o custo, o motivo de não dar para comprar ou o que ele já liberou. */
    private Component rodape(SincronizarKit s, No no, Arvore.Resultado estado) {
        if (no.tipo() == No.Tipo.DESPERTAR && nivelDe(s, no) > 0) {
            return Component.translatable(s.despertou() ? "tela.minerwk.arvore.desperto" : "tela.minerwk.arvore.pronto");
        }
        return switch (estado) {
            case NIVEL_MAXIMO -> Component.translatable("tela.minerwk.arvore.maximo");
            case NAO_DESPERTOU -> Component.translatable("tela.minerwk.arvore.exige_despertar");
            case MAESTRIA_BAIXA -> Component.translatable("tela.minerwk.arvore.exige_maestria", no.nivelMinimo());
            default -> Component.translatable("tela.minerwk.arvore.custo", no.custoDoProximo(nivelDe(s, no)));
        };
    }

    private List<Component> dica(SincronizarKit s, No no) {
        List<Component> linhas = new ArrayList<>();
        int nivel = nivelDe(s, no);
        String chave = "no.minerwk." + no.id();
        linhas.add(Component.translatable(chave).withStyle(ChatFormatting.GOLD));
        if (no.tipo() == No.Tipo.DESPERTAR) {
            linhas.add(Component.translatable(chave + ".desc").withStyle(ChatFormatting.GRAY));
        } else {
            if (nivel > 0) {
                linhas.add(Component.translatable("tela.minerwk.arvore.agora",
                        Component.translatable(chave + ".desc", porcento(no, nivel))).withStyle(ChatFormatting.WHITE));
            }
            if (nivel < no.niveis()) {
                linhas.add(Component.translatable("tela.minerwk.arvore.proximo",
                        Component.translatable(chave + ".desc", porcento(no, nivel + 1))).withStyle(ChatFormatting.GRAY));
            }
        }
        Arvore.Resultado estado = estadoDe(s, no);
        if (estado == Arvore.Resultado.OK) {
            linhas.add(Component.translatable("tela.minerwk.arvore.clique", no.custoDoProximo(nivel)).withStyle(ChatFormatting.YELLOW));
        } else if (estado == Arvore.Resultado.SEM_PONTOS) {
            linhas.add(Component.translatable("tela.minerwk.arvore.sem_pontos", no.custoDoProximo(nivel)).withStyle(ChatFormatting.RED));
        } else if (estado != Arvore.Resultado.NIVEL_MAXIMO) {
            linhas.add(rodape(s, no, estado).copy().withStyle(ChatFormatting.RED));
        }
        return linhas;
    }

    private static int porcento(No no, int nivel) {
        return (int) Math.round(no.porNivel() * nivel * 100);
    }

    private String cortar(String texto, int largura) {
        if (font.width(texto) <= largura) return texto;
        String reticencias = "…";
        return font.plainSubstrByWidth(texto, Math.max(0, largura - font.width(reticencias))) + reticencias;
    }

    @Override
    public boolean mouseClicked(double x, double y, int botao) {
        int i = botao == 0 ? layout.indiceEm(x, y, rolagem) : -1;
        if (i >= 0) {
            No no = nos.get(i);
            boolean pode = estadoDe(RecepcaoKit.estado(), no) == Arvore.Resultado.OK;
            if (pode) PacketDistributor.sendToServer(new ComprarNo(no.id()));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                    pode ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.UI_BUTTON_CLICK.value(), pode ? 1.2f : .6f));
            return true;
        }
        return super.mouseClicked(x, y, botao);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double rolagemX, double rolagemY) {
        if (rolagemY != 0 && layout.rolagemMaxima() > 0) {
            rolagem = Mth.clamp(rolagem - (int) Math.signum(rolagemY) * 12, 0, layout.rolagemMaxima());
            return true;
        }
        return super.mouseScrolled(x, y, rolagemX, rolagemY);
    }

    @Override
    public boolean keyPressed(int tecla, int scan, int modificadores) {
        // A mesma tecla que abre também fecha.
        if (TeclasKit.eDaArvore(tecla, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(tecla, scan, modificadores);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
