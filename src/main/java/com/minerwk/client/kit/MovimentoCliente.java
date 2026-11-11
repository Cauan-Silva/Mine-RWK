package com.minerwk.client.kit;

import com.minerwk.client.vfx.VisuaisAtivos;
import com.minerwk.arena.ArenasServidor;
import com.minerwk.core.arena.CaixaArena;
import com.minerwk.core.kit.Barreira;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.gojo.RoxoMaximo;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Regras de movimento do próprio jogador que vêm dos golpes: a barreira e o chão do domínio e a
 * sequência do R (avanço até o alvo e subida com ele). O servidor aplica a mesma regra da barreira (em
 * {@code BarreirasServidor}); aqui ela roda no fim do tick do jogador, antes de a posição ir para o
 * servidor, então não há tranco.
 */
public final class MovimentoCliente {
    private static final String DOMINIO = "gojo_vazio_infinito";
    private static final String ROXO_MAXIMO = "gojo_roxo_maximo";
    /** Altura dos olhos de quem está em pé (o visual guarda os olhos; a regra usa os pés). */
    private static final double ALTURA_OLHOS = 1.62;
    /** Ticks de queda lenta, no máximo, depois da sequência do R (o voo do servidor cobre o mesmo). */
    private static final int QUEDA_LENTA = 600;
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
        if (jogador.isSpectator() || jogador.isDeadOrDying()) return;
        // Antes do resto: a barreira da arena vale também para quem voa no criativo ou está montado.
        if (jogador.level().dimension() == ArenasServidor.DIMENSAO) arena(jogador);
        if (jogador.getAbilities().flying || jogador.isPassenger()) return;
        long agora = jogador.level().getGameTime();
        VisuaisAtivos.Visual sequencia = null;
        for (VisuaisAtivos.Visual v : VisuaisAtivos.ativos()) {
            if (v.encerrado() || agora - v.inicio() >= v.duracao()) continue;
            if (v.tipo().equals(DOMINIO)) dominio(jogador, v);
            else if (v.tipo().equals(ROXO_MAXIMO) && v.dono() == jogador.getId()) sequencia = v;
        }
        if (sequencia != null) {
            voo = sequencia;
            seguir(jogador, sequencia, agora);
        } else if (voo != null) {
            // A sequência acabou: desce devagar.
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

    /**
     * A barreira da arena: paredes e teto da caixa da vaga onde o jogador está. A caixa sai só da posição,
     * então não depende de nada que o servidor mande; ele confere a mesma regra do lado dele.
     */
    private static void arena(LocalPlayer jogador) {
        Vetor pes = new Vetor(jogador.getX(), jogador.getY(), jogador.getZ());
        Vec3 vel = jogador.getDeltaMovement();
        Barreira.Correcao c = CaixaArena.daPosicao(pes.x(), pes.z())
                .corrigir(pes, new Vetor(vel.x, vel.y, vel.z), jogador.getBbWidth() / 2, jogador.getBbHeight());
        if (c.posicao().distancia(pes) < 1e-6) return;
        jogador.setPos(c.posicao().x(), c.posicao().y(), c.posicao().z());
        jogador.setDeltaMovement(c.velocidade().x(), c.velocidade().y(), c.velocidade().z());
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

    /**
     * Leva o jogador até onde a sequência do R diz neste tick (avanço até o alvo, depois a subida junto
     * com ele). O movimento colide com os blocos: bateu, para ali.
     */
    private static void seguir(LocalPlayer jogador, VisuaisAtivos.Visual v, long agora) {
        // +1: a posição deste tick é a do fim dele (o servidor fere e segura o alvo no mesmo passo).
        Vetor desl = RoxoMaximo.deslocamentoGojo(agora - v.inicio() + 1, v.direcao(), v.forca());
        Vetor pes = v.origem().mais(desl).menos(new Vetor(0, ALTURA_OLHOS, 0));
        jogador.setDeltaMovement(Vec3.ZERO);
        jogador.move(MoverType.SELF, new Vec3(pes.x() - jogador.getX(), pes.y() - jogador.getY(), pes.z() - jogador.getZ()));
        jogador.setDeltaMovement(Vec3.ZERO);
        jogador.resetFallDistance();
    }
}
