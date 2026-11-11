package com.minerwk.kit;

import com.minerwk.core.kit.Habilidade;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.kit.Tecla;
import com.minerwk.rede.Envio;
import com.minerwk.rede.PersonagemEscolha;
import com.minerwk.rede.SincronizarKit;
import com.minerwk.rede.SincronizarKit.HabilidadeHud;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manda ao jogador o estado do kit: na entrada, ao escolher/trocar, quando uma recarga começa,
 * quando a habilidade em uso, a forma ou o XP de maestria muda e a cada 10 ticks se o recurso mudou.
 */
final class Sincronizacao {
    private static final int RECURSO_A_CADA = 10;

    private record Enviado(double recurso, Map<String, Long> recargas, String emUso, long xp, String forma) {
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
            List<HabilidadeHud> habilidades = new ArrayList<>();
            Map<String, Long> prontas = k.recargas().salvar();
            for (Tecla t : Tecla.values()) {
                // Só os golpes da forma em que ele está.
                k.habilidade(t).ifPresent(h -> habilidades.add(new HabilidadeHud(t, h.id(),
                        h.numeros().custo(), prontas.getOrDefault(h.id(), 0L), k.recargas().duracao(h.id()), k.trancada(h))));
            }
            String emUso = emUso(k);
            DadosMaestria maestria = ServicoKits.dados(jogador).maestriaAtual();
            long xp = maestria.xp;
            var progresso = com.minerwk.core.maestria.Maestria.progresso(xp);
            pacote = new SincronizarKit(k.personagem().id(), k.recurso().nome(), k.recurso().cor(),
                    k.recurso().atual(), k.recurso().maximo(), habilidades, emUso, List.of(),
                    progresso.nivel(), progresso.xpNoNivel(), progresso.xpParaProximo(),
                    Map.copyOf(maestria.nos), maestria.despertou, k.forma());
            ULTIMO.put(jogador.getUUID(), new Enviado(k.recurso().atual(), prontas, emUso, xp, k.forma()));
        } else {
            List<PersonagemEscolha> lista = Personagens.escolhiveis().stream().map(PersonagemEscolha::de).toList();
            pacote = new SincronizarKit("", "", 0, 0, 0, List.of(), "", lista, 0, 0, 0, Map.of(), false,
                    com.minerwk.core.kit.Forma.BASE);
            ULTIMO.remove(jogador.getUUID());
        }
        Envio.para(jogador, pacote);
    }

    private static String emUso(KitDoJogador k) {
        return k.execucao().map(e -> e.habilidade()).map(Habilidade::id).orElse("");
    }

    static void aoTick(ServerPlayer jogador, KitDoJogador kit) {
        Enviado ultimo = ULTIMO.get(jogador.getUUID());
        if (ultimo == null) {
            enviar(jogador);
            return;
        }
        boolean recargaMudou = !ultimo.recargas().equals(kit.recargas().salvar());
        boolean usoMudou = !ultimo.emUso().equals(emUso(kit));
        boolean recursoMudou = jogador.tickCount % RECURSO_A_CADA == 0 && ultimo.recurso() != kit.recurso().atual();
        // Um golpe em área dá XP várias vezes no mesmo tick: o envio é um só, aqui.
        boolean xpMudou = ultimo.xp() != ServicoKits.dados(jogador).maestriaAtual().xp;
        boolean formaMudou = !ultimo.forma().equals(kit.forma());
        if (recargaMudou || usoMudou || recursoMudou || xpMudou || formaMudou) enviar(jogador);
    }

    static void esquecer(ServerPlayer jogador) {
        ULTIMO.remove(jogador.getUUID());
    }
}
