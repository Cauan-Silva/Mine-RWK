package com.minerwk.boss;

import com.minerwk.arena.ArenasServidor;
import com.minerwk.core.arena.CaixaArena;
import com.minerwk.core.boss.TojiBoss;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.ArvoreGojo;
import com.minerwk.core.personagens.gojo.Gojo;
import com.minerwk.kit.DadosKit;
import com.minerwk.kit.ServicoKits;
import com.minerwk.registro.Entidades;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** As lutas contra o Toji em andamento: quem tem um Toji vivo, quem está caído, e a invocação. */
public final class LutasToji {
    /** O Toji de cada jogador que invocou um. */
    private static final Map<UUID, TojiEntity> VIVOS = new HashMap<>();
    /** Quem está no chão esperando o despertar: não leva dano. */
    static final Set<UUID> CAIDOS = new HashSet<>();

    private LutasToji() {
    }

    /** Registrar depois das proteções do kit: um golpe que o Infinito parou não chega aqui. */
    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener(LutasToji::aoReceberDano);
        // Os mapas são estáticos: no jogo de um jogador só eles sobreviveriam ao fechar e abrir o mundo.
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> {
            VIVOS.clear();
            CAIDOS.clear();
            ServicoKits.esquecerDespertosNaLuta();
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> {
            CAIDOS.remove(e.getEntity().getUUID());
        });
    }

    static void registrar(TojiEntity toji) {
        VIVOS.put(toji.invocadorId(), toji);
    }

    static void esquecer(TojiEntity toji) {
        if (toji.invocadorId() != null) VIVOS.remove(toji.invocadorId(), toji);
    }

    /** O Toji vivo que esse jogador invocou. */
    public static Optional<TojiEntity> de(UUID jogador) {
        TojiEntity toji = VIVOS.get(jogador);
        if (toji != null && (toji.isRemoved() || !toji.isAlive())) {
            VIVOS.remove(jogador);
            toji = null;
        }
        return Optional.ofNullable(toji);
    }

    public static boolean caido(Player jogador) {
        return CAIDOS.contains(jogador.getUUID());
    }

    private static void aoReceberDano(LivingIncomingDamageEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer vitima)) return;
        // /kill e o vazio continuam matando.
        if (evento.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (CAIDOS.contains(vitima.getUUID())) {
            evento.setCanceled(true);
            return;
        }
        // O golpe do Toji que mataria quem o invocou vira o despertar.
        if (evento.getSource().getEntity() instanceof TojiEntity toji && toji.podeDerrubar(vitima)
                && TojiBoss.golpeMortal(vitima.getHealth(), com.minerwk.kit.PoderServidor.recebidoPor(vitima, evento.getAmount()))) {
            evento.setCanceled(true);
            toji.derrubar(vitima);
        }
    }

    /**
     * Começa a luta: o jogador é levado para uma arena própria (ver {@link ArenasServidor}) e o Toji entra
     * lá, 16 blocos à frente dele, assim que ela fica pronta. Com {@code exigir}, vale a regra do Contrato
     * (ser Gojo e ter o nó Despertar); sem, só não pode haver outra luta dele (comando de teste). {@code OK}
     * quer dizer que a entrada começou; o Toji aparece até 2 segundos depois.
     */
    public static TojiBoss.Invocacao invocar(ServerPlayer jogador, boolean exigir) {
        if (ArenasServidor.naArena(jogador) || ArenasServidor.de(jogador.getUUID()).isPresent()) {
            return TojiBoss.Invocacao.NA_ARENA;
        }
        DadosKit dados = ServicoKits.dados(jogador);
        boolean temNo = dados.temPersonagem() && dados.maestriaAtual().nos.getOrDefault(ArvoreGojo.DESPERTAR, 0) > 0;
        boolean temToji = de(jogador.getUUID()).isPresent();
        TojiBoss.Invocacao resposta = exigir ? TojiBoss.podeInvocar(dados.personagem, Gojo.ID, temNo, temToji)
                : temToji ? TojiBoss.Invocacao.JA_EXISTE : TojiBoss.Invocacao.OK;
        if (resposta != TojiBoss.Invocacao.OK) return resposta;
        return ArenasServidor.entrar(jogador, LutasToji::entrarNaArena) ? TojiBoss.Invocacao.OK : TojiBoss.Invocacao.NA_ARENA;
    }

    /** A arena ficou pronta e {@code jogador} já está nela: o Toji entra de frente para ele. */
    private static void entrarNaArena(ServerPlayer jogador, CaixaArena caixa) {
        ServerLevel nivel = jogador.serverLevel();
        TojiEntity toji = Entidades.TOJI.get().create(nivel);
        if (toji != null) {
            Vetor ponto = caixa.entradaToji();
            // O jogador entra ao sul dele: yaw 0 olha para o sul.
            toji.moveTo(ponto.x(), ponto.y(), ponto.z(), 0, 0);
            toji.yBodyRot = 0;
            toji.yHeadRot = 0;
            toji.comecarLuta(jogador);
            if (nivel.addFreshEntity(toji)) return;
            esquecer(toji);
        }
        // Sem Toji não há luta: o jogador volta para de onde saiu.
        ArenasServidor.lutaAcabou(nivel.getServer(), jogador.getUUID(), false);
    }
}
