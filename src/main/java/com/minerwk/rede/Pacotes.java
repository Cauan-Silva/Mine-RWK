package com.minerwk.rede;

import com.minerwk.kit.ServicoKits;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registro dos payloads. Os que vão para o cliente chamam receptores que o lado do cliente
 * preenche ({@code client/kit/RecepcaoKit}); assim este arquivo não carrega classes do cliente.
 */
public final class Pacotes {
    public static volatile Consumer<SincronizarKit> aoSincronizar = p -> { };
    public static volatile Consumer<TocarApresentacao> aoTocar = p -> { };

    private Pacotes() {
    }

    public static void registrar(RegisterPayloadHandlersEvent evento) {
        PayloadRegistrar r = evento.registrar("1");
        r.playToServer(UsarTecla.TIPO, UsarTecla.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer jogador) ServicoKits.usarTecla(jogador, p.tecla(), p.apertou());
        });
        r.playToServer(EscolherPersonagem.TIPO, EscolherPersonagem.CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer jogador) ServicoKits.escolher(jogador, p.id());
        });
        r.playToClient(SincronizarKit.TIPO, SincronizarKit.CODEC, (p, ctx) -> aoSincronizar.accept(p));
        r.playToClient(TocarApresentacao.TIPO, TocarApresentacao.CODEC, (p, ctx) -> aoTocar.accept(p));
    }
}
