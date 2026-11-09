package com.minerwk.client.vfx;

import com.lowdragmc.photon.client.fx.BlockEffectExecutor;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.minerwk.MineRWK;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Atalhos para tocar efeitos do Photon2. Só pode ser chamado no cliente.
 * Os efeitos (.fx) ficam em {@code assets/minerwk/fx/} e são feitos no editor do Photon.
 */
public final class EfeitosVisuais {
    private EfeitosVisuais() {
    }

    /** Toca o efeito preso a um bloco. Devolve false se o efeito não existir. */
    public static boolean noBloco(ResourceLocation efeito, Level nivel, BlockPos pos) {
        FX fx = FXHelper.getFX(efeito);
        if (fx == null) {
            MineRWK.LOGGER.warn("Efeito do Photon não encontrado: {}", efeito);
            return false;
        }
        new BlockEffectExecutor(fx, nivel, pos).start();
        return true;
    }

    /** Toca o efeito seguindo uma entidade (girando junto com o olhar dela). */
    public static boolean naEntidade(ResourceLocation efeito, Level nivel, Entity entidade) {
        FX fx = FXHelper.getFX(efeito);
        if (fx == null) {
            MineRWK.LOGGER.warn("Efeito do Photon não encontrado: {}", efeito);
            return false;
        }
        new EntityEffectExecutor(fx, nivel, entidade, EntityEffectExecutor.AutoRotate.LOOK).start();
        return true;
    }
}
