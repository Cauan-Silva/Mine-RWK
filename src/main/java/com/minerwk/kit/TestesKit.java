package com.minerwk.kit;

import com.minerwk.MineRWK;
import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Personagens;
import com.minerwk.core.kit.Tecla;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests do núcleo dos kits (rodam com {@code ./gradlew runGameTestServer}). */
@GameTestHolder(MineRWK.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TestesKit {
    private static final EstadoDoJogador PARADO = new EstadoDoJogador(true, false, false);

    private TestesKit() {
    }

    @GameTest(template = "vazio")
    public static void usarHabilidadeGastaRecursoEIniciaRecarga(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Personagens.TESTE).orElseThrow());
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        long t = helper.getLevel().getGameTime();
        helper.assertTrue(kit.apertar(Tecla.Z, t), "Z deveria começar");
        ContextoServidor contexto = new ContextoServidor(jogador);
        for (int i = 0; i < 5; i++) kit.tick(t + i, PARADO, contexto);
        helper.assertTrue(Math.abs(kit.recurso().atual() - 80) < 1e-6, "recurso deveria ser 80, é " + kit.recurso().atual());
        helper.assertTrue(kit.recargas().restante("teste_golpe", t + 4) == 40, "recarga de 40 ticks deveria começar no disparo");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void morrerNaCargaDescartaSemCobrar(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Personagens.TESTE).orElseThrow());
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        ServicoKits.usarTecla(jogador, Tecla.X, true);
        helper.assertTrue(kit.execucao().isPresent(), "X deveria começar a carregar");
        jogador.setHealth(0);
        for (int i = 0; i < 60; i++) ServicoKits.tickJogador(jogador);
        helper.assertTrue(kit.execucao().isEmpty(), "a carga deveria ser descartada na morte");
        helper.assertTrue(kit.recurso().atual() == 100, "não deveria cobrar, recurso " + kit.recurso().atual());
        helper.assertTrue(kit.recargas().pronta("teste_carga", helper.getLevel().getGameTime()), "não deveria haver recarga");
        ServicoKits.usarTecla(jogador, Tecla.Z, true);
        helper.assertTrue(kit.execucao().isEmpty(), "morto não deveria começar habilidade");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void espectadorNaoUsaHabilidade(GameTestHelper helper) {
        // O jogador falso do GameTest sempre responde isSpectator() == false; aqui vai um ServerPlayer real sem conexão.
        ServerPlayer jogador = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "espectador_teste"), ClientInformation.createDefault());
        ServicoKits.definirPersonagem(jogador, Personagens.porId(Personagens.TESTE).orElseThrow());
        KitDoJogador kit = ServicoKits.kit(jogador).orElseThrow();
        ServicoKits.usarTecla(jogador, Tecla.X, true);
        helper.assertTrue(kit.execucao().isPresent(), "X deveria começar a carregar");
        // changeGameModeForPlayer avisa os outros jogadores pela conexão, que aqui não existe.
        CompoundTag modo = new CompoundTag();
        modo.putInt("playerGameType", GameType.SPECTATOR.getId());
        jogador.loadGameTypes(modo);
        helper.assertTrue(jogador.isSpectator(), "o jogador deveria estar no espectador");
        ServicoKits.tickJogador(jogador);
        helper.assertTrue(kit.execucao().isEmpty(), "virar espectador deveria descartar a carga");
        ServicoKits.usarTecla(jogador, Tecla.Z, true);
        helper.assertTrue(kit.execucao().isEmpty(), "espectador não deveria começar habilidade");
        helper.succeed();
    }

    @GameTest(template = "vazio")
    public static void escolhaRecusadaQuandoJaTemPersonagem(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(ServicoKits.escolher(jogador, Personagens.TESTE), "primeira escolha deveria valer");
        helper.assertFalse(ServicoKits.escolher(jogador, Personagens.TESTE), "segunda escolha deveria ser recusada");
        helper.assertFalse(ServicoKits.escolher(helper.makeMockServerPlayerInLevel(), "nao_existe"), "id inválido deveria ser recusado");
        helper.assertTrue(ServicoKits.dados(jogador).personagem.equals(Personagens.TESTE), "deveria continuar teste");
        helper.succeed();
    }
}
