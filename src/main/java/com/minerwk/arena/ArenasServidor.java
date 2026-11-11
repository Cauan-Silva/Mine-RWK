package com.minerwk.arena;

import com.minerwk.MineRWK;
import com.minerwk.core.arena.CaixaArena;
import com.minerwk.core.kit.Vetor;
import com.minerwk.kit.Anexos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** A dimensão da arena: quem vai, de onde saiu e como volta. */
public final class ArenasServidor {
    public static final ResourceKey<Level> DIMENSAO = ResourceKey.create(Registries.DIMENSION, MineRWK.id("arena"));

    private ArenasServidor() {
    }

    public static void registrarEventos() {
        // Sem a dimensão nada da arena funciona: melhor saber ao abrir o mundo do que no meio da luta.
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            ServerLevel arena = nivel(e.getServer());
            if (arena == null) {
                MineRWK.LOGGER.error("Arena: a dimensão {} não carregou; a luta do Toji não vai ter arena", DIMENSAO.location());
            } else {
                MineRWK.LOGGER.info("Arena: dimensão {} carregada (hora fixa {})", DIMENSAO.location(),
                        arena.dimensionType().fixedTime().orElse(-1));
            }
        });
        if (Boolean.getBoolean("minerwk.prova_arena")) provaAutomatica();
    }

    /**
     * Prova sem ninguém no teclado (parte 0; sai junto com o comando de prova): com
     * {@code -Dminerwk.prova_arena=true}, quem entra no mundo é levado à arena depois de 5 s e trazido de
     * volta 5 s depois, e o log diz se voltou ao lugar exato.
     */
    private static void provaAutomatica() {
        record Passo(ServerPlayer jogador, long tick, PontoRetorno origem) {
        }
        java.util.List<Passo> fila = new java.util.ArrayList<>();
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer j) fila.add(new Passo(j, j.server.getTickCount() + 100, null));
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
            for (Passo p : java.util.List.copyOf(fila)) {
                if (e.getServer().getTickCount() < p.tick()) continue;
                fila.remove(p);
                ServerPlayer j = p.jogador();
                if (p.origem() == null) {
                    PontoRetorno origem = PontoRetorno.de(j);
                    boolean foi = provaIr(j);
                    MineRWK.LOGGER.info("Prova da arena: ir = {}, na arena = {}, posição {} em {}", foi, naArena(j),
                            j.position(), j.level().dimension().location());
                    fila.add(new Passo(j, e.getServer().getTickCount() + 100, origem));
                } else {
                    boolean voltou = provaVoltar(j);
                    PontoRetorno agora = PontoRetorno.de(j);
                    MineRWK.LOGGER.info("Prova da arena: voltar = {}, lugar exato = {}, antes {} depois {}", voltou,
                            agora.equals(p.origem()), p.origem(), agora);
                }
            }
        });
    }

    /** A dimensão da arena, ou null se os dados do mod não a carregaram. */
    public static ServerLevel nivel(MinecraftServer servidor) {
        return servidor.getLevel(DIMENSAO);
    }

    public static boolean naArena(Entity e) {
        return e.level().dimension() == DIMENSAO;
    }

    /**
     * Guarda de onde o jogador saiu (se ainda não há ponto guardado) e o põe na entrada da vaga, olhando
     * para o norte.
     */
    static void levar(ServerPlayer jogador, CaixaArena caixa) {
        ServerLevel arena = nivel(jogador.server);
        if (arena == null) return;
        if (!jogador.hasData(Anexos.RETORNO_ARENA)) jogador.setData(Anexos.RETORNO_ARENA, PontoRetorno.de(jogador));
        jogador.stopRiding();
        Vetor entrada = caixa.entradaJogador();
        jogador.teleportTo(arena, entrada.x(), entrada.y(), entrada.z(), 180, 0);
        jogador.resetFallDistance();
    }

    /**
     * Devolve o jogador ao ponto de retorno e o apaga. Sem ponto, não faz nada. Com um ponto inválido (a
     * dimensão não existe mais), ele vai para o renascimento normal. Só devolve true se voltou ao ponto.
     */
    static boolean trazer(ServerPlayer jogador) {
        if (!jogador.hasData(Anexos.RETORNO_ARENA)) return false;
        PontoRetorno ponto = jogador.getData(Anexos.RETORNO_ARENA);
        jogador.removeData(Anexos.RETORNO_ARENA);
        ServerLevel destino = jogador.server.getLevel(ResourceKey.create(Registries.DIMENSION, ponto.dimensao()));
        jogador.stopRiding();
        if (destino == null || destino.dimension() == DIMENSAO) {
            paraORenascimento(jogador);
            return false;
        }
        jogador.teleportTo(destino, ponto.x(), ponto.y(), ponto.z(), ponto.yaw(), ponto.pitch());
        jogador.resetFallDistance();
        return true;
    }

    /** Prova (parte 0): plataforma lisa de pedra na vaga 0 e o jogador levado para lá. False se ele já está na arena. */
    public static boolean provaIr(ServerPlayer jogador) {
        ServerLevel arena = nivel(jogador.server);
        if (arena == null || naArena(jogador)) return false;
        CaixaArena caixa = CaixaArena.daVaga(0);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -30; dx <= 30; dx++) {
            for (int dz = -30; dz <= 30; dz++) {
                p.set(caixa.centroX() + dx, CaixaArena.Y_PATIO - 1, caixa.centroZ() + dz);
                arena.setBlock(p, Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        levar(jogador, caixa);
        return true;
    }

    /** Prova (parte 0): de volta ao ponto de retorno. */
    public static boolean provaVoltar(ServerPlayer jogador) {
        return trazer(jogador);
    }

    /** O lugar onde o jogador renasceria (cama, âncora ou o início do mundo), sem gastar a âncora. */
    static void paraORenascimento(ServerPlayer jogador) {
        jogador.changeDimension(jogador.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING));
        jogador.resetFallDistance();
    }
}
