package com.minerwk.registro;

import com.minerwk.MineRWK;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocos do mod (sem item: só aparecem colocados pelos golpes). */
public final class Blocos {
    public static final DeferredRegister.Blocks REGISTRO = DeferredRegister.createBlocks(MineRWK.MOD_ID);

    /** Chão sólido e invisível do Vazio Infinito. */
    public static final DeferredBlock<PisoDominio> PISO_DOMINIO = REGISTRO.register("piso_dominio",
            () -> new PisoDominio(BlockBehaviour.Properties.of()
                    .strength(-1f, 3600000f)
                    .noLootTable()
                    .noOcclusion()
                    .isValidSpawn((s, n, p, e) -> false)
                    .isViewBlocking((s, n, p) -> false)
                    .pushReaction(PushReaction.BLOCK)));

    private Blocos() {
    }

    /** Para os testes e outras classes não precisarem do holder. */
    public static Block pisoDominio() {
        return PISO_DOMINIO.get();
    }
}
