package com.minerwk.arena;

import com.minerwk.MineRWK;
import com.minerwk.boss.LutasToji;
import com.minerwk.boss.TojiEntity;
import com.minerwk.core.boss.TojiBoss;
import com.minerwk.kit.ServicoKits;
import com.minerwk.core.arena.CaixaArena;
import com.minerwk.core.arena.PlantaEscola;
import com.minerwk.core.kit.Vetor;
import com.minerwk.kit.Anexos;
import com.minerwk.kit.ContextoServidor;
import com.minerwk.kit.TestesGojo;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests da arena. Em lotes separados: os testes mexem na mesma vaga da dimensão. */
@GameTestHolder(MineRWK.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TestesArena {
    private TestesArena() {
    }

    /**
     * O servidor de GameTest monta o mundo com uma lista fixa de dimensões e ignora as de datapack. Aqui a
     * dimensão da arena é criada na mão, com o mesmo tipo (do JSON do mod) e o mesmo gerador vazio.
     */
    public static ServerLevel garantirDimensao(MinecraftServer servidor) {
        ServerLevel pronta = ArenasServidor.nivel(servidor);
        if (pronta != null) return pronta;
        var registros = servidor.registryAccess();
        Holder<DimensionType> tipo = registros.registryOrThrow(Registries.DIMENSION_TYPE)
                .getHolderOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE, MineRWK.id("arena")));
        Holder<Biome> bioma = registros.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.THE_VOID);
        LevelStem stem = new LevelStem(tipo,
                new FlatLevelSource(new FlatLevelGeneratorSettings(Optional.empty(), bioma, List.of())));
        ChunkProgressListener mudo = new ChunkProgressListener() {
            @Override
            public void updateSpawnPos(ChunkPos centro) {
            }

            @Override
            public void onStatusChange(ChunkPos chunk, ChunkStatus status) {
            }

            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }
        };
        ServerLevel nivel = new ServerLevel(servidor, servidor.executor, servidor.storageSource,
                new DerivedLevelData(servidor.getWorldData(), servidor.getWorldData().overworldData()),
                ArenasServidor.DIMENSAO, stem, mudo, false, 0L, List.of(), false, null);
        servidor.forgeGetWorldMap().put(ArenasServidor.DIMENSAO, nivel);
        servidor.markWorldsDirty();
        return nivel;
    }

    /** O jogador dos testes do Gojo, com uma conexão de mentira (trocar de dimensão manda pacotes). */
    public static ServerPlayer jogador(GameTestHelper helper) {
        garantirDimensao(helper.getLevel().getServer());
        ServerPlayer jogador = TestesGojo.gojo(helper);
        // O jogador dos testes do Gojo nasce dentro de um bloco do molde; aqui ele precisa de um lugar livre,
        // porque voltar para um lugar obstruído é recusado.
        while (!helper.getLevel().noCollision(jogador.getBoundingBox().deflate(1e-3))) {
            jogador.setPos(jogador.getX(), jogador.getY() + 1, jogador.getZ());
        }
        Connection conexao = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(conexao);
        jogador.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), conexao, jogador,
                CommonListenerCookie.createInitial(jogador.getGameProfile(), false));
        return jogador;
    }

    @GameTest(template = "vazio", batch = "arena_1")
    public static void dimensaoExiste(GameTestHelper helper) {
        ServerLevel nivel = garantirDimensao(helper.getLevel().getServer());
        helper.assertTrue(nivel != null, "a dimensão minerwk:arena deveria existir");
        helper.assertTrue(nivel.dimensionType().fixedTime().orElse(-1) == 12400, "hora fixa no pôr do sol (12400)");
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "arena_2")
    public static void levarETrazerVoltaAoLugarExato(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        jogador.setYRot(37);
        jogador.setXRot(-12);
        Level origem = jogador.level();
        double x = jogador.getX(), y = jogador.getY(), z = jogador.getZ();
        CaixaArena caixa = CaixaArena.daVaga(0);

        ArenasServidor.levar(jogador, caixa);
        Vetor entrada = caixa.entradaJogador();
        helper.assertTrue(ArenasServidor.naArena(jogador), "deveria estar na dimensão da arena");
        helper.assertTrue(jogador.position().distanceTo(new net.minecraft.world.phys.Vec3(entrada.x(), entrada.y(), entrada.z())) < .01,
                "deveria estar na entrada do jogador, está em " + jogador.position());
        helper.assertTrue(Math.abs(jogador.getYRot() - 180) < .01, "deveria olhar para o norte");
        helper.assertTrue(jogador.hasData(Anexos.RETORNO_ARENA), "o ponto de retorno deveria estar salvo");

        helper.assertTrue(ArenasServidor.trazer(jogador), "trazer deveria devolver true");
        helper.assertTrue(jogador.level() == origem, "deveria voltar à dimensão de origem");
        helper.assertTrue(Math.abs(jogador.getX() - x) < .01 && Math.abs(jogador.getY() - y) < .01
                && Math.abs(jogador.getZ() - z) < .01, "deveria voltar ao lugar exato, está em " + jogador.position());
        helper.assertTrue(Math.abs(jogador.getYRot() - 37) < .01 && Math.abs(jogador.getXRot() + 12) < .01,
                "deveria voltar olhando para onde olhava");
        helper.assertFalse(jogador.hasData(Anexos.RETORNO_ARENA), "o ponto de retorno deveria ser apagado");
        jogador.remove(Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "arena_3")
    public static void trazerSemRetornoNaoFazNada(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        var antes = jogador.position();
        helper.assertFalse(ArenasServidor.trazer(jogador), "sem ponto de retorno, trazer devolve false");
        helper.assertTrue(jogador.position().equals(antes) && !ArenasServidor.naArena(jogador), "o jogador não se move");
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "arena_4")
    public static void retornoInvalidoVaiParaORenascimento(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        ArenasServidor.levar(jogador, CaixaArena.daVaga(0));
        jogador.setData(Anexos.RETORNO_ARENA, new PontoRetorno(
                ResourceLocation.fromNamespaceAndPath(MineRWK.MOD_ID, "nao_existe"), 0, 80, 0, 0, 0));
        helper.assertFalse(ArenasServidor.trazer(jogador), "retorno inválido devolve false");
        helper.assertFalse(ArenasServidor.naArena(jogador), "mas o jogador sai da arena, para o renascimento normal");
        helper.assertFalse(jogador.hasData(Anexos.RETORNO_ARENA), "e o ponto de retorno é apagado");
        jogador.remove(Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "arena_5", timeoutTicks = 200)
    public static void montarPoePlataformaEApagarLimpa(GameTestHelper helper) {
        ServerLevel arena = garantirDimensao(helper.getLevel().getServer());
        CaixaArena caixa = CaixaArena.daVaga(40);
        BlockPos centro = new BlockPos(caixa.centroX(), CaixaArena.Y_PATIO, caixa.centroZ());
        BlockPos sujeira = centro.offset(35, 20, -35);
        arena.setBlock(sujeira, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        boolean[] montou = {false};
        boolean[] apagou = {false};
        long comeco = arena.getGameTime();
        // O retorno roda dentro do tick do servidor: os erros são guardados e cobrados depois.
        String[] erro = {null};
        ObrasArena.montar(arena, caixa, PlantaEscola.plataforma(), () -> {
            montou[0] = true;
            long levou = arena.getGameTime() - comeco;
            if (levou > 40) erro[0] = "a montagem deveria levar até 40 ticks, levou " + levou;
            else if (!arena.getBlockState(centro.below()).is(Blocks.STONE_BRICKS)) erro[0] = "tijolo de pedra sob o centro";
            else if (!arena.getBlockState(centro.below(4).offset(30, 0, -30)).is(Blocks.STONE)) erro[0] = "pedra no canto de baixo";
            else if (!arena.getBlockState(centro).isAir()) erro[0] = "ar na altura dos pés";
            else if (!arena.getBlockState(sujeira).isAir()) erro[0] = "o que havia na caixa é limpo antes de montar";
            arena.addFreshEntity(new ItemEntity(arena, centro.getX() + .5, centro.getY() + 1, centro.getZ() + .5,
                    new ItemStack(Items.DIAMOND)));
            ObrasArena.apagar(arena, caixa, () -> apagou[0] = true);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(montou[0] && apagou[0], "montar e apagar deveriam terminar");
            helper.assertTrue(erro[0] == null, String.valueOf(erro[0]));
            helper.assertTrue(arena.getBlockState(centro.below()).isAir() && arena.getBlockState(centro.below(4)).isAir(),
                    "a caixa deveria ficar só com ar");
            AABB tudo = new AABB(centro).inflate(CaixaArena.MEIA + 1, 64, CaixaArena.MEIA + 1);
            helper.assertTrue(arena.getEntitiesOfClass(ItemEntity.class, tudo).isEmpty(), "sem itens caídos na caixa");
            helper.assertTrue(ObrasArena.ocioso(), "sem obra pendente");
        });
    }

    // --- A luta do Toji na arena ---

    private static boolean perto(ServerPlayer jogador, Vec3 ponto) {
        return jogador.position().distanceTo(ponto) < .05;
    }

    private static Vec3 vec(Vetor v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    @GameTest(template = "vazio", batch = "arena_6", timeoutTicks = 300)
    public static void invocarLevaParaAArenaComOToji(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        helper.assertTrue(LutasToji.invocar(jogador, false) == TojiBoss.Invocacao.OK, "a invocação deveria começar");
        helper.assertFalse(ArenasServidor.naArena(jogador), "durante a montagem o jogador ainda está no mundo dele");
        helper.assertTrue(LutasToji.invocar(jogador, false) == TojiBoss.Invocacao.NA_ARENA, "com a arena reservada não dá para invocar de novo");
        helper.succeedWhen(() -> {
            CaixaArena caixa = ArenasServidor.de(jogador.getUUID()).orElseThrow();
            helper.assertTrue(ArenasServidor.naArena(jogador) && perto(jogador, vec(caixa.entradaJogador())),
                    "o jogador deveria estar na entrada da vaga, está em " + jogador.position());
            TojiEntity toji = LutasToji.de(jogador.getUUID()).orElse(null);
            helper.assertTrue(toji != null && ArenasServidor.naArena(toji), "o Toji deveria estar na arena");
            helper.assertTrue(Math.abs(toji.distanceTo(jogador) - 16) < .5, "a 16 blocos, está a " + toji.distanceTo(jogador));
            helper.assertTrue(LutasToji.invocar(jogador, false) == TojiBoss.Invocacao.NA_ARENA, "de dentro da arena não dá para invocar");
            toji.discard();
        });
    }

    @GameTest(template = "vazio", batch = "arena_7", timeoutTicks = 400)
    public static void vitoriaTrazDeVoltaEm100Ticks(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        ServicoKits.definirDespertou(jogador, false);
        Level origem = jogador.level();
        Vec3 antes = jogador.position();
        LutasToji.invocar(jogador, false);
        long[] morte = {-1};
        helper.succeedWhen(() -> {
            long agora = helper.getLevel().getGameTime();
            if (morte[0] < 0) {
                TojiEntity toji = LutasToji.de(jogador.getUUID()).orElse(null);
                helper.assertTrue(toji != null && ArenasServidor.naArena(jogador), "esperando a luta começar");
                toji.kill();
                morte[0] = agora;
            }
            if (agora - morte[0] <= 90) {
                helper.assertTrue(ArenasServidor.naArena(jogador), "antes dos 5 s o jogador ainda está na arena");
            }
            helper.assertTrue(agora - morte[0] >= 110, "esperando os 5 s");
            helper.assertTrue(jogador.level() == origem && perto(jogador, antes), "deveria estar de volta, está em " + jogador.position());
            helper.assertTrue(ServicoKits.dados(jogador).maestriaAtual().despertou, "a vitória dá o despertar");
            helper.assertFalse(jogador.hasData(Anexos.RETORNO_ARENA), "o ponto de retorno é apagado");
            helper.assertTrue(ArenasServidor.de(jogador.getUUID()).isEmpty(), "a arena dele acabou");
        });
    }

    @GameTest(template = "vazio", batch = "arena_8", timeoutTicks = 300)
    public static void tojiSomeTrazDeVoltaNaHora(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        ServicoKits.definirDespertou(jogador, false);
        Level origem = jogador.level();
        Vec3 antes = jogador.position();
        LutasToji.invocar(jogador, false);
        boolean[] sumiu = {false};
        helper.succeedWhen(() -> {
            if (!sumiu[0]) {
                TojiEntity toji = LutasToji.de(jogador.getUUID()).orElse(null);
                helper.assertTrue(toji != null && ArenasServidor.naArena(jogador), "esperando a luta começar");
                toji.discard();
                sumiu[0] = true;
            }
            helper.assertTrue(jogador.level() == origem && perto(jogador, antes), "deveria voltar na hora, está em " + jogador.position());
            helper.assertFalse(ServicoKits.dados(jogador).maestriaAtual().despertou, "sem vitória não há despertar");
            helper.assertFalse(jogador.hasData(Anexos.RETORNO_ARENA), "o ponto de retorno é apagado");
        });
    }

    @GameTest(template = "vazio", batch = "arena_9", timeoutTicks = 300)
    public static void sairDuranteAMontagemLiberaAVaga(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        int ocupadasAntes = ArenasServidor.vagasOcupadas();
        Vec3 antes = jogador.position();
        LutasToji.invocar(jogador, false);
        helper.assertTrue(ArenasServidor.vagasOcupadas() == ocupadasAntes + 1, "a vaga é reservada ao invocar");
        helper.runAfterDelay(1, () -> ArenasServidor.aoSair(jogador));
        helper.runAfterDelay(2, () -> helper.succeedWhen(() -> {
            helper.assertTrue(ArenasServidor.vagasOcupadas() == ocupadasAntes, "a vaga deveria ser liberada depois da limpeza");
            helper.assertTrue(ArenasServidor.de(jogador.getUUID()).isEmpty(), "sem arena para ele");
            helper.assertTrue(!ArenasServidor.naArena(jogador) && perto(jogador, antes), "ele nunca foi levado");
            helper.assertFalse(jogador.hasData(Anexos.RETORNO_ARENA), "sem ponto de retorno guardado");
            helper.assertTrue(LutasToji.de(jogador.getUUID()).isEmpty(), "e sem Toji");
        }));
    }

    @GameTest(template = "vazio", batch = "arena_10", timeoutTicks = 300)
    public static void duasLutasFicamEmVagasDiferentes(GameTestHelper helper) {
        ServerPlayer um = jogador(helper);
        ServerPlayer dois = jogador(helper);
        LutasToji.invocar(um, false);
        LutasToji.invocar(dois, false);
        helper.succeedWhen(() -> {
            CaixaArena a = ArenasServidor.de(um.getUUID()).orElseThrow();
            CaixaArena b = ArenasServidor.de(dois.getUUID()).orElseThrow();
            helper.assertFalse(a.equals(b), "cada luta na sua vaga");
            TojiEntity ta = LutasToji.de(um.getUUID()).orElse(null);
            TojiEntity tb = LutasToji.de(dois.getUUID()).orElse(null);
            helper.assertTrue(ta != null && tb != null, "esperando os dois Tojis");
            helper.assertTrue(perto(um, vec(a.entradaJogador())) && perto(dois, vec(b.entradaJogador())), "cada um na sua entrada");
            ta.discard();
            tb.discard();
        });
    }

    @GameTest(template = "vazio", batch = "arena_11")
    public static void entrarNoJogoNaArenaSemLutaVolta(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        Level origem = jogador.level();
        Vec3 antes = jogador.position();
        ArenasServidor.levar(jogador, CaixaArena.daVaga(50));
        ArenasServidor.aoEntrar(jogador);
        helper.assertTrue(jogador.level() == origem && perto(jogador, antes), "quem entra no jogo na arena sem luta volta ao ponto de retorno");

        ArenasServidor.levar(jogador, CaixaArena.daVaga(50));
        jogador.setData(Anexos.RETORNO_ARENA, new PontoRetorno(
                ResourceLocation.fromNamespaceAndPath(MineRWK.MOD_ID, "nao_existe"), 0, 80, 0, 0, 0));
        ArenasServidor.aoEntrar(jogador);
        helper.assertFalse(ArenasServidor.naArena(jogador), "com retorno inválido vai para o renascimento normal");

        ArenasServidor.levar(jogador, CaixaArena.daVaga(50));
        jogador.removeData(Anexos.RETORNO_ARENA);
        ArenasServidor.aoEntrar(jogador);
        helper.assertFalse(ArenasServidor.naArena(jogador), "sem ponto de retorno também");
        jogador.remove(Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "arena_22")
    public static void entrarMortoNaArenaEsperaORenascimento(GameTestHelper helper) {
        // Morreu na arena e fechou o jogo sem renascer: ao entrar de novo ele ainda está morto lá.
        ServerPlayer jogador = jogador(helper);
        ArenasServidor.levar(jogador, CaixaArena.daVaga(58));
        jogador.hasChangedDimension();
        jogador.kill();
        ArenasServidor.aoEntrar(jogador);
        helper.assertTrue(ArenasServidor.naArena(jogador), "o morto não é teleportado: renascer já o tira de lá");
        helper.assertFalse(jogador.hasData(Anexos.RETORNO_ARENA), "e não leva um ponto de retorno velho para a vida nova");
        jogador.remove(Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    // --- Morrer na arena ---

    /** Quem renasce é um jogador novo que copia o antigo: é aí que o jogo decide o que ele mantém. */
    private static ServerPlayer renascer(GameTestHelper helper, ServerPlayer morto) {
        ServerPlayer novo = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), morto.getGameProfile(),
                ClientInformation.createDefault());
        // Os avanços do jogo passam a apontar para o jogador novo: sem conexão, o próximo avanço quebraria.
        novo.connection = morto.connection;
        novo.restoreFrom(morto, false);
        return novo;
    }

    private static int diamantes(ServerPlayer jogador) {
        return jogador.getInventory().countItem(Items.DIAMOND);
    }

    @GameTest(template = "vazio", batch = "arena_12")
    public static void morrerNaArenaMantemItensEXp(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        CaixaArena caixa = CaixaArena.daVaga(51);
        ArenasServidor.levar(jogador, caixa);
        jogador.getInventory().add(new ItemStack(Items.DIAMOND, 3));
        jogador.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        jogador.setExperienceLevels(10);
        // Um cliente de verdade confirma a troca de dimensão; sem isso o jogador fica imune a dano.
        jogador.hasChangedDimension();
        jogador.kill();
        helper.assertTrue(jogador.isDeadOrDying(), "o jogador deveria ter morrido");
        // Morreu e fechou o jogo sem renascer: o que vai para o disco é o morto, então os itens ficam nele.
        helper.assertTrue(diamantes(jogador) == 3, "os itens continuam no inventário do morto, tem " + diamantes(jogador));
        ServerPlayer novo = renascer(helper, jogador);
        helper.assertTrue(diamantes(novo) == 3, "os 3 diamantes voltam, voltaram " + diamantes(novo));
        helper.assertTrue(novo.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "a armadura volta no lugar");
        helper.assertTrue(novo.experienceLevel == 10, "os 10 níveis voltam, voltaram " + novo.experienceLevel);
        ServerLevel arena = ArenasServidor.nivel(helper.getLevel().getServer());
        helper.assertTrue(arena.getEntitiesOfClass(ItemEntity.class, ObrasArena.volume(caixa)).isEmpty(), "nada cai na arena");
        helper.assertTrue(arena.getEntitiesOfClass(ExperienceOrb.class, ObrasArena.volume(caixa)).isEmpty(), "nem XP");
        jogador.remove(Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "arena_13")
    public static void morrerForaDaArenaDerrubaItens(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        jogador.getInventory().add(new ItemStack(Items.DIAMOND, 3));
        jogador.setExperienceLevels(10);
        jogador.kill();
        ServerPlayer novo = renascer(helper, jogador);
        helper.assertTrue(diamantes(novo) == 0 && novo.experienceLevel == 0, "fora da arena a morte continua como no jogo");
        helper.assertFalse(helper.getLevel().getEntitiesOfClass(ItemEntity.class, jogador.getBoundingBox().inflate(4)).isEmpty(),
                "os itens caem no chão");
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "arena_14", timeoutTicks = 400)
    public static void morrerDepoisDaVitoriaNaoTeleporta(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        ServicoKits.definirDespertou(jogador, false);
        jogador.getInventory().add(new ItemStack(Items.DIAMOND, 3));
        LutasToji.invocar(jogador, false);
        long[] vitoria = {-1};
        boolean[] morreu = {false};
        helper.succeedWhen(() -> {
            long agora = helper.getLevel().getGameTime();
            if (vitoria[0] < 0) {
                TojiEntity toji = LutasToji.de(jogador.getUUID()).orElse(null);
                helper.assertTrue(toji != null && ArenasServidor.naArena(jogador), "esperando a luta começar");
                toji.kill();
                vitoria[0] = agora;
            }
            helper.assertTrue(agora - vitoria[0] >= 40, "esperando 2 s");
            if (!morreu[0]) {
                jogador.hasChangedDimension();
                jogador.kill();
                morreu[0] = true;
            }
            helper.assertTrue(agora - vitoria[0] >= 110, "esperando passar a hora da volta");
            helper.assertTrue(ArenasServidor.naArena(jogador), "o morto não é teleportado");
            helper.assertFalse(jogador.hasData(Anexos.RETORNO_ARENA), "o ponto de retorno é apagado: ele renasce no lugar normal");
            helper.assertTrue(ArenasServidor.de(jogador.getUUID()).isEmpty(), "a arena dele acabou");
            helper.assertTrue(ServicoKits.dados(jogador).maestriaAtual().despertou, "a vitória continua valendo");
            helper.assertTrue(diamantes(renascer(helper, jogador)) == 3, "e os itens voltam ao renascer");
        });
    }

    // --- Barreira e queda no vazio ---

    @GameTest(template = "vazio", batch = "arena_15")
    public static void foraDaCaixaVoltaParaDentro(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        CaixaArena caixa = CaixaArena.daVaga(52);
        ArenasServidor.levar(jogador, caixa);
        // Pérola, fruta do coro, golpe que empurra: de qualquer jeito que ele pare do lado de fora.
        jogador.teleportTo(caixa.centroX() + 60.5, CaixaArena.Y_PATIO, caixa.centroZ() - 55.5);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(jogador.getX() <= caixa.centroX() + CaixaArena.MEIA + 1 && jogador.getZ() >= caixa.centroZ() - CaixaArena.MEIA,
                    "deveria estar de volta dentro da caixa, está em " + jogador.position());
            helper.assertTrue(jogador.getY() == CaixaArena.Y_PATIO, "na mesma altura");
            jogador.remove(Entity.RemovalReason.DISCARDED);
            helper.succeed();
        });
    }

    @GameTest(template = "vazio", batch = "arena_16", timeoutTicks = 300)
    public static void tojiNaoSaiDaCaixa(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        LutasToji.invocar(jogador, false);
        boolean[] empurrado = {false};
        TojiEntity[] visto = {null};
        helper.succeedWhen(() -> {
            TojiEntity toji = LutasToji.de(jogador.getUUID()).orElse(null);
            if (toji != null) visto[0] = toji;
            helper.assertTrue(toji != null, "esperando o Toji" + (visto[0] == null ? "" : ": sumiu por "
                    + visto[0].getRemovalReason() + " em " + visto[0].position() + " com vida " + visto[0].getHealth()));
            CaixaArena caixa = ArenasServidor.de(jogador.getUUID()).orElseThrow();
            if (!empurrado[0]) {
                // Fora da caixa, mas ainda nos chunks carregados da vaga (mais longe ele seria descarregado).
                toji.setPos(caixa.centroX() - CaixaArena.MEIA - 4.5, CaixaArena.Y_PATIO, caixa.centroZ() + .5);
                empurrado[0] = true;
                helper.assertTrue(false, "esperando o tick seguinte");
            }
            helper.assertTrue(toji.getX() >= caixa.centroX() - CaixaArena.MEIA, "o Toji deveria voltar para dentro, está em " + toji.position());
            toji.discard();
        });
    }

    @GameTest(template = "vazio", batch = "arena_17")
    public static void quedaNoVazioVoltaAoCentro(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        CaixaArena caixa = CaixaArena.daVaga(53);
        ServerLevel arena = ArenasServidor.nivel(helper.getLevel().getServer());
        ArenasServidor.levar(jogador, caixa);
        jogador.hasChangedDimension();
        jogador.teleportTo(caixa.centroX() + 10.5, CaixaArena.Y_FUNDO - 6, caixa.centroZ() + 10.5);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(perto(jogador, vec(caixa.centro())), "deveria voltar ao centro, está em " + jogador.position());
            helper.assertTrue(jogador.getHealth() == 16, "a queda tira 4 de vida, sobrou " + jogador.getHealth());
            BlockPos piso = BlockPos.containing(caixa.centro().x(), CaixaArena.Y_PATIO - 1, caixa.centro().z());
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    helper.assertTrue(arena.getBlockState(piso.offset(dx, 0, dz)).is(Blocks.STONE_BRICKS), "o 5 × 5 do centro é refeito");
                }
            }
            helper.assertTrue(arena.getBlockState(piso.offset(3, 0, 0)).isAir(), "e só ele");
            jogador.remove(Entity.RemovalReason.DISCARDED);
            ObrasArena.apagar(arena, caixa, () -> { });
            helper.succeed();
        });
    }

    @GameTest(template = "vazio", batch = "arena_18")
    public static void espectadorPassa(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        CaixaArena caixa = CaixaArena.daVaga(54);
        ArenasServidor.levar(jogador, caixa);
        jogador.setGameMode(GameType.SPECTATOR);
        jogador.teleportTo(caixa.centroX() + 60.5, CaixaArena.Y_FUNDO - 6, caixa.centroZ() + .5);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(jogador.getX() > caixa.centroX() + 60 && jogador.getY() < CaixaArena.Y_FUNDO,
                    "o espectador não é corrigido, está em " + jogador.position());
            jogador.remove(Entity.RemovalReason.DISCARDED);
            helper.succeed();
        });
    }

    // --- Blocos da arena ---

    @GameTest(template = "vazio", batch = "arena_19")
    public static void jogadorNaoQuebraNemPoeNaArena(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        CaixaArena caixa = CaixaArena.daVaga(55);
        ServerLevel arena = ArenasServidor.nivel(helper.getLevel().getServer());
        ArenasServidor.levar(jogador, caixa);
        BlockPos chao = BlockPos.containing(caixa.centro().x(), CaixaArena.Y_PATIO - 1, caixa.centro().z());
        arena.setBlockAndUpdate(chao, Blocks.STONE_BRICKS.defaultBlockState());

        helper.assertFalse(jogador.gameMode.destroyBlock(chao), "em sobrevivência não se quebra bloco da arena à mão");
        helper.assertTrue(arena.getBlockState(chao).is(Blocks.STONE_BRICKS), "o bloco continua lá");
        jogador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 4));
        BlockHitResult emCima = new BlockHitResult(Vec3.atCenterOf(chao).add(0, .5, 0), Direction.UP, chao, false);
        jogador.getMainHandItem().useOn(new UseOnContext(jogador, InteractionHand.MAIN_HAND, emCima));
        helper.assertTrue(arena.getBlockState(chao.above()).isAir(), "nem se põe bloco");

        jogador.setGameMode(GameType.CREATIVE);
        jogador.getMainHandItem().useOn(new UseOnContext(jogador, InteractionHand.MAIN_HAND, emCima));
        helper.assertTrue(arena.getBlockState(chao.above()).is(Blocks.DIRT), "no criativo dá para pôr");
        helper.assertTrue(jogador.gameMode.destroyBlock(chao.above()), "e para quebrar");

        jogador.remove(Entity.RemovalReason.DISCARDED);
        ObrasArena.apagar(arena, caixa, () -> { });
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "arena_20")
    public static void apagarNaoPassaDaCaixa(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        CaixaArena caixa = CaixaArena.daVaga(56);
        ServerLevel arena = ArenasServidor.nivel(helper.getLevel().getServer());
        ArenasServidor.levar(jogador, caixa);
        // A última coluna de dentro e a primeira de fora, lado a lado.
        BlockPos dentro = new BlockPos(caixa.centroX() + CaixaArena.MEIA, 70, caixa.centroZ());
        BlockPos fora = dentro.east();
        arena.setChunkForced(dentro.getX() >> 4, dentro.getZ() >> 4, true);
        arena.setChunkForced(fora.getX() >> 4, fora.getZ() >> 4, true);
        arena.setBlockAndUpdate(dentro, Blocks.STONE.defaultBlockState());
        arena.setBlockAndUpdate(fora, Blocks.STONE.defaultBlockState());
        Vec3 meio = Vec3.atCenterOf(dentro).add(.5, 0, 0);
        new ContextoServidor(jogador).apagarCasca(new Vetor(meio.x, meio.y, meio.z), 0, 6);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(arena.getBlockState(dentro).isAir(), "o bloco de dentro da caixa é apagado");
            helper.assertTrue(arena.getBlockState(fora).is(Blocks.STONE), "o de fora fica");
            arena.setBlockAndUpdate(fora, Blocks.AIR.defaultBlockState());
            arena.setChunkForced(dentro.getX() >> 4, dentro.getZ() >> 4, false);
            arena.setChunkForced(fora.getX() >> 4, fora.getZ() >> 4, false);
            jogador.remove(Entity.RemovalReason.DISCARDED);
            helper.succeed();
        });
    }

    @GameTest(template = "vazio", batch = "arena_21")
    public static void arrancarNaoPassaDaCaixa(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        CaixaArena caixa = CaixaArena.daVaga(57);
        ServerLevel arena = ArenasServidor.nivel(helper.getLevel().getServer());
        ArenasServidor.levar(jogador, caixa);
        BlockPos dentro = new BlockPos(caixa.centroX() + CaixaArena.MEIA, 70, caixa.centroZ());
        BlockPos fora = dentro.east();
        arena.setBlockAndUpdate(dentro, Blocks.DIRT.defaultBlockState());
        arena.setBlockAndUpdate(fora, Blocks.DIRT.defaultBlockState());
        Vec3 meio = Vec3.atCenterOf(dentro).add(.5, 0, 0);
        ContextoServidor ctx = new ContextoServidor(jogador);
        for (int i = 0; i < 80; i++) ctx.arrancarBlocos(new Vetor(meio.x, meio.y, meio.z), 1.5, 2, .6f);
        helper.assertTrue(arena.getBlockState(dentro).isAir(), "a terra de dentro da caixa é arrancada");
        helper.assertTrue(arena.getBlockState(fora).is(Blocks.DIRT), "a de fora fica");
        arena.setBlockAndUpdate(fora, Blocks.AIR.defaultBlockState());
        jogador.remove(Entity.RemovalReason.DISCARDED);
        ObrasArena.apagar(arena, caixa, () -> { });
        helper.succeed();
    }

    // --- Achados da revisão final ---

    @GameTest(template = "vazio", batch = "arena_23")
    public static void retornoObstruidoVaiParaORenascimento(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        Vec3 antes = jogador.position();
        ArenasServidor.levar(jogador, CaixaArena.daVaga(59));
        // Enquanto ele lutava, alguém construiu no lugar de onde ele saiu.
        BlockPos pes = BlockPos.containing(antes);
        helper.getLevel().setBlockAndUpdate(pes, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pes.above(), Blocks.STONE.defaultBlockState());
        helper.assertFalse(ArenasServidor.trazer(jogador), "ponto obstruído devolve false");
        helper.assertFalse(ArenasServidor.naArena(jogador), "mas ele sai da arena");
        helper.assertFalse(perto(jogador, antes), "e não é posto dentro dos blocos");
        helper.assertFalse(jogador.hasData(Anexos.RETORNO_ARENA), "o ponto de retorno é apagado");
        helper.getLevel().setBlockAndUpdate(pes, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pes.above(), Blocks.AIR.defaultBlockState());
        jogador.remove(Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "arena_24")
    public static void aoLigarOServidorNenhumChunkDaArenaFicaForcado(GameTestHelper helper) {
        // O servidor parou com uma luta em andamento: os chunks forçados ficaram salvos no disco.
        ServerLevel arena = garantirDimensao(helper.getLevel().getServer());
        CaixaArena caixa = CaixaArena.daVaga(60);
        int cx = caixa.centroX() >> 4, cz = caixa.centroZ() >> 4;
        arena.setChunkForced(cx, cz, true);
        helper.assertTrue(arena.getForcedChunks().contains(ChunkPos.asLong(cx, cz)), "o chunk deveria estar forçado");
        ObrasArena.soltarChunksEsquecidos(arena);
        helper.assertFalse(arena.getForcedChunks().contains(ChunkPos.asLong(cx, cz)), "ao ligar, o chunk é solto");
        helper.succeed();
    }

    @GameTest(template = "vazio", batch = "arena_25")
    public static void quemCaiNoVazioMontadoDesmonta(GameTestHelper helper) {
        ServerPlayer jogador = jogador(helper);
        CaixaArena caixa = CaixaArena.daVaga(61);
        ServerLevel arena = ArenasServidor.nivel(helper.getLevel().getServer());
        ArenasServidor.levar(jogador, caixa);
        jogador.hasChangedDimension();
        Boat barco = new Boat(arena, caixa.centroX() + 10.5, CaixaArena.Y_FUNDO - 6, caixa.centroZ() + 10.5);
        arena.addFreshEntity(barco);
        jogador.teleportTo(barco.getX(), barco.getY(), barco.getZ());
        helper.assertTrue(jogador.startRiding(barco, true), "o jogador deveria montar no barco");
        helper.runAfterDelay(3, () -> {
            helper.assertFalse(jogador.isPassenger(), "quem cai montado é tirado da montaria");
            helper.assertTrue(perto(jogador, vec(caixa.centro())), "e volta ao centro, está em " + jogador.position());
            helper.assertTrue(jogador.getHealth() == 16, "com um só dano de 4, sobrou " + jogador.getHealth());
            jogador.remove(Entity.RemovalReason.DISCARDED);
            ObrasArena.apagar(arena, caixa, () -> { });
            helper.succeed();
        });
    }
}
