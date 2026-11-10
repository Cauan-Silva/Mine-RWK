package com.minerwk.registro;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Chão invisível do Vazio Infinito: sólido para jogadores e mobs, inquebrável, sem drop. Quem o põe
 * agenda um tick, e nesse tick ele se apaga sozinho; o tick agendado é salvo com o chunk, então nada
 * sobra no mundo mesmo se o servidor cair no meio do domínio.
 */
public final class PisoDominio extends Block {
    public PisoDominio(Properties propriedades) {
        super(propriedades);
    }

    @Override
    protected RenderShape getRenderShape(BlockState estado) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState estado, BlockGetter nivel, BlockPos pos) {
        return true;
    }

    @Override
    protected void tick(BlockState estado, ServerLevel nivel, BlockPos pos, RandomSource aleatorio) {
        nivel.removeBlock(pos, false);
    }
}
