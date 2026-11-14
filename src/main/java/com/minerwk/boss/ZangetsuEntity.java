package com.minerwk.boss;

import com.minerwk.MineRWK;
import com.minerwk.core.boss.CutsceneMascara;
import com.minerwk.core.boss.Golpe;
import com.minerwk.core.boss.MotorBoss;
import com.minerwk.core.boss.TojiBoss;
import com.minerwk.core.boss.ZangetsuBoss;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.personagens.ichigo.Ichigo;
import com.minerwk.core.personagens.ichigo.SonsIchigo;
import com.minerwk.core.vfx.FormasIchigo;
import com.minerwk.core.vfx.GetsugaIchigo;
import com.minerwk.kit.ContextoServidor;
import com.minerwk.kit.ServicoKits;
import com.minerwk.rede.Cutscene;
import com.minerwk.rede.EncerrarVisual;
import com.minerwk.rede.Envio;
import com.minerwk.rede.MostrarVisual;
import com.minerwk.rede.TocarApresentacao;
import com.minerwk.registro.Efeitos;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
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
 * O Zangetsu interior como boss, Nêmesis do Ichigo: o Ichigo de Bankai branco. A escolha e os tempos dos
 * golpes vêm do {@link MotorBoss}; aqui fica o que cada golpe faz no mundo, a máscara que ele veste abaixo
 * de 40% de vida, a queda de quem o invocou (que se levanta de máscara) e o fim da luta. Os golpes usam os
 * visuais do Ichigo, em branco, e as marcas de chão do Toji. Não é salvo em disco.
 */
public final class ZangetsuEntity extends Monster implements GeoEntity {
    /** Dano que não muda com a dificuldade do mundo. */
    public static final ResourceKey<DamageType> DANO = ResourceKey.create(Registries.DAMAGE_TYPE, MineRWK.id("zangetsu"));
    private static final String CONTROLE = "principal";
    /** O que o cliente precisa saber para desenhá-lo: a máscara no rosto e a espada fora da mão. */
    private static final EntityDataAccessor<Byte> ESTADO = SynchedEntityData.defineId(ZangetsuEntity.class, EntityDataSerializers.BYTE);
    private static final int MASCARA = 1, SEM_ESPADA = 2;
    private static final RawAnimation PARADO = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANDANDO = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation MORTE = RawAnimation.begin().thenPlayAndHold("morte");
    private static final int TICKS_DA_MORTE = 30;
    private static final String GETSUGA = "ichigo_getsuga_branco", ARREMESSO = "ichigo_arremesso_branco";

    /** Uma meia-lua em voo: fere cada um uma vez e para em bloco. Vive além do golpe que a lançou. */
    private static final class MeiaLua {
        final Vec3 rumo;
        final Set<UUID> feridos = new HashSet<>();
        Vec3 ponto;
        double andou;

        MeiaLua(Vec3 ponto, Vec3 rumo) {
            this.ponto = ponto;
            this.rumo = rumo;
        }
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent barra = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.WHITE,
            BossEvent.BossBarOverlay.NOTCHED_10);
    private final MotorBoss motor = new MotorBoss(ZangetsuBoss.GOLPES, ZangetsuBoss.INTERVALO);
    private final List<MeiaLua> meiasLuas = new ArrayList<>();
    private final Set<UUID> atingidos = new HashSet<>();
    private UUID invocadorId;
    private ServerPlayer invocador;
    private int ticksDeLuta;
    private int fase = 1;
    /** Quem invocou já vestiu a máscara (nesta luta ou antes dela): não há mais queda. */
    private boolean despertarFeito;
    /** Tick de jogo em que quem invocou se levanta; -1 se não está caído. */
    private long quedaAte = -1;
    /** Tick de jogo em que a cutscene da queda acaba sozinha (0 sem cutscene). */
    private long quedaAteDaCutscene;
    /** Tick de jogo em que a máscara aparece no rosto dele (0: não está para aparecer). */
    private long mascaraEm;
    private boolean venceu;
    private boolean lutaEncerrada;
    private Vec3 direcao = new Vec3(0, 0, 1);
    /** Tick de jogo até o qual ele não age: a entrada, a máscara e o fim da cutscene da queda. */
    private long paradoAte;
    private boolean entrou;
    /** Onde o corte do Shunpo cai (null até ele surgir). */
    private Vec3 pontoDoGolpe;
    /** De onde a espada arremessada saiu e até onde vai. */
    private Vec3 saidaDaEspada = Vec3.ZERO;
    private double alcanceDaEspada;
    private boolean miraTravada;

    public ZangetsuEntity(EntityType<? extends ZangetsuEntity> tipo, Level nivel) {
        super(tipo, nivel);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder atributos() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, ZangetsuBoss.VIDA)
                .add(Attributes.MOVEMENT_SPEED, .33)
                .add(Attributes.FOLLOW_RANGE, ZangetsuBoss.ALCANCE_DE_VISAO)
                .add(Attributes.KNOCKBACK_RESISTANCE, .8)
                .add(Attributes.ATTACK_DAMAGE, ZangetsuBoss.DANO_CORTE);
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

    /** Se a máscara Hollow está no rosto dele (vale no cliente também). */
    public boolean mascarado() {
        return (entityData.get(ESTADO) & MASCARA) != 0;
    }

    /** Se a espada está fora da mão, no meio do arremesso (vale no cliente também). */
    public boolean semEspada() {
        return (entityData.get(ESTADO) & SEM_ESPADA) != 0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    // --- A luta ---

    /** Começa a luta contra {@code dono}. Quem já despertou de vez não cai no meio dela. */
    public void comecarLuta(ServerPlayer dono) {
        invocadorId = dono.getUUID();
        invocador = dono;
        // Vida e dano pela config ("Balanceamento"), a partir da constante: começar de novo não multiplica duas vezes.
        poder = com.minerwk.kit.PoderServidor.de(dono);
        var vida = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (vida != null) vida.setBaseValue(poder.vidaDoBoss(com.minerwk.core.boss.ZangetsuBoss.VIDA));
        setHealth(getMaxHealth());
        LutasZangetsu.registrar(this);
        despertarFeito = ServicoKits.desperto(dono);
        paradoAte = level().getGameTime() + ZangetsuBoss.CHEGADA + ZangetsuBoss.ENTRADA;
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
        if (invocador == null || quedaAte >= 0) return;
        paradoAte = 0;
        motor.forcar(golpe, level().getGameTime());
        comecar(golpe, invocador);
    }

    /** Se um golpe mortal em {@code jogador} deve virar a queda em vez de matar. */
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
        if (ZangetsuBoss.acabou(presente, ticksDeLuta)) {
            sumir();
            return;
        }
        long agora = level().getGameTime();
        barra.setProgress(getHealth() / getMaxHealth());
        if (mascaraEm > 0 && agora >= mascaraEm) {
            mascaraEm = 0;
            estado(MASCARA, true);
        }
        if (quedaAte >= 0) {
            getNavigation().stop();
            meiasLuas.clear();
            encarar(dono.position().subtract(position()));
            if (agora >= quedaAte) levantar(dono);
            return;
        }
        tickMeiasLuas();
        // No segundo tick quem está perto já recebeu a entidade e vê a animação.
        if (!entrou && ticksDeLuta >= ZangetsuBoss.CHEGADA) entrar(dono);
        if (agora < paradoAte) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            encarar(dono.position().subtract(position()));
            return;
        }
        if (fase == 1 && motor.atual() == null && ZangetsuBoss.fase(getHealth() / getMaxHealth()) == 2) {
            vestirAMascara(dono, agora);
            return;
        }
        double distancia = distanceTo(dono);
        Golpe antes = motor.atual();
        MotorBoss.Evento evento = motor.tick(distancia, agora, random.nextDouble());
        Golpe golpe = motor.atual() != null ? motor.atual() : antes;
        switch (evento) {
            case COMECOU -> comecar(golpe, dono);
            case ABRIU_JANELA -> abrirJanela(golpe);
            case FECHOU_JANELA, TERMINOU -> estado(SEM_ESPADA, false);
            default -> {
            }
        }
        if (motor.atual() != null) duranteGolpe(motor.atual(), dono);
        else perseguir(dono, distancia);
    }

    private void perseguir(LivingEntity alvo, double distancia) {
        getLookControl().setLookAt(alvo, 30, 30);
        if (distancia <= 3) getNavigation().stop();
        else if (tickCount % 5 == 0 || getNavigation().isDone()) {
            getNavigation().moveTo(alvo, fase >= 2 ? ZangetsuBoss.PASSO_DA_MASCARA : ZangetsuBoss.PASSO_NORMAL);
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

    private Vec3 peito() {
        return position().add(0, getBbHeight() * .6, 0);
    }

    private static boolean getsuga(Golpe golpe) {
        return golpe == ZangetsuBoss.GETSUGA || golpe == ZangetsuBoss.GETSUGA_3;
    }

    private void comecar(Golpe golpe, LivingEntity alvo) {
        getNavigation().stop();
        atingidos.clear();
        pontoDoGolpe = null;
        miraTravada = false;
        estado(SEM_ESPADA, false);
        encarar(alvo.position().subtract(position()));
        triggerAnim(CONTROLE, golpe.animacao());
        if (golpe == ZangetsuBoss.CORTE) {
            visual("toji_setor", position(), direcao, (float) ZangetsuBoss.RAIO_CORTE, golpe.aviso() + golpe.janela());
        } else if (getsuga(golpe)) {
            visual("toji_linha", position(), direcao, (float) ZangetsuBoss.ALCANCE_GETSUGA, golpe.aviso() + 2);
        } else if (golpe == ZangetsuBoss.ARREMESSO) {
            visual("toji_linha", position(), direcao, (float) ZangetsuBoss.ALCANCE_ARREMESSO, golpe.aviso() + 2);
        } else if (golpe == ZangetsuBoss.CERO) {
            visual("ichigo_cero_carga", peito(), direcao, 1f, golpe.aviso());
        }
    }

    private void duranteGolpe(Golpe golpe, LivingEntity alvo) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
        boolean naJanela = motor.naJanela();
        int tj = naJanela ? motor.tempoNaJanela() : -1;
        if (golpe == ZangetsuBoss.SHUNPO) {
            if (pontoDoGolpe == null && motor.tempoNoGolpe() >= ZangetsuBoss.TICK_DE_SURGIR) surgir(alvo);
            if (pontoDoGolpe == null) encarar(alvo.position().subtract(position()));
            else encarar(direcao);
            return;
        }
        if (golpe == ZangetsuBoss.CERO && motor.noAviso()) {
            // A mira acompanha o alvo e trava no fim do aviso, quando a linha aparece no chão.
            if (!miraTravada && motor.tempoNoGolpe() >= golpe.aviso() - ZangetsuBoss.CERO_TRAVA) {
                miraTravada = true;
                encarar(alvo.position().subtract(position()));
                visual("toji_linha", position(), direcao, (float) ZangetsuBoss.ALCANCE_CERO,
                        ZangetsuBoss.CERO_TRAVA + golpe.janela());
            }
            if (miraTravada) encarar(direcao);
            else encarar(alvo.position().subtract(position()));
            return;
        }
        if (getsuga(golpe) && naJanela) {
            if (ZangetsuBoss.getsugaMira(golpe, tj)) {
                encarar(alvo.position().subtract(position()));
                visual("toji_linha", position(), direcao, (float) ZangetsuBoss.ALCANCE_GETSUGA, ZangetsuBoss.MIRA_DO_GETSUGA + 2);
            }
            // O primeiro sai quando a janela abre; os outros dois, daqui.
            if (tj > 0 && ZangetsuBoss.getsugaSai(golpe, tj)) lancarGetsuga();
        }
        if (golpe == ZangetsuBoss.ARREMESSO && naJanela) espadaNoAr(tj);
        encarar(direcao);
    }

    private void abrirJanela(Golpe golpe) {
        if (golpe == ZangetsuBoss.CORTE) cortar();
        else if (getsuga(golpe)) lancarGetsuga();
        else if (golpe == ZangetsuBoss.SHUNPO) cortarAoSurgir();
        else if (golpe == ZangetsuBoss.ARREMESSO) arremessar();
        else if (golpe == ZangetsuBoss.CERO) dispararCero();
    }

    // --- Os golpes ---

    /** O rastro da lâmina à frente dele (o mesmo do clique da espada do jogador, que já traz o som). */
    private void rastroDoCorte() {
        visual("ichigo_corte", position().add(0, 1.62, 0), direcao, 1f, FormasIchigo.CORTE);
    }

    /** Corte de perto: todos no setor à frente. */
    private void cortar() {
        rastroDoCorte();
        for (LivingEntity e : candidatos(getBoundingBox().inflate(ZangetsuBoss.RAIO_CORTE + 1))) {
            Vec3 d = e.position().subtract(position());
            if (Math.abs(d.y) > 2.5 || paredeAte(peito(), e) || !TojiBoss.noSetor(d.x, d.z, direcao.x, direcao.z,
                    ZangetsuBoss.RAIO_CORTE + e.getBbWidth() / 2, ZangetsuBoss.ANGULO_CORTE)) continue;
            if (ferir(e, ZangetsuBoss.DANO_CORTE)) empurrar(e, .5);
        }
    }

    /** Solta uma meia-lua branca pela direção travada. */
    private void lancarGetsuga() {
        Vec3 de = peito().add(direcao);
        visual(GETSUGA, de, direcao, 1f, GetsugaIchigo.duracao(true));
        meiasLuas.add(new MeiaLua(de, direcao));
    }

    private void tickMeiasLuas() {
        // Sobre uma cópia: ferir pode derrubar o jogador ou encerrar a luta, e os dois limpam a lista.
        for (MeiaLua m : List.copyOf(meiasLuas)) {
            if (!meiasLuas.contains(m)) break;
            double passo = Math.min(ZangetsuBoss.PASSO_GETSUGA, ZangetsuBoss.ALCANCE_GETSUGA - m.andou);
            Vec3 proximo = m.ponto.add(m.rumo.scale(passo));
            // Quem está entre a meia-lua e a parede ainda é ferido; quem está atrás dela, não.
            Vec3 bloco = blocoEntre(m.ponto, proximo);
            for (LivingEntity e : naLinha(m.ponto, bloco != null ? bloco : proximo, ZangetsuBoss.RAIO_GETSUGA)) {
                if (m.feridos.add(e.getUUID()) && ferir(e, ZangetsuBoss.DANO_GETSUGA)) empurrar(e, .6, m.rumo);
            }
            if (bloco != null) {
                // O cliente para a meia-lua onde o servidor avisa.
                enviar(new EncerrarVisual(getId(), GETSUGA, vetor(m.ponto)));
                meiasLuas.remove(m);
                continue;
            }
            m.ponto = proximo;
            m.andou += passo;
            if (m.andou >= ZangetsuBoss.ALCANCE_GETSUGA - 1e-9) meiasLuas.remove(m);
        }
    }

    /** Shunpo: surge atrás do alvo e a sombra marca onde o corte cai. */
    private void surgir(LivingEntity alvo) {
        Vec3 centro = alvo.position();
        Vec3 olhar = horizontal(alvo.getLookAngle());
        if (olhar == null) olhar = horizontal(centro.subtract(position()));
        if (olhar == null) olhar = direcao;
        Vec3 destino = centro.subtract(olhar.scale(ZangetsuBoss.ATRAS_DO_ALVO));
        if (!cabeEm(destino)) destino = centro.add(olhar.scale(ZangetsuBoss.ATRAS_DO_ALVO));
        Vec3 de = position();
        if (cabeEm(destino)) {
            teleportTo(destino.x, destino.y, destino.z);
            visual("ichigo_shunpo_bankai", de.add(0, 1.62, 0), destino.subtract(de), 1f, FormasIchigo.SHUNPO);
        }
        pontoDoGolpe = centro;
        encarar(centro.subtract(position()));
        visual("toji_sombra", centro, direcao, (float) ZangetsuBoss.RAIO_SHUNPO,
                ZangetsuBoss.SHUNPO.aviso() - ZangetsuBoss.TICK_DE_SURGIR + ZangetsuBoss.SHUNPO.janela());
    }

    /** O corte do Shunpo: fere todos no círculo marcado pela sombra. */
    private void cortarAoSurgir() {
        // Sem lugar para surgir ele ficou onde estava: de longe o corte não alcança.
        if (pontoDoGolpe == null || position().distanceTo(pontoDoGolpe) > ZangetsuBoss.RAIO_SHUNPO + 2) return;
        rastroDoCorte();
        Vec3 centro = pontoDoGolpe;
        double r = ZangetsuBoss.RAIO_SHUNPO;
        for (LivingEntity e : candidatos(new AABB(centro, centro).inflate(r + 1, 2.5, r + 1))) {
            Vec3 d = e.position().subtract(centro);
            if (Math.abs(d.y) > 2.5 || d.horizontalDistance() > r + e.getBbWidth() / 2 || paredeAte(peito(), e)) continue;
            if (ferir(e, ZangetsuBoss.DANO_SHUNPO)) empurrar(e, .8);
        }
    }

    /** A espada sai da mão pela corrente: o cliente refaz o mesmo caminho a partir da mesma saída. */
    private void arremessar() {
        saidaDaEspada = peito();
        Vec3 fim = saidaDaEspada.add(direcao.scale(ZangetsuBoss.ALCANCE_ARREMESSO));
        Vec3 bloco = blocoEntre(saidaDaEspada, fim);
        alcanceDaEspada = FormasIchigo.alcanceDoArremesso(bloco == null ? ZangetsuBoss.ALCANCE_ARREMESSO
                : bloco.distanceTo(saidaDaEspada));
        estado(SEM_ESPADA, true);
        visual(ARREMESSO, saidaDaEspada, direcao, 1f, FormasIchigo.ARREMESSO);
    }

    /** Um tick da espada no ar: fere cada um uma vez, na ida ou na volta. */
    private void espadaNoAr(int tj) {
        Vec3 p = saidaDaEspada.add(direcao.scale(ZangetsuBoss.espadaEm(tj, alcanceDaEspada)));
        double r = ZangetsuBoss.RAIO_ARREMESSO;
        for (LivingEntity e : candidatos(new AABB(p, p).inflate(r + 1))) {
            if (e.getBoundingBox().getCenter().distanceTo(p) > r + e.getBbWidth() / 2 || paredeAte(saidaDaEspada, e)) continue;
            if (atingidos.add(e.getUUID()) && ferir(e, ZangetsuBoss.DANO_ARREMESSO)) empurrar(e, .4);
        }
    }

    /** O Cero: um raio carmesim da mão até o primeiro bloco, que fere todos na linha. */
    private void dispararCero() {
        Vec3 de = peito().add(direcao.scale(.8));
        Vec3 fim = de.add(direcao.scale(ZangetsuBoss.ALCANCE_CERO));
        Vec3 bloco = blocoEntre(de, fim);
        if (bloco != null) fim = bloco;
        double comprimento = fim.distanceTo(de);
        enviar(new EncerrarVisual(getId(), "ichigo_cero_carga", vetor(de)));
        visual("ichigo_cero", de, direcao, (float) comprimento, ZangetsuBoss.CERO_NA_TELA);
        if (comprimento < .5) return;
        for (LivingEntity e : naLinha(de, fim, ZangetsuBoss.RAIO_CERO)) {
            if (ferir(e, ZangetsuBoss.DANO_CERO)) empurrar(e, 1.0);
        }
    }

    // --- Ferir e procurar ---

    private void empurrar(LivingEntity e, double forca) {
        empurrar(e, forca, direcao);
    }

    private void empurrar(LivingEntity e, double forca, Vec3 rumo) {
        e.knockback(forca, -rumo.x, -rumo.z);
        e.hurtMarked = true;
    }

    /** Os multiplicadores do balanceamento, lidos ao começar a luta. */
    private com.minerwk.core.kit.Poder poder = com.minerwk.ConfigServidor.poder();

    private boolean ferir(LivingEntity alvo, float dano) {
        // Cada golpe dele vale: os três Getsugas da máscara saem quase no tempo de invulnerabilidade do anterior.
        alvo.invulnerableTime = 0;
        return alvo.hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DANO), this), poder.danoDoBoss(dano));
    }

    /** Onde o segmento bate num bloco sólido, ou null. */
    private Vec3 blocoEntre(Vec3 de, Vec3 ate) {
        HitResult r = level().clip(new ClipContext(de, ate, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return r.getType() == HitResult.Type.MISS ? null : r.getLocation();
    }

    /** Os vivos a até {@code raio} do segmento, sem parede no meio. */
    private List<LivingEntity> naLinha(Vec3 de, Vec3 ate, double raio) {
        List<LivingEntity> perto = candidatos(new AABB(de, ate).inflate(raio + 1));
        perto.removeIf(e -> distanciaAoSegmento(e.getBoundingBox().getCenter(), de, ate) > raio + e.getBbWidth() / 2
                || paredeAte(de, e));
        return perto;
    }

    /** Os vivos na caixa (menos outros bosses iguais), mais quem invocou: o jogador dos GameTests não está no mundo. */
    private List<LivingEntity> candidatos(AABB caixa) {
        List<LivingEntity> perto = new ArrayList<>(level().getEntitiesOfClass(LivingEntity.class, caixa,
                e -> e != this && e.isAlive() && !(e instanceof ZangetsuEntity)));
        if (invocador != null && invocador.isAlive() && !perto.contains(invocador)) perto.add(invocador);
        return perto;
    }

    /** Se há bloco sólido entre o começo do golpe e o meio do alvo: golpe não atravessa parede. */
    private boolean paredeAte(Vec3 de, LivingEntity alvo) {
        return blocoEntre(de, alvo.getBoundingBox().getCenter()) != null;
    }

    private static double distanciaAoSegmento(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = ab.lengthSqr() < 1e-9 ? 0 : Mth.clamp(p.subtract(a).dot(ab) / ab.lengthSqr(), 0, 1);
        return p.distanceTo(a.add(ab.scale(t)));
    }

    /** Se ele pode surgir nesse ponto: cabe de pé, fora de água e lava, com chão até 2 blocos abaixo. */
    private boolean cabeEm(Vec3 p) {
        AABB caixa = getType().getDimensions().makeBoundingBox(p.x, p.y, p.z);
        if (!level().noCollision(this, caixa) || level().containsAnyLiquid(caixa)) return false;
        for (double abaixo = .1; abaixo <= 2.1; abaixo += 1) {
            if (level().getBlockState(BlockPos.containing(p.x, p.y - abaixo, p.z)).blocksMotion()) return true;
        }
        return false;
    }

    // --- A máscara dele ---

    /** Abaixo de 40% de vida: para um instante, veste a máscara e volta mais rápido, com o Getsuga em sequência. */
    private void vestirAMascara(ServerPlayer dono, long agora) {
        fase = 2;
        motor.acelerar(ZangetsuBoss.ritmo(2));
        motor.trocarGolpes(ZangetsuBoss.golpes(2));
        paradoAte = agora + ZangetsuBoss.TROCA_DE_FASE;
        motor.parar(paradoAte);
        mascaraEm = agora + ZangetsuBoss.MASCARA_APARECE;
        getNavigation().stop();
        triggerAnim(CONTROLE, "mascara");
        tocar(SonsIchigo.MASCARA_VESTE, 1.8f, .85f);
        particulas(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 24, .4, .8, .4, .03);
        barra.setColor(BossEvent.BossBarColor.RED);
        avisar(dono, "mensagem.minerwk.zangetsu.mascara");
        // No mundo interior, a máscara traz a chuva: o céu escurece e chove no cliente de quem luta.
        if (dono != null) com.minerwk.arena.ArenasServidor.trocarCenario(dono, com.minerwk.rede.CenarioArena.INTERIOR_CHUVA);
    }

    // --- A queda de quem invocou ---

    /** Derruba quem invocou: 3 segundos no chão, sem agir e sem poder morrer. */
    void derrubar(ServerPlayer dono) {
        long agora = level().getGameTime();
        despertarFeito = true;
        quedaAte = agora + ZangetsuBoss.QUEDA;
        quedaAteDaCutscene = quedaAte;
        motor.parar(agora);
        estado(SEM_ESPADA, false);
        meiasLuas.clear();
        getNavigation().stop();
        LutasZangetsu.CAIDOS.add(dono.getUUID());
        dono.setHealth(1);
        // Caído já conta como paralisado nas regras do kit; o efeito é o que prende as pernas no cliente.
        if (dono.connection != null) {
            dono.addEffect(new MobEffectInstance(Efeitos.PARALISIA, ZangetsuBoss.QUEDA + 5, 0, false, false, false), this);
        }
        dono.setDeltaMovement(Vec3.ZERO);
        dono.hurtMarked = true;
        // A pose forçada do servidor não chega ao próprio jogador: quem o deita é a animação.
        Envio.paraQuemVe(dono, new TocarApresentacao(dono.getId(), CutsceneMascara.ANIM_QUEDA, "", ""));
        particulas(ParticleTypes.DAMAGE_INDICATOR, dono.getX(), dono.getY() + 1, dono.getZ(), 14, .3, .4, .3, .1);
        avisar(dono, "mensagem.minerwk.zangetsu.caiu");
        Vec3 para = horizontal(position().subtract(dono.position()));
        Envio.para(dono, new Cutscene(CutsceneMascara.ID, dono.getId(), vetor(dono.getEyePosition()),
                vetor(para != null ? para : direcao.scale(-1)), agora));
    }

    /** Quem invocou se levanta de máscara: vida e reiatsu cheios, recargas zeradas e os golpes trancados liberados. */
    private void levantar(ServerPlayer dono) {
        quedaAte = -1;
        soltar(dono);
        dono.setHealth(dono.getMaxHealth());
        ServicoKits.kit(dono).ifPresent(k -> k.recarregar());
        ServicoKits.definirDespertoNaLuta(dono, true);
        // Só o Ichigo tem máscara para vestir (o comando de teste chama o boss para qualquer um).
        if (ServicoKits.kit(dono).map(k -> k.personagem().id().equals(Ichigo.ID)).orElse(false)) {
            Ichigo.vestirAMascara(new ContextoServidor(dono));
            Envio.paraQuemVe(dono, new TocarApresentacao(dono.getId(), CutsceneMascara.ANIM_MASCARA, "", ""));
        }
        // Ele espera o fim da cutscene para voltar a atacar.
        paradoAte = level().getGameTime() + CutsceneMascara.DEPOIS;
        motor.parar(paradoAte);
        particulas(ParticleTypes.LARGE_SMOKE, dono.getX(), dono.getY() + 1, dono.getZ(), 30, .4, .8, .4, .05);
        avisar(dono, "mensagem.minerwk.zangetsu.despertou");
    }

    /** Tira quem invocou do chão. */
    private void soltar(ServerPlayer dono) {
        // Fora da hora (ele morreu ou sumiu durante a queda) a cutscene é cortada.
        if (level().getGameTime() < quedaAteDaCutscene) {
            Envio.para(dono, new Cutscene(Cutscene.FIM, dono.getId(), vetor(dono.getEyePosition()), vetor(direcao),
                    level().getGameTime()));
            Envio.paraQuemVe(dono, new TocarApresentacao(dono.getId(), TocarApresentacao.PARAR, "", ""));
        }
        quedaAteDaCutscene = 0;
        LutasZangetsu.CAIDOS.remove(dono.getUUID());
        dono.removeEffect(Efeitos.PARALISIA);
    }

    /** A chegada: a animação, o reiatsu no chão e a fala. */
    private void entrar(ServerPlayer dono) {
        entrou = true;
        triggerAnim(CONTROLE, "entrada");
        tocar(SonsIchigo.BANKAI_CARGA, 1.6f, .8f);
        particulas(ParticleTypes.WHITE_ASH, getX(), getY() + 1, getZ(), 60, .8, 1, .8, .02);
        if (dono.connection != null) {
            dono.displayClientMessage(Component.translatable("mensagem.minerwk.zangetsu.fala_entrada"), false);
        }
    }

    // --- O fim ---

    /** A luta acabou sem vitória: ele vai embora. */
    private void sumir() {
        tocar(SonsIchigo.SAIR, 1.2f, 1f);
        particulas(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 24, .3, .7, .3, .03);
        ServerPlayer dono = jogadorAtual();
        if (dono != null) avisar(dono, "mensagem.minerwk.zangetsu.sumiu");
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
        estado(SEM_ESPADA, false);
        meiasLuas.clear();
        tocar(SonsIchigo.MASCARA_QUEBRA, 1.6f, .8f);
        ServerPlayer dono = jogadorAtual();
        if (dono == null) return;
        if (quedaAte >= 0) {
            // Ele morreu com quem invocou no chão (dano de outra fonte): o jogador se levanta inteiro do mesmo jeito.
            quedaAte = -1;
            soltar(dono);
            dono.setHealth(dono.getMaxHealth());
            ServicoKits.kit(dono).ifPresent(k -> k.recarregar());
        }
        // Venceu: a máscara, o Mugetsu e o resto da árvore deixam de ser só desta luta.
        ServicoKits.definirDespertou(dono, true);
        ServicoKits.definirDespertoNaLuta(dono, false);
        ServicoKits.ganharXp(dono, ZangetsuBoss.XP_VITORIA);
        avisar(dono, "mensagem.minerwk.zangetsu.venceu");
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

    /** A paralisia é do jogador caído; nele não pega (o comando de teste o chama para qualquer personagem). */
    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance efeito) {
        return !efeito.is(Efeitos.PARALISIA) && super.canBeAffected(efeito);
    }

    /** Ele saiu do mundo, por qualquer caminho: solta quem estava caído e, sem vitória, tranca os golpes de novo. */
    private void encerrarLuta() {
        if (level().isClientSide || lutaEncerrada) return;
        lutaEncerrada = true;
        barra.removeAllPlayers();
        LutasZangetsu.esquecer(this);
        ServerPlayer dono = jogadorAtual();
        if (dono != null) {
            LutasZangetsu.CAIDOS.remove(dono.getUUID());
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

    /** Toca um som do Ichigo pelo id de {@link SonsIchigo}: os dois são o mesmo homem. */
    private void tocar(String id, float volume, float tom) {
        SoundEvent som = BuiltInRegistries.SOUND_EVENT.get(MineRWK.id(id));
        if (som != null) level().playSound(null, getX(), getY(), getZ(), som, SoundSource.HOSTILE, volume, tom);
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
        AnimationController<ZangetsuEntity> principal = new AnimationController<>(this, CONTROLE, 3, estado -> {
            if (isDeadOrDying()) return estado.setAndContinue(MORTE);
            return estado.setAndContinue(estado.isMoving() ? ANDANDO : PARADO);
        });
        for (String nome : List.of("corte", "getsuga", "getsuga_3", "shunpo", "arremesso", "cero", "mascara", "entrada")) {
            principal.triggerableAnim(nome, RawAnimation.begin().thenPlay(nome));
        }
        controladores.add(principal);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
