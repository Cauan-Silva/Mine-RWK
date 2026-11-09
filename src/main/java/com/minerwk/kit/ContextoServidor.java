package com.minerwk.kit;

import com.minerwk.core.kit.Arremessos;
import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.Efeito;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Vetor;
import com.minerwk.rede.Acertou;
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

    @Override
    public void danoEmArea(double raio, float dano) {
        danoEm(vetor(jogador.position()), raio, dano);
    }

    @Override
    public void apresentacao(String animacao, String efeito, String som) {
        Envio.paraQuemVe(jogador, new TocarApresentacao(jogador.getId(), animacao, efeito, som));
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
                        || e instanceof FallingBlockEntity);
        for (Entity e : alvos) {
            Vec3 dir = centro.subtract(e.getBoundingBox().getCenter());
            if (dir.lengthSqr() < 0.25 && puxar) continue; // já está no centro
            Vec3 passo = dir.normalize().scale(puxar ? forca : -forca);
            if (!puxar) passo = passo.add(0, 0.1 * forca, 0);
            e.setDeltaMovement(e.getDeltaMovement().add(passo));
            e.hurtMarked = true; // manda a velocidade para o cliente (jogadores)
            e.hasImpulse = true; // e para as outras entidades
        }
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
    public void danoEm(Vetor centro, double raio, float dano) {
        var fonte = jogador.damageSources().playerAttack(jogador);
        for (LivingEntity alvo : naEsfera(LivingEntity.class, vec(centro), raio, e -> true)) alvo.hurt(fonte, dano);
    }

    @Override
    public int danoUmaVez(Vetor centro, double raio, float dano, Set<Object> atingidos) {
        var fonte = jogador.damageSources().playerAttack(jogador);
        int feridos = 0;
        for (LivingEntity alvo : naEsfera(LivingEntity.class, vec(centro), raio, e -> true)) {
            if (atingidos.add(alvo.getUUID()) && alvo.hurt(fonte, dano)) feridos++;
        }
        return feridos;
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
            if (!nivel.isLoaded(p) || nivel.isOutsideBuildHeight(p) || !nivel.mayInteract(jogador, p)) continue;
            BlockState estado = nivel.getBlockState(p);
            float dureza = estado.getDestroySpeed(nivel, p);
            if (estado.isAir() || dureza < 0 || dureza > durezaMax || estado.hasBlockEntity()
                    || !estado.getFluidState().isEmpty() || !nivel.getBlockState(p.above()).isAir()) continue;
            FallingBlockEntity.fall(nivel, p, estado);
            feitos++;
        }
        return feitos;
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
        BlockPos[] sorteados = new BlockPos[DETRITOS];
        BlockState[] estados = new BlockState[DETRITOS];
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-r, -r, -r), base.offset(r, r, r))) {
            if (p.getCenter().distanceToSqr(c) > raio * raio) continue;
            if (!nivel.isLoaded(p) || nivel.isOutsideBuildHeight(p) || !nivel.mayInteract(jogador, p)) continue;
            BlockState estado = nivel.getBlockState(p);
            if (estado.isAir() || estado.getDestroySpeed(nivel, p) < 0 || estado.hasBlockEntity()
                    || !estado.getFluidState().isEmpty()) continue;
            nivel.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
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
    public void infinito(int ticks) {
        ProtecoesServidor.ligarInfinito(jogador, ticks);
        // Projéteis que chegam perto param no ar e caem.
        for (Projectile p : nivel().getEntitiesOfClass(Projectile.class, jogador.getBoundingBox().inflate(1.5),
                p -> p.getOwner() != jogador)) {
            p.setDeltaMovement(Vec3.ZERO);
            p.hurtMarked = true;
        }
    }

    @Override
    public void curar(float vida) {
        jogador.heal(vida);
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
    public void criarEfeito(Efeito efeito) {
        kit().ifPresent(k -> k.adicionarEfeito(efeito));
    }

    @Override
    public void visual(String tipo, Vetor origem, Vetor direcao, float forca, int duracao) {
        Envio.paraQuemVe(jogador, new MostrarVisual(jogador.getId(), tipo, origem, direcao, forca, duracao,
                nivel().getGameTime()));
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
