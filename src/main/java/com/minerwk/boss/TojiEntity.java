package com.minerwk.boss;

import com.minerwk.MineRWK;
import com.minerwk.core.boss.CutsceneDespertar;
import com.minerwk.core.boss.Golpe;
import com.minerwk.core.boss.MotorBoss;
import com.minerwk.core.boss.SonsToji;
import com.minerwk.core.boss.TojiBoss;
import com.minerwk.core.kit.Vetor;
import com.minerwk.kit.ProtecoesServidor;
import com.minerwk.kit.ServicoKits;
import com.minerwk.rede.Cutscene;
import com.minerwk.rede.Envio;
import com.minerwk.rede.MostrarVisual;
import com.minerwk.rede.TocarApresentacao;
import com.minerwk.registro.Efeitos;
import com.minerwk.registro.Sons;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * O Toji como boss, Nêmesis do Gojo. A escolha e os tempos dos golpes vêm do {@link MotorBoss}; aqui fica
 * o que cada golpe faz no mundo, o despertar de quem o invocou no meio da luta e o fim dela. Não é salvo
 * em disco: a luta acaba se o mundo fechar.
 */
public final class TojiEntity extends Monster implements GeoEntity {
    /** Dano que não muda com a dificuldade do mundo (os números do kit do jogador também não mudam). */
    public static final ResourceKey<DamageType> DANO = ResourceKey.create(Registries.DAMAGE_TYPE, MineRWK.id("toji"));
    private static final String CONTROLE = "principal";
    /**
     * Invisível no Desaparecer. Não é a invisibilidade comum do Minecraft: essa é recalculada pelos efeitos
     * de poção e o faria reaparecer no meio do golpe.
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> SUMIDO =
            net.minecraft.network.syncher.SynchedEntityData.defineId(TojiEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final RawAnimation PARADO = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANDANDO = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation MORTE = RawAnimation.begin().thenPlayAndHold("morte");
    /** A animação "morte" dura 1,5 s. */
    private static final int TICKS_DA_MORTE = 30;
    /** Quem bate no Toji sem ser quem invocou vira alvo por este tempo. */

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent barra = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_12);
    private final MotorBoss motor = new MotorBoss(TojiBoss.GOLPES, TojiBoss.INTERVALO);
    private UUID invocadorId;
    private ServerPlayer invocador;
    private int ticksDeLuta;
    private int fase = 1;
    /** Quem invocou já despertou (nesta luta ou antes dela): não há mais queda. */
    private boolean despertarFeito;
    /** A Lança em andamento é a que derruba quem invocou. */
    private boolean lancaDoDespertar;
    /** Tick de jogo em que quem invocou se levanta; -1 se não está caído. */
    private long quedaAte = -1;
    /** Tick de jogo em que a cutscene do despertar acaba sozinha (0 sem cutscene). */
    private long quedaAteDaCutscene;
    private boolean venceu;
    private boolean lutaEncerrada;
    private Vec3 direcao = new Vec3(0, 0, 1);
    private final Set<UUID> atingidos = new HashSet<>();
    private boolean puxando;
    /** Tick de jogo até o qual ele não age: a entrada, a troca de fase e o fim da cutscene do despertar. */
    private long paradoAte;
    private boolean entrou;
    /** Ao acabar a troca para a fase 3, emenda a Execução do Assassino. */
    private boolean execucaoPendente;
    /** Onde o golpe do Desaparecer cai (null até ele surgir). */
    private Vec3 pontoDoGolpe;
    /** A frente dele quando a Execução começou: os ângulos das investidas contam a partir dela. */
    private Vec3 baseDaExecucao = new Vec3(0, 0, 1);

    public TojiEntity(EntityType<? extends TojiEntity> tipo, Level nivel) {
        super(tipo, nivel);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder atributos() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, TojiBoss.VIDA)
                .add(Attributes.MOVEMENT_SPEED, .32)
                .add(Attributes.FOLLOW_RANGE, TojiBoss.ALCANCE_DE_VISAO)
                .add(Attributes.KNOCKBACK_RESISTANCE, .8)
                .add(Attributes.ATTACK_DAMAGE, TojiBoss.DANO_SOCO);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder dados) {
        super.defineSynchedData(dados);
        dados.define(SUMIDO, false);
    }

    private void sumido(boolean valor) {
        entityData.set(SUMIDO, valor);
    }

    @Override
    public boolean isInvisible() {
        return entityData.get(SUMIDO) || super.isInvisible();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    // --- A luta ---

    /** Começa a luta contra {@code dono}. Quem já despertou de vez pula a queda e enfrenta a fase 2. */
    public void comecarLuta(ServerPlayer dono) {
        invocadorId = dono.getUUID();
        invocador = dono;
        LutasToji.registrar(this);
        if (ServicoKits.desperto(dono)) {
            despertarFeito = true;
            passarParaFase(2);
        }
        // A entrada: ele chega, fala e só então ataca.
        paradoAte = level().getGameTime() + TojiBoss.ENTRADA;
        motor.parar(paradoAte);
    }

    public UUID invocadorId() {
        return invocadorId;
    }

    public int fase() {
        return fase;
    }

    /** O golpe em andamento, ou null. */
    public Golpe golpe() {
        return motor.atual();
    }

    /** Começa {@code golpe} agora contra quem invocou, sem olhar alcance nem recarga (testes e prévia). */
    public void forcar(Golpe golpe) {
        // Com quem invocou caído a luta está parada: o golpe nunca andaria.
        if (invocador == null || quedaAte >= 0) return;
        paradoAte = 0;
        execucaoPendente = false;
        lancaDoDespertar = false;
        motor.forcar(golpe, level().getGameTime());
        comecar(golpe, invocador);
    }

    /** Se um golpe mortal em {@code jogador} deve virar o despertar em vez de matar. */
    boolean podeDerrubar(ServerPlayer jogador) {
        return !despertarFeito && quedaAte < 0 && isAlive() && jogador.getUUID().equals(invocadorId);
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
        if (TojiBoss.fim(presente, ticksDeLuta) != TojiBoss.Fim.CONTINUA) {
            sumir();
            return;
        }
        long agora = level().getGameTime();
        barra.setProgress(getHealth() / getMaxHealth());
        if (quedaAte >= 0) {
            getNavigation().stop();
            encarar(dono.position().subtract(position()));
            if (agora >= quedaAte) levantar(dono);
            return;
        }
        // No segundo tick quem está perto já recebeu a entidade e vê a animação.
        if (!entrou && ticksDeLuta >= 2) entrar(dono);
        if (agora < paradoAte) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            encarar(dono.position().subtract(position()));
            return;
        }
        if (execucaoPendente) {
            execucaoPendente = false;
            motor.forcar(TojiBoss.EXECUCAO, agora);
            comecar(TojiBoss.EXECUCAO, dono);
        } else if (fase == 2 && motor.atual() == null && TojiBoss.fase(true, getHealth() / getMaxHealth()) == 3) {
            passarParaFase3(dono, agora);
            return;
        }
        if (!despertarFeito && !lancaDoDespertar && TojiBoss.horaDeDespertar(getHealth() / getMaxHealth(), ticksDeLuta)) {
            comecarLancaDoDespertar(dono, agora);
        }
        LivingEntity alvo = alvo(dono, agora);
        double distancia = distanceTo(alvo);
        Golpe antes = motor.atual();
        MotorBoss.Evento evento = motor.tick(distancia, agora, random.nextDouble());
        Golpe golpe = motor.atual() != null ? motor.atual() : antes;
        switch (evento) {
            case COMECOU -> comecar(golpe, alvo);
            case ABRIU_JANELA -> abrirJanela(golpe, alvo, dono);
            case FECHOU_JANELA -> fecharJanela(golpe, alvo);
            default -> {
            }
        }
        if (motor.atual() != null) duranteGolpe(motor.atual(), alvo);
        else perseguir(alvo, distancia);
    }

    /** Sempre quem invocou: a luta é solo, só ele entra na arena. */
    private LivingEntity alvo(ServerPlayer dono, long agora) {
        return dono;
    }

    private void perseguir(LivingEntity alvo, double distancia) {
        getLookControl().setLookAt(alvo, 30, 30);
        if (distancia <= 3) getNavigation().stop();
        else if (tickCount % 5 == 0 || getNavigation().isDone()) getNavigation().moveTo(alvo, 1.15);
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

    private Vec3 peito() {
        return position().add(0, getBbHeight() * .6, 0);
    }

    private static boolean nuvem(Golpe golpe) {
        return golpe == TojiBoss.NUVEM || golpe == TojiBoss.NUVEM_5;
    }

    private void comecar(Golpe golpe, LivingEntity alvo) {
        getNavigation().stop();
        atingidos.clear();
        puxando = false;
        pontoDoGolpe = null;
        sumido(false);
        encarar(alvo.position().subtract(position()));
        triggerAnim(CONTROLE, golpe.animacao());
        // Na fase 3 os golpes perdem o aviso sonoro: sobra a marca no chão.
        if (fase < 3) tocar(SonsToji.AVISO, 1.2f, 1f);
        int ate = golpe.aviso() + golpe.janela();
        if (golpe == TojiBoss.INVESTIDA) {
            visual("toji_linha", getId(), position(), direcao, (float) TojiBoss.ALCANCE_INVESTIDA, ate);
        } else if (nuvem(golpe)) {
            visual("toji_setor", getId(), position(), direcao, (float) TojiBoss.RAIO_NUVEM, ate);
        } else if (golpe == TojiBoss.CORRENTE) {
            corrente(alvo, golpe.aviso());
        } else if (golpe == TojiBoss.DESAPARECER) {
            tocar(SonsToji.SUMIR, 1.4f, 1f);
            particulas(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 16, .3, .7, .3, .02);
            sumido(true);
        } else if (golpe == TojiBoss.EXECUCAO) {
            baseDaExecucao = direcao;
            // A marca é o único aviso da Execução, e toca em qualquer fase.
            tocar(SonsToji.MARCA, 1.6f, 1f);
            visual("toji_marca", alvo.getId(), alvo.position(), direcao, 1.2f, ate);
        }
    }

    private void duranteGolpe(Golpe golpe, LivingEntity alvo) {
        boolean naJanela = motor.naJanela();
        int tj = naJanela ? motor.tempoNaJanela() : -1;
        boolean investindo = golpe == TojiBoss.INVESTIDA && naJanela;
        boolean executando = golpe == TojiBoss.EXECUCAO && naJanela && TojiBoss.avancaNaExecucao(tj);
        if (!investindo && !executando) setDeltaMovement(0, getDeltaMovement().y, 0);
        if (golpe == TojiBoss.DESAPARECER) {
            desaparecer(alvo);
            return;
        }
        if (golpe == TojiBoss.EXECUCAO) {
            executar(alvo, tj);
            return;
        }
        if (motor.noAviso()) {
            // A investida e a Nuvem travam a direção no começo (dá para sair); a Lança e a corrente acompanham o alvo.
            if (golpe == TojiBoss.INVESTIDA || nuvem(golpe)) encarar(direcao);
            else encarar(alvo.position().subtract(position()));
            if (golpe == TojiBoss.LANCA) {
                Vec3 ponta = peito().add(direcao.scale(1.6));
                particulas(ParticleTypes.CRIT, ponta.x, ponta.y, ponta.z, 2, .05, .05, .05, .01);
            }
        } else {
            encarar(direcao);
        }
        if (investindo) avancar(TojiBoss.ALCANCE_INVESTIDA / TojiBoss.INVESTIDA.janela(), TojiBoss.DANO_INVESTIDA, 1.2);
        if (nuvem(golpe) && naJanela && TojiBoss.nuvemBate(golpe, tj)) baterNuvem();
        if (golpe == TojiBoss.CORRENTE && naJanela && puxando) puxar(alvo);
    }

    private void abrirJanela(Golpe golpe, LivingEntity alvo, ServerPlayer dono) {
        if (golpe == TojiBoss.LANCA) estocar(dono);
        else if (golpe == TojiBoss.INVESTIDA) tocar(SonsToji.INVESTIDA, 1.4f, 1f);
        else if (golpe == TojiBoss.DESAPARECER) golpearAoSurgir();
        else if (golpe == TojiBoss.CORRENTE) {
            puxando = alvo.distanceTo(this) <= TojiBoss.ALCANCE_CORRENTE && hasLineOfSight(alvo);
            tocar(SonsToji.CORRENTE, 1.6f, 1f);
            corrente(alvo, golpe.janela());
        }
    }

    private void fecharJanela(Golpe golpe, LivingEntity alvo) {
        if (golpe != TojiBoss.CORRENTE || !puxando) return;
        puxando = false;
        if (alvo.distanceTo(this) <= TojiBoss.ALCANCE_SOCO && ferir(alvo, TojiBoss.DANO_SOCO)) {
            alvo.knockback(.9, -direcao.x, -direcao.z);
            alvo.hurtMarked = true;
            tocar(SonsToji.SOCO, 1.2f, 1f);
        }
    }

    /** A corrente esticada do peito dele até o alvo (ou até onde ela alcança). */
    private void corrente(LivingEntity alvo, int duracao) {
        Vec3 ate = alvo.getBoundingBox().getCenter().subtract(peito());
        if (ate.lengthSqr() < 1e-6) return;
        visual("toji_corrente", getId(), peito(), ate.normalize(),
                (float) Math.min(ate.length(), TojiBoss.ALCANCE_CORRENTE), duracao);
    }

    /** Um tick de avanço pela linha travada (Investida Celeste, Execução): cada um no caminho leva o golpe uma vez. */
    private void avancar(double passo, float dano, double empurrao) {
        Vec3 de = position();
        move(MoverType.SELF, direcao.scale(passo));
        Vec3 ate = position();
        particulas(ParticleTypes.CLOUD, ate.x, getY() + .4, ate.z, 6, .3, .3, .3, .02);
        for (LivingEntity e : naLinha(de.add(0, 1, 0), ate.add(0, 1, 0), 1.3)) {
            if (!atingidos.add(e.getUUID())) continue;
            if (ferir(e, dano)) {
                e.knockback(empurrao, -direcao.x, -direcao.z);
                e.hurtMarked = true;
            }
        }
    }

    /** Nuvem Brincalhona: um golpe do bastão em todos no setor à frente. São 3 (ou 5) por uso. */
    private void baterNuvem() {
        tocar(SonsToji.NUVEM, 1.3f, .9f + random.nextFloat() * .2f);
        Vec3 frente = position().add(direcao.scale(1.8));
        particulas(ParticleTypes.SWEEP_ATTACK, frente.x, getY() + 1.1, frente.z, 1, .3, .1, .3, 0);
        for (LivingEntity e : candidatos(getBoundingBox().inflate(TojiBoss.RAIO_NUVEM + 1))) {
            Vec3 d = e.position().subtract(position());
            if (Math.abs(d.y) > 2.5 || paredeAte(peito(), e) || !TojiBoss.noSetor(d.x, d.z, direcao.x, direcao.z,
                    TojiBoss.RAIO_NUVEM + e.getBbWidth() / 2, TojiBoss.ANGULO_NUVEM)) continue;
            // Os golpes vêm a menos de meio segundo um do outro: sem isto só o primeiro feriria.
            e.invulnerableTime = 0;
            if (ferir(e, TojiBoss.DANO_NUVEM)) {
                e.knockback(.3, -direcao.x, -direcao.z);
                e.hurtMarked = true;
            }
        }
    }

    /** Desaparecer: invisível até o tick de surgir; aí aparece atrás do alvo e a sombra marca onde o golpe cai. */
    private void desaparecer(LivingEntity alvo) {
        if (motor.tempoNoGolpe() < TojiBoss.TICK_DE_SURGIR) return;
        if (pontoDoGolpe == null) surgir(alvo);
        encarar(direcao);
    }

    private void surgir(LivingEntity alvo) {
        Vec3 centro = alvo.position();
        Vec3 olhar = horizontal(alvo.getLookAngle());
        if (olhar == null) olhar = horizontal(centro.subtract(position()));
        if (olhar == null) olhar = direcao;
        Vec3 destino = centro.subtract(olhar.scale(TojiBoss.ATRAS_DO_ALVO));
        if (!cabeEm(destino)) destino = centro.add(olhar.scale(TojiBoss.ATRAS_DO_ALVO));
        if (cabeEm(destino)) teleportTo(destino.x, destino.y, destino.z);
        pontoDoGolpe = centro;
        sumido(false);
        encarar(centro.subtract(position()));
        tocar(SonsToji.SURGIR, 1.4f, 1f);
        particulas(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 10, .3, .7, .3, .02);
        visual("toji_sombra", getId(), centro, direcao, (float) TojiBoss.RAIO_SURGIR,
                TojiBoss.DESAPARECER.aviso() - TojiBoss.TICK_DE_SURGIR + TojiBoss.DESAPARECER.janela());
    }

    /** O golpe do Desaparecer: fere todos no círculo marcado pela sombra. */
    private void golpearAoSurgir() {
        // Sem lugar para surgir ele ficou onde estava: de longe o golpe não alcança.
        if (pontoDoGolpe == null || position().distanceTo(pontoDoGolpe) > TojiBoss.RAIO_SURGIR + 2) return;
        tocar(SonsToji.SOCO, 1.4f, .85f);
        Vec3 centro = pontoDoGolpe;
        for (LivingEntity e : candidatos(new AABB(centro, centro).inflate(TojiBoss.RAIO_SURGIR + 1, 2.5, TojiBoss.RAIO_SURGIR + 1))) {
            Vec3 d = e.position().subtract(centro);
            if (Math.abs(d.y) > 2.5 || d.horizontalDistance() > TojiBoss.RAIO_SURGIR + e.getBbWidth() / 2
                    || paredeAte(peito(), e)) continue;
            if (ferir(e, TojiBoss.DANO_SURGIR)) {
                e.knockback(.8, -direcao.x, -direcao.z);
                e.hurtMarked = true;
            }
        }
    }

    /** Execução do Assassino: 4 investidas seguidas, cada uma de um ângulo em volta do alvo marcado. */
    private void executar(LivingEntity alvo, int tj) {
        int i = tj < 0 ? -1 : TojiBoss.investidaDaExecucao(tj);
        if (i < 0) {
            encarar(alvo.position().subtract(position()));
            return;
        }
        int t = TojiBoss.tickDaInvestida(tj);
        if (t == 0) posicionarInvestida(alvo, i);
        encarar(direcao);
        if (t == TojiBoss.AVISO_DA_EXECUCAO) tocar(SonsToji.INVESTIDA, 1.3f, 1.2f);
        if (TojiBoss.avancaNaExecucao(tj)) {
            avancar(TojiBoss.ALCANCE_DA_EXECUCAO / TojiBoss.AVANCO_DA_EXECUCAO, TojiBoss.DANO_EXECUCAO, .6);
        }
    }

    /** Salta para o lado do alvo de onde a investida {@code i} vem e trava a linha, que aparece no chão. */
    private void posicionarInvestida(LivingEntity alvo, int i) {
        atingidos.clear();
        double a = Math.toRadians(TojiBoss.anguloDaExecucao(i));
        Vec3 b = baseDaExecucao;
        Vec3 rumo = new Vec3(b.x * Math.cos(a) - b.z * Math.sin(a), 0, b.x * Math.sin(a) + b.z * Math.cos(a));
        for (double recuo : new double[] {TojiBoss.RECUO_DA_EXECUCAO, 4, 2}) {
            Vec3 p = alvo.position().subtract(rumo.scale(recuo));
            if (!cabeEm(p)) continue;
            particulas(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 8, .3, .6, .3, .02);
            teleportTo(p.x, p.y, p.z);
            break;
        }
        encarar(alvo.position().subtract(position()));
        visual("toji_linha", getId(), position(), direcao, (float) TojiBoss.ALCANCE_DA_EXECUCAO,
                TojiBoss.AVISO_DA_EXECUCAO + TojiBoss.AVANCO_DA_EXECUCAO);
    }

    /** Se ele pode surgir nesse ponto: cabe de pé, fora de água e lava, com chão até 2 blocos abaixo. */
    private boolean cabeEm(Vec3 p) {
        AABB caixa = getType().getDimensions().makeBoundingBox(p.x, p.y, p.z);
        if (!level().noCollision(this, caixa) || level().containsAnyLiquid(caixa)) return false;
        for (double abaixo = .1; abaixo <= 2.1; abaixo += 1) {
            if (level().getBlockState(net.minecraft.core.BlockPos.containing(p.x, p.y - abaixo, p.z)).blocksMotion()) return true;
        }
        return false;
    }

    /** Lança Invertida do Céu: atravessa o Infinito, que cai e fica em recarga, e só então fere. */
    private void estocar(ServerPlayer dono) {
        Vec3 de = peito();
        Vec3 ate = de.add(direcao.scale(TojiBoss.ALCANCE_LANCA));
        tocar(SonsToji.LANCA, 1.5f, 1f);
        visual("toji_estocada", getId(), de, direcao, (float) TojiBoss.ALCANCE_LANCA, 8);
        boolean derrubar = lancaDoDespertar;
        lancaDoDespertar = false;
        for (LivingEntity e : naLinha(de, ate, 1.1)) {
            if (e instanceof ServerPlayer jogador && ProtecoesServidor.desligarInfinito(jogador)) {
                tocar(SonsToji.ANULA, 1.6f, 1f);
            }
            if (derrubar && e == dono) continue;
            ferir(e, TojiBoss.DANO_LANCA);
        }
        // A Lança do despertar sempre acerta quem invocou: é ela que o derruba.
        if (derrubar && podeDerrubar(dono)) derrubar(dono);
    }

    /** Corrente de Mil Milhas: arrasta o alvo até o Toji. O Infinito segura a corrente. */
    private void puxar(LivingEntity alvo) {
        if (alvo instanceof Player jogador && ProtecoesServidor.infinitoAtivo(jogador)) return;
        Vec3 ate = position().subtract(alvo.position());
        if (ate.lengthSqr() < 2.5 * 2.5) return;
        Vec3 passo = ate.normalize().scale(1.3);
        alvo.setDeltaMovement(passo.x, Math.max(passo.y, .12), passo.z);
        alvo.hurtMarked = true;
        alvo.hasImpulse = true;
    }

    private boolean ferir(LivingEntity alvo, float dano) {
        return alvo.hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DANO), this), dano);
    }

    /** Os vivos a até {@code raio} do segmento, mais quem invocou (o jogador dos GameTests não está no mundo). */
    private List<LivingEntity> naLinha(Vec3 de, Vec3 ate, double raio) {
        List<LivingEntity> perto = candidatos(new AABB(de, ate).inflate(raio + 1));
        perto.removeIf(e -> distanciaAoSegmento(e.getBoundingBox().getCenter(), de, ate) > raio + e.getBbWidth() / 2
                || paredeAte(de, e));
        return perto;
    }

    /** Os vivos na caixa (menos outros Tojis), mais quem invocou: o jogador dos GameTests não está no mundo. */
    private List<LivingEntity> candidatos(AABB caixa) {
        List<LivingEntity> perto = new ArrayList<>(level().getEntitiesOfClass(LivingEntity.class, caixa,
                e -> e != this && e.isAlive() && !(e instanceof TojiEntity)));
        if (invocador != null && !perto.contains(invocador)) perto.add(invocador);
        return perto;
    }

    /** Se há bloco sólido entre o começo do golpe e o meio do alvo: golpe não atravessa parede. */
    private boolean paredeAte(Vec3 de, LivingEntity alvo) {
        return level().clip(new net.minecraft.world.level.ClipContext(de, alvo.getBoundingBox().getCenter(),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE,
                this)).getType() != net.minecraft.world.phys.HitResult.Type.MISS;
    }

    private static double distanciaAoSegmento(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = ab.lengthSqr() < 1e-9 ? 0 : Mth.clamp(p.subtract(a).dot(ab) / ab.lengthSqr(), 0, 1);
        return p.distanceTo(a.add(ab.scale(t)));
    }

    // --- O despertar no meio da luta ---

    /** Chega perto de quem invocou e arma a Lança que vai derrubá-lo. */
    private void comecarLancaDoDespertar(ServerPlayer dono, long agora) {
        lancaDoDespertar = true;
        if (distanceTo(dono) > 4) {
            Vec3 lado = horizontal(position().subtract(dono.position()));
            if (lado == null) lado = horizontal(dono.getLookAngle());
            if (lado == null) lado = new Vec3(0, 0, 1);
            // O lado de onde ele vinha; sem lugar ali (parede, vazio, água), os outros três.
            Vec3 perpendicular = new Vec3(-lado.z, 0, lado.x);
            for (Vec3 rumo : new Vec3[] {lado, perpendicular, perpendicular.scale(-1), lado.scale(-1)}) {
                Vec3 p = dono.position().add(rumo.scale(3));
                if (!cabeEm(p)) continue;
                particulas(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 12, .3, .6, .3, .02);
                teleportTo(p.x, p.y, p.z);
                break;
            }
        }
        motor.forcar(TojiBoss.LANCA, agora);
        comecar(TojiBoss.LANCA, dono);
    }

    /** Derruba quem invocou: 3 segundos no chão, sem agir e sem poder morrer. */
    void derrubar(ServerPlayer dono) {
        long agora = level().getGameTime();
        despertarFeito = true;
        lancaDoDespertar = false;
        quedaAte = agora + TojiBoss.QUEDA;
        quedaAteDaCutscene = quedaAte;
        motor.parar(agora);
        sumido(false);
        getNavigation().stop();
        ProtecoesServidor.desligarInfinito(dono);
        LutasToji.CAIDOS.add(dono.getUUID());
        dono.setHealth(1);
        // Caído já conta como paralisado nas regras do kit; o efeito é o que prende as pernas no cliente.
        if (dono.connection != null) {
            dono.addEffect(new MobEffectInstance(Efeitos.PARALISIA, TojiBoss.QUEDA + 5, 0, false, false, false), this);
        }
        dono.setDeltaMovement(Vec3.ZERO);
        dono.hurtMarked = true;
        // A pose forçada do servidor não chega ao próprio jogador: quem o deita é a animação.
        Envio.paraQuemVe(dono, new TocarApresentacao(dono.getId(), CutsceneDespertar.ANIM_QUEDA, "", ""));
        triggerAnim(CONTROLE, "fase");
        particulas(ParticleTypes.DAMAGE_INDICATOR, dono.getX(), dono.getY() + 1, dono.getZ(), 14, .3, .4, .3, .1);
        tocar(SonsToji.QUEDA, 1.6f, 1f);
        avisar(dono, "mensagem.minerwk.toji.caiu");
        // A câmera olha de cima e fecha em quem caiu; a frente da cutscene aponta para o Toji.
        Vec3 para = horizontal(position().subtract(dono.position()));
        Envio.para(dono, new Cutscene(CutsceneDespertar.ID, dono.getId(), vetor(dono.getEyePosition()),
                vetor(para != null ? para : direcao.scale(-1)), agora));
    }

    /** Quem invocou se levanta desperto: vida e energia cheias, recargas zeradas e os golpes trancados liberados. */
    private void levantar(ServerPlayer dono) {
        quedaAte = -1;
        sumido(false);
        soltar(dono);
        dono.setHealth(dono.getMaxHealth());
        ServicoKits.kit(dono).ifPresent(k -> k.recarregar());
        ServicoKits.definirDespertoNaLuta(dono, true);
        passarParaFase(2);
        // Ele espera o fim da cutscene para voltar a atacar.
        paradoAte = level().getGameTime() + CutsceneDespertar.DEPOIS;
        motor.parar(paradoAte);
        particulas(ParticleTypes.END_ROD, dono.getX(), dono.getY() + 1, dono.getZ(), 40, .4, .8, .4, .15);
        tocar(SonsToji.DESPERTAR, 1.6f, 1f);
        avisar(dono, "mensagem.minerwk.toji.despertou");
    }

    /** Tira quem invocou do chão. */
    private void soltar(ServerPlayer dono) {
        // Fora da hora (o Toji morreu ou sumiu durante a queda) a cutscene é cortada.
        if (level().getGameTime() < quedaAteDaCutscene) {
            Envio.para(dono, new Cutscene(Cutscene.FIM, dono.getId(), vetor(dono.getEyePosition()), vetor(direcao),
                    level().getGameTime()));
            Envio.paraQuemVe(dono, new TocarApresentacao(dono.getId(), TocarApresentacao.PARAR, "", ""));
        }
        quedaAteDaCutscene = 0;
        LutasToji.CAIDOS.remove(dono.getUUID());
        dono.removeEffect(Efeitos.PARALISIA);
    }

    private void passarParaFase(int nova) {
        fase = nova;
        motor.acelerar(TojiBoss.ritmo(nova));
        motor.trocarGolpes(TojiBoss.golpes(nova));
    }

    /** Abaixo de 35% de vida: para um instante, os golpes perdem o aviso sonoro e vem a Execução do Assassino. */
    private void passarParaFase3(ServerPlayer dono, long agora) {
        passarParaFase(3);
        paradoAte = agora + TojiBoss.TROCA_DE_FASE;
        motor.parar(paradoAte);
        execucaoPendente = true;
        getNavigation().stop();
        triggerAnim(CONTROLE, "fase");
        tocar(SonsToji.FASE, 1.6f, 1f);
        barra.setColor(BossEvent.BossBarColor.PURPLE);
        avisar(dono, "mensagem.minerwk.toji.fase3");
    }

    /** A chegada: a animação, o impacto no chão e a fala. */
    private void entrar(ServerPlayer dono) {
        entrou = true;
        triggerAnim(CONTROLE, "entrada");
        tocar(SonsToji.ENTRADA, 1.8f, 1f);
        particulas(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + .1, getZ(), 14, .6, .05, .6, .01);
        if (dono.connection != null) {
            dono.displayClientMessage(Component.translatable("mensagem.minerwk.toji.fala_entrada"), false);
        }
    }

    // --- O fim ---

    /** A luta acabou sem vitória: o Toji vai embora. */
    private void sumir() {
        tocar(SonsToji.SUMIR, 1.2f, 1f);
        particulas(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 24, .3, .7, .3, .03);
        ServerPlayer dono = jogadorAtual();
        if (dono != null) avisar(dono, "mensagem.minerwk.toji.sumiu");
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
        sumido(false);
        tocar(SonsToji.MORTE, 1.6f, 1f);
        ServerPlayer dono = jogadorAtual();
        if (dono == null) return;
        if (quedaAte >= 0) {
            // Um ajudante matou o Toji durante a queda: quem invocou se levanta inteiro do mesmo jeito.
            quedaAte = -1;
            soltar(dono);
            dono.setHealth(dono.getMaxHealth());
            ServicoKits.kit(dono).ifPresent(k -> k.recarregar());
        }
        // Venceu: o despertar deixa de ser só desta luta.
        ServicoKits.definirDespertou(dono, true);
        ServicoKits.definirDespertoNaLuta(dono, false);
        ServicoKits.ganharXp(dono, TojiBoss.XP_VITORIA);
        avisar(dono, "mensagem.minerwk.toji.venceu");
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

    /** O Toji saiu do mundo, por qualquer caminho: solta quem estava caído e, sem vitória, tranca os golpes de novo. */
    private void encerrarLuta() {
        if (level().isClientSide || lutaEncerrada) return;
        lutaEncerrada = true;
        barra.removeAllPlayers();
        LutasToji.esquecer(this);
        ServerPlayer dono = jogadorAtual();
        if (dono != null) {
            LutasToji.CAIDOS.remove(dono.getUUID());
            // Quem saiu do jogo já foi limpo na saída; mexer no kit dele o recriaria.
            if (!dono.hasDisconnected()) {
                if (quedaAte >= 0) {
                    quedaAte = -1;
                    soltar(dono);
                }
                if (!venceu) ServicoKits.definirDespertoNaLuta(dono, false);
            }
        }
        // Por último: a arena leva o jogador de volta, e ele já tem de estar solto e com o kit certo.
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
    public boolean hurt(DamageSource fonte, float dano) {
        return super.hurt(fonte, dano);
    }

    /** Sem energia amaldiçoada, o Vazio Infinito não tem o que paralisar. */
    @Override
    public boolean canBeAffected(MobEffectInstance efeito) {
        return !efeito.is(Efeitos.PARALISIA) && super.canBeAffected(efeito);
    }

    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fonte) {
        return false;
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

    private void tocar(String id, float volume, float tom) {
        level().playSound(null, getX(), getY(), getZ(), Sons.doToji(id), SoundSource.HOSTILE, volume, tom);
    }

    private static Vetor vetor(Vec3 v) {
        return new Vetor(v.x, v.y, v.z);
    }

    /** Manda um visual de golpe a quem está vendo o Toji; {@code dono} é a entidade que o visual segue. */
    private void visual(String tipo, int dono, Vec3 origem, Vec3 rumo, float forca, int duracao) {
        if (!(level() instanceof ServerLevel nivel)) return;
        MostrarVisual pacote = new MostrarVisual(dono, tipo, vetor(origem), vetor(rumo), forca, duracao, nivel.getGameTime());
        for (ServerPlayer p : nivel.getChunkSource().chunkMap.getPlayers(chunkPosition(), false)) Envio.para(p, pacote);
    }

    private static void avisar(ServerPlayer jogador, String chave) {
        // Jogador sem conexão (GameTest) não recebe mensagem.
        if (jogador.connection != null) jogador.displayClientMessage(Component.translatable(chave), true);
    }

    // --- Animação (GeckoLib) ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
        AnimationController<TojiEntity> principal = new AnimationController<>(this, CONTROLE, 3, estado -> {
            if (isDeadOrDying()) return estado.setAndContinue(MORTE);
            return estado.setAndContinue(estado.isMoving() ? ANDANDO : PARADO);
        });
        for (String nome : List.of("investida", "estocada", "arremesso", "fase", "giro", "sumir", "execucao", "entrada")) {
            principal.triggerableAnim(nome, RawAnimation.begin().thenPlay(nome));
        }
        controladores.add(principal);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
