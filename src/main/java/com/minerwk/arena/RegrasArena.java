package com.minerwk.arena;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** As regras que só valem dentro da dimensão da arena. */
public final class RegrasArena {
    /** O que o jogador tinha ao morrer na arena, até ele renascer. */
    private record Guardado(ListTag inventario, int nivel, float progresso, int total) {
    }

    private static final Map<UUID, Guardado> GUARDADOS = new HashMap<>();

    private RegrasArena() {
    }

    static void registrarEventos() {
        // A arena é apagada quando a luta acaba: o que caísse nela sumiria. Quem morre lá não perde nada.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RegrasArena::aoMorrer);
        NeoForge.EVENT_BUS.addListener(RegrasArena::aoDerrubar);
        NeoForge.EVENT_BUS.addListener(RegrasArena::aoDerrubarXp);
        NeoForge.EVENT_BUS.addListener(RegrasArena::aoRenascer);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> GUARDADOS.clear());
    }

    /** Por último e só se a morte valeu: o inventário é copiado antes de o jogo esvaziá-lo. */
    private static void aoMorrer(LivingDeathEvent evento) {
        if (evento.isCanceled() || !(evento.getEntity() instanceof ServerPlayer j) || !ArenasServidor.naArena(j)) return;
        GUARDADOS.put(j.getUUID(), new Guardado(j.getInventory().save(new ListTag()), j.experienceLevel,
                j.experienceProgress, j.totalExperience));
    }

    private static void aoDerrubar(LivingDropsEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer j && GUARDADOS.containsKey(j.getUUID())) evento.setCanceled(true);
    }

    private static void aoDerrubarXp(LivingExperienceDropEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer j && GUARDADOS.containsKey(j.getUUID())) evento.setCanceled(true);
    }

    private static void aoRenascer(PlayerEvent.Clone evento) {
        if (!evento.isWasDeath() || !(evento.getEntity() instanceof ServerPlayer novo)) return;
        Guardado g = GUARDADOS.remove(novo.getUUID());
        if (g == null) return;
        novo.getInventory().load(g.inventario());
        novo.experienceLevel = g.nivel();
        novo.experienceProgress = g.progresso();
        novo.totalExperience = g.total();
    }
}
