package com.minerwk.client.kit;

import com.minerwk.client.vfx.VisuaisAtivos;
import com.minerwk.core.kit.Barreira;
import com.minerwk.core.kit.Vetor;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Regras de movimento do próprio jogador que vêm dos golpes: a barreira e o chão do domínio e o voo do
 * Roxo 200%. O servidor aplica a mesma regra (em {@code BarreirasServidor}); aqui ela roda no fim do
 * tick do jogador, antes de a posição ir para o servidor, então não há tranco.
 */
public final class MovimentoCliente {
    private static final String DOMINIO = "gojo_vazio_infinito";
    private static final String CARGA_ROXO_200 = "gojo_roxo_200_carga";
    /** Altura dos olhos de quem está em pé (o visual guarda os olhos; a regra usa os pés). */
    private static final double ALTURA_OLHOS = 1.62;
    /** Ticks de queda lenta, no máximo, depois do encantamento do Roxo 200%. */
    private static final int QUEDA_LENTA = 200;
    private static final double QUEDA_MAX = -0.12;

    /** Lado do jogador em cada domínio, decidido quando o domínio aparece. */
    private static final Map<VisuaisAtivos.Visual, Boolean> LADO = new WeakHashMap<>();
    private static VisuaisAtivos.Visual voo;
    private static long quedaLentaAte = -1;
    private static net.minecraft.world.level.Level mundo;

    private MovimentoCliente() {
    }

    public static void aoTickJogador(PlayerTickEvent.Post evento) {
        if (!(evento.getEntity() instanceof LocalPlayer jogador) || jogador != Minecraft.getInstance().player) return;
        if (jogador.level() != mundo) {
            // Mundo novo: nada do voo de antes vale aqui.
            mundo = jogador.level();
            voo = null;
            quedaLentaAte = -1;
        }
        if (jogador.isSpectator() || jogador.getAbilities().flying || jogador.isPassenger() || jogador.isDeadOrDying()) return;
        long agora = jogador.level().getGameTime();
        VisuaisAtivos.Visual carga = null;
        for (VisuaisAtivos.Visual v : VisuaisAtivos.ativos()) {
            if (v.encerrado() || agora - v.inicio() >= v.duracao()) continue;
            if (v.tipo().equals(DOMINIO)) dominio(jogador, v);
            else if (v.tipo().equals(CARGA_ROXO_200) && v.dono() == jogador.getId()) carga = v;
        }
        if (carga != null) {
            voo = carga;
            subir(jogador, carga, agora);
        } else if (voo != null) {
            // O encantamento acabou (disparou ou foi cortado): desce devagar.
            voo = null;
            quedaLentaAte = agora + QUEDA_LENTA;
        }
        if (quedaLentaAte >= 0) {
            if (jogador.onGround() || agora > quedaLentaAte) {
                quedaLentaAte = -1;
            } else {
                Vec3 v = jogador.getDeltaMovement();
                if (v.y < QUEDA_MAX) jogador.setDeltaMovement(v.x, QUEDA_MAX, v.z);
                jogador.resetFallDistance();
            }
        }
    }

    private static void dominio(LocalPlayer jogador, VisuaisAtivos.Visual v) {
        Vetor centro = v.origem();
        Vetor pes = new Vetor(jogador.getX(), jogador.getY(), jogador.getZ());
        boolean dentro = LADO.computeIfAbsent(v, k -> v.dono() == jogador.getId() || Barreira.dentro(centro, 12, pes));
        Vec3 vel = jogador.getDeltaMovement();
        Barreira.Correcao c = Barreira.corrigir(centro, 12, centro.y() - ALTURA_OLHOS, dentro, pes,
                new Vetor(vel.x, vel.y, vel.z));
        if (c.posicao().distancia(pes) < 1e-6 && !c.noChao()) return;
        jogador.setPos(c.posicao().x(), c.posicao().y(), c.posicao().z());
        jogador.setDeltaMovement(c.velocidade().x(), c.velocidade().y(), c.velocidade().z());
        if (c.noChao()) {
            jogador.setOnGround(true);
            jogador.resetFallDistance();
        }
    }

    /** Leva o jogador até a altura da subida neste tick, parando se bater no teto. */
    private static void subir(LocalPlayer jogador, VisuaisAtivos.Visual carga, long agora) {
        double alvo = carga.origem().y() - ALTURA_OLHOS + Barreira.subidaRoxo200(agora - carga.inicio());
        double dy = alvo - jogador.getY();
        Vec3 v = jogador.getDeltaMovement();
        jogador.setDeltaMovement(v.x * .5, 0, v.z * .5);
        if (dy > 0) jogador.move(MoverType.SELF, new Vec3(0, dy, 0));
        jogador.resetFallDistance();
    }
}
