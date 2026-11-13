package com.minerwk.kit;

import com.minerwk.ConfigServidor;
import com.minerwk.MineRWK;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A camada de poder no jogador (spec "Balanceamento"): quem tem personagem ganha vida extra e recebe menos
 * dano. Os números vêm de {@link ConfigServidor}; o multiplicador dos golpes fica no {@link ContextoServidor}.
 */
public final class PoderServidor {
    private static final ResourceLocation VIDA = MineRWK.id("vida_do_personagem");
    /** De quanto em quanto tempo a vida é conferida: pega troca de config e de personagem sem pesar. */
    private static final int CONFERIR_A_CADA = 20;

    /** Sem camada nenhuma: o poder de quem está nos GameTests antigos, que conferem os números de antes. */
    static final com.minerwk.core.kit.Poder NEUTRO = new com.minerwk.core.kit.Poder(0, 0, 1, 1, 1);
    /** Jogadores de teste sem a camada ({@link #semCamada}). Nunca usado fora dos GameTests. */
    private static final java.util.Set<java.util.UUID> SEM_CAMADA = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private PoderServidor() {
    }

    /**
     * O poder que vale para este jogador: o da config, ou nenhum para os jogadores de teste marcados. Os
     * bosses invocados por ele seguem o mesmo valor.
     */
    public static com.minerwk.core.kit.Poder de(net.minecraft.world.entity.player.Player jogador) {
        return jogador != null && SEM_CAMADA.contains(jogador.getUUID()) ? NEUTRO : ConfigServidor.poder();
    }

    /**
     * Ferramenta dos GameTests: este jogador fica sem a camada, para os testes de mecânica conferirem os
     * números de cada golpe sem o multiplicador. Os testes da camada ({@code TestesPoder}) não usam.
     */
    public static void semCamada(ServerPlayer jogador) {
        SEM_CAMADA.add(jogador.getUUID());
        aplicarVida(jogador);
    }

    /** Desfaz {@link #semCamada}. */
    public static void comCamada(ServerPlayer jogador) {
        SEM_CAMADA.remove(jogador.getUUID());
        aplicarVida(jogador);
    }

    public static void registrarEventos() {
        // Ordem fixa no LivingDamageEvent.Pre: o traje do Cid (HIGHEST) mede o golpe antes da camada, a camada
        // (HIGH) reduz, e a forma Hollow (NORMAL) decide se o golpe mata já com a redução.
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, PoderServidor::aoReceberDano);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer j) aplicarVida(j);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.Clone e) -> {
            if (e.getEntity() instanceof ServerPlayer j) aoRenascer(j);
        });
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer j && j.tickCount % CONFERIR_A_CADA == 0) aplicarVida(j);
        });
    }

    /**
     * Põe ou tira o modificador de vida conforme o jogador tenha personagem. A vida atual nunca passa do novo
     * máximo; ganhar o modificador não cura.
     */
    public static void aplicarVida(ServerPlayer jogador) {
        AttributeInstance vida = jogador.getAttribute(Attributes.MAX_HEALTH);
        if (vida == null) return;
        double extra = ServicoKits.dados(jogador).temPersonagem() ? de(jogador).vidaExtra() : 0;
        AttributeModifier atual = vida.getModifier(VIDA);
        if (extra <= 0) {
            if (atual != null) vida.removeModifier(VIDA);
        } else if (atual == null || Math.abs(atual.amount() - extra) > 1e-6) {
            // Salvo com o jogador: carregado antes da vida, então sair e entrar não corta a vida em 20.
            vida.removeModifier(VIDA);
            vida.addPermanentModifier(new AttributeModifier(VIDA, extra, AttributeModifier.Operation.ADD_VALUE));
        }
        if (jogador.getHealth() > jogador.getMaxHealth()) jogador.setHealth(jogador.getMaxHealth());
    }

    /** Quem renasce é um jogador novo: recebe o modificador e começa com a vida cheia, como no jogo. */
    static void aoRenascer(ServerPlayer novo) {
        boolean cheio = novo.getHealth() >= novo.getMaxHealth() - 1e-3;
        aplicarVida(novo);
        if (cheio) novo.setHealth(novo.getMaxHealth());
    }

    /**
     * O dano que de fato chega a este jogador por causa da camada (sem a armadura). Para quem decide antes do
     * {@code LivingDamageEvent}, como a queda das lutas contra o Toji e o Zangetsu.
     */
    public static float recebidoPor(ServerPlayer jogador, float dano) {
        return ServicoKits.dados(jogador).temPersonagem() ? de(jogador).recebido(dano) : dano;
    }

    /** Depois da armadura, para somar com ela; {@code /kill} e o vazio passam inteiros. */
    private static void aoReceberDano(LivingDamageEvent.Pre evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador) || !ServicoKits.dados(jogador).temPersonagem()) return;
        if (evento.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        evento.setNewDamage(de(jogador).recebido(evento.getNewDamage()));
    }
}
