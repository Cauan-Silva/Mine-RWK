package com.minerwk.kit;

import com.minerwk.registro.Efeitos;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Estados de golpes que protegem ou travam no servidor: Infinito (nada físico chega no jogador)
 * e Paralisia (quem está paralisado não causa dano).
 */
public final class ProtecoesServidor {
    /** Tick de jogo em que o Infinito de cada jogador acaba. */
    private static final Map<UUID, Long> INFINITO = new HashMap<>();

    private ProtecoesServidor() {
    }

    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener(ProtecoesServidor::aoReceberDano);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> INFINITO.remove(e.getEntity().getUUID()));
    }

    static void ligarInfinito(ServerPlayer jogador, int ticks) {
        INFINITO.merge(jogador.getUUID(), jogador.level().getGameTime() + ticks, Math::max);
    }

    public static boolean infinitoAtivo(Player jogador) {
        Long fim = INFINITO.get(jogador.getUUID());
        return fim != null && jogador.level().getGameTime() < fim;
    }

    public static boolean paralisado(LivingEntity entidade) {
        return entidade.hasEffect(Efeitos.PARALISIA);
    }

    private static void aoReceberDano(LivingIncomingDamageEvent evento) {
        Entity atacante = evento.getSource().getEntity();
        // Paralisado não ataca: o golpe dele não chega.
        if (atacante instanceof LivingEntity vivo && paralisado(vivo)) {
            evento.setCanceled(true);
            return;
        }
        // Infinito: corpo a corpo e projéteis não chegam; queda, fogo e afins continuam valendo.
        if (evento.getEntity() instanceof Player jogador && infinitoAtivo(jogador)) {
            boolean fisico = atacante != null || evento.getSource().getDirectEntity() instanceof Projectile;
            if (fisico) evento.setCanceled(true);
        }
    }
}
