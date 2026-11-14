package com.minerwk.client.arena;

import com.minerwk.client.ConfigCliente;
import com.minerwk.core.arena.CaixaArena;
import com.minerwk.core.arena.PlantaOriana;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * A fumaça e as fagulhas de Oriana sob ataque: colunas que sobem atrás da muralha, de onde a capital queima, e de
 * cima da torre destruída. Pontos fixos em relação ao centro da arena em que o jogador está. Com o VFX reduzido,
 * a metade.
 */
public final class FumacaOriana {
    /** Os pontos de onde a fumaça sobe: dx, dz em relação ao centro, e a altura acima do pátio. */
    private static final int[][] FONTES = {{-40, -49, 11}, {-28, -49, 11}, {-8, -49, 11}, {10, -49, 11}, {26, -49, 11},
            {38, -49, 11}, {PlantaOriana.TORRE_DESTRUIDA, -47, PlantaOriana.TORRE_TOPO - 5}};
    private static final RandomSource ACASO = RandomSource.create();

    private FumacaOriana() {
    }

    public static void aoTick(ClientTickEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || !NoiteOriana.ativa()) return;
        CaixaArena caixa = CaixaArena.daPosicao(mc.player.getX(), mc.player.getZ());
        boolean reduzido = ConfigCliente.vfxReduzido() || HorizonteCliente.reduzidoNaPrevia;
        for (int[] f : FONTES) {
            if (reduzido && ACASO.nextBoolean()) continue;
            double x = caixa.centroX() + f[0] + .5 + ACASO.nextGaussian() * 1.5;
            double y = CaixaArena.Y_PATIO + f[2] + ACASO.nextDouble();
            double z = caixa.centroZ() + f[1] + .5 + ACASO.nextGaussian() * 1.5;
            mc.level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, y, z, 0, .07, 0);
            if (ACASO.nextInt(3) == 0) {
                mc.level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
            }
        }
    }
}
