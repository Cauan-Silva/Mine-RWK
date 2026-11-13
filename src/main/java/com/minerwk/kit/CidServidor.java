package com.minerwk.kit;

import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.cid.Cid;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * O traje de slime do Cid no servidor: a cada 12 s, o próximo golpe forte que ele recebe vem pela metade
 * (a regra está em {@link Cid#amortecer}).
 */
public final class CidServidor {
    /** Quanto dura o visual do traje endurecendo. */
    private static final int VISUAL = 12;

    private CidServidor() {
    }

    public static void registrarEventos() {
        // O dano que de fato tiraria vida, já com armadura e absorção descontadas, como na forma Hollow.
        // Antes da camada de poder (HIGH): o golpe forte é medido como antes do balanceamento.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CidServidor::aoReceberDano);
    }

    private static void aoReceberDano(LivingDamageEvent.Pre evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador)) return;
        // /kill e o vazio passam inteiros.
        if (evento.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        KitDoJogador kit = ServicoKits.kit(jogador).orElse(null);
        if (kit == null || !Cid.ID.equals(kit.personagem().id())) return;
        // Se um dia as passivas do Cid forem trancadas pelo despertar, o traje segue a mesma tranca.
        if (!kit.desperto() && kit.personagem().trancas().passivas()) return;
        long agora = jogador.level().getGameTime();
        float dano = evento.getNewDamage();
        if (!Cid.gastaOTraje(dano, kit.recargas().pronta(Cid.TRAJE, agora))) return;
        evento.setNewDamage(Cid.amortecer(dano, true));
        kit.recargas().iniciar(Cid.TRAJE, agora, Cid.RECARGA_DO_TRAJE);
        ContextoServidor contexto = new ContextoServidor(jogador);
        contexto.visual(Cid.TRAJE, contexto.olhos(), new Vetor(0, 1, 0), 0f, com.minerwk.core.vfx.FormasCid.TRAJE);
    }
}
