package com.minerwk.kit;

import com.minerwk.MineRWK;
import com.minerwk.arena.ArenasServidor;
import com.minerwk.arena.PontoRetorno;
import com.minerwk.core.kit.Vetor;
import com.minerwk.rede.Envio;
import com.minerwk.rede.MostrarVisual;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * As viagens do R do Cid (o I Am Atomic final): o Cid e o alvo são levados de ponto em ponto, sem gravidade,
 * sobem para a dimensão do espaço e voltam aos pontos de partida. Enquanto dura, o Cid não recebe dano e o
 * alvo só recebe o do golpe. O espaço é um vazio sem chão; cada viagem ganha o seu ponto, longe dos outros.
 */
public final class EspacoServidor {
    public static final ResourceKey<Level> DIMENSAO = ResourceKey.create(Registries.DIMENSION, MineRWK.id("espaco"));
    /**
     * Onde as viagens ficam no espaço: uma a cada 512 blocos, acima do teto de construção da dimensão. Lá o
     * cliente desenha as criaturas sem esperar o terreno (que não existe) ficar pronto.
     */
    private static final int ENTRE_VAGAS = 512;
    private static final double ALTURA = 400;
    /**
     * Segura carregados os pedaços do mundo de onde a viagem saiu e do ponto dela no espaço: sem jogador por
     * perto eles seriam descarregados, e o alvo que ficou (ou que acabou de chegar) sumiria para o servidor.
     */
    private static final TicketType<UUID> BILHETE = TicketType.create("minerwk_viagem", UUID::compareTo);
    /** Quanto o jogador pode escorregar do ponto antes de ser puxado de volta. */
    private static final double FOLGA = .3;
    /** De quanto em quanto tempo o servidor procura quem ficou no espaço sem viagem. */
    private static final int CONFERIR_A_CADA = 20;

    private static final class Viagem {
        final UUID cid, alvo;
        final ResourceKey<Level> origem;
        /** Os pontos de partida: os pés dos dois. */
        final Vec3 pesDoCid, pesDoAlvo;
        final float giro, inclinacao;
        /** Como o alvo era antes: volta a ser assim. */
        final boolean alvoSemGravidade, alvoSemIa;
        /** Onde segurar cada um agora (os pés do Cid e o centro do alvo); null = ainda onde estavam. */
        Vec3 pontoDoCid, centroDoAlvo;
        float giroAgora;
        int vaga = -1;
        /** A luz no céu do mundo de origem: quem volta do espaço ainda a vê. */
        MostrarVisual luz;
        /** A volta já começou: o que ela dispara (a troca de dimensão) não a começa de novo. */
        boolean acabando;
        /** Em que dimensão cada um deve estar agora: quem aparece em outra foi tirado da viagem por fora. */
        ResourceKey<Level> ondeOCid, ondeOAlvo;
        /** O alvo morreu: quem renasce com o mesmo id (um jogador) já não é o alvo. */
        boolean alvoMorto;
        /** O alvo em pessoa: ao mudar de dimensão a criatura vira outro objeto, que a busca pelo id demora a achar. */
        LivingEntity corpo;

        Viagem(ServerPlayer cid, LivingEntity alvo) {
            this.cid = cid.getUUID();
            this.alvo = alvo.getUUID();
            this.origem = cid.level().dimension();
            this.ondeOCid = origem;
            this.ondeOAlvo = origem;
            this.corpo = alvo;
            this.pesDoCid = cid.position();
            this.pesDoAlvo = alvo.position();
            this.giro = cid.getYRot();
            this.giroAgora = giro;
            this.inclinacao = cid.getXRot();
            this.alvoSemGravidade = alvo.isNoGravity();
            this.alvoSemIa = alvo instanceof Mob m && m.isNoAi();
        }
    }

    /** As viagens em curso, pelo UUID do Cid. */
    private static final Map<UUID, Viagem> VIAGENS = new HashMap<>();
    private static final Set<Integer> VAGAS = new HashSet<>();
    /** O golpe do Cid está passando: é o único dano que chega ao alvo. */
    private static boolean golpe;

    private EspacoServidor() {
    }

    public static void registrarEventos() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            if (nivel(e.getServer()) == null) {
                MineRWK.LOGGER.error("Espaço: a dimensão {} não carregou; o R do Cid acontece sem sair do mundo", DIMENSAO.location());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, EspacoServidor::aoReceberDano);
        NeoForge.EVENT_BUS.addListener(EspacoServidor::aoDerrubar);
        NeoForge.EVENT_BUS.addListener(EspacoServidor::aoDerrubarXp);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer j) {
                // O servidor caiu no meio de uma viagem: o jogador foi salvo sem gravidade.
                if (!emViagem(j) && j.isNoGravity()) j.setNoGravity(false);
                devolverPerdido(j);
            }
        });
        NeoForge.EVENT_BUS.addListener(EspacoServidor::aoSair);
        // Ao fechar, todos voltam antes de o mundo ser salvo.
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> {
            for (UUID cid : List.copyOf(VIAGENS.keySet())) terminar(e.getServer(), cid);
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            VIAGENS.clear();
            VAGAS.clear();
        });
    }

    public static ServerLevel nivel(MinecraftServer servidor) {
        return servidor.getLevel(DIMENSAO);
    }

    public static boolean noEspaco(Entity e) {
        return e.level().dimension() == DIMENSAO;
    }

    public static boolean emViagem(Entity e) {
        return VIAGENS.containsKey(e.getUUID()) || viagemDoAlvo(e.getUUID()) != null;
    }

    private static Viagem viagemDoAlvo(UUID alvo) {
        for (Viagem v : VIAGENS.values()) if (v.alvo.equals(alvo)) return v;
        return null;
    }

    /** O alvo da viagem desse jogador, esteja em que dimensão estiver; null se não é esse, morreu ou sumiu. */
    static LivingEntity alvo(ServerPlayer cid, UUID id) {
        Viagem v = VIAGENS.get(cid.getUUID());
        return v == null || !v.alvo.equals(id) ? null : alvo(cid.server, v);
    }

    private static LivingEntity alvo(MinecraftServer servidor, Viagem v) {
        if (v.alvoMorto) return null;
        if (v.corpo != null) {
            if (v.corpo.isAlive() && !v.corpo.isRemoved()) return v.corpo;
            if (v.corpo.isDeadOrDying()) {
                v.alvoMorto = true;
                return null;
            }
        }
        ServerLevel nivel = servidor.getLevel(v.ondeOAlvo);
        if (nivel != null && nivel.getEntity(v.alvo) instanceof LivingEntity e && e.isAlive()) {
            v.corpo = e;
            return e;
        }
        return null;
    }

    // --- O ciclo ---

    /** Começa a viagem. Devolve false se um dos dois já está numa, ou se o jogador já está no espaço. */
    static boolean comecar(ServerPlayer cid, LivingEntity alvo) {
        if (emViagem(cid) || emViagem(alvo) || noEspaco(cid)) return false;
        Viagem v = new Viagem(cid, alvo);
        VIAGENS.put(v.cid, v);
        v.vaga = reservar();
        bilhetes(cid.server, v, true);
        cid.stopRiding();
        cid.setNoGravity(true);
        new ContextoServidor(cid).firmar(true);
        alvo.stopRiding();
        alvo.ejectPassengers();
        alvo.setNoGravity(true);
        if (alvo instanceof Mob m) {
            m.setNoAi(true);
            m.getNavigation().stop();
        }
        return true;
    }

    /** Os pontos em que os dois ficam a partir de agora, e para onde o Cid olha. */
    static void pontos(ServerPlayer cid, Vec3 pes, Vec3 centroDoAlvo, Vec3 encarar) {
        Viagem v = VIAGENS.get(cid.getUUID());
        if (v == null) return;
        boolean mudou = v.pontoDoCid == null || v.pontoDoCid.distanceToSqr(pes) > 1e-6;
        v.pontoDoCid = pes;
        LivingEntity alvo = alvo(cid.server, v);
        // O alvo que não subiu para o espaço fica onde estava: os pontos de lá não valem no mundo dele.
        if (alvo != null && alvo.level() == cid.level()) v.centroDoAlvo = centroDoAlvo;
        if (encarar != null) {
            Vec3 ate = encarar.subtract(pes);
            if (ate.horizontalDistanceSqr() > 1e-6) v.giroAgora = (float) (Math.toDegrees(Math.atan2(ate.z, ate.x)) - 90);
        }
        if (mudou) porOCid(cid, v);
        if (alvo != null && v.centroDoAlvo != null) segurarOAlvo(alvo, v.centroDoAlvo);
    }

    private static void porOCid(ServerPlayer cid, Viagem v) {
        Vec3 p = v.pontoDoCid;
        cid.setDeltaMovement(Vec3.ZERO);
        cid.resetFallDistance();
        if (cid.connection != null) {
            cid.connection.teleport(p.x, p.y, p.z, v.giroAgora, 0);
            cid.connection.aboveGroundTickCount = 0;
        } else {
            cid.moveTo(p.x, p.y, p.z, v.giroAgora, 0);
        }
    }

    private static void segurarOAlvo(LivingEntity alvo, Vec3 centro) {
        double y = centro.y - alvo.getBbHeight() / 2;
        alvo.setDeltaMovement(Vec3.ZERO);
        alvo.resetFallDistance();
        if (alvo.position().distanceToSqr(centro.x, y, centro.z) < 1e-6) return;
        if (alvo instanceof ServerPlayer p && p.connection != null) {
            p.connection.teleport(centro.x, y, centro.z, p.getYRot(), p.getXRot());
            p.connection.aboveGroundTickCount = 0;
        } else {
            alvo.teleportTo(centro.x, y, centro.z);
            alvo.hurtMarked = true;
        }
    }

    /**
     * Leva o Cid para o espaço, e o alvo junto se ele pode mudar de dimensão. Devolve os pés do Cid lá. Sem a
     * dimensão, dentro de uma arena ou com um jogador sem conexão (GameTest), ninguém sai: devolve onde ele está.
     */
    static Vec3 subir(ServerPlayer cid) {
        Viagem v = VIAGENS.get(cid.getUUID());
        ServerLevel espaco = nivel(cid.server);
        if (v == null || espaco == null || cid.connection == null || ArenasServidor.naArena(cid) || noEspaco(cid)) {
            return cid.position();
        }
        Vec3 base = base(v);
        LivingEntity alvo = alvo(cid.server, v);
        cid.setData(Anexos.RETORNO_ESPACO, new PontoRetorno(v.origem.location(), v.pesDoCid.x, v.pesDoCid.y, v.pesDoCid.z, v.giro, v.inclinacao));
        cid.teleportTo(espaco, base.x, base.y, base.z, v.giroAgora, 0);
        cid.setNoGravity(true);
        v.pontoDoCid = base;
        v.ondeOCid = DIMENSAO;
        if (alvo != null && podeIr(alvo, espaco)) {
            Vec3 ao = base.add(0, 2, 0);
            if (alvo instanceof ServerPlayer p) {
                if (p.connection != null) {
                    p.setData(Anexos.RETORNO_ESPACO, new PontoRetorno(v.origem.location(), v.pesDoAlvo.x, v.pesDoAlvo.y, v.pesDoAlvo.z,
                            p.getYRot(), p.getXRot()));
                    p.teleportTo(espaco, ao.x, ao.y, ao.z, p.getYRot(), p.getXRot());
                    v.ondeOAlvo = DIMENSAO;
                }
            } else if (alvo.changeDimension(new DimensionTransition(espaco, ao, Vec3.ZERO, alvo.getYRot(), alvo.getXRot(),
                    DimensionTransition.DO_NOTHING)) instanceof LivingEntity la) {
                v.ondeOAlvo = DIMENSAO;
                v.corpo = la;
                la.setNoGravity(true);
            }
        }
        return base;
    }

    /** Quem não muda de dimensão: o Dragão do Fim, o Wither e o que o próprio jogo não deixa. */
    private static boolean podeIr(LivingEntity alvo, ServerLevel espaco) {
        if (alvo instanceof EnderDragon || alvo instanceof WitherBoss) return false;
        return alvo.canChangeDimensions(alvo.level(), espaco);
    }

    private static Vec3 base(Viagem v) {
        return new Vec3(v.vaga * (double) ENTRE_VAGAS + .5, ALTURA, .5);
    }

    /** Põe ou tira os bilhetes da viagem: o ponto de partida e, já na subida, o ponto no espaço (ele carrega enquanto os socos acontecem). */
    private static void bilhetes(MinecraftServer servidor, Viagem v, boolean por) {
        ServerLevel origem = servidor.getLevel(v.origem), espaco = nivel(servidor);
        ChunkPos la = new ChunkPos(BlockPos.containing(v.pesDoAlvo)), ca = new ChunkPos(BlockPos.containing(base(v)));
        if (origem != null) {
            if (por) origem.getChunkSource().addRegionTicket(BILHETE, la, 2, v.cid);
            else origem.getChunkSource().removeRegionTicket(BILHETE, la, 2, v.cid);
        }
        if (espaco != null) {
            if (por) espaco.getChunkSource().addRegionTicket(BILHETE, ca, 2, v.cid);
            else espaco.getChunkSource().removeRegionTicket(BILHETE, ca, 2, v.cid);
        }
    }

    private static int reservar() {
        int i = 0;
        while (!VAGAS.add(i)) i++;
        return i;
    }

    /** Acaba a viagem desse jogador: os dois voltam aos pontos de partida. Sem viagem, não faz nada. */
    static void terminar(MinecraftServer servidor, UUID idDoCid) {
        terminar(servidor, idDoCid, servidor.getPlayerList().getPlayer(idDoCid));
    }

    /** Com o jogador em mãos (o do kit): ele pode já não estar na lista do servidor. */
    static void terminar(ServerPlayer cid) {
        terminar(cid.server, cid.getUUID(), cid);
    }

    private static void terminar(MinecraftServer servidor, UUID idDoCid, ServerPlayer cid) {
        Viagem v = VIAGENS.get(idDoCid);
        if (v == null || v.acabando) return;
        // A viagem só sai da lista no fim: enquanto os dois voltam ainda vale a proteção, e a troca de
        // dimensão não derruba os efeitos do kit.
        v.acabando = true;
        try {
            devolver(servidor, v, cid);
        } finally {
            VIAGENS.remove(idDoCid);
            bilhetes(servidor, v, false);
            VAGAS.remove(v.vaga);
        }
    }

    private static void devolver(MinecraftServer servidor, Viagem v, ServerPlayer cid) {
        ServerLevel origem = servidor.getLevel(v.origem);
        // O Cid é solto antes de tudo: se a volta do alvo falhar, ele não fica preso no ar.
        if (cid != null) {
            cid.setNoGravity(false);
            new ContextoServidor(cid).firmar(false);
        }
        try {
            devolverOAlvo(servidor, v, origem);
        } catch (RuntimeException e) {
            MineRWK.LOGGER.error("Espaço: a volta do alvo falhou", e);
        }
        if (cid == null) return;
        // Quem sai do jogo no espaço fica com o ponto guardado e volta por ele ao entrar; quem sai durante a
        // subida é salvo já no chão. O morto renasce no lugar normal.
        if (cid.hasDisconnected()) {
            if (!noEspaco(cid)) cid.moveTo(v.pesDoCid.x, v.pesDoCid.y, v.pesDoCid.z, v.giro, v.inclinacao);
            return;
        }
        if (!cid.isAlive()) {
            cid.removeData(Anexos.RETORNO_ESPACO);
            return;
        }
        // Tirado da viagem por fora (a arena fechou, um comando o levou): fica onde está, só é solto.
        if (cid.level().dimension() != v.ondeOCid) {
            if (!noEspaco(cid)) cid.removeData(Anexos.RETORNO_ESPACO);
            return;
        }
        boolean veio = noEspaco(cid);
        trazer(cid, origem, v.pesDoCid, v.giro, v.inclinacao);
        if (veio && v.luz != null) Envio.para(cid, v.luz);
    }

    private static void devolverOAlvo(MinecraftServer servidor, Viagem v, ServerLevel origem) {
        LivingEntity alvo = alvo(servidor, v);
        if (alvo != null) {
            alvo.setNoGravity(v.alvoSemGravidade);
            if (alvo instanceof Mob m) m.setNoAi(v.alvoSemIa);
            alvo.setDeltaMovement(Vec3.ZERO);
            if (alvo instanceof ServerPlayer p) {
                if (!p.hasDisconnected() && p.level().dimension() == v.ondeOAlvo) {
                    boolean veio = noEspaco(p);
                    trazer(p, origem, v.pesDoAlvo, p.getYRot(), p.getXRot());
                    if (veio && v.luz != null) Envio.para(p, v.luz);
                }
            } else if (origem != null && alvo.level() != origem) {
                if (alvo.changeDimension(new DimensionTransition(origem, v.pesDoAlvo, Vec3.ZERO, alvo.getYRot(), alvo.getXRot(),
                        DimensionTransition.DO_NOTHING)) instanceof LivingEntity la) {
                    alvo = la;
                    alvo.setNoGravity(v.alvoSemGravidade);
                    if (alvo instanceof Mob m) m.setNoAi(v.alvoSemIa);
                }
            } else {
                alvo.teleportTo(v.pesDoAlvo.x, v.pesDoAlvo.y, v.pesDoAlvo.z);
                alvo.hurtMarked = true;
            }
            alvo.resetFallDistance();
        }
    }

    /** Põe o jogador de volta no ponto de partida (ou no renascimento, se o mundo de lá não existe mais). */
    private static void trazer(ServerPlayer jogador, ServerLevel destino, Vec3 pes, float giro, float inclinacao) {
        jogador.removeData(Anexos.RETORNO_ESPACO);
        jogador.setNoGravity(false);
        jogador.setDeltaMovement(Vec3.ZERO);
        if (destino == null || destino.dimension() == DIMENSAO) {
            ArenasServidor.paraORenascimento(jogador);
        } else if (jogador.connection == null) {
            jogador.moveTo(pes.x, pes.y, pes.z, giro, inclinacao);
        } else {
            jogador.teleportTo(destino, pes.x, pes.y, pes.z, giro, inclinacao);
        }
        jogador.resetFallDistance();
    }

    /** Quem está no espaço sem uma viagem em curso (entrou no jogo lá, veio por comando) é devolvido. */
    private static void devolverPerdido(ServerPlayer jogador) {
        if (!noEspaco(jogador) || emViagem(jogador) || !jogador.isAlive() || jogador.isSpectator()) return;
        if (jogador.hasData(Anexos.RETORNO_ESPACO)) {
            PontoRetorno p = jogador.getData(Anexos.RETORNO_ESPACO);
            trazer(jogador, jogador.server.getLevel(ResourceKey.create(Registries.DIMENSION, p.dimensao())),
                    new Vec3(p.x(), p.y(), p.z()), p.yaw(), p.pitch());
        } else {
            trazer(jogador, null, Vec3.ZERO, 0, 0);
        }
    }

    /** O alvo que é jogador saiu do jogo no meio: é salvo como era (no espaço, volta pelo ponto guardado ao entrar). */
    private static void aoSair(PlayerEvent.PlayerLoggedOutEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer p)) return;
        Viagem v = viagemDoAlvo(p.getUUID());
        if (v == null) return;
        p.setNoGravity(v.alvoSemGravidade);
        if (!noEspaco(p)) p.moveTo(v.pesDoAlvo.x, v.pesDoAlvo.y, v.pesDoAlvo.z, p.getYRot(), p.getXRot());
    }

    private static void tick(MinecraftServer servidor) {
        for (Viagem v : List.copyOf(VIAGENS.values())) {
            if (v.acabando) continue;
            ServerPlayer cid = servidor.getPlayerList().getPlayer(v.cid);
            // O golpe do Cid é quem acaba a viagem; isto é só para o que escapa dele (o jogador sumiu do servidor).
            if (cid == null) {
                terminar(servidor, v.cid);
                continue;
            }
            // Tirado do mundo em que devia estar (a arena fechou, um comando): a viagem acaba, sem puxá-lo de volta.
            if (cid.isAlive() && cid.level().dimension() != v.ondeOCid) {
                terminar(servidor, v.cid, cid);
                continue;
            }
            if (v.pontoDoCid != null && cid.isAlive() && cid.position().distanceToSqr(v.pontoDoCid) > FOLGA * FOLGA) porOCid(cid, v);
            LivingEntity alvo = alvo(servidor, v);
            if (alvo != null && v.centroDoAlvo != null) segurarOAlvo(alvo, v.centroDoAlvo);
        }
        if (servidor.getTickCount() % CONFERIR_A_CADA != 0) return;
        ServerLevel espaco = nivel(servidor);
        if (espaco == null) return;
        for (ServerPlayer j : List.copyOf(espaco.players())) devolverPerdido(j);
    }

    // --- Dano, itens e XP ---

    /** O dano do golpe no alvo da viagem: passa pela proteção, e o soco anterior não protege do seguinte. */
    static void ferir(ServerPlayer cid, LivingEntity alvo, float dano) {
        boolean antes = golpe;
        golpe = true;
        try {
            alvo.invulnerableTime = 0;
            alvo.hurt(cid.damageSources().playerAttack(cid), dano);
        } finally {
            golpe = antes;
        }
    }

    private static void aoReceberDano(LivingIncomingDamageEvent evento) {
        LivingEntity e = evento.getEntity();
        // /kill e o vazio passam sempre.
        if (VIAGENS.isEmpty() || evento.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (VIAGENS.containsKey(e.getUUID())) evento.setCanceled(true);
        else if (!golpe && viagemDoAlvo(e.getUUID()) != null) evento.setCanceled(true);
    }

    /** O alvo morreu no espaço: o que ele larga cai no ponto de onde saiu, não lá em cima. */
    private static void aoDerrubar(LivingDropsEvent evento) {
        Viagem v = viagemDoAlvo(evento.getEntity().getUUID());
        if (v == null || !noEspaco(evento.getEntity())) return;
        ServerLevel origem = evento.getEntity().getServer().getLevel(v.origem);
        if (origem == null) return;
        for (ItemEntity item : evento.getDrops()) {
            origem.addFreshEntity(new ItemEntity(origem, v.pesDoAlvo.x, v.pesDoAlvo.y + .2, v.pesDoAlvo.z, item.getItem().copy()));
        }
        evento.setCanceled(true);
    }

    private static void aoDerrubarXp(LivingExperienceDropEvent evento) {
        Viagem v = viagemDoAlvo(evento.getEntity().getUUID());
        if (v == null || !noEspaco(evento.getEntity())) return;
        ServerLevel origem = evento.getEntity().getServer().getLevel(v.origem);
        if (origem == null) return;
        ExperienceOrb.award(origem, v.pesDoAlvo, evento.getDroppedExperience());
        evento.setCanceled(true);
    }

    // --- A luz no céu ---

    /** Manda um visual a todos a até {@code raio} do ponto de onde a viagem desse jogador saiu. */
    static void visualNaOrigem(ServerPlayer cid, String tipo, float forca, int duracao, double raio) {
        Viagem v = VIAGENS.get(cid.getUUID());
        ServerLevel origem = v == null ? null : cid.server.getLevel(v.origem);
        if (origem == null) return;
        Vetor ponto = new Vetor(v.pesDoCid.x, v.pesDoCid.y, v.pesDoCid.z);
        MostrarVisual pacote = new MostrarVisual(cid.getId(), tipo, ponto, new Vetor(0, 1, 0), forca, duracao, origem.getGameTime());
        v.luz = pacote;
        for (ServerPlayer j : origem.players()) {
            if (j.position().distanceToSqr(v.pesDoCid) <= raio * raio) Envio.para(j, pacote);
        }
    }
}
