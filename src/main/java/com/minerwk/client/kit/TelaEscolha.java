package com.minerwk.client.kit;

import com.minerwk.MineRWK;
import com.minerwk.core.hud.LayoutEscolha;
import com.minerwk.rede.EscolherPersonagem;
import com.minerwk.rede.PersonagemEscolha;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tela de escolha do personagem: grade de emblemas à esquerda (rola com a roda do mouse) e painel com
 * emblema grande, obra e golpes do selecionado. "Escolher" abre a confirmação, porque a escolha é definitiva.
 */
public final class TelaEscolha extends Screen {
    private static final int EMBLEMA_GRANDE_L = 96;
    private static final int EMBLEMA_GRANDE_A = 128;
    private static final int LINHA = 11;
    private static final int BOTAO_A = 20;
    private static final ResourceLocation GENERICO = emblema("_generico");
    private static final Map<String, ResourceLocation> EMBLEMAS = new HashMap<>();

    private final List<PersonagemEscolha> personagens;
    /** Aberta pelo {@code /rwk tela}: só para ver, nunca envia a escolha ao servidor. */
    private final boolean demonstracao;
    private LayoutEscolha layout;
    private int rolagem;
    private int selecionado;
    private boolean confirmando;

    public TelaEscolha(List<PersonagemEscolha> personagens) {
        this(personagens, false);
    }

    public TelaEscolha(List<PersonagemEscolha> personagens, boolean demonstracao) {
        super(Component.translatable("tela.minerwk.escolha"));
        this.personagens = List.copyOf(personagens);
        this.demonstracao = demonstracao;
    }

    @Override
    protected void init() {
        layout = new LayoutEscolha(width, height, personagens.size());
        rolagem = Mth.clamp(rolagem, 0, layout.rolagemMaxima());
        if (confirmando) {
            int l = 104;
            int x = width / 2 - l - 4;
            int y = height / 2;
            addRenderableWidget(Button.builder(Component.translatable("tela.minerwk.escolha.sim"), b -> enviar())
                    .bounds(x, y, l, BOTAO_A).build());
            addRenderableWidget(Button.builder(Component.translatable("tela.minerwk.escolha.nao"), b -> confirmar(false))
                    .bounds(width / 2 + 4, y, l, BOTAO_A).build());
        } else {
            LayoutEscolha.Retangulo p = layout.painel();
            Button escolher = Button.builder(Component.translatable("tela.minerwk.escolha.escolher"), b -> confirmar(true))
                    .bounds(p.x(), p.y() + p.altura() - BOTAO_A, Math.min(100, p.largura()), BOTAO_A).build();
            escolher.active = atual() != null;
            addRenderableWidget(escolher);
        }
    }

    private PersonagemEscolha atual() {
        return selecionado >= 0 && selecionado < personagens.size() ? personagens.get(selecionado) : null;
    }

    private void confirmar(boolean sim) {
        confirmando = sim && atual() != null;
        rebuildWidgets();
    }

    private void enviar() {
        PersonagemEscolha p = atual();
        if (p != null && !demonstracao) PacketDistributor.sendToServer(new EscolherPersonagem(p.id()));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float parcial) {
        super.render(g, mouseX, mouseY, parcial);
        PersonagemEscolha p = atual();
        if (confirmando && p != null) {
            g.drawCenteredString(font, Component.translatable("tela.minerwk.escolha.confirmar", p.nome()),
                    width / 2, height / 2 - 30, 0xFFFFFF);
            g.drawCenteredString(font, Component.translatable("tela.minerwk.escolha.definitiva"),
                    width / 2, height / 2 - 18, 0xFFAA55);
            return;
        }
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        desenharGrade(g, mouseX, mouseY);
        if (p != null) desenharPainel(g, p);
    }

    private void desenharGrade(GuiGraphics g, int mouseX, int mouseY) {
        int sobMouse = layout.indiceEm(mouseX, mouseY, rolagem);
        for (int linha = 0; linha < layout.linhasVisiveis(); linha++) {
            for (int coluna = 0; coluna < layout.colunas(); coluna++) {
                int i = (linha + rolagem) * layout.colunas() + coluna;
                if (i >= personagens.size()) return;
                int x = layout.celulaX(coluna);
                int y = layout.celulaY(linha);
                int l = LayoutEscolha.EMBLEMA_L;
                int a = LayoutEscolha.EMBLEMA_A;
                g.blit(emblemaDe(personagens.get(i).id()), x, y, l, a, 0, 0,
                        EMBLEMA_GRANDE_L, EMBLEMA_GRANDE_A, EMBLEMA_GRANDE_L, EMBLEMA_GRANDE_A);
                if (i == sobMouse) g.fill(x, y, x + l, y + a, 0x40FFFFFF);
                if (i == selecionado) g.renderOutline(x - 1, y - 1, l + 2, a + 2, 0xFFFFD24A);
            }
        }
        if (layout.rolagemMaxima() > 0) {
            LayoutEscolha.Retangulo r = layout.grade();
            int trilho = r.altura();
            int barra = Math.max(8, trilho * layout.linhasVisiveis() / (layout.linhasVisiveis() + layout.rolagemMaxima()));
            int y = r.y() + (trilho - barra) * rolagem / layout.rolagemMaxima();
            int x = r.x() + r.largura() - 2;
            g.fill(x, r.y(), x + 2, r.y() + trilho, 0x40FFFFFF);
            g.fill(x, y, x + 2, y + barra, 0xC0FFFFFF);
        }
    }

    private void desenharPainel(GuiGraphics g, PersonagemEscolha p) {
        LayoutEscolha.Retangulo r = layout.painel();
        int golpesA = p.golpes().size() * LINHA;
        boolean cabeInteiro = r.altura() >= EMBLEMA_GRANDE_A + 6 + golpesA + BOTAO_A + 4
                && r.largura() >= EMBLEMA_GRANDE_L + 40;
        int el = cabeInteiro ? EMBLEMA_GRANDE_L : EMBLEMA_GRANDE_L / 2;
        int ea = cabeInteiro ? EMBLEMA_GRANDE_A : EMBLEMA_GRANDE_A / 2;
        g.blit(emblemaDe(p.id()), r.x(), r.y(), el, ea, 0, 0,
                EMBLEMA_GRANDE_L, EMBLEMA_GRANDE_A, EMBLEMA_GRANDE_L, EMBLEMA_GRANDE_A);

        int tx = r.x() + el + 6;
        int tl = r.x() + r.largura() - tx;
        g.drawString(font, cortar(p.nome(), tl), tx, r.y() + 2, 0xFFFFFF);
        g.drawString(font, cortar(p.obra(), tl), tx, r.y() + 2 + LINHA, 0xA0A0A0);

        int y = r.y() + ea + 6;
        int limite = r.y() + r.altura() - BOTAO_A - 4;
        for (PersonagemEscolha.GolpeEscolha golpe : p.golpes()) {
            if (y + font.lineHeight > limite) break;
            String chave = "habilidade." + MineRWK.MOD_ID + "." + golpe.id();
            String texto = golpe.tecla().name() + "  " + Component.translatable(chave).getString()
                    + " — " + Component.translatable(chave + ".desc").getString();
            g.drawString(font, cortar(texto, r.largura()), r.x(), y, 0xE0E0E0);
            y += LINHA;
        }
    }

    private String cortar(String texto, int largura) {
        if (font.width(texto) <= largura) return texto;
        String reticencias = "…";
        return font.plainSubstrByWidth(texto, Math.max(0, largura - font.width(reticencias))) + reticencias;
    }

    @Override
    public boolean mouseClicked(double x, double y, int botao) {
        if (!confirmando && botao == 0) {
            int i = layout.indiceEm(x, y, rolagem);
            if (i >= 0) {
                selecionado = i;
                return true;
            }
        }
        return super.mouseClicked(x, y, botao);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double rolagemX, double rolagemY) {
        if (!confirmando && rolagemY != 0) {
            rolagem = Mth.clamp(rolagem - (int) Math.signum(rolagemY), 0, layout.rolagemMaxima());
            return true;
        }
        return super.mouseScrolled(x, y, rolagemX, rolagemY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static ResourceLocation emblema(String id) {
        return MineRWK.id("textures/gui/personagens/" + id + ".png");
    }

    /** Emblema do personagem, ou o genérico se o arquivo não existir. */
    private static ResourceLocation emblemaDe(String id) {
        return EMBLEMAS.computeIfAbsent(id, i -> {
            ResourceLocation rl = emblema(i);
            return Minecraft.getInstance().getResourceManager().getResource(rl).isPresent() ? rl : GENERICO;
        });
    }
}
