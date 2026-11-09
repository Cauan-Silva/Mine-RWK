package com.minerwk.kit;

import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Personagens;
import com.minerwk.rede.Envio;
import com.minerwk.rede.SincronizarKit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manda ao jogador o estado do kit: na entrada, ao escolher/trocar, quando uma recarga começa
 * e a cada 10 ticks se o recurso mudou.
 */
final class Sincronizacao {
    private static final int RECURSO_A_CADA = 10;

    private record Enviado(double recurso, Map<String, Long> recargas) {
    }

    private static final Map<UUID, Enviado> ULTIMO = new HashMap<>();

    private Sincronizacao() {
    }

    /** Envia o estado completo agora. Sem personagem, manda a lista para a tela de escolha. */
    static void enviar(ServerPlayer jogador) {
        var kit = ServicoKits.kit(jogador);
        SincronizarKit pacote;
        if (kit.isPresent()) {
            KitDoJogador k = kit.get();
            pacote = new SincronizarKit(k.personagem().id(), k.recurso().atual(), k.recurso().maximo(),
                    k.recargas().salvar(), List.of());
            ULTIMO.put(jogador.getUUID(), new Enviado(k.recurso().atual(), k.recargas().salvar()));
        } else {
            List<String> lista = Personagens.escolhiveis().stream()
                    .map(p -> p.id() + "|" + p.nome() + "|" + p.obra()).toList();
            pacote = new SincronizarKit("", 0, 0, Map.of(), lista);
            ULTIMO.remove(jogador.getUUID());
        }
        Envio.para(jogador, pacote);
    }

    static void aoTick(ServerPlayer jogador, KitDoJogador kit) {
        Enviado ultimo = ULTIMO.get(jogador.getUUID());
        if (ultimo == null) {
            enviar(jogador);
            return;
        }
        boolean recargaMudou = !ultimo.recargas().equals(kit.recargas().salvar());
        boolean recursoMudou = jogador.tickCount % RECURSO_A_CADA == 0 && ultimo.recurso() != kit.recurso().atual();
        if (recargaMudou || recursoMudou) enviar(jogador);
    }

    static void esquecer(ServerPlayer jogador) {
        ULTIMO.remove(jogador.getUUID());
    }
}
