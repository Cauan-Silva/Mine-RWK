package com.minerwk.boss;

import com.minerwk.arena.ArenasServidor;
import com.minerwk.core.arena.CaixaArena;
import com.minerwk.core.arena.PlantaOriana;
import com.minerwk.core.boss.TojiBoss;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.cid.ArvoreCid;
import com.minerwk.core.personagens.cid.Cid;
import com.minerwk.kit.DadosKit;
import com.minerwk.kit.ServicoKits;
import com.minerwk.registro.Entidades;
import com.minerwk.registro.Itens;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** As lutas contra o Ragnarok em andamento: quem tem um vivo e a entrada nos arredores de Oriana. */
public final class LutasRagnarok {
    /** O Ragnarok de cada jogador que abriu a Rosa Negra. */
    private static final Map<UUID, RagnarokEntity> VIVOS = new HashMap<>();
    /** Quem gastou um Anel da Sucessão e ainda não viu o Ragnarok entrar: se ele não entrar, o anel é devolvido. */
    private static final Set<UUID> PAGARAM = new HashSet<>();
    /** Quem saiu do jogo com a entrada paga e desfeita: recebe o anel ao entrar de novo (até o servidor fechar). */
    private static final Set<UUID> DEVEMOS = new HashSet<>();

    private LutasRagnarok() {
    }

    public static void registrarEventos() {
        // Os mapas são estáticos: no jogo de um jogador só eles sobreviveriam ao fechar e abrir o mundo.
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            VIVOS.clear();
            PAGARAM.clear();
            DEVEMOS.clear();
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer jogador && DEVEMOS.remove(jogador.getUUID())) devolver(jogador);
        });
    }

    /** Devolve um anel: no inventário de quem está vivo, ou no chão aos pés de quem morreu. */
    private static void devolver(ServerPlayer jogador) {
        ItemStack anel = new ItemStack(Itens.ANEL_DA_SUCESSAO.get());
        if (jogador.isAlive()) jogador.getInventory().placeItemBackInInventory(anel);
        else jogador.spawnAtLocation(anel);
    }

    /** A entrada foi desfeita antes de a arena ficar pronta: quem gastou o anel o recebe de volta. */
    private static void desistiu(MinecraftServer servidor, UUID id) {
        if (!PAGARAM.remove(id)) return;
        ServerPlayer jogador = servidor.getPlayerList().getPlayer(id);
        if (jogador != null) devolver(jogador);
        else DEVEMOS.add(id);
    }

    /** O Anel da Sucessão de {@code jogador} foi gasto nesta entrada. */
    static void pagou(ServerPlayer jogador) {
        PAGARAM.add(jogador.getUUID());
    }

    static void registrar(RagnarokEntity ragnarok) {
        VIVOS.put(ragnarok.invocadorId(), ragnarok);
    }

    static void esquecer(RagnarokEntity ragnarok) {
        if (ragnarok.invocadorId() != null) VIVOS.remove(ragnarok.invocadorId(), ragnarok);
    }

    /** O Ragnarok vivo da luta desse jogador. */
    public static Optional<RagnarokEntity> de(UUID jogador) {
        RagnarokEntity r = VIVOS.get(jogador);
        if (r != null && (r.isRemoved() || !r.isAlive())) {
            VIVOS.remove(jogador);
            r = null;
        }
        return Optional.ofNullable(r);
    }

    /**
     * Começa a luta: o jogador é levado para uma arena própria, com os arredores de Oriana, e o Ragnarok
     * entra lá, à frente dele, assim que ela fica pronta. Com {@code exigir}, vale a regra do Anel da
     * Sucessão (ser o Cid e ter o nó Despertar, que pede maestria 20); sem, só não pode haver outra luta
     * dele (comando de teste). As respostas são as mesmas do Contrato do Toji.
     */
    public static TojiBoss.Invocacao invocar(ServerPlayer jogador, boolean exigir) {
        if (ArenasServidor.naArena(jogador) || ArenasServidor.de(jogador.getUUID()).isPresent()) {
            return TojiBoss.Invocacao.NA_ARENA;
        }
        DadosKit dados = ServicoKits.dados(jogador);
        boolean temNo = dados.temPersonagem() && dados.maestriaAtual().nos.getOrDefault(ArvoreCid.DESPERTAR, 0) > 0;
        boolean temBoss = de(jogador.getUUID()).isPresent();
        TojiBoss.Invocacao resposta = exigir ? TojiBoss.podeInvocar(dados.personagem, Cid.ID, temNo, temBoss)
                : temBoss ? TojiBoss.Invocacao.JA_EXISTE : TojiBoss.Invocacao.OK;
        if (resposta != TojiBoss.Invocacao.OK) return resposta;
        MinecraftServer servidor = jogador.server;
        UUID id = jogador.getUUID();
        return ArenasServidor.entrar(jogador, PlantaOriana.cenario(), LutasRagnarok::entrarNaArena, () -> desistiu(servidor, id))
                ? TojiBoss.Invocacao.OK : TojiBoss.Invocacao.NA_ARENA;
    }

    /** A arena ficou pronta e {@code jogador} já está nela: o Ragnarok entra de frente para ele. */
    private static void entrarNaArena(ServerPlayer jogador, CaixaArena caixa) {
        ServerLevel nivel = jogador.serverLevel();
        RagnarokEntity ragnarok = Entidades.RAGNAROK.get().create(nivel);
        if (ragnarok != null) {
            Vetor centro = caixa.centro();
            // O jogador entra ao sul dele: yaw 0 olha para o sul.
            ragnarok.moveTo(centro.x(), centro.y(), centro.z() + PlantaOriana.ENTRADA_DO_BOSS, 0, 0);
            ragnarok.yBodyRot = 0;
            ragnarok.yHeadRot = 0;
            ragnarok.comecarLuta(jogador);
            if (nivel.addFreshEntity(ragnarok)) {
                PAGARAM.remove(jogador.getUUID());
                return;
            }
            esquecer(ragnarok);
        }
        if (PAGARAM.remove(jogador.getUUID())) devolver(jogador);
        // Sem ele não há luta: o jogador volta para de onde saiu.
        ArenasServidor.lutaAcabou(nivel.getServer(), jogador.getUUID(), false);
    }
}
