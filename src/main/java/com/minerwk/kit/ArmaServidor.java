package com.minerwk.kit;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.Arma;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * O ataque básico de quem tem arma própria (a Zangetsu do Ichigo). O kit pede a arma todo tick; aqui o
 * dano do clique vira o da arma, seja qual for o item na mão, e a velocidade e o alcance do ataque vão
 * para os atributos do jogador. Sem pedido no tick, tudo volta ao normal.
 */
public final class ArmaServidor {
    private static final ResourceLocation MODIFICADOR = ResourceLocation.fromNamespaceAndPath(MineRWK.MOD_ID, "arma_do_kit");
    /** O golpe crítico do Minecraft multiplica por 1,5: é o teto da proporção que o clique carrega. */
    private static final float PROPORCAO_MAXIMA = 1.5f;
    private static final Map<UUID, Arma> PEDIDOS = new HashMap<>();
    private static final Map<UUID, Arma> ATUAIS = new HashMap<>();

    private ArmaServidor() {
    }

    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener(ArmaServidor::aoFerir);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> esquecer(e.getEntity().getUUID()));
    }

    /** O kit pede a arma deste tick; o último pedido ganha. */
    static void pedir(ServerPlayer jogador, Arma arma) {
        PEDIDOS.put(jogador.getUUID(), arma);
    }

    /** A arma que vale agora para o jogador (ou null). */
    public static Arma atual(ServerPlayer jogador) {
        return ATUAIS.get(jogador.getUUID());
    }

    /** Fim do tick do kit: passa a valer o que foi pedido, ou nada. */
    static void aplicar(ServerPlayer jogador) {
        Arma arma = PEDIDOS.remove(jogador.getUUID());
        if (arma == null) {
            ATUAIS.remove(jogador.getUUID());
        } else {
            ATUAIS.put(jogador.getUUID(), arma);
        }
        ajustar(jogador, Attributes.ATTACK_SPEED, arma == null ? 0 : arma.velocidade());
        ajustar(jogador, Attributes.ENTITY_INTERACTION_RANGE, arma == null ? 0 : arma.alcance());
    }

    private static void esquecer(UUID id) {
        PEDIDOS.remove(id);
        ATUAIS.remove(id);
    }

    /**
     * Leva o atributo ao valor {@code desejado} com um modificador nosso, descontando o que o item na mão
     * já soma (uma espada tira velocidade); 0 tira o modificador.
     */
    private static void ajustar(ServerPlayer jogador, Holder<Attribute> atributo, double desejado) {
        AttributeInstance inst = jogador.getAttribute(atributo);
        if (inst == null) return;
        AttributeModifier atual = inst.getModifier(MODIFICADOR);
        double nosso = atual == null ? 0 : atual.amount();
        if (desejado <= 0) {
            if (atual != null) inst.removeModifier(MODIFICADOR);
            return;
        }
        double novo = desejado - (inst.getValue() - nosso);
        if (Math.abs(novo - nosso) < 1e-6) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(MODIFICADOR, novo, AttributeModifier.Operation.ADD_VALUE));
    }

    /**
     * O dano do clique vira o da arma. A proporção entre o dano que chegou e o ataque do jogador guarda a
     * recarga do ataque (golpe apressado é fraco) e o crítico, e é mantida.
     */
    private static void aoFerir(LivingIncomingDamageEvent evento) {
        DamageSource fonte = evento.getSource();
        if (!(fonte.getDirectEntity() instanceof ServerPlayer jogador) || !fonte.is(DamageTypes.PLAYER_ATTACK)) return;
        if (ServicoKits.emGolpeDoKit(jogador)) return;
        Arma arma = ATUAIS.get(jogador.getUUID());
        if (arma == null) return;
        evento.setAmount(dano(arma, evento.getAmount(), (float) jogador.getAttributeValue(Attributes.ATTACK_DAMAGE)));
    }

    static float dano(Arma arma, float chegou, float ataqueDoJogador) {
        float proporcao = ataqueDoJogador <= 0 ? 1 : Math.max(0, Math.min(PROPORCAO_MAXIMA, chegou / ataqueDoJogador));
        return arma.dano() * proporcao;
    }
}
