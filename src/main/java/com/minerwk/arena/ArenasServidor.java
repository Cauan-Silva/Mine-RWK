package com.minerwk.arena;

import com.minerwk.MineRWK;
import com.minerwk.core.arena.CaixaArena;
import com.minerwk.core.arena.PlantaEscola;
import com.minerwk.core.arena.VagasArena;
import com.minerwk.core.kit.Vetor;
import com.minerwk.kit.Anexos;
import com.minerwk.rede.Envio;
import com.minerwk.rede.EscurecerTela;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A dimensão da arena e o ciclo de cada luta nela: reservar a vaga, montar, levar o jogador, trazê-lo de
 * volta quando a luta acaba, apagar e liberar a vaga.
 */
public final class ArenasServidor {
    public static final ResourceKey<Level> DIMENSAO = ResourceKey.create(Registries.DIMENSION, MineRWK.id("arena"));
    /** Depois da vitória o jogador fica este tempo na arena antes de voltar. */
    public static final int TICKS_DEPOIS_DA_VITORIA = 100;
    /** Teto da espera pela montagem: os efeitos que seguram o jogador acabam sozinhos se algo der errado. */
    private static final int TICKS_DE_ESPERA = 80;
    /** A tela leva este tempo para escurecer antes de um teleporte e este para clarear depois. */
    private static final int TICKS_ESCURECENDO = 10;
    private static final int TICKS_CLAREANDO = 15;
    /** Tela preta logo depois de um teleporte, enquanto o cliente carrega o lugar novo. */
    private static final int TICKS_PRETO_AO_CHEGAR = 5;

    private enum Estado { MONTANDO, LUTANDO, VOLTANDO }

    /** A arena de um jogador, do pedido até ele sair dela. Depois disso só a limpeza segura a vaga. */
    private static final class Arena {
        final UUID dono;
        /** Quem pediu; se ele morrer e renascer, o servidor conhece o objeto novo pelo {@link #dono}. */
        final ServerPlayer pediu;
        final int indice;
        final CaixaArena caixa;
        final BiConsumer<ServerPlayer, CaixaArena> aoChegar;
        Estado estado = Estado.MONTANDO;
        long voltarNoTick;
        /** A tela do jogador já começou a escurecer para a volta. */
        boolean escureceu;

        Arena(ServerPlayer pediu, int indice, BiConsumer<ServerPlayer, CaixaArena> aoChegar) {
            this.dono = pediu.getUUID();
            this.pediu = pediu;
            this.indice = indice;
            this.caixa = CaixaArena.daVaga(indice);
            this.aoChegar = aoChegar;
        }
    }

    private static final VagasArena VAGAS = new VagasArena();
    private static final Map<UUID, Arena> ARENAS = new HashMap<>();

    private ArenasServidor() {
    }

    public static void registrarEventos() {
        ObrasArena.registrarEventos();
        RegrasArena.registrarEventos();
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
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer j) aoSair(j);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer j) aoEntrar(j);
        });
        // Os mapas são estáticos: no jogo de um jogador só eles sobreviveriam ao fechar e abrir o mundo.
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            ARENAS.clear();
            VAGAS.limpar();
        });
        if (Boolean.getBoolean("minerwk.prova_arena")) ProvaArena.ligar();
    }

    /** A dimensão da arena, ou null se os dados do mod não a carregaram. */
    public static ServerLevel nivel(MinecraftServer servidor) {
        return servidor.getLevel(DIMENSAO);
    }

    public static boolean naArena(Entity e) {
        return e.level().dimension() == DIMENSAO;
    }

    /**
     * Se um golpe pode apagar ou arrancar o bloco. Fora da dimensão da arena, sempre; dentro, só o que está
     * na caixa da vaga daquela posição: a destruição não vaza para o vazio em volta.
     */
    public static boolean podeMexer(Level nivel, BlockPos pos) {
        if (nivel.dimension() != DIMENSAO) return true;
        return CaixaArena.daPosicao(pos.getX(), pos.getZ()).contemBloco(pos.getX(), pos.getY(), pos.getZ());
    }

    /** A caixa da arena desse jogador, do pedido até ele sair dela. */
    public static Optional<CaixaArena> de(UUID jogador) {
        Arena a = ARENAS.get(jogador);
        return a == null ? Optional.empty() : Optional.of(a.caixa);
    }

    /** Vagas em uso, contando as que ainda estão sendo apagadas. */
    static int vagasOcupadas() {
        return VAGAS.ocupadas();
    }

    // --- O ciclo ---

    /**
     * Reserva uma vaga, guarda de onde o jogador saiu e monta a arena. Quando ela fica pronta, o jogador é
     * levado e {@code aoChegar} recebe quem chegou e a caixa. Durante a montagem ele fica parado e não
     * leva dano. Devolve false se ele já tem arena, já está na dimensão ou a dimensão não existe.
     */
    public static boolean entrar(ServerPlayer jogador, BiConsumer<ServerPlayer, CaixaArena> aoChegar) {
        ServerLevel arena = nivel(jogador.server);
        if (arena == null || naArena(jogador) || ARENAS.containsKey(jogador.getUUID())) return false;
        Arena a = new Arena(jogador, VAGAS.reservar(), aoChegar);
        ARENAS.put(a.dono, a);
        jogador.setData(Anexos.RETORNO_ARENA, PontoRetorno.de(jogador));
        jogador.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TICKS_DE_ESPERA, 9, false, false, false));
        jogador.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, TICKS_DE_ESPERA, 4, false, false, false));
        // Preta até a arena ficar pronta; ao chegar, outro pedido a clareia.
        Envio.para(jogador, new EscurecerTela(TICKS_ESCURECENDO, TICKS_DE_ESPERA, TICKS_CLAREANDO));
        ObrasArena.montar(arena, a.caixa, PlantaEscola.cenario(), () -> chegar(arena, a));
        return true;
    }

    /** A arena ficou pronta. */
    private static void chegar(ServerLevel arena, Arena a) {
        ServerPlayer jogador = atual(arena.getServer(), a);
        // Saiu do jogo, morreu ou foi parar em outra arena enquanto ela era montada: a entrada é desfeita.
        if (ARENAS.get(a.dono) != a || jogador == null || !jogador.isAlive() || naArena(jogador)) {
            if (ARENAS.remove(a.dono, a) && jogador != null) {
                jogador.removeData(Anexos.RETORNO_ARENA);
                clarear(jogador);
            }
            apagar(arena, a);
            return;
        }
        soltar(jogador);
        levar(jogador, a.caixa);
        clarear(jogador);
        a.estado = Estado.LUTANDO;
        a.aoChegar.accept(jogador, a.caixa);
    }

    /**
     * A luta do jogador acabou (o boss saiu do mundo). Com vitória ele volta em
     * {@link #TICKS_DEPOIS_DA_VITORIA}; sem, volta já, se ainda estiver vivo na arena. Depois a arena é
     * apagada e a vaga, liberada.
     */
    public static void lutaAcabou(MinecraftServer servidor, UUID dono, boolean venceu) {
        Arena a = ARENAS.get(dono);
        if (a == null || a.estado != Estado.LUTANDO) return;
        // Nunca na hora: o boss avisa de dentro da própria remoção (até quando o chunk descarrega ou o
        // servidor fecha), e teleportar ou apagar entidades ali corrompe a lista que o jogo está percorrendo.
        a.estado = Estado.VOLTANDO;
        // Sem vitória ele volta assim que a tela escurece.
        a.voltarNoTick = servidor.getTickCount() + (venceu ? TICKS_DEPOIS_DA_VITORIA : TICKS_ESCURECENDO);
    }

    /** Tira o jogador da arena (se ele estiver lá, vivo e no jogo) e a apaga. */
    private static void fechar(MinecraftServer servidor, Arena a) {
        ARENAS.remove(a.dono, a);
        ServerPlayer jogador = atual(servidor, a);
        if (jogador != null) {
            if (jogador.isAlive() && naArena(jogador)) {
                trazer(jogador);
                clarear(jogador);
            }
            // Morreu (renasce no lugar normal) ou saiu da dimensão por comando: o ponto guardado já não vale.
            else jogador.removeData(Anexos.RETORNO_ARENA);
        }
        // Quem saiu do jogo continua com o ponto salvo: ao entrar de novo, é por ele que volta.
        ServerLevel arena = nivel(servidor);
        if (arena != null) apagar(arena, a);
        else VAGAS.liberar(a.indice);
    }

    private static void apagar(ServerLevel arena, Arena a) {
        ObrasArena.apagar(arena, a.caixa, () -> VAGAS.liberar(a.indice));
    }

    private static void tick(MinecraftServer servidor) {
        if (ARENAS.isEmpty()) return;
        for (Arena a : List.copyOf(ARENAS.values())) {
            if (a.estado != Estado.VOLTANDO) continue;
            if (servidor.getTickCount() >= a.voltarNoTick) {
                fechar(servidor, a);
            } else if (!a.escureceu && servidor.getTickCount() >= a.voltarNoTick - TICKS_ESCURECENDO) {
                a.escureceu = true;
                ServerPlayer jogador = atual(servidor, a);
                // Só para quem vai ser teleportado: o morto vê a tela de morte, não a preta.
                if (jogador != null && jogador.isAlive() && naArena(jogador)) {
                    Envio.para(jogador, new EscurecerTela(TICKS_ESCURECENDO, TICKS_PRETO_AO_CHEGAR + 20, TICKS_CLAREANDO));
                }
            }
        }
    }

    /** Quem saiu do jogo durante a montagem nunca foi levado: a entrada é desfeita e a vaga, liberada na limpeza. */
    static void aoSair(ServerPlayer jogador) {
        Arena a = ARENAS.get(jogador.getUUID());
        if (a == null || a.estado != Estado.MONTANDO) return;
        ARENAS.remove(a.dono, a);
        soltar(jogador);
        jogador.removeData(Anexos.RETORNO_ARENA);
    }

    /** Quem entra no jogo dentro da dimensão sem uma luta em andamento volta para de onde saiu. */
    static void aoEntrar(ServerPlayer jogador) {
        if (!naArena(jogador) || ARENAS.containsKey(jogador.getUUID())) return;
        if (!trazer(jogador) && naArena(jogador)) paraORenascimento(jogador);
    }

    /** O jogador como está agora no servidor (depois de morrer ele é outro objeto); null se saiu do jogo. */
    private static ServerPlayer atual(MinecraftServer servidor, Arena a) {
        ServerPlayer j = servidor.getPlayerList().getPlayer(a.dono);
        if (j == null) j = a.pediu;
        return j.hasDisconnected() ? null : j;
    }

    /** A tela, que estava preta para esconder o teleporte, fica mais um instante assim e clareia. */
    private static void clarear(ServerPlayer jogador) {
        Envio.para(jogador, new EscurecerTela(0, TICKS_PRETO_AO_CHEGAR, TICKS_CLAREANDO));
    }

    private static void soltar(ServerPlayer jogador) {
        jogador.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        jogador.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }

    // --- Ir e voltar ---

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

    /** O lugar onde o jogador renasceria (cama, âncora ou o início do mundo), sem gastar a âncora. */
    static void paraORenascimento(ServerPlayer jogador) {
        jogador.changeDimension(jogador.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING));
        jogador.resetFallDistance();
    }
}
