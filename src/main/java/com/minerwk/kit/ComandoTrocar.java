package com.minerwk.kit;

import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Personagens;
import com.mojang.brigadier.CommandDispatcher;
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
