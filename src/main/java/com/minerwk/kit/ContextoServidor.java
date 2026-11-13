package com.minerwk.kit;

import com.minerwk.arena.ArenasServidor;
import com.minerwk.core.kit.Arremessos;
import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.Efeito;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Vetor;
import com.minerwk.rede.Acertou;
import com.minerwk.rede.Cutscene;
import com.minerwk.rede.EncerrarVisual;
import com.minerwk.rede.Envio;
import com.minerwk.rede.MostrarVisual;
import com.minerwk.rede.TocarApresentacao;
import com.minerwk.rede.Tremer;
import com.minerwk.registro.Efeitos;
import com.minerwk.registro.Regras;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** O {@link Contexto} do núcleo, executado no servidor em nome de um jogador. Nada aqui afeta o próprio jogador. */
public final class ContextoServidor implements Contexto {
    /** Marca do projétil que o Infinito já parou (o toque aparece uma vez só). */
    private static final String PARADO_PELO_INFINITO = "minerwk_parado_infinito";
    private final ServerPlayer jogador;

    public ContextoServidor(ServerPlayer jogador) {
        this.jogador = jogador;
    }

    public ServerPlayer jogador() {
        return jogador;
    }

    private ServerLevel nivel() {
        return jogador.serverLevel();
    }

    private static Vec3 vec(Vetor v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    private static Vetor vetor(Vec3 v) {
        return new Vetor(v.x, v.y, v.z);
    }

    /**
     * Quem os golpes atingem: vivo, não espectador; nunca o próprio jogador, os bichos dele, a montaria
     * ou os projéteis que ele lançou; outros jogadores só com PvP.
     */
    private boolean afetavel(Entity e) {
        if (e == jogador || !e.isAlive() || e.isSpectator()) return false;
        if (e == jogador.getVehicle() || e.getVehicle() == jogador) return false;
        if (e instanceof OwnableEntity dono && jogador.getUUID().equals(dono.getOwnerUUID())) return false;
        if (e instanceof Projectile p && p.getOwner() == jogador) return false;
        if (e instanceof Player p) return !p.isCreative() && jogador.server.isPvpAllowed();
        return true;
    }

    private <T extends Entity> List<T> naEsfera(Class<T> tipo, Vec3 centro, double raio, Predicate<T> filtro) {
        return nivel().getEntitiesOfClass(tipo, new AABB(centro, centro).inflate(raio),
                e -> afetavel(e) && e.getBoundingBox().getCenter().distanceToSqr(centro) <= raio * raio && filtro.test(e));
    }

    /**
     * O dano de um golpe do kit, com o multiplicador da config ("Balanceamento"). Todo método de dano daqui
     * passa por ele uma vez só; o clique ({@code ArmaServidor}) não.
     */
    private float golpe(float dano) {
        return PoderServidor.de(jogador).golpe(dano);
    }

    @Override
    public void danoEmArea(double raio, float dano) {
        danoEm(vetor(jogador.position()), raio, dano);
    }

    @Override
    public void danoSeguidoEmArea(double raio, float danoBase) {
        float dano = golpe(danoBase);
        var fonte = jogador.damageSources().playerAttack(jogador);
        Vec3 centro = jogador.position();
        ServicoKits.comoGolpeDoKit(jogador, () -> {
            for (LivingEntity alvo : naEsfera(LivingEntity.class, centro, raio, e -> true)) {
                alvo.invulnerableTime = 0;
                alvo.hurt(fonte, dano);
            }
        });
    }

    @Override
    public void chamariz(Vetor posicao, int ticks) {
        ChamarizServidor.criar(jogador, vec(posicao), ticks);
    }

    @Override
    public void velocidade(int nivel, int ticks) {
        // Jogador sem conexão (GameTest): o efeito manda pacote ao próprio jogador e não tem para quem.
        if (nivel < 1 || ticks <= 0 || jogador.connection == null) return;
        jogador.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, ticks, nivel - 1,
                false, false, true));
    }

    @Override
    public void recuo(float dano) {
        if (dano <= 0 || jogador.isCreative() || jogador.getHealth() <= 1) return;
        jogador.setHealth(Math.max(1, jogador.getHealth() - dano));
    }

    @Override
    public void encerrarEfeito(Class<? extends Efeito> tipo) {
        kit().ifPresent(k -> k.encerrarEfeitos(tipo, this));
    }

    @Override
    public void apresentacao(String animacao, String efeito, String som) {
        // Outra animação por cima da pose de descida: ao pousar, ela não é cortada.
        if (!animacao.isEmpty()) BarreirasServidor.animacaoTrocada(jogador);
        Envio.paraQuemVe(jogador, new TocarApresentacao(jogador.getId(), animacao, efeito, som));
    }

    @Override
    public void pararAnimacao() {
        Envio.paraQuemVe(jogador, new TocarApresentacao(jogador.getId(), TocarApresentacao.PARAR, "", ""));
    }

    @Override
    public void tremor(float forca, int duracao, double raio) {
        for (ServerPlayer outro : nivel().players()) {
            double d = Math.sqrt(outro.distanceToSqr(jogador));
            if (d > raio) continue;
            float f = (float) (forca * (1 - d / raio));
            if (f > 0) Envio.para(outro, new Tremer(f, duracao));
        }
    }

    @Override
    public Vetor olhos() {
        return vetor(jogador.getEyePosition());
    }

    @Override
    public Vetor pes() {
        return vetor(jogador.position());
    }

    @Override
    public Vetor olhar() {
        return vetor(jogador.getLookAngle());
    }

    @Override
    public Vetor alvoAte(double distancia) {
        Vec3 de = jogador.getEyePosition();
        Vec3 ate = de.add(jogador.getLookAngle().scale(distancia));
        BlockHitResult hit = nivel().clip(new ClipContext(de, ate, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jogador));
        if (hit.getType() == HitResult.Type.MISS) return vetor(ate);
        // Recua meio bloco para o ponto não ficar dentro da parede.
        return vetor(hit.getLocation().subtract(jogador.getLookAngle().scale(0.5)));
    }

    private void mover(Vec3 centro, double raio, double forca, boolean puxar) {
        List<Entity> alvos = naEsfera(Entity.class, centro, raio,
                e -> e instanceof LivingEntity || e instanceof ItemEntity || e instanceof Projectile
                        || e instanceof FallingBlockEntity && e.getTags().contains(ARRANCADO));
        for (Entity e : alvos) {
            if (puxar && e instanceof FallingBlockEntity) {
                orbitar(e, centro);
                continue;
            }
            Vec3 dir = centro.subtract(e.getBoundingBox().getCenter());
            if (dir.lengthSqr() < 0.25 && puxar) continue; // já está no centro
            // O Toji não tem energia amaldiçoada para o Azul agarrar: é puxado com metade da força.
            double f = puxar && e instanceof com.minerwk.boss.TojiEntity ? forca * .5 : forca;
            Vec3 passo = dir.normalize().scale(puxar ? f : -f);
            if (!puxar) passo = passo.add(0, 0.1 * forca, 0);
            e.setDeltaMovement(e.getDeltaMovement().add(passo));
            e.hurtMarked = true; // manda a velocidade para o cliente (jogadores)
            e.hasImpulse = true; // e para as outras entidades
        }
    }

    /**
     * Bloco arrancado pelo Azul: flutua e gira em volta de um ponto um pouco acima do centro (senão pousa
     * logo e vira bloco de novo). Quando o campo acaba, a gravidade volta e ele cai.
     */
    private static void orbitar(Entity e, Vec3 centro) {
        Vec3 alvo = centro.add(0, 1.5, 0);
        Vec3 d = alvo.subtract(e.position());
        double dist = d.length();
        Vec3 ida = dist < 1e-3 ? Vec3.ZERO : d.scale(Math.min(.45, dist * .2) / dist);
        Vec3 giro = d.cross(new Vec3(0, 1, 0));
        giro = giro.lengthSqr() < 1e-6 ? Vec3.ZERO : giro.normalize().scale(.22);
        // +0,04 desfaz a gravidade do bloco caindo neste tick.
        e.setDeltaMovement(ida.add(giro).add(0, .04, 0));
        e.hurtMarked = true;
        e.hasImpulse = true;
    }

    @Override
    public void puxar(Vetor centro, double raio, double forca) {
        mover(vec(centro), raio, forca, true);
    }

    @Override
    public void empurrar(Vetor centro, double raio, double forca) {
        mover(vec(centro), raio, forca, false);
    }

    @Override
    public void danoEm(Vetor centro, double raio, float danoBase) {
        float dano = golpe(danoBase);
        var fonte = jogador.damageSources().playerAttack(jogador);
        ServicoKits.comoGolpeDoKit(jogador, () -> {
            for (LivingEntity alvo : naEsfera(LivingEntity.class, vec(centro), raio, e -> true)) alvo.hurt(fonte, dano);
        });
    }

    @Override
    public int danoUmaVez(Vetor centro, double raio, float danoBase, Set<Object> atingidos) {
        float dano = golpe(danoBase);
        var fonte = jogador.damageSources().playerAttack(jogador);
        int[] feridos = {0};
        ServicoKits.comoGolpeDoKit(jogador, () -> {
            for (LivingEntity alvo : naEsfera(LivingEntity.class, vec(centro), raio, e -> true)) {
                if (atingidos.add(alvo.getUUID()) && alvo.hurt(fonte, dano)) feridos[0]++;
            }
        });
        return feridos[0];
    }

    /** Dano que não conta a armadura do alvo (o Mugetsu); o tipo está em {@code data/minerwk/damage_type}. */
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> DANO_DIRETO =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    com.minerwk.MineRWK.id("direto"));

    @Override
    public int danoDiretoUmaVez(Vetor centro, double raio, float danoBase, Set<Object> atingidos) {
        float dano = golpe(danoBase);
        var fonte = new net.minecraft.world.damagesource.DamageSource(nivel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(DANO_DIRETO), jogador);
        int[] feridos = {0};
        ServicoKits.comoGolpeDoKit(jogador, () -> {
            for (LivingEntity alvo : naEsfera(LivingEntity.class, vec(centro), raio, e -> true)) {
                if (atingidos.add(alvo.getUUID()) && alvo.hurt(fonte, dano)) feridos[0]++;
            }
        });
        return feridos[0];
    }

    @Override
    public void arremessar(Vetor centro, double raio, double forca, int congelar, Vetor eixo) {
        for (LivingEntity alvo : naEsfera(LivingEntity.class, vec(centro), raio, e -> true)) {
            Vetor v = Arremessos.velocidade(centro, vetor(alvo.getBoundingBox().getCenter()), forca, eixo);
            ArremessosServidor.iniciar(alvo, v, congelar);
        }
    }

    @Override
    public int arrancarBlocos(Vetor centro, double raio, int maximo, float durezaMax) {
        ServerLevel nivel = nivel();
        if (maximo <= 0 || !nivel.getGameRules().getBoolean(Regras.APAGAR_BLOCOS)) return 0;
        Vec3 c = vec(centro);
        int feitos = 0;
        // Sorteia pontos na esfera; só sai o que está exposto por cima (o chão, não o que está enterrado).
        for (int tentativa = 0; tentativa < maximo * 8 && feitos < maximo; tentativa++) {
            double dx = (nivel.random.nextDouble() * 2 - 1) * raio;
            double dy = (nivel.random.nextDouble() * 2 - 1) * raio;
            double dz = (nivel.random.nextDouble() * 2 - 1) * raio;
            if (dx * dx + dy * dy + dz * dz > raio * raio) continue;
            BlockPos p = BlockPos.containing(c.x + dx, c.y + dy, c.z + dz);
            if (!nivel.isLoaded(p) || nivel.isOutsideBuildHeight(p) || !nivel.mayInteract(jogador, p)
                    || !ArenasServidor.podeMexer(nivel, p)) continue;
            BlockState estado = nivel.getBlockState(p);
            float dureza = estado.getDestroySpeed(nivel, p);
            if (estado.isAir() || dureza < 0 || dureza > durezaMax || estado.hasBlockEntity()
                    || !estado.getFluidState().isEmpty() || !nivel.getBlockState(p.above()).isAir()
                    || estado.is(BlockTags.CROPS) || !podeQuebrar(nivel, p, estado)) continue;
            FallingBlockEntity bloco = FallingBlockEntity.fall(nivel, p, estado);
            bloco.addTag(ARRANCADO);
            bloco.setDeltaMovement(0, .35, 0); // sai do chão antes de começar a girar
            bloco.hurtMarked = true;
            feitos++;
        }
        return feitos;
    }

    /** Etiqueta dos blocos que o Azul arrancou: só esses são puxados (não a areia caindo dos outros). */
    static final String ARRANCADO = "minerwk_arrancado";

    /** Pergunta aos mods de proteção de terreno (claims) se o jogador pode quebrar aqui. */
    private boolean podeQuebrar(ServerLevel nivel, BlockPos p, BlockState estado) {
        consultandoGolpe = true;
        try {
            return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(nivel, p, estado, jogador)).isCanceled();
        } finally {
            consultandoGolpe = false;
        }
    }

    /** Só na thread do servidor: verdadeiro enquanto um golpe pergunta se pode quebrar um bloco. */
    private static boolean consultandoGolpe;

    /**
     * Se o evento de quebra em andamento é a pergunta de um golpe, e não alguém quebrando à mão. Quem só
     * proíbe a quebra à mão (a arena) usa isto para deixar os golpes passarem.
     */
    public static boolean quebraDeGolpe() {
        return consultandoGolpe;
    }

    @Override
    public void barreira(Vetor centro, double raio, double chao, int ticks) {
        BarreirasServidor.criar(jogador, centro, raio, chao, ticks);
    }

    @Override
    public Object marcarAlvo(double alcance, double largura) {
        return marcar(alcance, largura, false);
    }

    @Override
    public Object marcarAlvoOuChefe(double alcance, double largura) {
        return marcar(alcance, largura, true);
    }

    private Object marcar(double alcance, double largura, boolean chefes) {
        Vec3 olhos = jogador.getEyePosition();
        Vec3 dir = jogador.getLookAngle();
        AABB caixa = new AABB(olhos, olhos.add(dir.scale(alcance))).inflate(largura);
        LivingEntity melhor = null;
        double melhorFora = Double.MAX_VALUE;
        double melhorLonge = Double.MAX_VALUE;
        for (LivingEntity e : nivel().getEntitiesOfClass(LivingEntity.class, caixa,
                e -> afetavel(e) && !(e instanceof ArmorStand) && (chefes || !(e instanceof EnderDragon) && !(e instanceof WitherBoss)))) {
            Vec3 c = e.getBoundingBox().getCenter();
            Vec3 ate = c.subtract(olhos);
            double longe = ate.dot(dir);
            if (longe < 0 || longe > alcance) continue;
            double fora = ate.subtract(dir.scale(longe)).length();
            if (fora > largura || !jogador.hasLineOfSight(e)) continue;
            // O mais perto da linha; no empate, o mais perto do Gojo.
            if (fora < melhorFora - 1e-6 || (Math.abs(fora - melhorFora) <= 1e-6 && longe < melhorLonge)) {
                melhor = e;
                melhorFora = fora;
                melhorLonge = longe;
            }
        }
        return melhor == null ? null : melhor.getUUID();
    }

    @Override
    public Object inimigoProximo(double raio) {
        Vec3 olhos = jogador.getEyePosition();
        LivingEntity melhor = null;
        double melhorLonge = raio * raio;
        for (LivingEntity e : nivel().getEntitiesOfClass(LivingEntity.class, jogador.getBoundingBox().inflate(raio),
                e -> afetavel(e) && inimigo(e))) {
            double longe = e.getBoundingBox().getCenter().distanceToSqr(olhos);
            if (longe > melhorLonge || !jogador.hasLineOfSight(e)) continue;
            melhor = e;
            melhorLonge = longe;
        }
        return melhor == null ? null : melhor.getUUID();
    }

    /**
     * Quem um golpe sem mira pode escolher: monstros, quem está atrás do jogador, quem o feriu por último e
     * outros jogadores (o PvP já foi conferido). Animais, aldeões e bichos dos outros ficam de fora.
     */
    private boolean inimigo(LivingEntity e) {
        if (e instanceof ArmorStand) return false;
        if (e instanceof Player) return !e.isAlliedTo(jogador);
        if (e instanceof OwnableEntity dono && dono.getOwnerUUID() != null) {
            return e instanceof net.minecraft.world.entity.Mob m && m.getTarget() == jogador;
        }
        return e instanceof net.minecraft.world.entity.monster.Enemy || e == jogador.getLastHurtByMob()
                || e instanceof net.minecraft.world.entity.Mob m && m.getTarget() == jogador;
    }

    @Override
    public void vida(float vida) {
        jogador.setHealth(Math.max(1, Math.min(vida, jogador.getMaxHealth())));
    }

    /** O alvo marcado, se ainda está vivo neste mundo (o da viagem do R do Cid, no mundo em que estiver). */
    private LivingEntity marcado(Object alvo) {
        if (!(alvo instanceof java.util.UUID id)) return null;
        LivingEntity daViagem = EspacoServidor.alvo(jogador, id);
        if (daViagem != null) return daViagem;
        return nivel().getEntity(id) instanceof LivingEntity e && e.isAlive() ? e : null;
    }

    @Override
    public Vetor posicaoDe(Object alvo) {
        LivingEntity e = marcado(alvo);
        return e == null ? null : vetor(e.getBoundingBox().getCenter());
    }

    @Override
    public void suspender(Object alvo, Vetor ponto, int ticks) {
        LivingEntity e = marcado(alvo);
        if (e != null) SuspensosServidor.suspender(e, vec(ponto), ticks);
    }

    @Override
    public void ferir(Object alvo, float danoBase) {
        float dano = golpe(danoBase);
        LivingEntity e = marcado(alvo);
        if (e == null) return;
        // Na viagem do R do Cid só este dano chega ao alvo, e um soco não o protege do seguinte.
        if (EspacoServidor.alvo(jogador, e.getUUID()) != null) {
            ServicoKits.comoGolpeDoKit(jogador, () -> EspacoServidor.ferir(jogador, e, dano));
        } else {
            ServicoKits.comoGolpeDoKit(jogador, () -> e.hurt(jogador.damageSources().playerAttack(jogador), dano));
        }
    }

    @Override
    public void devolverRecurso(double quantidade) {
        kit().ifPresent(k -> k.recurso().carregar(k.recurso().atual() + quantidade));
    }

    @Override
    public boolean alvoVivo(Object alvo) {
        return marcado(alvo) != null;
    }

    @Override
    public boolean viagem(Object alvo) {
        LivingEntity e = marcado(alvo);
        return e != null && EspacoServidor.comecar(jogador, e);
    }

    @Override
    public void pontosDaViagem(Object alvo, Vetor pes, Vetor centroDoAlvo, Vetor encarar) {
        EspacoServidor.pontos(jogador, vec(pes), vec(centroDoAlvo), encarar == null ? null : vec(encarar));
    }

    @Override
    public Vetor aoEspaco(Object alvo) {
        return vetor(EspacoServidor.subir(jogador));
    }

    @Override
    public void fimDaViagem() {
        EspacoServidor.terminar(jogador);
    }

    @Override
    public void visualNaOrigem(String tipo, float forca, int duracao, double raio) {
        EspacoServidor.visualNaOrigem(jogador, tipo, forca, duracao, raio);
    }

    @Override
    public void voo(int ticks) {
        BarreirasServidor.voar(jogador, ticks);
    }

    @Override
    public void apagarCasca(Vetor centro, double de, double ate) {
        ServerLevel nivel = nivel();
        if (!nivel.getGameRules().getBoolean(Regras.APAGAR_BLOCOS)) return;
        ApagadorServidor.apagarCasca(nivel, vec(centro), de, ate,
                (p, estado) -> nivel.mayInteract(jogador, p) && ArenasServidor.podeMexer(nivel, p)
                        && podeQuebrar(nivel, p.immutable(), estado));
    }

    @Override
    public void acertou(float forca) {
        Envio.para(jogador, new Acertou(forca));
    }

    @Override
    public boolean colide(Vetor de, Vetor ate) {
        Vec3 a = vec(de);
        Vec3 b = vec(ate);
        if (nivel().clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jogador)).getType()
                != HitResult.Type.MISS) return true;
        return !nivel().getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(1),
                e -> afetavel(e) && e.getBoundingBox().inflate(0.3).clip(a, b).isPresent()).isEmpty();
    }

    /** Blocos apagados que soltam pedaços, por chamada. */
    private static final int DETRITOS = 12;

    @Override
    public void apagarBlocos(Vetor centro, double raio) {
        ServerLevel nivel = nivel();
        if (!nivel.getGameRules().getBoolean(Regras.APAGAR_BLOCOS)) return;
        Vec3 c = vec(centro);
        int r = (int) Math.ceil(raio);
        BlockPos base = BlockPos.containing(c);
        int apagados = 0;
        // Massa grande (Roxo 200%): avisa só os clientes, sem atualizar vizinhos, que pesa bem menos.
        int flags = raio > 4 ? Block.UPDATE_CLIENTS : Block.UPDATE_ALL;
        BlockPos[] sorteados = new BlockPos[DETRITOS];
        BlockState[] estados = new BlockState[DETRITOS];
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-r, -r, -r), base.offset(r, r, r))) {
            if (p.getCenter().distanceToSqr(c) > raio * raio) continue;
            if (!nivel.isLoaded(p) || nivel.isOutsideBuildHeight(p) || !nivel.mayInteract(jogador, p)
                    || !ArenasServidor.podeMexer(nivel, p)) continue;
            BlockState estado = nivel.getBlockState(p);
            if (estado.isAir() || estado.getDestroySpeed(nivel, p) < 0 || estado.hasBlockEntity()
                    || !estado.getFluidState().isEmpty() || !podeQuebrar(nivel, p, estado)) continue;
            nivel.setBlock(p, Blocks.AIR.defaultBlockState(), flags);
            // Sorteio de reservatório: guarda DETRITOS blocos ao acaso entre os apagados.
            int vaga = apagados < DETRITOS ? apagados : nivel.random.nextInt(apagados + 1);
            if (vaga < DETRITOS) {
                sorteados[vaga] = p.immutable();
                estados[vaga] = estado;
            }
            apagados++;
        }
        // Pedaços do bloco de verdade voam: no máximo DETRITOS pacotes por chamada.
        for (int i = 0; i < Math.min(apagados, DETRITOS); i++) {
            BlockPos p = sorteados[i];
            nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, estados[i]),
                    p.getX() + .5, p.getY() + .5, p.getZ() + .5, 4, .4, .4, .4, .15);
        }
    }

    @Override
    public void paralisar(Vetor centro, double raio, int ticks) {
        for (LivingEntity alvo : naEsfera(LivingEntity.class, vec(centro), raio, e -> true)) {
            alvo.addEffect(new MobEffectInstance(Efeitos.PARALISIA, ticks, 0, false, false, false), jogador);
            alvo.setDeltaMovement(0, Math.min(0, alvo.getDeltaMovement().y), 0);
            alvo.hurtMarked = true;
        }
    }

    @Override
    public void paralisarAlvo(Object alvo, int ticks) {
        LivingEntity e = marcado(alvo);
        if (e == null || !afetavel(e)) return;
        e.addEffect(new MobEffectInstance(Efeitos.PARALISIA, ticks, 0, false, false, false), jogador);
        e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
        e.hurtMarked = true;
    }

    @Override
    public void infinito(int ticks) {
        ProtecoesServidor.ligarInfinito(jogador, ticks);
        // Projéteis que chegam perto param no ar e caem.
        for (Projectile p : nivel().getEntitiesOfClass(Projectile.class, jogador.getBoundingBox().inflate(1.5),
                p -> p.getOwner() != jogador)) {
            // Só a primeira parada é um toque (o que já parou volta a ganhar um pouco de queda a cada tick).
            if (p.addTag(PARADO_PELO_INFINITO)) ProtecoesServidor.toque(jogador, p.position(), true);
            p.setDeltaMovement(Vec3.ZERO);
            p.hurtMarked = true;
        }
    }

    @Override
    public void curar(float vida) {
        jogador.heal(vida);
    }

    @Override
    public int danoEmArco(Vetor origem, Vetor direcao, double alcance, double graus, double altura, float danoBase) {
        float dano = golpe(danoBase);
        var fonte = jogador.damageSources().playerAttack(jogador);
        Vec3 centro = vec(origem);
        int[] feridos = {0};
        ServicoKits.comoGolpeDoKit(jogador, () -> {
            for (LivingEntity alvo : nivel().getEntitiesOfClass(LivingEntity.class, new AABB(centro, centro).inflate(alcance, altura + 2, alcance),
                    e -> afetavel(e) && com.minerwk.core.kit.Arco.contem(origem, direcao, vetor(e.getBoundingBox().getCenter()), alcance, graus, altura))) {
                if (alvo.hurt(fonte, dano)) feridos[0]++;
            }
        });
        return feridos[0];
    }

    /** O modificador que prende o jogador ao chão: zera a velocidade de andar e a força do pulo. */
    private static final ResourceLocation FIRME = com.minerwk.MineRWK.id("firme");

    /** Se o jogador está preso ao chão por {@link #firmar}. */
    public static boolean firme(ServerPlayer jogador) {
        var andar = jogador.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        return andar != null && andar.hasModifier(FIRME);
    }

    @Override
    public void firmar(boolean preso) {
        // Os atributos chegam sozinhos ao cliente, que é quem move o jogador. Não são salvos: quem sai no meio volta solto.
        for (var atributo : List.of(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH)) {
            var instancia = jogador.getAttribute(atributo);
            if (instancia == null) continue;
            instancia.removeModifier(FIRME);
            if (preso) {
                instancia.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(FIRME, -1,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        if (preso) {
            jogador.setDeltaMovement(0, Math.min(0, jogador.getDeltaMovement().y), 0);
            jogador.hurtMarked = true;
        }
    }

    @Override
    public int restaurar(double raio) {
        Vec3 centro = jogador.position();
        // Aliados: os outros jogadores e os animais que têm dono. Nunca monstros, nem os bichos soltos.
        List<LivingEntity> curados = new java.util.ArrayList<>(nivel().getEntitiesOfClass(LivingEntity.class,
                new AABB(centro, centro).inflate(raio), e -> e != jogador && e.isAlive() && !e.isSpectator()
                        && e.position().distanceToSqr(centro) <= raio * raio
                        && (e instanceof Player || e instanceof OwnableEntity dono && dono.getOwnerUUID() != null)));
        curados.add(jogador);
        for (LivingEntity e : curados) {
            e.setHealth(e.getMaxHealth());
            // Jogador sem conexão (GameTest): tirar o efeito manda pacote a ele e não tem para quem.
            if (e instanceof ServerPlayer p && p.connection == null) continue;
            for (var efeito : List.copyOf(e.getActiveEffects())) {
                if (efeito.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                    e.removeEffect(efeito.getEffect());
                }
            }
            e.clearFire();
        }
        return curados.size();
    }

    private Optional<KitDoJogador> kit() {
        return ServicoKits.kit(jogador);
    }

    @Override
    public boolean gastarRecurso(double quantidade) {
        Optional<KitDoJogador> kit = kit();
        if (kit.isEmpty() || !kit.get().recurso().pode(quantidade)) return false;
        kit.get().recurso().gastar(quantidade);
        return true;
    }

    @Override
    public void sinalAoRecurso(String sinal, int ticks) {
        kit().ifPresent(k -> k.recurso().sinal(sinal, ticks));
    }

    @Override
    public boolean efeitoAtivo(Class<? extends com.minerwk.core.kit.Efeito> tipo) {
        return kit().map(k -> k.efeitos().stream().anyMatch(tipo::isInstance)).orElse(false);
    }

    @Override
    public String golpeEmAndamento() {
        return kit().flatMap(KitDoJogador::execucao).map(e -> e.habilidade().id()).orElse("");
    }

    @Override
    public com.minerwk.core.maestria.Melhorias melhorias() {
        return kit().map(KitDoJogador::melhorias).orElse(com.minerwk.core.maestria.Melhorias.NENHUMA);
    }

    @Override
    public String forma() {
        return kit().map(KitDoJogador::forma).orElse(com.minerwk.core.kit.Forma.BASE);
    }

    @Override
    public boolean mudarForma(String forma) {
        return kit().map(k -> k.mudarForma(forma, this)).orElse(false);
    }

    @Override
    public double recurso() {
        return kit().map(k -> k.recurso().atual()).orElse(0.0);
    }

    @Override
    public void recarga(String habilidade, int ticks) {
        kit().ifPresent(k -> k.recargas().iniciar(habilidade, nivel().getGameTime(), ticks));
    }

    @Override
    public void arma(com.minerwk.core.kit.Arma arma) {
        ArmaServidor.pedir(jogador, arma);
    }

    @Override
    public boolean colideBloco(Vetor de, Vetor ate) {
        return nivel().clip(new ClipContext(vec(de), vec(ate), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jogador))
                .getType() != HitResult.Type.MISS;
    }

    @Override
    public int desfazerProjeteis(Vetor centro, double raio) {
        List<Projectile> projeteis = naEsfera(Projectile.class, vec(centro), raio, p -> true);
        for (Projectile p : projeteis) p.discard();
        return projeteis.size();
    }

    /** Tamanho de cada passo do deslocamento: menor que a largura do jogador, para não pular parede fina. */
    private static final double PASSO_DO_DESLOCAMENTO = 0.5;

    @Override
    public Vetor deslocar(Vetor direcao, double distancia, Vetor encarar) {
        Vec3 dir = vec(direcao);
        Vec3 pos = jogador.position();
        if (dir.lengthSqr() > 1e-9 && distancia > 0) {
            dir = dir.normalize();
            AABB caixa = jogador.getBoundingBox();
            for (double andou = 0; andou < distancia - 1e-6; andou += PASSO_DO_DESLOCAMENTO) {
                Vec3 passo = passoLivre(caixa, dir.scale(Math.min(PASSO_DO_DESLOCAMENTO, distancia - andou)));
                if (passo == null) break;
                caixa = caixa.move(passo);
                pos = pos.add(passo);
            }
        }
        float giro = jogador.getYRot();
        float inclinacao = jogador.getXRot();
        if (encarar != null) {
            Vec3 ate = vec(encarar).subtract(pos.add(0, jogador.getEyeHeight(), 0));
            if (ate.lengthSqr() > 1e-6) {
                giro = (float) (Math.toDegrees(Math.atan2(ate.z, ate.x)) - 90);
                inclinacao = (float) -Math.toDegrees(Math.atan2(ate.y, ate.horizontalDistance()));
            }
        }
        jogador.stopRiding();
        jogador.fallDistance = 0;
        if (jogador.connection != null && encarar == null) {
            // Sem ponto para encarar, a câmera do jogador fica como está (giro relativo zero), sem tranco.
            jogador.connection.teleport(pos.x, pos.y, pos.z, 0, 0, java.util.Set.of(
                    net.minecraft.world.entity.RelativeMovement.X_ROT, net.minecraft.world.entity.RelativeMovement.Y_ROT));
        } else if (jogador.connection != null) {
            jogador.connection.teleport(pos.x, pos.y, pos.z, giro, inclinacao);
        } else {
            jogador.moveTo(pos.x, pos.y, pos.z, giro, inclinacao);
        }
        return vetor(pos);
    }

    /**
     * O passo que cabe: reto; senão rente ao chão ou ao teto (quem olha um pouco para baixo não empaca);
     * senão subindo um degrau. Null se há parede.
     */
    private Vec3 passoLivre(AABB caixa, Vec3 passo) {
        if (nivel().noCollision(jogador, caixa.move(passo))) return passo;
        Vec3 rente = new Vec3(passo.x, 0, passo.z);
        if (passo.y != 0 && rente.lengthSqr() > 1e-6 && nivel().noCollision(jogador, caixa.move(rente))) return rente;
        Vec3 degrau = new Vec3(passo.x, Math.max(passo.y, 0) + 1, passo.z);
        if (rente.lengthSqr() > 1e-6 && nivel().noCollision(jogador, caixa.move(degrau))) return degrau;
        return null;
    }

    @Override
    public void criarEfeito(Efeito efeito) {
        kit().ifPresent(k -> k.adicionarEfeito(efeito));
    }

    @Override
    public void visual(String tipo, Vetor origem, Vetor direcao, float forca, int duracao) {
        Envio.paraQuemVe(jogador, new MostrarVisual(jogador.getId(), tipo, origem, direcao, forca, duracao,
                nivel().getGameTime()));
    }

    @Override
    public void cutscene(String id) {
        Envio.para(jogador, new Cutscene(id, jogador.getId(), olhos(), olhar(), nivel().getGameTime()));
    }

    @Override
    public void cutscene(String id, Vetor origem, Vetor direcao) {
        Envio.para(jogador, new Cutscene(id, jogador.getId(), origem, direcao, nivel().getGameTime()));
    }

    @Override
    public void cutsceneParaAlvo(Object alvo, String id, Vetor origem, Vetor direcao, int passados) {
        if (marcado(alvo) instanceof ServerPlayer outro) {
            Envio.para(outro, new Cutscene(id, jogador.getId(), origem, direcao, nivel().getGameTime() - passados));
        }
    }

    @Override
    public void encerrarCutscene() {
        Envio.paraQuemVe(jogador, new Cutscene(Cutscene.FIM, jogador.getId(), olhos(), olhar(), nivel().getGameTime()));
    }

    @Override
    public void descida(String animacao, int ticksMax) {
        if (BarreirasServidor.pousado(jogador)) {
            BarreirasServidor.pousar(jogador);
            return;
        }
        apresentacao(animacao, "", "");
        BarreirasServidor.descer(jogador, ticksMax);
    }

    @Override
    public void visualFim(String tipo, Vetor ponto) {
        Envio.paraQuemVe(jogador, new EncerrarVisual(jogador.getId(), tipo, ponto));
    }

    @Override
    public void particulas(String tipo, Vetor ponto, int quantidade) {
        if (BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.withDefaultNamespace(tipo)) instanceof SimpleParticleType s) {
            nivel().sendParticles(s, ponto.x(), ponto.y(), ponto.z(), quantidade, 0.3, 0.3, 0.3, 0.02);
        }
    }
}
