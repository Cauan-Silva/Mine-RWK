package com.minerwk.boss;

import com.minerwk.MineRWK;
import com.minerwk.core.boss.Golpe;
import com.minerwk.core.boss.MotorBoss;
import com.minerwk.core.boss.TojiBoss;
import com.minerwk.kit.ProtecoesServidor;
import com.minerwk.kit.ServicoKits;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
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
    private static final RawAnimation PARADO = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANDANDO = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation MORTE = RawAnimation.begin().thenPlayAndHold("morte");
    /** A animação "morte" dura 1,5 s. */
    private static final int TICKS_DA_MORTE = 30;
    /** Quem bate no Toji sem ser quem invocou vira alvo por este tempo. */
    private static final int TICKS_DE_REVIDE = 100;

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
    private boolean venceu;
    private boolean lutaEncerrada;
    private Vec3 direcao = new Vec3(0, 0, 1);
    private final Set<UUID> atingidos = new HashSet<>();
    private boolean puxando;
    private UUID revide;
    private long revideAte;

    public TojiEntity(EntityType<? extends TojiEntity> tipo, Level nivel) {
        super(tipo, nivel);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder atributos() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, TojiBoss.VIDA)
                .add(Attributes.MOVEMENT_SPEED, .32)
                .add(Attributes.FOLLOW_RANGE, TojiBoss.DISTANCIA_MAXIMA)
                .add(Attributes.KNOCKBACK_RESISTANCE, .8)
                .add(Attributes.ATTACK_DAMAGE, TojiBoss.DANO_SOCO);
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
            passarParaFase2();
        }
        // Um respiro antes do primeiro golpe.
        motor.parar(level().getGameTime() + TojiBoss.INTERVALO);
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
        if (invocador == null) return;
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
        if (TojiBoss.fim(presente, presente ? dono.distanceTo(this) : 0, ticksDeLuta) != TojiBoss.Fim.CONTINUA) {
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

    /** Quem invocou, ou por alguns segundos outro jogador que acabou de ferir o Toji. */
    private LivingEntity alvo(ServerPlayer dono, long agora) {
        if (lancaDoDespertar) return dono;
        if (revide != null && agora < revideAte) {
            Player outro = level().getPlayerByUUID(revide);
            if (outro != null && outro.isAlive() && !outro.isSpectator() && outro.distanceTo(this) < 32) return outro;
        }
        revide = null;
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

    private void comecar(Golpe golpe, LivingEntity alvo) {
        getNavigation().stop();
        atingidos.clear();
        puxando = false;
        encarar(alvo.position().subtract(position()));
        triggerAnim(CONTROLE, golpe.animacao());
        som(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.2f, .7f);
    }

    private void duranteGolpe(Golpe golpe, LivingEntity alvo) {
        boolean investindo = golpe == TojiBoss.INVESTIDA && motor.naJanela();
        if (!investindo) setDeltaMovement(0, getDeltaMovement().y, 0);
        if (motor.noAviso()) {
            // A investida trava a linha no começo (dá para sair dela); os outros dois acompanham o alvo.
            if (golpe != TojiBoss.INVESTIDA) encarar(alvo.position().subtract(position()));
            else encarar(direcao);
            avisar(golpe, alvo);
        } else {
            encarar(direcao);
        }
        if (investindo) investir();
        if (golpe == TojiBoss.CORRENTE && motor.naJanela() && puxando) puxar(alvo);
    }

    /** A marca que avisa o golpe: a linha da investida no chão, o brilho na ponta da lança, a corrente esticando. */
    private void avisar(Golpe golpe, LivingEntity alvo) {
        int t = motor.tempoNoGolpe();
        if (golpe == TojiBoss.INVESTIDA && t % 2 == 0) {
            for (double d = 1; d <= TojiBoss.ALCANCE_INVESTIDA; d += 1) {
                Vec3 p = position().add(direcao.scale(d));
                particulas(ParticleTypes.CRIT, p.x, getY() + .15, p.z, 1, .15, 0, .15, 0);
            }
        } else if (golpe == TojiBoss.LANCA) {
            Vec3 ponta = peito().add(direcao.scale(1.6));
            particulas(ParticleTypes.END_ROD, ponta.x, ponta.y, ponta.z, 2, .05, .05, .05, .01);
        } else if (golpe == TojiBoss.CORRENTE && t % 3 == 0) {
            Vec3 ate = alvo.getBoundingBox().getCenter().subtract(peito());
            double alcance = Math.min(ate.length(), TojiBoss.ALCANCE_CORRENTE);
            Vec3 passo = ate.lengthSqr() < 1e-6 ? direcao : ate.normalize();
            for (double d = 1; d <= alcance; d += 1.5) {
                Vec3 p = peito().add(passo.scale(d));
                particulas(ParticleTypes.ENCHANTED_HIT, p.x, p.y, p.z, 1, .05, .05, .05, 0);
            }
        }
    }

    private void abrirJanela(Golpe golpe, LivingEntity alvo, ServerPlayer dono) {
        if (golpe == TojiBoss.LANCA) estocar(dono);
        else if (golpe == TojiBoss.INVESTIDA) som(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, .6f);
        else if (golpe == TojiBoss.CORRENTE) {
            puxando = alvo.distanceTo(this) <= TojiBoss.ALCANCE_CORRENTE && hasLineOfSight(alvo);
            som(SoundEvents.CHAIN_BREAK, 1.6f, .7f);
        }
    }

    private void fecharJanela(Golpe golpe, LivingEntity alvo) {
        if (golpe != TojiBoss.CORRENTE || !puxando) return;
        puxando = false;
        if (alvo.distanceTo(this) <= TojiBoss.ALCANCE_SOCO && ferir(alvo, TojiBoss.DANO_SOCO)) {
            alvo.knockback(.9, -direcao.x, -direcao.z);
            alvo.hurtMarked = true;
            som(SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, .7f);
        }
    }

    /** Investida Celeste: 3 blocos por tick pela linha travada; cada um no caminho leva o golpe uma vez. */
    private void investir() {
        Vec3 de = position();
        move(MoverType.SELF, direcao.scale(TojiBoss.ALCANCE_INVESTIDA / TojiBoss.INVESTIDA.janela()));
        Vec3 ate = position();
        particulas(ParticleTypes.CLOUD, ate.x, getY() + .4, ate.z, 6, .3, .3, .3, .02);
        for (LivingEntity e : naLinha(de.add(0, 1, 0), ate.add(0, 1, 0), 1.3)) {
            if (!atingidos.add(e.getUUID())) continue;
            if (ferir(e, TojiBoss.DANO_INVESTIDA)) {
                e.knockback(1.2, -direcao.x, -direcao.z);
                e.hurtMarked = true;
            }
        }
    }

    /** Lança Invertida do Céu: atravessa o Infinito, que cai e fica em recarga, e só então fere. */
    private void estocar(ServerPlayer dono) {
        Vec3 de = peito();
        Vec3 ate = de.add(direcao.scale(TojiBoss.ALCANCE_LANCA));
        som(SoundEvents.TRIDENT_THROW.value(), 1.5f, .6f);
        for (double d = 1; d <= TojiBoss.ALCANCE_LANCA; d += .75) {
            Vec3 p = de.add(direcao.scale(d));
            particulas(ParticleTypes.CRIT, p.x, p.y, p.z, 2, .05, .05, .05, .02);
        }
        boolean derrubar = lancaDoDespertar;
        lancaDoDespertar = false;
        for (LivingEntity e : naLinha(de, ate, 1.1)) {
            if (e instanceof ServerPlayer jogador) ProtecoesServidor.desligarInfinito(jogador);
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
        List<LivingEntity> perto = new ArrayList<>(level().getEntitiesOfClass(LivingEntity.class,
                new AABB(de, ate).inflate(raio + 1), e -> e != this && e.isAlive() && !(e instanceof TojiEntity)));
        if (invocador != null && !perto.contains(invocador)) perto.add(invocador);
        perto.removeIf(e -> distanciaAoSegmento(e.getBoundingBox().getCenter(), de, ate) > raio + e.getBbWidth() / 2
                || paredeAte(de, e));
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
            Vec3 p = dono.position().add(lado.scale(3));
            particulas(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 12, .3, .6, .3, .02);
            teleportTo(p.x, dono.getY(), p.z);
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
        motor.parar(agora);
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
        dono.setForcedPose(Pose.SWIMMING);
        triggerAnim(CONTROLE, "fase");
        particulas(ParticleTypes.DAMAGE_INDICATOR, dono.getX(), dono.getY() + 1, dono.getZ(), 14, .3, .4, .3, .1);
        som(SoundEvents.PLAYER_HURT, 1.4f, .6f);
        avisar(dono, "mensagem.minerwk.toji.caiu");
    }

    /** Quem invocou se levanta desperto: vida e energia cheias, recargas zeradas e os golpes trancados liberados. */
    private void levantar(ServerPlayer dono) {
        quedaAte = -1;
        soltar(dono);
        dono.setHealth(dono.getMaxHealth());
        ServicoKits.kit(dono).ifPresent(k -> k.recarregar());
        ServicoKits.definirDespertoNaLuta(dono, true);
        passarParaFase2();
        motor.parar(level().getGameTime());
        particulas(ParticleTypes.TOTEM_OF_UNDYING, dono.getX(), dono.getY() + 1, dono.getZ(), 60, .4, .8, .4, .4);
        som(SoundEvents.TOTEM_USE, 1f, 1.3f);
        avisar(dono, "mensagem.minerwk.toji.despertou");
    }

    /** Tira quem invocou do chão. */
    private void soltar(ServerPlayer dono) {
        LutasToji.CAIDOS.remove(dono.getUUID());
        dono.setForcedPose(null);
        dono.removeEffect(Efeitos.PARALISIA);
    }

    private void passarParaFase2() {
        fase = 2;
        motor.acelerar(TojiBoss.RITMO_FASE_2);
    }

    // --- O fim ---

    /** A luta acabou sem vitória: o Toji vai embora. */
    private void sumir() {
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
        if (dono == null) return;
        LutasToji.CAIDOS.remove(dono.getUUID());
        // Quem saiu do jogo já foi limpo na saída; mexer no kit dele o recriaria.
        if (dono.hasDisconnected()) return;
        if (quedaAte >= 0) {
            quedaAte = -1;
            soltar(dono);
        }
        if (!venceu) ServicoKits.definirDespertoNaLuta(dono, false);
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
        if (fonte.getEntity() instanceof ServerPlayer p && invocadorId != null && !p.getUUID().equals(invocadorId)) {
            revide = p.getUUID();
            revideAte = level().getGameTime() + TICKS_DE_REVIDE;
        }
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

    private void som(SoundEvent som, float volume, float tom) {
        level().playSound(null, getX(), getY(), getZ(), som, SoundSource.HOSTILE, volume, tom);
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
        for (String nome : List.of("investida", "estocada", "arremesso", "fase")) {
            principal.triggerableAnim(nome, RawAnimation.begin().thenPlay(nome));
        }
        controladores.add(principal);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
