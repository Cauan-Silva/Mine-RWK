package com.minerwk.boss;

import com.minerwk.arena.ArenasServidor;
import com.minerwk.core.arena.CaixaArena;
import com.minerwk.core.arena.PlantaMundoInterior;
import com.minerwk.core.boss.TojiBoss;
import com.minerwk.core.boss.ZangetsuBoss;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.ichigo.ArvoreIchigo;
import com.minerwk.core.personagens.ichigo.Ichigo;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** As lutas contra o Zangetsu interior em andamento: quem tem um vivo, quem está caído, e a entrada no mundo interior. */
public final class LutasZangetsu {
    /** O Zangetsu de cada jogador que entrou no mundo interior. */
    private static final Map<UUID, ZangetsuEntity> VIVOS = new HashMap<>();
    /** Quem está no chão esperando a máscara: não leva dano. */
    static final Set<UUID> CAIDOS = new HashSet<>();
    /** Quem gastou uma Máscara Rachada e ainda não viu o Zangetsu entrar: se ele não entrar, ela é devolvida. */
    private static final Set<UUID> PAGARAM = new HashSet<>();

    private LutasZangetsu() {
    }

    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener(LutasZangetsu::aoReceberDano);
        // Os mapas são estáticos: no jogo de um jogador só eles sobreviveriam ao fechar e abrir o mundo.
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            VIVOS.clear();
            CAIDOS.clear();
            PAGARAM.clear();
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> CAIDOS.remove(e.getEntity().getUUID()));
    }

    /** A Máscara Rachada de {@code jogador} foi gasta nesta entrada. */
    static void pagou(ServerPlayer jogador) {
        PAGARAM.add(jogador.getUUID());
    }

    static void registrar(ZangetsuEntity zangetsu) {
        VIVOS.put(zangetsu.invocadorId(), zangetsu);
    }

    static void esquecer(ZangetsuEntity zangetsu) {
        if (zangetsu.invocadorId() != null) VIVOS.remove(zangetsu.invocadorId(), zangetsu);
    }

    /** O Zangetsu vivo da luta desse jogador. */
    public static Optional<ZangetsuEntity> de(UUID jogador) {
        ZangetsuEntity z = VIVOS.get(jogador);
        if (z != null && (z.isRemoved() || !z.isAlive())) {
            VIVOS.remove(jogador);
            z = null;
        }
        return Optional.ofNullable(z);
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
        // O que mataria quem o invocou vira a queda: ele se levanta de máscara. Vale para qualquer dano durante
        // a luta, não só o golpe dele: um empurrão para o vão entre os prédios também fere.
        ZangetsuEntity zangetsu = de(vitima.getUUID()).orElse(null);
        if (zangetsu != null && zangetsu.level() == vitima.level() && zangetsu.podeDerrubar(vitima)
                && ZangetsuBoss.golpeMortal(vitima.getHealth(), com.minerwk.kit.PoderServidor.recebidoPor(vitima, evento.getAmount()))) {
            evento.setCanceled(true);
            zangetsu.derrubar(vitima);
        }
    }

    /**
     * Começa a luta: o jogador é levado para uma arena própria, com o cenário do mundo interior, e o Zangetsu
     * entra lá, 16 blocos à frente dele, assim que ela fica pronta. Com {@code exigir}, vale a regra da
     * Máscara Rachada (ser Ichigo e ter o nó Despertar, que pede maestria 20); sem, só não pode haver outra
     * luta dele (comando de teste). As respostas são as mesmas do Contrato do Toji.
     */
    public static TojiBoss.Invocacao invocar(ServerPlayer jogador, boolean exigir) {
        if (ArenasServidor.naArena(jogador) || ArenasServidor.de(jogador.getUUID()).isPresent()) {
            return TojiBoss.Invocacao.NA_ARENA;
        }
        DadosKit dados = ServicoKits.dados(jogador);
        boolean temNo = dados.temPersonagem() && dados.maestriaAtual().nos.getOrDefault(ArvoreIchigo.DESPERTAR, 0) > 0;
        boolean temBoss = de(jogador.getUUID()).isPresent();
        TojiBoss.Invocacao resposta = exigir ? TojiBoss.podeInvocar(dados.personagem, Ichigo.ID, temNo, temBoss)
                : temBoss ? TojiBoss.Invocacao.JA_EXISTE : TojiBoss.Invocacao.OK;
        if (resposta != TojiBoss.Invocacao.OK) return resposta;
        return ArenasServidor.entrar(jogador, com.minerwk.rede.CenarioArena.INTERIOR, PlantaMundoInterior.cenario(), LutasZangetsu::entrarNaArena)
                ? TojiBoss.Invocacao.OK : TojiBoss.Invocacao.NA_ARENA;
    }

    /** A arena ficou pronta e {@code jogador} já está nela: o Zangetsu entra de frente para ele. */
    private static void entrarNaArena(ServerPlayer jogador, CaixaArena caixa) {
        ServerLevel nivel = jogador.serverLevel();
        ZangetsuEntity zangetsu = Entidades.ZANGETSU.get().create(nivel);
        if (zangetsu != null) {
            Vetor ponto = caixa.entradaToji();
            // O jogador entra ao sul dele: yaw 0 olha para o sul.
            zangetsu.moveTo(ponto.x(), ponto.y(), ponto.z(), 0, 0);
            zangetsu.yBodyRot = 0;
            zangetsu.yHeadRot = 0;
            zangetsu.comecarLuta(jogador);
            if (nivel.addFreshEntity(zangetsu)) {
                PAGARAM.remove(jogador.getUUID());
                return;
            }
            esquecer(zangetsu);
        }
        if (PAGARAM.remove(jogador.getUUID())) {
            jogador.getInventory().placeItemBackInInventory(new net.minecraft.world.item.ItemStack(com.minerwk.registro.Itens.MASCARA_RACHADA.get()));
        }
        // Sem ele não há luta: o jogador volta para de onde saiu.
        ArenasServidor.lutaAcabou(nivel.getServer(), jogador.getUUID(), false);
    }
}
