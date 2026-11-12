package com.minerwk.kit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Chamarizes (a Pós-imagem do Cid): os mobs que miravam o jogador quando a imagem ficou para trás largam
 * dele e vão até ela, enquanto ela durar. Depois voltam a escolher o alvo sozinhos. Mobs que escolhem o
 * alvo pela memória (piglins, o Warden) podem voltar a ele antes; nada vai para o disco.
 */
public final class ChamarizServidor {
    /** Até onde um mob que mirava o jogador é enganado. */
    static final double ALCANCE = 32;
    /** A velocidade com que eles andam até a imagem. */
    private static final double PRESSA = 1.2;

    private record Chamariz(ServerLevel nivel, UUID dono, Vec3 ponto, long fim, Set<UUID> enganados) {
    }

    private static final List<Chamariz> ATIVOS = new ArrayList<>();

    private ChamarizServidor() {
    }

    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        // Antes do tick de cada mob: se ele voltou a mirar o dono, larga de novo antes de os golpes dele rodarem.
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Pre e) -> {
            if (!ATIVOS.isEmpty() && e.getEntity() instanceof Mob m) largar(m);
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> ATIVOS.clear());
    }

    /** Deixa um chamariz de {@code dono} em {@code ponto} por {@code ticks}; o anterior dele some. */
    static void criar(ServerPlayer dono, Vec3 ponto, int ticks) {
        if (ticks <= 0) return;
        ServerLevel nivel = dono.serverLevel();
        ATIVOS.removeIf(c -> c.dono().equals(dono.getUUID()));
        Set<UUID> enganados = new HashSet<>();
        for (Mob m : nivel.getEntitiesOfClass(Mob.class, dono.getBoundingBox().inflate(ALCANCE),
                m -> m.getTarget() == dono && enganavel(m))) {
            enganados.add(m.getUUID());
        }
        if (enganados.isEmpty()) return;
        Chamariz c = new Chamariz(nivel, dono.getUUID(), ponto, nivel.getGameTime() + ticks, enganados);
        ATIVOS.add(c);
        atrair(c);
    }

    /** Quantos mobs o chamariz do jogador está enganando agora. */
    static int enganados(ServerPlayer dono) {
        return ATIVOS.stream().filter(c -> c.dono().equals(dono.getUUID())).mapToInt(c -> c.enganados().size()).sum();
    }

    /** Bosses não caem na imagem: os do mod (a luta lê o alvo deles), o Wither e o Dragão. */
    static boolean enganavel(Mob m) {
        return !(m instanceof com.minerwk.boss.TojiEntity || m instanceof com.minerwk.boss.ZangetsuEntity || m instanceof com.minerwk.boss.RagnarokEntity
                || m instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                || m instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon);
    }

    private static void largar(Mob m) {
        if (m.getTarget() == null) return;
        UUID alvo = m.getTarget().getUUID();
        for (Chamariz c : ATIVOS) {
            if (c.dono().equals(alvo) && c.enganados().contains(m.getUUID())) {
                m.setTarget(null);
                return;
            }
        }
    }

    private static void atrair(Chamariz c) {
        for (Iterator<UUID> it = c.enganados().iterator(); it.hasNext(); ) {
            Entity e = c.nivel().getEntity(it.next());
            if (!(e instanceof Mob m) || !m.isAlive()) {
                it.remove();
                continue;
            }
            // Quem apanhou de outro e mudou de alvo não é mais enganado; quem voltou ao dono, sim.
            if (m.getTarget() != null && !m.getTarget().getUUID().equals(c.dono())) {
                it.remove();
                continue;
            }
            m.setTarget(null);
            m.getNavigation().moveTo(c.ponto().x, c.ponto().y, c.ponto().z, PRESSA);
            m.getLookControl().setLookAt(c.ponto().x, c.ponto().y + 1.5, c.ponto().z);
        }
    }

    static void tick(MinecraftServer servidor) {
        for (Iterator<Chamariz> it = ATIVOS.iterator(); it.hasNext(); ) {
            Chamariz c = it.next();
            if (c.nivel().getGameTime() >= c.fim() || c.enganados().isEmpty()) {
                it.remove();
                continue;
            }
            atrair(c);
        }
    }
}
