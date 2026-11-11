package com.minerwk.arena;

import com.minerwk.MineRWK;
import com.minerwk.boss.LutasToji;
import com.minerwk.boss.TojiEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Prova da arena num mundo de verdade, sem ninguém no teclado. Só liga com {@code -Dminerwk.prova_arena=true}
 * ({@code ./gradlew runClient -Pmundo="..." -Pprova_arena}): 5 s depois de entrar no mundo, o jogador invoca
 * o Toji; na arena ele fica imune a dano (ninguém está jogando), o Toji é morto 10 s depois e o log conta
 * cada passo e se a volta caiu no lugar exato. Ferramenta de desenvolvimento: não faz nada num jogo normal.
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
                        if (j.isDeadOrDying()) {
                            // Ficou morto de uma prova anterior: renasce e tenta de novo.
                            MineRWK.LOGGER.info("Prova da arena: o jogador estava morto em {}; renascendo", j.level().dimension().location());
                            e.getServer().getPlayerList().respawn(j, false, Entity.RemovalReason.KILLED);
                            fila.add(new Passo(j, agora + 60, 0, null));
                            break;
                        }
                        PontoRetorno origem = PontoRetorno.de(j);
                        MineRWK.LOGGER.info("Prova da arena: invocar = {}", LutasToji.invocar(j, false));
                        fila.add(new Passo(j, agora + 70, 1, origem));
                    }
                    case 1 -> {
                        // Já na arena (a entrada tira a Resistência dela ao chegar): ninguém está jogando.
                        j.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 4, false, false, false));
                        MineRWK.LOGGER.info("Prova da arena: chegou = {}, posição {}, jogadores na dimensão = {}", ArenasServidor.naArena(j),
                                j.position(), j.serverLevel().players().size());
                        // A barreira: posto 20 blocos para fora da parede leste, ele tem de voltar para dentro.
                        ArenasServidor.de(j.getUUID()).ifPresent(caixa ->
                                j.connection.teleport(caixa.centroX() + 60.5, 64, caixa.centroZ() + .5, j.getYRot(), j.getXRot()));
                        fila.add(new Passo(j, agora + 40, 5, p.origem()));
                    }
                    case 5 -> {
                        MineRWK.LOGGER.info("Prova da arena: 2 s depois de posto fora da caixa, posição {} (a parede leste fica em x = {})",
                                j.position(), ArenasServidor.de(j.getUUID()).map(c -> c.centroX() + com.minerwk.core.arena.CaixaArena.MEIA + 1).orElse(-1));
                        fila.add(new Passo(j, agora + 90, 2, p.origem()));
                    }
                    case 2 -> {
                        TojiEntity toji = LutasToji.de(j.getUUID()).orElse(null);
                        MineRWK.LOGGER.info("Prova da arena: na arena = {}, vivo = {}, posição {}, Toji a {} blocos, caixa {}",
                                ArenasServidor.naArena(j), j.isAlive(), j.position(), toji == null ? "nenhum" : toji.distanceTo(j),
                                ArenasServidor.de(j.getUUID()).orElse(null));
                        if (toji != null) toji.kill();
                        fila.add(new Passo(j, agora + 60, 3, p.origem()));
                    }
                    case 3 -> {
                        MineRWK.LOGGER.info("Prova da arena: 3 s depois da vitória, ainda na arena = {}", ArenasServidor.naArena(j));
                        fila.add(new Passo(j, agora + 80, 4, p.origem()));
                    }
                    default -> {
                        j.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                        MineRWK.LOGGER.info("Prova da arena: 7 s depois da vitória, lugar exato = {}, antes {} depois {}, vagas ocupadas = {}",
                                PontoRetorno.de(j).equals(p.origem()), p.origem(), PontoRetorno.de(j), ArenasServidor.vagasOcupadas());
                    }
                }
            }
        });
    }
}
