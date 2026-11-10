package com.minerwk.kit;

import com.minerwk.registro.Regras;
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

    /** Uma casca para apagar: só as posições entre os dois raios, coluna por coluna. */
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
            this.posicoes = new Casca(centro, de, ate);
        }
    }

    /**
     * Percorre só os blocos (pelo centro) com distância em (de, ate] — ou [0, ate] se de = 0 — sem olhar
     * o cubo inteiro: em cada coluna, os trechos de cima e de baixo do buraco da casca de dentro.
     */
    static final class Casca implements Iterator<BlockPos> {
        private final Vec3 c;
        private final double de2;
        private final double ate2;
        private final int x0, x1, z0, z1;
        private int x, z;
        /** Trechos de y que faltam nesta coluna: {inicio, fim} inclusive. */
        private final ArrayDeque<int[]> trechos = new ArrayDeque<>(2);
        private int y, yFim;
        private BlockPos proximo;

        Casca(Vec3 centro, double de, double ate) {
            this.c = centro;
            this.de2 = de > 0 ? de * de : -1;
            this.ate2 = ate * ate;
            int r = (int) Math.ceil(ate) + 1;
            x0 = (int) Math.floor(c.x) - r;
            x1 = (int) Math.floor(c.x) + r;
            z0 = (int) Math.floor(c.z) - r;
            z1 = (int) Math.floor(c.z) + r;
            x = x0;
            z = z0 - 1;
            y = 1;
            yFim = 0;
            avancar();
        }

        private void coluna() {
            double hx = x + .5 - c.x, hz = z + .5 - c.z;
            double h2 = hx * hx + hz * hz;
            if (h2 > ate2) return;
            double fora = Math.sqrt(ate2 - h2);
            int baixo = (int) Math.ceil(c.y - .5 - fora);
            int cima = (int) Math.floor(c.y - .5 + fora);
            if (de2 - h2 < 0) {
                trechos.add(new int[] {baixo, cima});
                return;
            }
            double dentro = Math.sqrt(de2 - h2);
            // Buraco: |y + .5 - c.y| <= dentro.
            int furoBaixo = (int) Math.ceil(c.y - .5 - dentro);
            int furoCima = (int) Math.floor(c.y - .5 + dentro);
            if (baixo <= furoBaixo - 1) trechos.add(new int[] {baixo, furoBaixo - 1});
            if (furoCima + 1 <= cima) trechos.add(new int[] {furoCima + 1, cima});
        }

        private void avancar() {
            proximo = null;
            while (true) {
                if (y <= yFim) {
                    proximo = new BlockPos(x, y++, z);
                    return;
                }
                int[] t = trechos.poll();
                if (t != null) {
                    y = t[0];
                    yFim = t[1];
                    continue;
                }
                if (++z > z1) {
                    z = z0;
                    if (++x > x1) return;
                }
                coluna();
            }
        }

        @Override
        public boolean hasNext() {
            return proximo != null;
        }

        @Override
        public BlockPos next() {
            BlockPos p = proximo;
            avancar();
            return p;
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
            // A regra pode ter sido desligada no meio da explosão.
            if (!t.nivel.getGameRules().getBoolean(Regras.APAGAR_BLOCOS)) {
                FILA.pollFirst();
                continue;
            }
            while (t.posicoes.hasNext() && apagados < POR_TICK && olhados < OLHADOS_POR_TICK) {
                BlockPos p = t.posicoes.next();
                olhados++;
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
