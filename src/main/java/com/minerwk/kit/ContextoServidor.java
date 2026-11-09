package com.minerwk.kit;

import com.minerwk.core.kit.Contexto;
import com.minerwk.rede.TocarApresentacao;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** O {@link Contexto} do núcleo, executado no servidor em nome de um jogador. */
public final class ContextoServidor implements Contexto {
    private final ServerPlayer jogador;

    public ContextoServidor(ServerPlayer jogador) {
        this.jogador = jogador;
    }

    public ServerPlayer jogador() {
        return jogador;
    }

    @Override
    public void danoEmArea(double raio, float dano) {
        var fonte = jogador.damageSources().playerAttack(jogador);
        for (LivingEntity alvo : jogador.level().getEntitiesOfClass(LivingEntity.class,
                jogador.getBoundingBox().inflate(raio),
                e -> e != jogador && e.isAlive() && e.distanceToSqr(jogador) <= raio * raio)) {
            alvo.hurt(fonte, dano);
        }
    }

    @Override
    public void apresentacao(String animacao, String efeito, String som) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(jogador,
                new TocarApresentacao(jogador.getId(), animacao, efeito, som));
    }
}
