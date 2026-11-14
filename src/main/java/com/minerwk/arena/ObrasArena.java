package com.minerwk.arena;

import com.minerwk.core.arena.CaixaArena;
import com.minerwk.core.arena.PlantaEscola;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Monta e apaga a arena de uma vaga aos poucos, para o servidor não travar: no máximo {@link #POR_TICK}
 * blocos mexidos e {@link #OLHADOS_POR_TICK} posições olhadas por tick, somando todas as obras. Os chunks
 * da vaga ficam forçados do começo da montagem até o fim da limpeza.
 */
public final class ObrasArena {
    /** Blocos postos ou apagados por tick. */
    static final int POR_TICK = 4000;
    /** Posições olhadas por tick na limpeza (a caixa tem cerca de 775 mil: uns 20 ticks). */
    static final int OLHADOS_POR_TICK = 40000;
    /** Sem avisar vizinhos nem recalcular formas: a planta já diz como cada bloco fica. */
    private static final int MARCAS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final class Obra {
        final ServerLevel nivel;
        final CaixaArena caixa;
        final Iterator<BlockPos> limpeza;
        final Iterator<PlantaEscola.Bloco> blocos;
        final boolean soltarChunks;
        final Runnable aoTerminar;

        Obra(ServerLevel nivel, CaixaArena caixa, List<PlantaEscola.Bloco> blocos, boolean soltarChunks, Runnable aoTerminar) {
            this.nivel = nivel;
            this.caixa = caixa;
            this.limpeza = BlockPos.betweenClosed(
                    caixa.centroX() - CaixaArena.MEIA, CaixaArena.Y_FUNDO, caixa.centroZ() - CaixaArena.MEIA,
                    caixa.centroX() + CaixaArena.MEIA, CaixaArena.Y_TETO - 1, caixa.centroZ() + CaixaArena.MEIA).iterator();
            this.blocos = blocos.iterator();
            this.soltarChunks = soltarChunks;
            this.aoTerminar = aoTerminar;
        }
    }

    private static final Deque<Obra> FILA = new ArrayDeque<>();

    private ObrasArena() {
    }

    static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> FILA.clear());
    }

    /** Limpa a caixa, põe os blocos da planta e chama {@code aoTerminar}. */
    static void montar(ServerLevel nivel, CaixaArena caixa, List<PlantaEscola.Bloco> blocos, Runnable aoTerminar) {
        forcarChunks(nivel, caixa, true);
        FILA.addLast(new Obra(nivel, caixa, blocos, false, aoTerminar));
    }

    /** Tira da caixa tudo o que não é jogador, apaga todos os blocos, solta os chunks e chama {@code aoTerminar}. */
    static void apagar(ServerLevel nivel, CaixaArena caixa, Runnable aoTerminar) {
        forcarChunks(nivel, caixa, true);
        tirarEntidades(nivel, caixa);
        FILA.addLast(new Obra(nivel, caixa, List.of(), true, aoTerminar));
    }

    /**
     * Solta todo chunk da dimensão que ficou forçado. Ao ligar o servidor nenhuma vaga está em uso, mas o
     * jogo salva os chunks forçados no disco: se ele parou com uma luta em andamento, eles voltariam
     * carregados para sempre.
     */
    static void soltarChunksEsquecidos(ServerLevel arena) {
        for (long chunk : arena.getForcedChunks().toLongArray()) {
            arena.setChunkForced(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false);
        }
    }

    static boolean ocioso() {
        return FILA.isEmpty();
    }

    /** A coluna inteira da caixa, com folga para o que estiver encostado nas faces ou caindo no vazio. */
    static AABB volume(CaixaArena caixa) {
        return new AABB(caixa.centroX() - CaixaArena.MEIA - 2, CaixaArena.Y_FUNDO - 64, caixa.centroZ() - CaixaArena.MEIA - 2,
                caixa.centroX() + CaixaArena.MEIA + 3, CaixaArena.Y_TETO + 64, caixa.centroZ() + CaixaArena.MEIA + 3);
    }

    private static void tirarEntidades(ServerLevel nivel, CaixaArena caixa) {
        for (Entity e : nivel.getEntities((Entity) null, volume(caixa), e -> !(e instanceof Player))) e.discard();
    }

    private static void forcarChunks(ServerLevel nivel, CaixaArena caixa, boolean forcar) {
        // Um anel a mais em volta da caixa: quem é empurrado para fora da parede continua num chunk ativo
        // até a barreira o puxar de volta (num chunk parado o boss sairia do mundo e a luta acabaria).
        int x0 = ((caixa.centroX() - CaixaArena.MEIA) >> 4) - 1, x1 = ((caixa.centroX() + CaixaArena.MEIA) >> 4) + 1;
        int z0 = ((caixa.centroZ() - CaixaArena.MEIA) >> 4) - 1, z1 = ((caixa.centroZ() + CaixaArena.MEIA) >> 4) + 1;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) nivel.setChunkForced(x, z, forcar);
        }
    }

    private static void tick() {
        int mexidos = 0;
        int olhados = 0;
        while (!FILA.isEmpty() && mexidos < POR_TICK && olhados < OLHADOS_POR_TICK) {
            Obra o = FILA.peekFirst();
            while (o.limpeza.hasNext() && mexidos < POR_TICK && olhados < OLHADOS_POR_TICK) {
                BlockPos p = o.limpeza.next();
                olhados++;
                if (o.nivel.getBlockState(p).isAir()) continue;
                // Baús e afins soltariam o conteúdo: o bloco-entidade sai antes do bloco.
                o.nivel.removeBlockEntity(p);
                o.nivel.setBlock(p, Blocks.AIR.defaultBlockState(), MARCAS);
                mexidos++;
            }
            if (o.limpeza.hasNext()) break;
            int cx = o.caixa.centroX(), cz = o.caixa.centroZ();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            while (o.blocos.hasNext() && mexidos < POR_TICK) {
                PlantaEscola.Bloco b = o.blocos.next();
                p.set(cx + b.dx(), CaixaArena.Y_PATIO + b.dy(), cz + b.dz());
                if (b.material() == PlantaEscola.Material.MURO || b.material() == PlantaEscola.Material.CERCA_ESCURA
                        || b.material() == PlantaEscola.Material.SHOJI) {
                    // Muro, cerca e painel se ligam aos vizinhos: eles e os vizinhos recalculam a forma.
                    o.nivel.setBlock(p, Block.updateFromNeighbourShapes(estado(b.material()), o.nivel, p), Block.UPDATE_CLIENTS);
                } else {
                    o.nivel.setBlock(p, estado(b.material()), MARCAS);
                }
                mexidos++;
            }
            if (o.blocos.hasNext()) break;
            FILA.pollFirst();
            if (o.soltarChunks) {
                // O que caiu durante a limpeza (itens de blocos, flechas) também sai.
                tirarEntidades(o.nivel, o.caixa);
                forcarChunks(o.nivel, o.caixa, false);
            }
            o.aoTerminar.run();
        }
    }

    /** O bloco do Minecraft de cada material da planta. */
    static BlockState estado(PlantaEscola.Material material) {
        return switch (material) {
            case PEDRA -> Blocks.STONE.defaultBlockState();
            case TIJOLO_PEDRA -> Blocks.STONE_BRICKS.defaultBlockState();
            case LAJE_PEDRA -> Blocks.STONE_BRICK_SLAB.defaultBlockState();
            case ANDESITO_POLIDO -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            case ESCADA_PEDRA -> Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            case MADEIRA_ESCURA -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            case MADEIRA_VERMELHA -> Blocks.MANGROVE_PLANKS.defaultBlockState();
            case TRONCO -> Blocks.SPRUCE_LOG.defaultBlockState();
            case FOLHA -> Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            case LANTERNA -> Blocks.LANTERN.defaultBlockState();
            case MURO -> Blocks.STONE_BRICK_WALL.defaultBlockState();
            case GRAMA -> Blocks.GRASS_BLOCK.defaultBlockState();
            case PAREDE_BRANCA -> Blocks.SMOOTH_QUARTZ.defaultBlockState();
            case PILAR -> Blocks.DARK_OAK_LOG.defaultBlockState();
            case ASSOALHO -> Blocks.SPRUCE_PLANKS.defaultBlockState();
            case TELHA -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            case LAJE_TELHA -> Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState();
            case CONCRETO_CLARO -> Blocks.WHITE_CONCRETE.defaultBlockState();
            case VIDRO_AZUL -> Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
            case FUNDO_ESCURO -> Blocks.BLUE_CONCRETE.defaultBlockState();
            case PORTAL_NEGRO -> Blocks.BLACK_CONCRETE.defaultBlockState();
            case PORTAL_ROXO -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            case TATAME, FENO -> Blocks.HAY_BLOCK.defaultBlockState();
            case BAMBU_PISO -> Blocks.BAMBOO_MOSAIC.defaultBlockState();
            case SHOJI -> Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState();
            case GRADE_NS -> grade(Direction.NORTH);
            case GRADE_LO -> grade(Direction.EAST);
            case VELA -> Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true);
            case BARRIL -> Blocks.BARREL.defaultBlockState();
            case ESTANTE -> Blocks.BOOKSHELF.defaultBlockState();
            case SUPORTE_ARMADURA -> Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP);
            case LAJE_MADEIRA -> Blocks.DARK_OAK_SLAB.defaultBlockState();
            case TAPETE -> Blocks.RED_CARPET.defaultBlockState();
            case ALVO -> Blocks.TARGET.defaultBlockState();
            case QUADRO -> Blocks.BLACK_CONCRETE.defaultBlockState();
            case CERCA_ESCURA -> Blocks.DARK_OAK_FENCE.defaultBlockState();
            case AGUA -> Blocks.WATER.defaultBlockState();
            case VITORIA_REGIA -> Blocks.LILY_PAD.defaultBlockState();
            case CASCALHO -> Blocks.GRAVEL.defaultBlockState();
            case PEDRA_MUSGO -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            case FOLHA_CEREJEIRA -> Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            case TRONCO_CEREJEIRA -> Blocks.CHERRY_LOG.defaultBlockState();
            case BAMBU -> Blocks.BAMBOO.defaultBlockState();
            case SINO -> Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING);
            case ESCADA_MADEIRA_N -> escadaMadeira(Direction.NORTH);
            case ESCADA_MADEIRA_S -> escadaMadeira(Direction.SOUTH);
            case ESCADA_MADEIRA_L -> escadaMadeira(Direction.EAST);
            case ESCADA_MADEIRA_O -> escadaMadeira(Direction.WEST);
            case NETHERRACK -> Blocks.NETHERRACK.defaultBlockState();
            case FOGO -> Blocks.FIRE.defaultBlockState();
            case FOGUEIRA -> Blocks.CAMPFIRE.defaultBlockState();
            case TERRA_QUEIMADA -> Blocks.COARSE_DIRT.defaultBlockState();
            case PEDRA_NEGRA -> Blocks.BLACKSTONE.defaultBlockState();
            case BRASA -> Blocks.MAGMA_BLOCK.defaultBlockState();
            case TIJOLO_RACHADO -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            case TIJOLO_MUSGO -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            case LA_VERMELHA -> Blocks.RED_WOOL.defaultBlockState();
            case TRONCO_QUEIMADO -> Blocks.DARK_OAK_WOOD.defaultBlockState();
            case BARREIRA -> Blocks.BARRIER.defaultBlockState();
        };
    }

    /** Um alçapão de abeto aberto, em pé, como a grade de uma janela. */
    private static BlockState grade(Direction lado) {
        return Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.FACING, lado);
    }

    private static BlockState escadaMadeira(Direction lado) {
        return Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, lado);
    }
}
