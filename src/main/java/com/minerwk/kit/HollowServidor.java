package com.minerwk.kit;

import com.minerwk.arena.ArenasServidor;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.personagens.ichigo.Ichigo;
import com.minerwk.core.personagens.ichigo.Reiatsu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * A forma Hollow completa do Ichigo no servidor: o golpe que mataria no Bankai vira a forma, e nela ele
 * recebe metade do dano, não morre e o clique não ataca (quem ataca é a forma, em {@link Ichigo}).
 */
public final class HollowServidor {
    /** Durante a forma ele recebe essa parte do dano. */
    public static final float PARTE_DO_DANO = .5f;

    private HollowServidor() {
    }

    public static void registrarEventos() {
        // O dano que de fato tiraria vida, já com armadura, encantamentos e absorção descontados: um golpe
        // forte que a armadura seguraria não gasta a forma à toa.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, HollowServidor::aoReceberDano);
        NeoForge.EVENT_BUS.addListener(HollowServidor::aoAtacar);
        // Na forma o jogador só anda e pula: nada de arco, poção, bloco ou alavanca.
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> {
            if (naForma(e.getEntity())) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (naForma(e.getEntity())) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock e) -> {
            if (naForma(e.getEntity())) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> {
            if (naForma(e.getEntity())) e.setCanceled(true);
        });
    }

    /** Se o jogador está na forma Hollow completa. */
    public static boolean naForma(Player jogador) {
        return jogador instanceof ServerPlayer servidor
                && ServicoKits.kit(servidor).map(k -> Reiatsu.HOLLOW.equals(k.forma())).orElse(false);
    }

    private static void aoAtacar(AttackEntityEvent evento) {
        if (naForma(evento.getEntity())) evento.setCanceled(true);
    }

    private static void aoReceberDano(LivingDamageEvent.Pre evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador)) return;
        // /kill e o vazio continuam matando.
        if (evento.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        KitDoJogador kit = ServicoKits.kit(jogador).orElse(null);
        if (kit == null || !Ichigo.ID.equals(kit.personagem().id())) return;
        if (Reiatsu.HOLLOW.equals(kit.forma())) {
            // Não morre: o golpe que mataria o deixa com 1 de vida.
            evento.setNewDamage(Math.max(0, Math.min(evento.getNewDamage() * PARTE_DO_DANO, jogador.getHealth() - 1)));
            return;
        }
        if (evento.getNewDamage() < jogador.getHealth()) return;
        boolean pronta = kit.recargas().pronta(Ichigo.PASSIVA_HOLLOW, jogador.level().getGameTime());
        // O despertar de vez, o da vitória sobre o Zangetsu: o de dentro da luta não conta.
        boolean desperto = ServicoKits.dados(jogador).maestriaAtual().despertou;
        if (!Ichigo.podeVirarHollow(kit.forma(), desperto, pronta, ArenasServidor.naArena(jogador))) return;
        evento.setNewDamage(0);
        Ichigo.virarHollow(new ContextoServidor(jogador));
    }
}
