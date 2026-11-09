package com.minerwk.kit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Alvos segurados no ar por um golpe (o uppercut do R): a cada tick o centro vai para o ponto pedido,
 * sem velocidade nem caminho. Como no congelamento do arremesso, nada vai para o disco: um mob salvo no
 * meio do golpe volta normal. Jogadores são levados por teleporte a cada tick.
 */
public final class SuspensosServidor {
    private record Suspenso(ServerLevel nivel, Vec3 centro, long fim) {
    }

    private static final Map<UUID, Suspenso> ATIVOS = new HashMap<>();

    private SuspensosServidor() {
    }

    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> ATIVOS.clear());
    }

    /** Segura {@code alvo} com o centro em {@code centro} por {@code ticks} (renova se já estava segurado). */
    static void suspender(LivingEntity alvo, Vec3 centro, int ticks) {
        if (!(alvo.level() instanceof ServerLevel nivel) || !alvo.isAlive()) return;
        // Montado ou com passageiro, o veículo brigaria com o teleporte a cada tick.
        alvo.stopRiding();
        alvo.ejectPassengers();
        ATIVOS.put(alvo.getUUID(), new Suspenso(nivel, centro, nivel.getGameTime() + ticks));
        segurar(alvo, centro);
    }

    public static boolean suspenso(Entity e) {
        return ATIVOS.containsKey(e.getUUID());
    }

    private static void segurar(LivingEntity alvo, Vec3 centro) {
        double x = centro.x;
        double y = centro.y - alvo.getBbHeight() / 2;
        double z = centro.z;
        if (alvo instanceof Mob m) m.getNavigation().stop();
        alvo.setDeltaMovement(Vec3.ZERO);
        // Sem queda acumulada: ao ser solto, o dano de queda conta só a partir daqui.
        alvo.resetFallDistance();
        if (alvo instanceof ServerPlayer jogador && jogador.connection != null) {
            jogador.connection.teleport(x, y, z, jogador.getYRot(), jogador.getXRot());
            jogador.connection.aboveGroundTickCount = 0;
        } else {
            alvo.teleportTo(x, y, z);
            alvo.hurtMarked = true;
            alvo.hasImpulse = true;
        }
    }

    private static void tick(MinecraftServer servidor) {
        for (Iterator<Map.Entry<UUID, Suspenso>> it = ATIVOS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Suspenso> e = it.next();
            Suspenso s = e.getValue();
            Entity ent = s.nivel().getEntity(e.getKey());
            if (!(ent instanceof LivingEntity alvo) || !alvo.isAlive() || ent.level() != s.nivel()
                    || s.nivel().getGameTime() >= s.fim()) {
                it.remove();
                continue;
            }
            segurar(alvo, s.centro());
        }
    }
}
