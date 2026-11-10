package com.minerwk.client.comando;

import com.minerwk.MineRWK;
import com.minerwk.client.animacao.AnimacoesJogador;
import com.minerwk.client.vfx.EfeitosVisuais;
import com.minerwk.registro.Sons;
import com.minerwk.client.kit.TelaEscolha;
import com.minerwk.core.kit.Tecla;
import com.minerwk.rede.PersonagemEscolha;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Comandos de teste da prova de conceito, só no cliente (tocam apenas para quem digitou):
 * {@code /rwk fx <efeito>}, {@code /rwk anim <animação>}, {@code /rwk som} e {@code /rwk tela <quantos>}
 * (abre a tela de escolha com personagens falsos, para ver a grade e a rolagem; a confirmação não envia nada ao servidor).
 * Sem namespace, o id vira {@code minerwk:<nome>}.
 */
public final class ComandosTeste {
    private ComandosTeste() {
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("rwk")
                .then(Commands.literal("fx").then(Commands.argument("efeito", ResourceLocationArgument.id()).executes(ComandosTeste::fx)))
                .then(Commands.literal("anim").then(Commands.argument("animacao", ResourceLocationArgument.id()).executes(ComandosTeste::anim)))
                .then(Commands.literal("parar").executes(ComandosTeste::parar))
                .then(Commands.literal("previa").executes(ComandosTeste::previa))
                .then(Commands.literal("som").executes(ComandosTeste::som))
                .then(Commands.literal("tela").then(Commands.argument("quantos", IntegerArgumentType.integer(1, 200))
                        .executes(ComandosTeste::tela))));
    }

    private static ResourceLocation idDoMod(CommandContext<CommandSourceStack> ctx, String arg) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, arg);
        // O argumento põe "minecraft:" quando não tem namespace; aqui o padrão é o do mod.
        return id.getNamespace().equals("minecraft") && !ctx.getInput().contains("minecraft:") ? MineRWK.id(id.getPath()) : id;
    }

    private static int fx(CommandContext<CommandSourceStack> ctx) {
        LocalPlayer jogador = Minecraft.getInstance().player;
        if (jogador == null) return 0;
        ResourceLocation efeito = idDoMod(ctx, "efeito");
        boolean ok = EfeitosVisuais.noBloco(efeito, jogador.level(), jogador.blockPosition());
        return responder(ctx, ok, "Efeito " + efeito);
    }

    /** Prévia do VFX do R só no cliente (fusão perto e explosão longe), sem alvo nem servidor. */
    private static int previa(CommandContext<CommandSourceStack> ctx) {
        com.minerwk.client.vfx.DemoVfx.previa();
        return responder(ctx, true, "Prévia do R");
    }

    private static int anim(CommandContext<CommandSourceStack> ctx) {
        LocalPlayer jogador = Minecraft.getInstance().player;
        if (jogador == null) return 0;
        ResourceLocation animacao = idDoMod(ctx, "animacao");
        boolean ok = AnimacoesJogador.tocar(jogador, animacao);
        if (ok) tocarSom(jogador);
        return responder(ctx, ok, "Animação " + animacao);
    }

    private static int parar(CommandContext<CommandSourceStack> ctx) {
        LocalPlayer jogador = Minecraft.getInstance().player;
        if (jogador != null) AnimacoesJogador.parar(jogador);
        return 1;
    }

    private static int som(CommandContext<CommandSourceStack> ctx) {
        LocalPlayer jogador = Minecraft.getInstance().player;
        if (jogador == null) return 0;
        tocarSom(jogador);
        return responder(ctx, true, "Som minerwk:teste");
    }

    private static final String[] EMBLEMAS = {"gojo", "asta", "cid", "dio", "douma", "frieren", "guts", "homura",
            "ichigo", "jinwoo", "killua", "rudeus", "subaru", "tanjiro", "sem_emblema"};

    private static int tela(CommandContext<CommandSourceStack> ctx) {
        int quantos = IntegerArgumentType.getInteger(ctx, "quantos");
        List<PersonagemEscolha> falsos = new ArrayList<>();
        for (int i = 0; i < quantos; i++) {
            String id = EMBLEMAS[i % EMBLEMAS.length];
            falsos.add(new PersonagemEscolha(id, "Falso " + (i + 1) + " (" + id + ")", "Obra de teste",
                    List.of(new PersonagemEscolha.GolpeEscolha(Tecla.Z, "teste_golpe"),
                            new PersonagemEscolha.GolpeEscolha(Tecla.X, "teste_carga"))));
        }
        // Adia para depois de o chat fechar, senão ele fecha a tela junto.
        Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new TelaEscolha(falsos, true)));
        return 1;
    }

    private static void tocarSom(LocalPlayer jogador) {
        jogador.level().playLocalSound(jogador.getX(), jogador.getY(), jogador.getZ(),
                Sons.TESTE.get(), SoundSource.PLAYERS, 1.0f, 1.0f, false);
    }

    private static int responder(CommandContext<CommandSourceStack> ctx, boolean ok, String oQue) {
        if (ok) {
            ctx.getSource().sendSuccess(() -> Component.literal(oQue + " tocando"), false);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal(oQue + " não encontrado(a)"));
        return 0;
    }
}
