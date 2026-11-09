package com.minerwk.kit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Apaga volumes grandes (a explosão do Roxo 200%) aos poucos: no máximo {@link #POR_TICK} blocos
 * apagados e {@link #OLHADOS_POR_TICK} posições olhadas por tick no servidor todo. O que sobra fica para
 * o tick seguinte. Avisa só os clientes (sem atualizar vizinhos), que é o que pesa menos.
 */
public final class ApagadorServidor {
    static final int POR_TICK = 4000;
    static final int OLHADOS_POR_TICK = 20000;
    private static final int DETRITOS = 12;

    /** Uma casca para apagar: percorre o cubo em volta e pega o que está entre os dois raios. */
    private static final class Trabalho {
        final ServerLevel nivel;
        final Vec3 centro;
        final double de;
        final double ate;
        final Iterator<BlockPos> posicoes;
        /** Se o bloco pode ser apagado (regras do jogador, proteções); já sabe quem apaga. */
        final BiPredicate<BlockPos, BlockState> pode;

        Trabalho(ServerLevel nivel, Vec3 centro, double de, double ate, BiPredicate<BlockPos, BlockState> pode) {
            this.nivel = nivel;
            this.centro = centro;
            this.de = de;
            this.ate = ate;
            this.pode = pode;
            int r = (int) Math.ceil(ate);
            BlockPos base = BlockPos.containing(centro);
            this.posicoes = BlockPos.betweenClosed(base.offset(-r, -r, -r), base.offset(r, r, r)).iterator();
        }
    }

    private static final Deque<Trabalho> FILA = new ArrayDeque<>();

    private ApagadorServidor() {
    }

    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> FILA.clear());
    }

    static void apagarCasca(ServerLevel nivel, Vec3 centro, double de, double ate, BiPredicate<BlockPos, BlockState> pode) {
        if (ate <= de) return;
        FILA.addLast(new Trabalho(nivel, centro, de, ate, pode));
    }

    static boolean ocioso() {
        return FILA.isEmpty();
    }

    private static void tick() {
        int apagados = 0;
        int olhados = 0;
        int detritos = 0;
        while (!FILA.isEmpty() && apagados < POR_TICK && olhados < OLHADOS_POR_TICK) {
            Trabalho t = FILA.peekFirst();
            while (t.posicoes.hasNext() && apagados < POR_TICK && olhados < OLHADOS_POR_TICK) {
                BlockPos p = t.posicoes.next();
                olhados++;
                double d2 = p.getCenter().distanceToSqr(t.centro);
                // A primeira casca (de = 0) inclui o próprio centro.
                if ((t.de > 0 && d2 <= t.de * t.de) || d2 > t.ate * t.ate) continue;
                if (!t.nivel.isLoaded(p) || t.nivel.isOutsideBuildHeight(p)) continue;
                BlockState estado = t.nivel.getBlockState(p);
                if (estado.isAir() || estado.getDestroySpeed(t.nivel, p) < 0 || estado.hasBlockEntity()
                        || !estado.getFluidState().isEmpty() || !t.pode.test(p, estado)) continue;
                t.nivel.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                apagados++;
                if (detritos < DETRITOS && t.nivel.random.nextInt(64) == 0) {
                    detritos++;
                    t.nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, estado),
                            p.getX() + .5, p.getY() + .5, p.getZ() + .5, 6, .5, .5, .5, .3);
                }
            }
            if (!t.posicoes.hasNext()) FILA.pollFirst();
        }
    }
}
