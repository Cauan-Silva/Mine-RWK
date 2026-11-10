package com.minerwk.kit;

import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Personagens;
import com.mojang.brigadier.CommandDispatcher;
import com.minerwk.core.maestria.Maestria;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /rwk trocar <jogador> <personagem>} e {@code /rwk recarregar [jogador]}: só para operadores (administração e testes). */
public final class ComandoTrocar {
    private ComandoTrocar() {
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("rwk").then(Commands.literal("trocar")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("jogador", EntityArgument.player())
                        .then(Commands.argument("personagem", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Personagens.todos().stream().map(Personagem::id), b))
                                .executes(ComandoTrocar::trocar)))));
    }

    /** {@code /rwk recarregar [jogador]}: enche a energia e zera as recargas (teste), só para operadores. */
    public static void registrarRecarregar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("rwk").then(Commands.literal("recarregar")
                .requires(s -> s.hasPermission(2))
                .executes(c -> recarregar(c, c.getSource().getPlayerOrException()))
                .then(Commands.argument("jogador", EntityArgument.player())
                        .executes(c -> recarregar(c, EntityArgument.getPlayer(c, "jogador"))))));
    }

    /**
     * {@code /rwk maestria <jogador> nivel <n>}, {@code ... xp <quantia>} e {@code ... zerar}: mexe na maestria
     * do personagem atual (teste), só para operadores.
     */
    public static void registrarMaestria(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("rwk").then(Commands.literal("maestria")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("jogador", EntityArgument.player())
                        .then(Commands.literal("nivel").then(Commands.argument("n",
                                        IntegerArgumentType.integer(1, Maestria.NIVEL_MAX))
                                .executes(c -> maestria(c, Maestria.xpAte(IntegerArgumentType.getInteger(c, "n")), false))))
                        .then(Commands.literal("xp").then(Commands.argument("quantia", IntegerArgumentType.integer(1))
                                .executes(c -> maestria(c, IntegerArgumentType.getInteger(c, "quantia"), true))))
                        .then(Commands.literal("zerar").executes(c -> maestria(c, 0, false)))
                        .then(Commands.literal("comprar").then(Commands.argument("no", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(nosDe(c), b))
                                .executes(ComandoTrocar::comprar))))));
    }

    private static java.util.stream.Stream<String> nosDe(CommandContext<CommandSourceStack> c) {
        try {
            String id = ServicoKits.dados(EntityArgument.getPlayer(c, "jogador")).personagem;
            return Personagens.porId(id).stream().flatMap(p -> p.arvore().nos().stream()).map(n -> n.id());
        } catch (CommandSyntaxException e) {
            return java.util.stream.Stream.empty();
        }
    }

    /** {@code /rwk maestria <jogador> comprar <no>}: a mesma compra que a tela da árvore faz, com as mesmas regras. */
    private static int comprar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer jogador = EntityArgument.getPlayer(c, "jogador");
        String no = StringArgumentType.getString(c, "no");
        var r = ServicoKits.comprarNo(jogador, no);
        if (r != com.minerwk.core.maestria.Arvore.Resultado.OK) {
            c.getSource().sendFailure(Component.literal("Não comprou " + no + ": " + r));
            return 0;
        }
        DadosMaestria m = ServicoKits.dados(jogador).maestriaAtual();
        String texto = jogador.getName().getString() + ": " + no + " no nível " + m.nos.get(no);
        c.getSource().sendSuccess(() -> Component.literal(texto), true);
        return 1;
    }

    private static int maestria(CommandContext<CommandSourceStack> c, long xp, boolean somar) throws CommandSyntaxException {
        ServerPlayer jogador = EntityArgument.getPlayer(c, "jogador");
        DadosKit dados = ServicoKits.dados(jogador);
        if (!dados.temPersonagem()) {
            c.getSource().sendFailure(Component.literal(jogador.getName().getString() + " ainda não escolheu um personagem"));
            return 0;
        }
        if (somar) ServicoKits.ganharXp(jogador, xp);
        else {
            // Zerar também devolve os pontos: os nós comprados somem.
            if (xp == 0) {
                dados.maestriaAtual().nos.clear();
                ServicoKits.atualizarMelhorias(jogador);
            }
            ServicoKits.definirXp(jogador, xp);
        }
        var p = Maestria.progresso(dados.maestriaAtual().xp);
        String texto = jogador.getName().getString() + ": maestria " + p.nivel() + " (" + p.xpNoNivel() + "/" + p.xpParaProximo() + " XP)";
        c.getSource().sendSuccess(() -> Component.literal(texto), true);
        return 1;
    }

    private static int recarregar(CommandContext<CommandSourceStack> c, ServerPlayer jogador) {
        var kit = ServicoKits.kit(jogador);
        if (kit.isEmpty()) {
            c.getSource().sendFailure(Component.literal(jogador.getName().getString() + " ainda não escolheu um personagem"));
            return 0;
        }
        kit.get().recarregar();
        String nome = jogador.getName().getString() + ": energia cheia e recargas zeradas";
        c.getSource().sendSuccess(() -> Component.literal(nome), true);
        return 1;
    }

    private static int trocar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer jogador = EntityArgument.getPlayer(c, "jogador");
        String id = StringArgumentType.getString(c, "personagem");
        Optional<Personagem> personagem = Personagens.porId(id);
        if (personagem.isEmpty()) {
            c.getSource().sendFailure(Component.literal("Personagem desconhecido: " + id));
            return 0;
        }
        ServicoKits.definirPersonagem(jogador, personagem.get());
        String nome = jogador.getName().getString() + " agora é " + personagem.get().nome();
        c.getSource().sendSuccess(() -> Component.literal(nome), true);
        return 1;
    }
}
