package com.minerwk.client.kit;

import com.minerwk.MineRWK;
import com.minerwk.core.hud.FormaArvore;
import com.minerwk.core.hud.FormaArvore.Galho;
import com.minerwk.core.hud.FormasArvore;
import com.minerwk.core.hud.LayoutArvore;
import com.minerwk.core.hud.LayoutArvore.Alvo;
import com.minerwk.core.hud.LayoutArvore.Linha;
import com.minerwk.core.hud.LayoutEscolha.Retangulo;
import com.minerwk.core.hud.LogicaHud;
import com.minerwk.core.hud.TemaUi;
import com.minerwk.core.hud.Temas;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.maestria.Arvore;
import com.minerwk.core.maestria.No;
import com.minerwk.rede.ComprarNo;
import com.minerwk.rede.SincronizarKit;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tela da árvore de habilidades do personagem atual, no tema dele (spec [[2026-11-12 UI dos personagens]]): o
 * emblema na raiz, os galhos de antes do Despertar, o Despertar no tronco e os galhos de depois em cima. Clicar
 * num nó pede a compra ao servidor, que é quem decide; a tela só mostra o estado que o servidor mandou.
 */
public final class TelaArvore extends Screen {
    private static final int COR_PODE = 0xFFE8B84A;
    private static final int COR_PODE_APAGADA = 0xFF7A5A20;
    private static final int COR_APAGADO = 0xFF4A4A55;
    private static final int VEU_TRANCADO = 0xA0000000;
    private static final int TEXTO = 0xFFFFFFFF;
    private static final int LINHA = 2;

    private final Personagem personagem;
    private final List<Galho> galhos = new ArrayList<>();
    private final Map<String, No> porId = new HashMap<>();
    private final FormaArvore forma;
    private final No despertar;
    private LayoutArvore layout;
    /** -1 até o primeiro {@link #init()}, que começa a tela embaixo, na raiz. */
    private int rolagem = -1;

    public TelaArvore(Personagem personagem, FormaArvore forma) {
        super(Component.translatable("tela.minerwk.arvore"));
        this.personagem = personagem;
        this.forma = forma;
        galhos.addAll(forma.antes());
        galhos.addAll(forma.depois());
        personagem.arvore().nos().forEach(n -> porId.put(n.id(), n));
        despertar = personagem.arvore().nos().stream().filter(n -> n.tipo() == No.Tipo.DESPERTAR).findFirst().orElseThrow();
    }

    /** Abre a árvore do personagem atual, se ele tem uma (com o Despertar e a forma desenhada). */
    public static void abrir() {
        Personagens.porId(RecepcaoKit.estado().personagem())
                .filter(p -> p.arvore().nos().stream().anyMatch(n -> n.tipo() == No.Tipo.DESPERTAR))
                .ifPresent(p -> FormasArvore.de(p.id())
                        .ifPresent(f -> Minecraft.getInstance().setScreen(new TelaArvore(p, f))));
    }

    @Override
    protected void init() {
        layout = new LayoutArvore(width, height, forma.antes().stream().map(g -> g.nos().size()).toList(),
                forma.depois().stream().map(g -> g.nos().size()).toList());
        rolagem = rolagem < 0 ? layout.rolagemMaxima() : Mth.clamp(rolagem, 0, layout.rolagemMaxima());
    }

    private static int nivelDe(SincronizarKit s, No no) {
        return Math.min(no.niveis(), s.nos().getOrDefault(no.id(), 0));
    }

    private Arvore.Resultado estadoDe(SincronizarKit s, No no) {
        return personagem.arvore().podeComprar(s.nos(), no.id(), s.nivel(), s.despertou());
    }

    /** O nó sob o alvo: o Despertar ou um nó de galho; null na ponta e no vazio. */
    private No noDe(Alvo alvo) {
        if (alvo == null) return null;
        if (alvo.galho() == LayoutArvore.DESPERTAR) return despertar;
        if (alvo.no() < 0) return null;
        return porId.get(galhos.get(alvo.galho()).nos().get(alvo.no()));
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
        TemaUi tema = Temas.tema(s.personagem(), s.forma());
        desenharFundo(g, tema);
        g.enableScissor(0, LayoutArvore.TOPO - 2, width, height);
        List<Linha> linhas = layout.linhas();
        for (int i = 0; i < linhas.size(); i++) desenharLinha(g, s, tema, linhas.get(i), i);
        desenharRaiz(g, tema);
        desenharNo(g, s, tema, despertar, layout.despertar(), despertar.id(), null);
        for (int i = 0; i < galhos.size(); i++) {
            Galho galho = galhos.get(i);
            Retangulo p = layout.ponta(i);
            int y = p.y() - rolagem;
            Molduras.desenhar(g, p.x(), y, p.largura(), tema, tema.corBorda());
            g.fill(p.x(), y, p.x() + p.largura(), y + p.altura(), 0xC0000000);
            if (Molduras.temIcone(galho.icone())) Molduras.icone(g, galho.icone(), p.x(), y, p.largura());
            if (galho.depoisDoDespertar() && !s.despertou()) g.fill(p.x(), y, p.x() + p.largura(), y + p.altura(), VEU_TRANCADO);
            for (int n = 0; n < galho.nos().size(); n++) {
                No no = porId.get(galho.nos().get(n));
                String selo = "selo_" + forma.selo(no).name().toLowerCase(Locale.ROOT);
                desenharNo(g, s, tema, no, layout.no(i, n), galho.icone(), selo);
            }
        }
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
        No sob = noDe(layout.alvoEm(mouseX, mouseY, rolagem));
        if (layout.painel().isPresent()) {
            desenharPainel(g, s, tema, layout.painel().get(), sob);
        } else if (sob != null) {
            g.renderComponentTooltip(font, dica(s, sob), mouseX, mouseY);
        }
    }

    /** O fundo do tema repetido pela tela, escurecido pela cor de fundo do tema. */
    private void desenharFundo(GuiGraphics g, TemaUi tema) {
        ResourceLocation fundo = MineRWK.id("textures/gui/fundos/" + tema.fundoArvore() + ".png");
        for (int x = 0; x < width; x += 256) {
            for (int y = 0; y < height; y += 256) g.blit(fundo, x, y, 0, 0, 256, 256, 256, 256);
        }
        g.fill(0, 0, width, height, 0x60000000 | (tema.corFundo() & 0xFFFFFF));
    }

    /** Uma ligação de 2 px: acesa na cor do tema quando o nó de destino tem nível; o tronco acima do Despertar só depois de despertar. */
    private void desenharLinha(GuiGraphics g, SincronizarKit s, TemaUi tema, Linha l, int indice) {
        boolean acesa;
        if (l.galho() < 0) {
            acesa = indice == 0 || s.despertou();
        } else {
            Galho galho = galhos.get(l.galho());
            acesa = l.no() < 0 ? galho.nos().stream().anyMatch(id -> nivelDe(s, porId.get(id)) > 0)
                    : nivelDe(s, porId.get(galho.nos().get(l.no()))) > 0;
        }
        int cor = acesa ? tema.corPrincipal() : COR_APAGADO;
        int x0 = Math.min(l.x1(), l.x2()), x1 = Math.max(l.x1(), l.x2());
        int y0 = Math.min(l.y1(), l.y2()) - rolagem, y1 = Math.max(l.y1(), l.y2()) - rolagem;
        g.fill(x0 - LINHA / 2, y0 - LINHA / 2, x1 + LINHA / 2, y1 + LINHA / 2, cor);
    }

    private void desenharRaiz(GuiGraphics g, TemaUi tema) {
        Retangulo r = layout.raiz();
        int y = r.y() - rolagem;
        Molduras.desenhar(g, r.x(), y, r.largura(), tema, tema.corPrincipal());
        g.fill(r.x(), y, r.x() + r.largura(), y + r.altura(), 0xC0000000);
        RenderSystem.enableBlend();
        // O emblema da tela de escolha (96x128), de pé dentro do quadro.
        g.blit(MineRWK.id("textures/gui/personagens/" + personagem.id() + ".png"), r.x() + 3, y, 18, 24, 0, 0, 96, 128, 96, 128);
    }

    /** Um nó: o ícone (com o selo), a moldura no estado dele e os pontos de nível embaixo. */
    private void desenharNo(GuiGraphics g, SincronizarKit s, TemaUi tema, No no, Retangulo r, String icone, String selo) {
        int y = r.y() - rolagem;
        int nivel = nivelDe(s, no);
        Arvore.Resultado estado = estadoDe(s, no);
        boolean trancado = estado == Arvore.Resultado.NAO_DESPERTOU || estado == Arvore.Resultado.MAESTRIA_BAIXA;
        boolean pisca = (System.currentTimeMillis() / 400) % 2 == 0;
        int borda = estado == Arvore.Resultado.OK ? (pisca ? COR_PODE : COR_PODE_APAGADA)
                : nivel > 0 ? tema.corPrincipal() : COR_APAGADO;
        Molduras.desenhar(g, r.x(), y, r.largura(), tema, borda);
        g.fill(r.x(), y, r.x() + r.largura(), y + r.altura(), 0xC0000000);
        if (Molduras.temIcone(icone)) Molduras.icone(g, icone, r.x(), y, r.largura());
        if (selo != null && Molduras.temIcone(selo)) Molduras.icone(g, selo, r.x(), y, r.largura());
        if (trancado && nivel == 0) g.fill(r.x(), y, r.x() + r.largura(), y + r.altura(), VEU_TRANCADO);
        if (no.tipo() == No.Tipo.DESPERTAR) return;
        // Os pontos de nível: um por nível, cheios na cor do tema.
        int total = no.niveis(), largura = total * 4 - 2;
        int x0 = r.x() + (r.largura() - largura) / 2;
        for (int i = 0; i < total; i++) {
            g.fill(x0 + i * 4, y + r.altura() + 3, x0 + i * 4 + 2, y + r.altura() + 5, i < nivel ? tema.corPrincipal() : COR_APAGADO);
        }
    }

    /** O painel ao lado da árvore: o detalhe do nó sob o mouse. */
    private void desenharPainel(GuiGraphics g, SincronizarKit s, TemaUi tema, Retangulo p, No sob) {
        g.fill(p.x() - 1, p.y() - 1, p.x() + p.largura() + 1, p.y() + p.altura() + 1, tema.corBorda());
        g.fill(p.x(), p.y(), p.x() + p.largura(), p.y() + p.altura(), 0xD0000000 | (tema.corFundo() & 0xFFFFFF));
        if (sob == null) return;
        int y = p.y() + 4;
        for (Component linha : dica(s, sob)) {
            for (var parte : font.split(linha, p.largura() - 8)) {
                if (y + 9 > p.y() + p.altura()) return;
                g.drawString(font, parte, p.x() + 4, y, TEXTO, true);
                y += 10;
            }
            y += 2;
        }
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
        No no = botao == 0 ? noDe(layout.alvoEm(x, y, rolagem)) : null;
        if (no != null) {
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
