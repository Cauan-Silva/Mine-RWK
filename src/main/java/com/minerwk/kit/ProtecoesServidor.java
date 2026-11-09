package com.minerwk.kit;

import com.minerwk.core.kit.Vetor;
import com.minerwk.rede.Envio;
import com.minerwk.rede.MostrarVisual;
import com.minerwk.registro.Efeitos;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
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
    /** Último tick em que cada Infinito mostrou um toque (no máximo um a cada {@link #INTERVALO_TOQUE}). */
    private static final Map<UUID, Long> ULTIMO_TOQUE = new HashMap<>();
    private static final int INTERVALO_TOQUE = 3;

    private ProtecoesServidor() {
    }

    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener(ProtecoesServidor::aoReceberDano);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            INFINITO.remove(e.getEntity().getUUID());
            ULTIMO_TOQUE.remove(e.getEntity().getUUID());
        });
    }

    static void ligarInfinito(ServerPlayer jogador, int ticks) {
        INFINITO.merge(jogador.getUUID(), jogador.level().getGameTime() + ticks, Math::max);
    }

    public static boolean infinitoAtivo(Player jogador) {
        Long fim = INFINITO.get(jogador.getUUID());
        return fim != null && jogador.level().getGameTime() < fim;
    }

    /**
     * Mostra a quem vê que o Infinito de {@code dono} parou algo vindo de {@code de}: ondas na bolha a
     * partir desse lado e, se {@code projetil}, um halo tremendo no ponto.
     */
    static void toque(ServerPlayer dono, Vec3 de, boolean projetil) {
        long agora = dono.level().getGameTime();
        // Projétil sempre passa (já aparece uma vez só); golpes seguidos se limitam.
        if (!projetil) {
            Long ultimo = ULTIMO_TOQUE.get(dono.getUUID());
            if (ultimo != null && agora - ultimo < INTERVALO_TOQUE && agora >= ultimo) return;
            ULTIMO_TOQUE.put(dono.getUUID(), agora);
        }
        Vec3 centro = dono.getBoundingBox().getCenter();
        Vec3 d = de.subtract(centro);
        d = d.lengthSqr() < 1e-6 ? dono.getLookAngle() : d.normalize();
        Vetor ponto = projetil ? new Vetor(de.x, de.y, de.z) : new Vetor(centro.x + d.x * 1.4, centro.y + d.y * 1.4, centro.z + d.z * 1.4);
        Envio.paraQuemVe(dono, new MostrarVisual(dono.getId(), "gojo_infinito_toque", ponto, new Vetor(d.x, d.y, d.z),
                projetil ? 2f : 1f, projetil ? 30 : 16, agora));
    }

    public static boolean paralisado(LivingEntity entidade) {
        return entidade.hasEffect(Efeitos.PARALISIA);
    }

    private static void aoReceberDano(LivingIncomingDamageEvent evento) {
        Entity atacante = evento.getSource().getEntity();
        // Paralisado não ataca: o golpe dele nos outros não chega (Espinhos e dano em si mesmo continuam).
        if (atacante instanceof LivingEntity vivo && vivo != evento.getEntity() && paralisado(vivo)
                && !evento.getSource().is(DamageTypes.THORNS)) {
            evento.setCanceled(true);
            return;
        }
        // Infinito: corpo a corpo e projéteis não chegam; queda, fogo e afins continuam valendo.
        if (evento.getEntity() instanceof Player jogador && infinitoAtivo(jogador)) {
            boolean fisico = atacante != null || evento.getSource().getDirectEntity() instanceof Projectile;
            if (fisico) {
                evento.setCanceled(true);
                Entity direto = evento.getSource().getDirectEntity();
                Entity de = direto != null ? direto : atacante;
                if (jogador instanceof ServerPlayer dono && de != null) {
                    toque(dono, de.getBoundingBox().getCenter(), de instanceof Projectile);
                }
            }
        }
    }
}
