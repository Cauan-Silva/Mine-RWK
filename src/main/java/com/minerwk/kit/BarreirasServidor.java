package com.minerwk.kit;

import com.minerwk.core.kit.Barreira;
import com.minerwk.core.kit.Vetor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Barreiras esféricas (domínio) e voo (Roxo 200%) no servidor. A regra é a de {@link Barreira}; o
 * cliente aplica a mesma ao próprio jogador, então aqui o jogador só é puxado de volta se fugir muito
 * (cliente sem a regra). Enquanto está numa barreira ou voando, o jogador não leva dano de queda e não é
 * chutado por "voar".
 */
public final class BarreirasServidor {
    /** Quanto o jogador pode estar fora da regra antes de o servidor o puxar de volta. */
    private static final double TOLERANCIA_JOGADOR = 1.5;

    private static final class Ativa {
        final ResourceKey<Level> dimensao;
        final Vetor centro;
        final double raio;
        final double chao;
        final long fim;
        /** Lado de cada entidade (dentro?), decidido quando ela é vista pela primeira vez. */
        final Map<UUID, Boolean> lado = new HashMap<>();

        Ativa(ResourceKey<Level> dimensao, Vetor centro, double raio, double chao, long fim) {
            this.dimensao = dimensao;
            this.centro = centro;
            this.raio = raio;
            this.chao = chao;
            this.fim = fim;
        }
    }

    private static final List<Ativa> ATIVAS = new ArrayList<>();
    /** Tick em que o voo de cada jogador acaba. */
    private static final Map<UUID, Long> VOO = new HashMap<>();

    private BarreirasServidor() {
    }

    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> {
            ATIVAS.clear();
            VOO.clear();
        });
        NeoForge.EVENT_BUS.addListener(BarreirasServidor::aoCair);
        // Morreu no voo: quem renasce não fica sem dano de queda.
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent e) -> VOO.remove(e.getEntity().getUUID()));
    }

    static void criar(ServerPlayer dono, Vetor centro, double raio, double chao, int ticks) {
        ServerLevel nivel = dono.serverLevel();
        Ativa a = new Ativa(nivel.dimension(), centro, raio, chao, nivel.getGameTime() + ticks);
        for (Entity e : nivel.getEntities((Entity) null, caixa(a), BarreirasServidor::afetada)) {
            a.lado.put(e.getUUID(), Barreira.dentro(centro, raio, pes(e)));
        }
        a.lado.put(dono.getUUID(), true);
        ATIVAS.add(a);
    }

    static void voar(ServerPlayer jogador, int ticks) {
        VOO.merge(jogador.getUUID(), jogador.level().getGameTime() + ticks, Math::max);
        livreNoAr(jogador);
    }

    private static AABB caixa(Ativa a) {
        Vec3 c = new Vec3(a.centro.x(), a.centro.y(), a.centro.z());
        // Folga maior que o passo de um projétil rápido: quem vem de fora é visto ainda fora.
        return new AABB(c, c).inflate(a.raio + 6);
    }

    /**
     * Só criaturas e projéteis em voo. Quadros, armor stands, displays e itens ficam de fora (mudar a
     * posição deles os solta), e o dragão também (as partes se reposicionam sozinhas).
     */
    private static boolean afetada(Entity e) {
        if (!e.isAlive() || e.isSpectator() || e.isPassenger()) return false;
        // Flecha cravada (parada) fica onde está.
        if (e instanceof Projectile) return !(e instanceof AbstractArrow && e.getDeltaMovement().lengthSqr() < 1e-4);
        return e instanceof LivingEntity && !(e instanceof ArmorStand) && !(e instanceof EnderDragon);
    }

    private static Vetor pes(Entity e) {
        return new Vetor(e.getX(), e.getY(), e.getZ());
    }

    private static void tick(MinecraftServer servidor) {
        for (Iterator<Ativa> it = ATIVAS.iterator(); it.hasNext(); ) {
            Ativa a = it.next();
            ServerLevel nivel = servidor.getLevel(a.dimensao);
            if (nivel == null || nivel.getGameTime() >= a.fim) {
                it.remove();
                continue;
            }
            for (Entity e : nivel.getEntities((Entity) null, caixa(a), BarreirasServidor::afetada)) {
                boolean dentro = a.lado.computeIfAbsent(e.getUUID(), k -> Barreira.dentro(a.centro, a.raio, pes(e)));
                Vec3 v = e.getDeltaMovement();
                Barreira.Correcao c = Barreira.corrigir(a.centro, a.raio, a.chao, dentro, pes(e), new Vetor(v.x, v.y, v.z));
                double desvio = c.posicao().distancia(pes(e));
                if (e instanceof ServerPlayer jogador) {
                    if (dentro) livreNoAr(jogador);
                    if (desvio > TOLERANCIA_JOGADOR) {
                        jogador.teleportTo(c.posicao().x(), c.posicao().y(), c.posicao().z());
                    }
                    continue;
                }
                if (desvio < 1e-6 && !c.noChao()) continue;
                e.setPos(c.posicao().x(), c.posicao().y(), c.posicao().z());
                e.setDeltaMovement(c.velocidade().x(), c.velocidade().y(), c.velocidade().z());
                if (c.noChao()) {
                    e.setOnGround(true);
                    e.resetFallDistance();
                }
                // Parado no chão, a gravidade o tira do lugar só um pouco a cada tick: aí a posição já
                // chega pelo rastreador, sem um pacote de velocidade por tick.
                if (desvio > .2) e.hurtMarked = true;
            }
        }
        for (Iterator<Map.Entry<UUID, Long>> it = VOO.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> v = it.next();
            ServerPlayer jogador = servidor.getPlayerList().getPlayer(v.getKey());
            if (jogador == null || jogador.level().getGameTime() >= v.getValue()) {
                it.remove();
                continue;
            }
            livreNoAr(jogador);
        }
    }

    /** Sem queda acumulada e sem o contador que chuta quem fica muito tempo no ar. */
    private static void livreNoAr(ServerPlayer jogador) {
        jogador.resetFallDistance();
        jogador.connection.aboveGroundTickCount = 0;
    }

    private static void aoCair(LivingFallEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            Long fim = VOO.get(jogador.getUUID());
            if (fim != null && jogador.level().getGameTime() < fim) evento.setCanceled(true);
        }
    }
}
