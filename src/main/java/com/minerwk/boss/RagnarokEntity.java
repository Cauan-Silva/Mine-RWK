package com.minerwk.boss;

import com.minerwk.MineRWK;
import com.minerwk.core.arena.CaixaArena;
import com.minerwk.core.boss.CutsceneAsa;
import com.minerwk.core.boss.Golpe;
import com.minerwk.core.boss.MotorBoss;
import com.minerwk.core.boss.RagnarokBoss;
import com.minerwk.core.boss.SonsRagnarok;
import com.minerwk.rede.Cutscene;
import com.minerwk.registro.Sons;
import com.minerwk.core.kit.Vetor;
import com.minerwk.kit.ServicoKits;
import com.minerwk.rede.Envio;
import com.minerwk.rede.MostrarVisual;
import com.minerwk.registro.Efeitos;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * O Ragnarok como boss, Nêmesis do Cid: o demônio colossal em chamas. A escolha e os tempos dos golpes vêm do
 * {@link MotorBoss}; aqui fica o que cada golpe faz no mundo, o calor do corpo, a asa cortada abaixo de 50%
 * de vida e o fim da luta. As marcas de chão são as do Toji. Não quebra blocos e não é salvo em disco.
 */
public final class RagnarokEntity extends Monster implements GeoEntity {
    /** Dano que não muda com a dificuldade do mundo. */
    public static final ResourceKey<DamageType> DANO = ResourceKey.create(Registries.DAMAGE_TYPE, MineRWK.id("ragnarok"));
    private static final String CONTROLE = "principal";
    /** O que o cliente precisa saber para desenhá-lo: a asa que falta e se está no ar. */
    private static final EntityDataAccessor<Byte> ESTADO = SynchedEntityData.defineId(RagnarokEntity.class, EntityDataSerializers.BYTE);
    private static final int SEM_ASA = 1, VOANDO = 2;
    private static final RawAnimation PARADO = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANDANDO = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation MORTE = RawAnimation.begin().thenPlayAndHold("morte");
    private static final int TICKS_DA_MORTE = 50;

    /** Um projétil da rajada em voo: fere cada um uma vez e para em bloco. Vive além do golpe que o lançou. */
    private static final class Projetil {
        final Vec3 rumo;
        final Set<UUID> feridos = new HashSet<>();
        Vec3 ponto;
        double andou;

        Projetil(Vec3 ponto, Vec3 rumo) {
            this.ponto = ponto;
            this.rumo = rumo;
        }
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent barra = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10);
    private final MotorBoss motor = new MotorBoss(RagnarokBoss.GOLPES, RagnarokBoss.INTERVALO);
    private final List<Projetil> projeteis = new ArrayList<>();
    private final Set<UUID> atingidos = new HashSet<>();
    private UUID invocadorId;
    private ServerPlayer invocador;
    private int ticksDeLuta;
    private int fase = 1;
    private boolean venceu;
    private boolean lutaEncerrada;
    private Vec3 direcao = new Vec3(0, 0, 1);
    /** Tick de jogo até o qual ele não age: a entrada e a cena da asa. */
    private long paradoAte;
    private boolean entrou;
    /** Tick de jogo em que a asa cortada some do corpo (0: não está para sumir). */
    private long asaCaiEm;
    /** Ticks de jogo da cena da asa: o corte do Shadow e o urro (0: já passou). */
    private long corteEm, urroEm;
    /** Onde o murro cai. */
    private Vec3 pontoDoGolpe;
    private boolean miraTravada;
    /** Para que lado o Sopro varre: +1 ou −1. */
    private int sentido = 1;
    /** A investida: de onde saiu, quanto atravessa e a altura do chão. */
    private Vec3 saida = Vec3.ZERO;
    private double comprimento;
    private Vec3 antes = Vec3.ZERO;

    public RagnarokEntity(EntityType<? extends RagnarokEntity> tipo, Level nivel) {
        super(tipo, nivel);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder atributos() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, RagnarokBoss.VIDA)
                .add(Attributes.MOVEMENT_SPEED, .26)
                .add(Attributes.FOLLOW_RANGE, RagnarokBoss.ALCANCE_DE_VISAO)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.STEP_HEIGHT, 1.6)
                .add(Attributes.ATTACK_DAMAGE, RagnarokBoss.DANO_SOCO);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder dados) {
        super.defineSynchedData(dados);
        dados.define(ESTADO, (byte) 0);
    }

    private void estado(int marca, boolean valor) {
        int atual = entityData.get(ESTADO);
        entityData.set(ESTADO, (byte) (valor ? atual | marca : atual & ~marca));
    }

    /** Se a asa esquerda já foi cortada (vale no cliente também). */
    public boolean semAsa() {
        return (entityData.get(ESTADO) & SEM_ASA) != 0;
    }

    /** Se está no ar, no meio da investida (vale no cliente também). */
    public boolean voando() {
        return (entityData.get(ESTADO) & VOANDO) != 0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    // --- A luta ---

    public void comecarLuta(ServerPlayer dono) {
        invocadorId = dono.getUUID();
        invocador = dono;
        // Vida e dano pela config ("Balanceamento"), a partir da constante: começar de novo não multiplica duas vezes.
        poder = com.minerwk.kit.PoderServidor.de(dono);
        var vida = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (vida != null) vida.setBaseValue(poder.vidaDoBoss(com.minerwk.core.boss.RagnarokBoss.VIDA));
        setHealth(getMaxHealth());
        LutasRagnarok.registrar(this);
        paradoAte = level().getGameTime() + RagnarokBoss.CHEGADA + RagnarokBoss.ENTRADA;
        motor.parar(paradoAte);
    }

    public UUID invocadorId() {
        return invocadorId;
    }

    public int fase() {
        return fase;
    }

    /** Quanto a investida em curso (ou a última) atravessa, depois de cortada por paredes e pela beirada da arena. */
    public double comprimentoDaInvestida() {
        return comprimento;
    }

    /** O golpe em andamento, ou null. */
    public Golpe golpe() {
        return motor.atual();
    }

    /** Começa {@code golpe} agora contra quem invocou, sem olhar alcance nem recarga (testes e prévia). */
    public void forcar(Golpe golpe) {
        if (invocador == null || !isAlive() || lutaEncerrada) return;
        if (motor.atual() == RagnarokBoss.INVESTIDA) pousar();
        paradoAte = 0;
        motor.forcar(golpe, level().getGameTime());
        comecar(golpe, invocador);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        tickLuta();
    }

    private void tickLuta() {
        ServerPlayer dono = invocador;
        ticksDeLuta++;
        boolean presente = dono != null && dono.isAlive() && !dono.isRemoved() && !dono.hasDisconnected()
                && dono.level() == level();
        if (RagnarokBoss.acabou(presente, ticksDeLuta)) {
            sumir();
            return;
        }
        long agora = level().getGameTime();
        barra.setProgress(getHealth() / getMaxHealth());
        if (corteEm > 0 && agora >= corteEm) {
            corteEm = 0;
            cortarAAsa();
        }
        if (asaCaiEm > 0 && agora >= asaCaiEm) {
            asaCaiEm = 0;
            estado(SEM_ASA, true);
            Vec3 chao = naBase(CutsceneAsa.ASA.x() * 1.7, 0, CutsceneAsa.ASA.z());
            particulas(ParticleTypes.LARGE_SMOKE, chao.x, chao.y + .5, chao.z, 40, 1.2, .4, 1.2, .04);
            particulas(ParticleTypes.CAMPFIRE_COSY_SMOKE, chao.x, chao.y + .2, chao.z, 12, 1.5, .1, 1.5, .02);
        }
        if (urroEm > 0 && agora >= urroEm) {
            urroEm = 0;
            som(SonsRagnarok.RUGIDO, 6f);
        }
        tickProjeteis();
        if (ticksDeLuta % RagnarokBoss.CALOR_A_CADA == 0) calor();
        if (tickCount % 4 == 0) particulas(ParticleTypes.FLAME, getX(), getY() + 3.5, getZ(), 3, 1.1, 2.2, 1.1, .01);
        // No segundo tick quem está perto já recebeu a entidade e vê a animação.
        if (!entrou && ticksDeLuta >= RagnarokBoss.CHEGADA) entrar(dono);
        if (agora < paradoAte) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            encarar(dono.position().subtract(position()));
            return;
        }
        if (fase == 1 && motor.atual() == null && RagnarokBoss.fase(getHealth() / getMaxHealth()) == 2) {
            perderAAsa(dono, agora);
            return;
        }
        double distancia = distanceTo(dono);
        Golpe anterior = motor.atual();
        MotorBoss.Evento evento = motor.tick(distancia, agora, random.nextDouble());
        Golpe golpe = motor.atual() != null ? motor.atual() : anterior;
        switch (evento) {
            case COMECOU -> comecar(golpe, dono);
            case ABRIU_JANELA -> abrirJanela(golpe);
            case FECHOU_JANELA, TERMINOU -> {
                if (golpe == RagnarokBoss.INVESTIDA) pousar();
            }
            default -> {
            }
        }
        if (motor.atual() != null) duranteGolpe(motor.atual(), dono);
        else perseguir(dono, distancia);
    }

    private void perseguir(LivingEntity alvo, double distancia) {
        getLookControl().setLookAt(alvo, 30, 30);
        if (distancia <= 5) getNavigation().stop();
        else if (tickCount % 5 == 0 || getNavigation().isDone()) {
            getNavigation().moveTo(alvo, fase >= 2 ? RagnarokBoss.PASSO_SEM_ASA : RagnarokBoss.PASSO_NORMAL);
        }
    }

    private static Vec3 horizontal(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1e-6 ? null : h.normalize();
    }

    /** Vira o corpo para {@code para} (só no plano) e guarda como a direção do golpe. */
    private void encarar(Vec3 para) {
        Vec3 h = horizontal(para);
        if (h != null) direcao = h;
        float yaw = (float) (Mth.atan2(direcao.z, direcao.x) * Mth.RAD_TO_DEG) - 90f;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    /** {@code v} girado {@code graus} em volta do eixo vertical. */
    private static Vec3 girar(Vec3 v, double graus) {
        double a = Math.toRadians(graus), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    private Vec3 peito() {
        return position().add(0, getBbHeight() * .6, 0);
    }

    private Vec3 boca() {
        return position().add(0, getBbHeight() * .82, 0).add(direcao.scale(1.4));
    }

    /** Marca no chão onde o próximo murro cai: no alvo, até onde o braço alcança. */
    private void mirarOSoco(LivingEntity alvo, double raio, int ticks) {
        encarar(alvo.position().subtract(position()));
        double d = RagnarokBoss.pontoDoSoco(alvo.position().subtract(position()).horizontalDistance());
        pontoDoGolpe = position().add(direcao.scale(d));
        visual("toji_sombra", pontoDoGolpe, direcao, (float) raio, ticks);
    }

    private void comecar(Golpe golpe, LivingEntity alvo) {
        getNavigation().stop();
        atingidos.clear();
        pontoDoGolpe = null;
        miraTravada = false;
        encarar(alvo.position().subtract(position()));
        triggerAnim(CONTROLE, golpe.animacao());
        if (golpe == RagnarokBoss.SOCO) {
            mirarOSoco(alvo, RagnarokBoss.RAIO_SOCO, golpe.aviso() + golpe.janela());
        } else if (golpe == RagnarokBoss.BARRAGEM) {
            mirarOSoco(alvo, RagnarokBoss.RAIO_BARRAGEM, golpe.aviso() + 1);
        } else if (golpe == RagnarokBoss.RAJADA) {
            for (int i = 0; i < RagnarokBoss.PROJETEIS; i++) {
                visual("toji_linha", position(), girar(direcao, RagnarokBoss.desvioDaRajada(i)), 22f, golpe.aviso() + 2);
            }
        } else if (golpe == RagnarokBoss.INVESTIDA) {
            decolar(golpe);
        } else if (golpe == RagnarokBoss.SOPRO) {
            som(SonsRagnarok.SOPRO_CARGA, 4f);
            visual("ragnarok_carga", boca(), direcao, 1f, golpe.aviso());
        } else if (golpe == RagnarokBoss.PISAO) {
            visual("toji_sombra", position(), direcao, (float) RagnarokBoss.RAIO_PISAO, golpe.aviso() + golpe.janela());
        }
    }

    private void duranteGolpe(Golpe golpe, LivingEntity alvo) {
        boolean naJanela = motor.naJanela();
        int tj = naJanela ? motor.tempoNaJanela() : -1;
        if (golpe == RagnarokBoss.INVESTIDA) {
            voar(tj);
            return;
        }
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (golpe == RagnarokBoss.BARRAGEM && naJanela) {
            if (RagnarokBoss.miraDaBarragem(tj)) mirarOSoco(alvo, RagnarokBoss.RAIO_BARRAGEM, RagnarokBoss.MIRA_DA_BARRAGEM + 1);
            // O primeiro cai quando a janela abre; os outros três, daqui.
            if (tj > 0 && RagnarokBoss.socoDaBarragem(tj)) socar(RagnarokBoss.RAIO_BARRAGEM, RagnarokBoss.DANO_BARRAGEM);
        }
        if (golpe == RagnarokBoss.SOPRO) {
            if (motor.noAviso()) {
                // A mira acompanha o alvo e trava no fim do aviso, quando as duas beiradas da varredura aparecem.
                if (!miraTravada && motor.tempoNoGolpe() >= golpe.aviso() - RagnarokBoss.SOPRO_TRAVA) {
                    miraTravada = true;
                    encarar(alvo.position().subtract(position()));
                    sentido = random.nextBoolean() ? 1 : -1;
                    int ticks = RagnarokBoss.SOPRO_TRAVA + golpe.janela();
                    visual("toji_linha", position(), girar(direcao, RagnarokBoss.VARREDURA / 2), (float) RagnarokBoss.ALCANCE_SOPRO, ticks);
                    visual("toji_linha", position(), girar(direcao, -RagnarokBoss.VARREDURA / 2), (float) RagnarokBoss.ALCANCE_SOPRO, ticks);
                }
                if (!miraTravada) encarar(alvo.position().subtract(position()));
            } else if (naJanela) {
                soprar(tj);
            }
        }
        if (golpe == RagnarokBoss.PISAO && naJanela) onda(tj);
        encarar(direcao);
    }

    private void abrirJanela(Golpe golpe) {
        if (golpe == RagnarokBoss.SOCO) socar(RagnarokBoss.RAIO_SOCO, RagnarokBoss.DANO_SOCO);
        else if (golpe == RagnarokBoss.BARRAGEM) socar(RagnarokBoss.RAIO_BARRAGEM, RagnarokBoss.DANO_BARRAGEM);
        else if (golpe == RagnarokBoss.RAJADA) lancarRajada();
        else if (golpe == RagnarokBoss.INVESTIDA) som(SonsRagnarok.INVESTIDA, 4f);
        else if (golpe == RagnarokBoss.SOPRO) {
            som(SonsRagnarok.SOPRO, 5f);
            // O cliente refaz a varredura com as mesmas contas: só precisa da mira travada e do sentido.
            visual("ragnarok_sopro", position(), direcao, sentido, golpe.janela());
        } else if (golpe == RagnarokBoss.PISAO) {
            som(SonsRagnarok.PISAO, 4f);
            visual("ragnarok_onda", position(), direcao, (float) RagnarokBoss.RAIO_PISAO, golpe.janela());
            particulas(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + .2, getZ(), 12, 1.5, .1, 1.5, .03);
        }
    }

    // --- Os golpes ---

    /** O murro: fere todos no círculo marcado. A cratera é só desenho: nenhum bloco quebra. */
    private void socar(double raio, float dano) {
        if (pontoDoGolpe == null) return;
        Vec3 centro = pontoDoGolpe;
        som(SonsRagnarok.SOCO, 4f);
        visual("ragnarok_cratera", centro, direcao, (float) raio, 30);
        particulas(ParticleTypes.CAMPFIRE_COSY_SMOKE, centro.x, centro.y + .3, centro.z, 6, raio * .4, .2, raio * .4, .02);
        for (LivingEntity e : candidatos(new AABB(centro, centro).inflate(raio + 1, 3, raio + 1))) {
            Vec3 d = e.position().subtract(centro);
            if (Math.abs(d.y) > 3 || d.horizontalDistance() > raio + e.getBbWidth() / 2) continue;
            if (ferir(e, dano)) empurrar(e, .9, direcao);
        }
    }

    /** Cinco projéteis de magia em leque, rentes ao chão, na altura do peito de um jogador. */
    private void lancarRajada() {
        som(SonsRagnarok.RAJADA, 4f);
        for (int i = 0; i < RagnarokBoss.PROJETEIS; i++) {
            Vec3 rumo = girar(direcao, RagnarokBoss.desvioDaRajada(i));
            Vec3 de = position().add(0, 1.2, 0).add(rumo.scale(RagnarokBoss.LARGURA / 2 + .5));
            projeteis.add(new Projetil(de, rumo));
            // O cliente desenha o voo inteiro: até a primeira parede ou o alcance.
            Vec3 bloco = blocoEntre(de, de.add(rumo.scale(RagnarokBoss.ALCANCE_RAJADA)));
            double ate = bloco == null ? RagnarokBoss.ALCANCE_RAJADA : bloco.distanceTo(de);
            visual("ragnarok_projetil", de, rumo, (float) ate, (int) Math.ceil(ate / RagnarokBoss.PASSO_RAJADA) + 7);
        }
    }

    private void tickProjeteis() {
        // Sobre uma cópia: ferir pode encerrar a luta, que limpa a lista.
        for (Projetil p : List.copyOf(projeteis)) {
            if (!projeteis.contains(p)) break;
            double passo = Math.min(RagnarokBoss.PASSO_RAJADA, RagnarokBoss.ALCANCE_RAJADA - p.andou);
            Vec3 proximo = p.ponto.add(p.rumo.scale(passo));
            Vec3 bloco = blocoEntre(p.ponto, proximo);
            for (LivingEntity e : naLinha(p.ponto, bloco != null ? bloco : proximo, RagnarokBoss.RAIO_RAJADA)) {
                if (p.feridos.add(e.getUUID()) && ferir(e, RagnarokBoss.DANO_RAJADA)) empurrar(e, .4, p.rumo);
            }
            if (bloco != null) {
                projeteis.remove(p);
                continue;
            }
            p.ponto = proximo;
            p.andou += passo;
            if (p.andou >= RagnarokBoss.ALCANCE_RAJADA - 1e-9) projeteis.remove(p);
        }
    }

    /** A investida: sobe onde está e marca a linha que vai atravessar, até a primeira parede. */
    private void decolar(Golpe golpe) {
        saida = position();
        comprimento = RagnarokBoss.COMPRIMENTO_INVESTIDA;
        // O corpo inteiro tem de passar: três raios (o meio e as duas beiradas) em duas alturas.
        Vec3 lado = new Vec3(-direcao.z, 0, direcao.x).scale(RagnarokBoss.LARGURA / 2);
        for (double altura : new double[] {1.5, RagnarokBoss.ALTURA - 1}) {
            for (int k = -1; k <= 1; k++) {
                Vec3 de = saida.add(0, altura, 0).add(lado.scale(k));
                Vec3 bloco = blocoEntre(de, de.add(direcao.scale(RagnarokBoss.COMPRIMENTO_INVESTIDA)));
                if (bloco != null) comprimento = Math.min(comprimento, Math.max(0, bloco.distanceTo(de) - RagnarokBoss.LARGURA));
            }
        }
        // Dentro da arena a linha acaba antes da beirada da caixa, que não tem parede.
        if (com.minerwk.arena.ArenasServidor.naArena(this)) {
            CaixaArena caixa = CaixaArena.daPosicao(getX(), getZ());
            comprimento = Math.min(comprimento, RagnarokBoss.ateABeirada(saida.x - caixa.centroX() - .5, saida.z - caixa.centroZ() - .5,
                    direcao.x, direcao.z, CaixaArena.MEIA - RagnarokBoss.LARGURA));
        }
        antes = saida;
        setNoGravity(true);
        estado(VOANDO, true);
        som(SonsRagnarok.ASAS, 4f);
        visual("toji_linha", saida, direcao, (float) comprimento, golpe.aviso() + golpe.janela());
    }

    /** Um tick da investida: sobe no aviso, atravessa a linha rasante na janela (ferindo cada um uma vez) e pousa. */
    private void voar(int tj) {
        double avanco = tj < 0 ? (motor.noAviso() ? 0 : 1) : RagnarokBoss.avancoDaInvestida(tj);
        Vec3 chao = saida.add(direcao.scale(comprimento * avanco));
        Vec3 ponto = chao.add(0, RagnarokBoss.alturaNaInvestida(motor.tempoNoGolpe()), 0);
        setDeltaMovement(Vec3.ZERO);
        teleportTo(ponto.x, ponto.y, ponto.z);
        encarar(direcao);
        if (tj >= 0) {
            for (LivingEntity e : naLinha(antes.add(0, 1, 0), chao.add(0, 1, 0), RagnarokBoss.RAIO_INVESTIDA)) {
                if (atingidos.add(e.getUUID()) && ferir(e, RagnarokBoss.DANO_INVESTIDA)) empurrar(e, 1.2, direcao);
            }
            particulas(ParticleTypes.FLAME, ponto.x, ponto.y + 2, ponto.z, 4, 1.2, 1.5, 1.2, .03);
        }
        antes = chao;
    }

    private void pousar() {
        setNoGravity(false);
        estado(VOANDO, false);
    }

    /** Um tick do Sopro: o feixe sai da boca, bate no chão à frente dele e corre rente a ele, varrendo de lado. */
    private void soprar(int tj) {
        Vec3 rumo = girar(direcao, RagnarokBoss.desvioDoSopro(tj, sentido));
        Vec3 de = position().add(0, 1, 0).add(rumo.scale(RagnarokBoss.LARGURA / 2 + 1));
        Vec3 fim = position().add(0, 1, 0).add(rumo.scale(RagnarokBoss.ALCANCE_SOPRO));
        Vec3 bloco = blocoEntre(de, fim);
        if (bloco != null) fim = bloco;
        double longe = fim.distanceTo(de);
        // O feixe é desenhado no cliente; aqui só a fumaça de onde ele bate.
        if (tj % 3 == 0) particulas(ParticleTypes.LARGE_SMOKE, fim.x, fim.y, fim.z, 2, .4, .2, .4, .02);
        if (longe < .5) return;
        for (LivingEntity e : naLinha(de, fim, RagnarokBoss.RAIO_SOPRO)) {
            if (atingidos.add(e.getUUID()) && ferir(e, RagnarokBoss.DANO_SOPRO)) empurrar(e, 1.0, rumo);
        }
    }

    /** Um tick do pisão: a onda abre em aro pelo chão e fere quem não pulou. */
    private void onda(int tj) {
        for (LivingEntity e : candidatos(getBoundingBox().inflate(RagnarokBoss.RAIO_PISAO + 2, 2, RagnarokBoss.RAIO_PISAO + 2))) {
            Vec3 d = e.position().subtract(position());
            if (!RagnarokBoss.naOnda(tj, d.horizontalDistance(), d.y)) continue;
            Vec3 fora = horizontal(d);
            if (atingidos.add(e.getUUID()) && ferir(e, RagnarokBoss.DANO_PISAO)) empurrar(e, .8, fora != null ? fora : direcao);
        }
    }

    /** O corpo em chamas: quem fica colado nele queima, sem empurrão e sem zerar a invulnerabilidade. */
    private void calor() {
        double r = RagnarokBoss.raioDoCalor(fase) + RagnarokBoss.LARGURA / 2 + 1;
        for (LivingEntity e : candidatos(getBoundingBox().inflate(r, 2, r))) {
            Vec3 d = e.position().subtract(position());
            if (!RagnarokBoss.noCalor(d.horizontalDistance(), d.y, fase)) continue;
            // Com ele como autor (a mensagem de morte tem o nome dele), mas sem zerar a invulnerabilidade.
            e.hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DANO), this),
                    poder.danoDoBoss(RagnarokBoss.danoDoCalor(fase)));
        }
    }

    // --- Ferir e procurar ---

    private void empurrar(LivingEntity e, double forca, Vec3 rumo) {
        e.knockback(forca, -rumo.x, -rumo.z);
        e.hurtMarked = true;
    }

    /** Os multiplicadores do balanceamento, lidos ao começar a luta. */
    private com.minerwk.core.kit.Poder poder = com.minerwk.ConfigServidor.poder();

    private boolean ferir(LivingEntity alvo, float dano) {
        // Cada golpe dele vale: os socos da barragem saem quase no tempo de invulnerabilidade do anterior.
        alvo.invulnerableTime = 0;
        return alvo.hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DANO), this), poder.danoDoBoss(dano));
    }

    /** Onde o segmento bate num bloco sólido, ou null. */
    private Vec3 blocoEntre(Vec3 de, Vec3 ate) {
        HitResult r = level().clip(new ClipContext(de, ate, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return r.getType() == HitResult.Type.MISS ? null : r.getLocation();
    }

    /** Os vivos a até {@code raio} do segmento. */
    private List<LivingEntity> naLinha(Vec3 de, Vec3 ate, double raio) {
        List<LivingEntity> perto = candidatos(new AABB(de, ate).inflate(raio + 1));
        perto.removeIf(e -> distanciaAoSegmento(e.getBoundingBox().getCenter(), de, ate) > raio + e.getBbWidth() / 2);
        return perto;
    }

    /** Os vivos na caixa (menos outros iguais), mais quem invocou: o jogador dos GameTests não está no mundo. */
    private List<LivingEntity> candidatos(AABB caixa) {
        List<LivingEntity> perto = new ArrayList<>(level().getEntitiesOfClass(LivingEntity.class, caixa,
                e -> e != this && e.isAlive() && !(e instanceof RagnarokEntity)));
        if (invocador != null && invocador.isAlive() && !perto.contains(invocador)
                && invocador.getBoundingBox().intersects(caixa)) perto.add(invocador);
        return perto;
    }

    private static double distanciaAoSegmento(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = ab.lengthSqr() < 1e-9 ? 0 : Mth.clamp(p.subtract(a).dot(ab) / ab.lengthSqr(), 0, 1);
        return p.distanceTo(a.add(ab.scale(t)));
    }

    // --- A asa ---

    /** Abaixo de 50% de vida: o Shadow corta uma asa. Ele não voa mais, bate mais rápido e queima mais longe. */
    private void perderAAsa(ServerPlayer dono, long agora) {
        fase = 2;
        motor.acelerar(RagnarokBoss.ritmo(2));
        motor.trocarGolpes(RagnarokBoss.golpes(2));
        paradoAte = agora + RagnarokBoss.TROCA_DE_FASE;
        motor.parar(paradoAte);
        asaCaiEm = agora + RagnarokBoss.ASA_CAI;
        getNavigation().stop();
        triggerAnim(CONTROLE, "asa");
        corteEm = agora + CutsceneAsa.CORTE;
        urroEm = agora + CutsceneAsa.URRO;
        // Quem invocou assiste: a câmera presa aos olhos dele, virada para quem ele encara.
        if (dono.connection != null) {
            Envio.para(dono, new Cutscene(CutsceneAsa.ID, getId(), vetor(getEyePosition()), vetor(direcao), agora));
        }
        barra.setColor(BossEvent.BossBarColor.PURPLE);
        avisar(dono, "mensagem.minerwk.ragnarok.asa");
    }

    /** Um ponto em volta dos pés dele: (lado, cima, frente), na base da {@link CutsceneAsa} (lado negativo: a asa esquerda). */
    private Vec3 naBase(double lado, double cima, double frente) {
        Vec3 l = new Vec3(-direcao.z, 0, direcao.x);
        return position().add(l.scale(lado)).add(0, cima, 0).add(direcao.scale(frente));
    }

    /** O Shadow passa: o risco preto atravessa a raiz da asa esquerda e deixa o vulto dele no ar. */
    private void cortarAAsa() {
        som(SonsRagnarok.CORTE, 4f);
        Vec3 raiz = naBase(CutsceneAsa.ASA.x() * .5, CutsceneAsa.ASA.y() + .3, CutsceneAsa.ASA.z());
        // Para fora, do lado da asa.
        Vec3 l = new Vec3(-direcao.z, 0, direcao.x).scale(Math.signum(CutsceneAsa.ASA.x()));
        Vec3 risco = new Vec3(0, 1, 0).add(l.scale(.35)).add(direcao.scale(.25)).normalize();
        visual("ragnarok_corte", raiz, risco, 1f, 16);
        // O vulto do Pós-imagem do Cid, de onde o corte começou: é ele quem passou.
        visual("cid_pos_imagem", raiz.subtract(risco.scale(4.5)).add(l.scale(1.2)).add(0, 1.62, 0), risco, 0f, 30); // o vulto espera a altura dos olhos
        particulas(ParticleTypes.DAMAGE_INDICATOR, raiz.x, raiz.y, raiz.z, 10, .4, .8, .4, .1);
    }

    /** A chegada: o rugido, as chamas e a fala. */
    private void entrar(ServerPlayer dono) {
        entrou = true;
        triggerAnim(CONTROLE, "entrada");
        som(SonsRagnarok.ENTRADA, 6f);
        particulas(ParticleTypes.FLAME, getX(), getY() + 3, getZ(), 120, 1.5, 3, 1.5, .08);
        if (dono.connection != null) {
            dono.displayClientMessage(Component.translatable("mensagem.minerwk.ragnarok.fala_entrada"), false);
        }
    }

    // --- O fim ---

    /** A luta acabou sem vitória: ele volta pelo portal. */
    private void sumir() {
        som(SonsRagnarok.RUGIDO, 4f);
        particulas(ParticleTypes.LARGE_SMOKE, getX(), getY() + 3, getZ(), 80, 1.5, 3, 1.5, .03);
        ServerPlayer dono = jogadorAtual();
        if (dono != null) avisar(dono, "mensagem.minerwk.ragnarok.sumiu");
        discard();
    }

    /** Quem invocou como está agora no servidor (depois de morrer ele é outro objeto), ou o que a luta guardou. */
    private ServerPlayer jogadorAtual() {
        if (invocadorId == null) return null;
        ServerPlayer p = getServer() == null ? null : getServer().getPlayerList().getPlayer(invocadorId);
        return p != null ? p : invocador;
    }

    @Override
    public void die(DamageSource fonte) {
        super.die(fonte);
        if (level().isClientSide || venceu) return;
        venceu = true;
        barra.setProgress(0);
        projeteis.clear();
        pousar();
        corteEm = urroEm = 0;
        som(SonsRagnarok.MORTE, 6f);
        ServerPlayer dono = jogadorAtual();
        if (dono == null) return;
        // Venceu: a Espada Atômica, o I Am Atomic e o Recovery Atomic são dele, e o resto da árvore abre.
        ServicoKits.definirDespertou(dono, true);
        ServicoKits.ganharXp(dono, RagnarokBoss.XP_VITORIA);
        avisar(dono, "mensagem.minerwk.ragnarok.venceu");
    }

    @Override
    public void remove(RemovalReason motivo) {
        super.remove(motivo);
        encerrarLuta();
    }

    /** Também quando o chunk descarrega, que não passa por {@link #remove}. */
    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        encerrarLuta();
    }

    /** As correntes do Cid não seguram um demônio de sete blocos. */
    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance efeito) {
        return !efeito.is(Efeitos.PARALISIA) && super.canBeAffected(efeito);
    }

    private void encerrarLuta() {
        if (level().isClientSide || lutaEncerrada) return;
        lutaEncerrada = true;
        barra.removeAllPlayers();
        LutasRagnarok.esquecer(this);
        if (invocadorId != null && getServer() != null) {
            com.minerwk.arena.ArenasServidor.lutaAcabou(getServer(), invocadorId, venceu);
        }
    }

    @Override
    protected void tickDeath() {
        if (++deathTime >= TICKS_DA_MORTE && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    // --- Regras da entidade ---

    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fonte) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distancia) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer jogador) {
        super.startSeenByPlayer(jogador);
        barra.addPlayer(jogador);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer jogador) {
        super.stopSeenByPlayer(jogador);
        barra.removePlayer(jogador);
    }

    private void particulas(ParticleOptions tipo, double x, double y, double z, int quantidade, double dx, double dy,
                            double dz, double velocidade) {
        if (level() instanceof ServerLevel nivel) nivel.sendParticles(tipo, x, y, z, quantidade, dx, dy, dz, velocidade);
    }

    /** Um som dele ({@link SonsRagnarok}); o volume acima de 1 só aumenta o alcance. */
    private void som(String id, float volume) {
        level().playSound(null, getX(), getY(), getZ(), Sons.doRagnarok(id), SoundSource.HOSTILE, volume, 1f);
    }

    private static Vetor vetor(Vec3 v) {
        return new Vetor(v.x, v.y, v.z);
    }

    /** Manda um visual de golpe, preso a ele, a quem o está vendo. */
    private void visual(String tipo, Vec3 origem, Vec3 rumo, float forca, int duracao) {
        enviar(new MostrarVisual(getId(), tipo, vetor(origem), vetor(rumo), forca, duracao, level().getGameTime()));
    }

    private void enviar(CustomPacketPayload pacote) {
        if (!(level() instanceof ServerLevel nivel)) return;
        for (ServerPlayer p : nivel.getChunkSource().chunkMap.getPlayers(chunkPosition(), false)) Envio.para(p, pacote);
    }

    private static void avisar(ServerPlayer jogador, String chave) {
        // Jogador sem conexão (GameTest) não recebe mensagem.
        if (jogador.connection != null) jogador.displayClientMessage(Component.translatable(chave), true);
    }

    // --- Animação (GeckoLib) ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
        AnimationController<RagnarokEntity> principal = new AnimationController<>(this, CONTROLE, 4, estado -> {
            if (isDeadOrDying()) return estado.setAndContinue(MORTE);
            return estado.setAndContinue(estado.isMoving() ? ANDANDO : PARADO);
        });
        for (String nome : List.of("soco", "barragem", "rajada", "investida", "sopro", "pisao", "asa", "entrada")) {
            principal.triggerableAnim(nome, RawAnimation.begin().thenPlay(nome));
        }
        controladores.add(principal);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
