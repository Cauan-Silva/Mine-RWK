package com.minerwk.arena;

import com.minerwk.MineRWK;
import com.minerwk.boss.LutasToji;
import com.minerwk.boss.TojiEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Prova da arena num mundo de verdade, sem ninguém no teclado. Só liga com {@code -Dminerwk.prova_arena=true}
 * ({@code ./gradlew runClient -Pmundo="..." -Pprova_arena}): 5 s depois de entrar no mundo, o jogador invoca
 * o Toji; 5 s depois de chegar à arena, o Toji é morto; o log conta cada passo e se a volta caiu no lugar
 * exato. Ferramenta de desenvolvimento: não faz nada num jogo normal.
 */
final class ProvaArena {
    private record Passo(ServerPlayer jogador, long tick, int numero, PontoRetorno origem) {
    }

    private ProvaArena() {
    }

    static void ligar() {
        List<Passo> fila = new ArrayList<>();
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer j) fila.add(new Passo(j, j.server.getTickCount() + 100, 0, null));
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            long agora = e.getServer().getTickCount();
            for (Passo p : List.copyOf(fila)) {
                if (agora < p.tick()) continue;
                fila.remove(p);
                ServerPlayer j = e.getServer().getPlayerList().getPlayer(p.jogador().getUUID());
                if (j == null) continue;
                switch (p.numero()) {
                    case 0 -> {
                        PontoRetorno origem = PontoRetorno.de(j);
                        MineRWK.LOGGER.info("Prova da arena: invocar = {}", LutasToji.invocar(j, false));
                        fila.add(new Passo(j, agora + 200, 1, origem));
                    }
                    case 1 -> {
                        TojiEntity toji = LutasToji.de(j.getUUID()).orElse(null);
                        MineRWK.LOGGER.info("Prova da arena: na arena = {}, posição {}, Toji a {} blocos, caixa {}",
                                ArenasServidor.naArena(j), j.position(), toji == null ? "nenhum" : toji.distanceTo(j),
                                ArenasServidor.de(j.getUUID()).orElse(null));
                        if (toji != null) toji.kill();
                        fila.add(new Passo(j, agora + 60, 2, p.origem()));
                    }
                    case 2 -> {
                        MineRWK.LOGGER.info("Prova da arena: 3 s depois da vitória, ainda na arena = {}", ArenasServidor.naArena(j));
                        fila.add(new Passo(j, agora + 80, 3, p.origem()));
                    }
                    default -> MineRWK.LOGGER.info("Prova da arena: 7 s depois da vitória, lugar exato = {}, antes {} depois {}, vagas ocupadas = {}",
                            PontoRetorno.de(j).equals(p.origem()), p.origem(), PontoRetorno.de(j), ArenasServidor.vagasOcupadas());
                }
            }
        });
    }
}
