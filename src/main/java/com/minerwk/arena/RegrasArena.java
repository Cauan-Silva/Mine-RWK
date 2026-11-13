package com.minerwk.arena;

import com.minerwk.boss.TojiEntity;
import com.minerwk.core.arena.CaixaArena;
import com.minerwk.core.arena.PlantaEscola;
import com.minerwk.core.kit.Barreira;
import com.minerwk.core.kit.Vetor;
import com.minerwk.kit.ContextoServidor;
import com.minerwk.registro.Entidades;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** As regras que só valem dentro da dimensão da arena. */
public final class RegrasArena {
    /**
     * O inventário de quem está morrendo na arena, do evento da morte até o dos itens caídos (no mesmo
     * tick). Depois disso os itens voltam para o próprio morto: é ele que vai para o disco se o jogo fechar
     * antes de o jogador renascer.
     */
    private static final Map<UUID, ListTag> MORRENDO = new HashMap<>();

    private RegrasArena() {
    }

    static void registrarEventos() {
        // Arenas indestrutíveis (spec "Balanceamento"): explosões do jogo (creeper, TNT, cristal) também não quebram.
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.ExplosionEvent.Detonate e) -> {
            if (e.getLevel().dimension() == ArenasServidor.DIMENSAO) e.getAffectedBlocks().clear();
        });
        // A arena é apagada quando a luta acaba: o que caísse nela sumiria. Quem morre lá não perde nada.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RegrasArena::aoMorrer);
        NeoForge.EVENT_BUS.addListener(RegrasArena::aoDerrubar);
        NeoForge.EVENT_BUS.addListener(RegrasArena::aoDerrubarXp);
        NeoForge.EVENT_BUS.addListener(RegrasArena::aoRenascer);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> MORRENDO.clear());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(RegrasArena::aoQuebrar);
        NeoForge.EVENT_BUS.addListener(RegrasArena::aoColocar);
    }

    // --- Blocos ---

    /** Na arena só os golpes destroem: à mão, só quem está no criativo (para testes). */
    private static boolean protegido(Entity quem) {
        return quem instanceof Player p && ArenasServidor.naArena(p) && !p.isCreative();
    }

    private static void aoQuebrar(BlockEvent.BreakEvent evento) {
        // Os golpes perguntam com este mesmo evento se podem quebrar: eles passam.
        if (!ContextoServidor.quebraDeGolpe() && protegido(evento.getPlayer())) evento.setCanceled(true);
    }

    private static void aoColocar(BlockEvent.EntityPlaceEvent evento) {
        if (protegido(evento.getEntity())) evento.setCanceled(true);
    }

    // --- Barreira e queda no vazio ---

    /** Quanto o jogador pode estar fora da barreira antes de o servidor o puxar (o cliente dele já a aplica). */
    private static final double TOLERANCIA_JOGADOR = 1.5;
    /** Dano de quem cai abaixo do fundo da caixa. */
    static final float DANO_DA_QUEDA = 4;

    private static void tick(MinecraftServer servidor) {
        ServerLevel arena = ArenasServidor.nivel(servidor);
        if (arena == null || arena.players().isEmpty()) return;
        for (ServerPlayer j : List.copyOf(arena.players())) {
            if (j.isSpectator() || !j.isAlive()) continue;
            segurar(arena, j, caixaDe(j.getUUID(), j), TOLERANCIA_JOGADOR);
        }
        for (TojiEntity toji : arena.getEntities(Entidades.TOJI.get(), LivingEntity::isAlive)) {
            segurar(arena, toji, caixaDe(toji.invocadorId(), toji), 0);
        }
        for (com.minerwk.boss.ZangetsuEntity z : arena.getEntities(Entidades.ZANGETSU.get(), LivingEntity::isAlive)) {
            segurar(arena, z, caixaDe(z.invocadorId(), z), 0);
        }
        for (com.minerwk.boss.RagnarokEntity r : arena.getEntities(Entidades.RAGNAROK.get(), LivingEntity::isAlive)) {
            segurar(arena, r, caixaDe(r.invocadorId(), r), 0);
        }
    }

    /** A caixa da luta desse jogador; sem luta (alguém levado por comando), a da vaga onde a entidade está. */
    private static CaixaArena caixaDe(UUID dono, Entity e) {
        return (dono == null ? Optional.<CaixaArena>empty() : ArenasServidor.de(dono))
                .orElseGet(() -> CaixaArena.daPosicao(e.getX(), e.getZ()));
    }

    private static void segurar(ServerLevel arena, LivingEntity e, CaixaArena caixa, double tolerancia) {
        if (caixa.abaixoDoFundo(e.getY())) {
            cairNoVazio(arena, e, caixa);
            return;
        }
        Vetor pes = new Vetor(e.getX(), e.getY(), e.getZ());
        Vec3 v = e.getDeltaMovement();
        Barreira.Correcao c = caixa.corrigir(pes, new Vetor(v.x, v.y, v.z), e.getBbWidth() / 2, e.getBbHeight());
        if (c.posicao().distancia(pes) <= tolerancia) return;
        // Montado, quem manda na posição é a montaria: ele desce para a barreira valer.
        e.stopRiding();
        e.teleportTo(c.posicao().x(), c.posicao().y(), c.posicao().z());
        e.setDeltaMovement(c.velocidade().x(), c.velocidade().y(), c.velocidade().z());
        e.hurtMarked = true;
    }

    /** Os golpes destruíram o chão e alguém caiu: o meio do pátio é refeito e a entidade volta para lá, ferida. */
    private static void cairNoVazio(ServerLevel arena, LivingEntity e, CaixaArena caixa) {
        // Senão a montaria o levaria de volta para o vazio no tick seguinte, e o dano se repetiria.
        e.stopRiding();
        Vetor centro = caixa.centro();
        BlockPos piso = BlockPos.containing(centro.x(), CaixaArena.Y_PATIO - 1, centro.z());
        for (BlockPos p : BlockPos.betweenClosed(piso.offset(-2, 0, -2), piso.offset(2, 0, 2))) {
            // Só o que foi destruído: o chão que sobrou é do cenário (cada boss tem o seu).
            if (arena.getBlockState(p).getCollisionShape(arena, p).isEmpty()) {
                arena.setBlockAndUpdate(p, ObrasArena.estado(PlantaEscola.Material.TIJOLO_PEDRA));
            }
        }
        // E espaço para ficar de pé, caso o centro esteja entulhado.
        for (BlockPos p : BlockPos.betweenClosed(piso.offset(-1, 1, -1), piso.offset(1, 3, 1))) {
            if (!arena.getBlockState(p).isAir()) arena.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
        }
        e.teleportTo(centro.x(), centro.y(), centro.z());
        e.setDeltaMovement(Vec3.ZERO);
        e.hurtMarked = true;
        e.resetFallDistance();
        e.hurt(e.damageSources().fellOutOfWorld(), DANO_DA_QUEDA);
    }

    /** Por último e só se a morte valeu: o inventário é copiado antes de o jogo esvaziá-lo. */
    private static void aoMorrer(LivingDeathEvent evento) {
        if (evento.isCanceled() || !(evento.getEntity() instanceof ServerPlayer j) || !ArenasServidor.naArena(j)) return;
        MORRENDO.put(j.getUUID(), j.getInventory().save(new ListTag()));
    }

    /** Nada cai na arena: os itens voltam para o inventário do morto. */
    private static void aoDerrubar(LivingDropsEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer j)) return;
        ListTag inventario = MORRENDO.remove(j.getUUID());
        if (inventario == null) return;
        evento.setCanceled(true);
        j.getInventory().load(inventario);
    }

    private static void aoDerrubarXp(LivingExperienceDropEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer j && ArenasServidor.naArena(j)) evento.setCanceled(true);
    }

    /** Quem renasce depois de morrer na arena recebe o que o morto tinha (itens e XP). */
    private static void aoRenascer(PlayerEvent.Clone evento) {
        if (!evento.isWasDeath() || !(evento.getEntity() instanceof ServerPlayer novo)
                || !(evento.getOriginal() instanceof ServerPlayer morto) || !ArenasServidor.naArena(morto)) return;
        novo.getInventory().replaceWith(morto.getInventory());
        novo.experienceLevel = morto.experienceLevel;
        novo.experienceProgress = morto.experienceProgress;
        novo.totalExperience = morto.totalExperience;
    }
}
