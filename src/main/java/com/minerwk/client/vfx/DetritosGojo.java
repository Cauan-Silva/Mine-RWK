package com.minerwk.client.vfx;

import com.minerwk.client.ConfigCliente;
import com.minerwk.core.kit.Vetor;
import com.minerwk.core.vfx.PerfilVfxGojo;
import com.minerwk.core.vfx.TrajetoriaGojo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Partículas do jogo que dão peso aos golpes: chão levantado, faíscas, poeira. Só no cliente. */
public final class DetritosGojo {
    private static final double ALCANCE = 64;
    private static final DustParticleOptions FAISCA_VERMELHA = new DustParticleOptions(new Vector3f(1f, .15f, .12f), 1.6f);
    private static final BlockParticleOption FOLHA = new BlockParticleOption(ParticleTypes.BLOCK,
            net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState());
    private static final DustParticleOptions FAISCA_VIOLETA = new DustParticleOptions(new Vector3f(.75f, .4f, 1f), 1.4f);

    private DetritosGojo() {
    }

    private static int qtd(int completo) {
        return ConfigCliente.vfxReduzido() ? Math.max(1, completo / 2) : completo;
    }

    private static boolean perto(Vetor p) {
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return cam.distanceToSqr(p.x(), p.y(), p.z()) < ALCANCE * ALCANCE;
    }

    /** Primeiro bloco sólido até 4 abaixo de {@code (x, y, z)}, ou null. */
    private static BlockPos chao(ClientLevel l, double x, double y, double z) {
        BlockPos.MutableBlockPos p = BlockPos.containing(x, y, z).mutable();
        for (int i = 0; i < 5; i++, p.move(0, -1, 0)) {
            BlockState s = l.getBlockState(p);
            if (!s.isAir() && s.getFluidState().isEmpty()) return p.immutable();
        }
        return null;
    }

    private static void bloco(ClientLevel l, BlockPos pos, double x, double y, double z, double vx, double vy, double vz) {
        BlockState s = l.getBlockState(pos);
        if (s.isAir()) return;
        l.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, s), x, y, z, vx, vy, vz);
    }

    private static void varias(ClientLevel l, ParticleOptions tipo, int n, Vec3 c, double espalha, double vel) {
        RandomSource rnd = l.random;
        for (int i = 0; i < n; i++) {
            l.addParticle(tipo, c.x + (rnd.nextDouble() - .5) * espalha, c.y + (rnd.nextDouble() - .5) * espalha,
                    c.z + (rnd.nextDouble() - .5) * espalha, (rnd.nextDouble() - .5) * vel, (rnd.nextDouble() - .5) * vel,
                    (rnd.nextDouble() - .5) * vel);
        }
    }

    /** Um visual começou: o que acontece uma vez só. */
    public static void aoComecar(String tipo, Vetor origem) {
        ClientLevel l = Minecraft.getInstance().level;
        if (l != null && tipo.equals("gojo_roxo_explosao")) {
            explosaoRoxa(l, origem);
            return;
        }
        if (l == null || !tipo.equals("gojo_vazio_infinito") || !perto(origem)) return;
        // Anel de poeira que corre pelo chão até a borda do domínio.
        int n = qtd(36);
        double y = origem.y() - 1.5;
        double v = PerfilVfxGojo.DOMINIO_RAIO / 20.0;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            l.addParticle(ParticleTypes.CLOUD, origem.x() + Math.cos(a), y, origem.z() + Math.sin(a),
                    Math.cos(a) * v, .01, Math.sin(a) * v);
        }
    }

    /** O Roxo 200% explodiu em {@code p}: estouros espalhados pela esfera e fumaça grossa. */
    private static void explosaoRoxa(ClientLevel l, Vetor p) {
        RandomSource rnd = l.random;
        for (int i = 0; i < qtd(24); i++) {
            double r = 4 + rnd.nextDouble() * 34;
            double a = rnd.nextDouble() * Math.PI * 2, b = (rnd.nextDouble() - .5) * Math.PI;
            double x = p.x() + Math.cos(a) * Math.cos(b) * r, y = p.y() + Math.sin(b) * r, z = p.z() + Math.sin(a) * Math.cos(b) * r;
            l.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 0, 0, 0);
        }
        for (int i = 0; i < qtd(60); i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            l.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.x() + Math.cos(a) * 36 * rnd.nextDouble(), p.y(),
                    p.z() + Math.sin(a) * 36 * rnd.nextDouble(), 0, .08, 0);
        }
    }

    /** A esfera do Vermelho estourou em {@code p}. */
    public static void explosao(Vetor p) {
        ClientLevel l = Minecraft.getInstance().level;
        if (l == null || !perto(p)) return;
        RandomSource rnd = l.random;
        Vec3 c = new Vec3(p.x(), p.y(), p.z());
        int n = qtd(24);
        for (int i = 0; i < n; i++) {
            double a = (i + rnd.nextDouble() * .5) * Math.PI * 2 / n;
            double dx = Math.cos(a), dz = Math.sin(a);
            BlockPos chao = chao(l, c.x + dx * 1.2, c.y, c.z + dz * 1.2);
            if (chao == null) continue;
            bloco(l, chao, c.x + dx * 1.2, chao.getY() + 1.1, c.z + dz * 1.2,
                    dx * (.4 + rnd.nextDouble() * .3), .35 + rnd.nextDouble() * .35, dz * (.4 + rnd.nextDouble() * .3));
        }
        varias(l, FAISCA_VERMELHA, qtd(20), c, 1.2, .8);
        varias(l, ParticleTypes.CRIT, qtd(10), c, .6, 1.2);
        varias(l, ParticleTypes.POOF, qtd(10), c, 1.5, .3);
        l.addParticle(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0);
    }

    /** A cada tick, para os visuais em andamento. */
    public static void tick() {
        ClientLevel l = Minecraft.getInstance().level;
        if (l == null) return;
        long agora = l.getGameTime();
        RandomSource rnd = l.random;
        for (VisuaisAtivos.Visual v : VisuaisAtivos.ativos()) {
            if (v.fimEm >= 0 || v.opacidade(agora, 0) <= .05f) continue;
            double t = v.idade(agora, 0);
            switch (v.tipo) {
                case "gojo_azul" -> {
                    if (!perto(v.origem)) break;
                    for (int i = 0; i < qtd(3); i++) {
                        double a = rnd.nextDouble() * Math.PI * 2;
                        double r = 2 + rnd.nextDouble() * 2;
                        double x = v.origem.x() + Math.cos(a) * r;
                        double z = v.origem.z() + Math.sin(a) * r;
                        BlockPos chao = chao(l, x, v.origem.y() + 1, z);
                        if (chao == null) continue;
                        double y = chao.getY() + 1.1;
                        Vec3 puxa = new Vec3(v.origem.x() - x, v.origem.y() - y, v.origem.z() - z).scale(.12);
                        bloco(l, chao, x, y, z, puxa.x, puxa.y + .1, puxa.z);
                    }
                    // Poeira e folhas sugadas pelo ar até o núcleo.
                    for (int i = 0; i < qtd(3); i++) {
                        Vec3 de = new Vec3(v.origem.x(), v.origem.y(), v.origem.z())
                                .add((rnd.nextDouble() - .5) * 8, (rnd.nextDouble() - .5) * 4, (rnd.nextDouble() - .5) * 8);
                        Vec3 puxa = new Vec3(v.origem.x() - de.x, v.origem.y() - de.y, v.origem.z() - de.z).scale(.09);
                        l.addParticle(i == 0 ? FOLHA : ParticleTypes.WHITE_ASH, de.x, de.y, de.z, puxa.x, puxa.y, puxa.z);
                    }
                }
                case "gojo_infinito" -> {
                    // Poeira que entra devagar perto do Gojo e quase para: o tempo fica lento ali.
                    net.minecraft.world.entity.Entity e = l.getEntity(v.dono);
                    if (e == null || !perto(new Vetor(e.getX(), e.getY(), e.getZ()))) break;
                    Vec3 c = e.position().add(0, e.getBbHeight() / 2, 0);
                    for (int i = 0; i < qtd(2); i++) {
                        double a = rnd.nextDouble() * Math.PI * 2, y = (rnd.nextDouble() - .5) * 2;
                        Vec3 p = c.add(Math.cos(a) * 2.6, y, Math.sin(a) * 2.6);
                        Vec3 vel = c.subtract(p).scale(.02);
                        l.addParticle(ParticleTypes.WHITE_ASH, p.x, p.y, p.z, vel.x, vel.y, vel.z);
                    }
                }
                case "gojo_roxo", "gojo_roxo_200" -> {
                    boolean maior = v.tipo.equals("gojo_roxo_200");
                    Vetor c = TrajetoriaGojo.roxo(v.origem, v.direcao, VisuaisAtivos.passoRoxo(v.tipo), t - 1);
                    if (!perto(c)) break;
                    varias(l, FAISCA_VIOLETA, qtd(maior ? 8 : 4), new Vec3(c.x(), c.y(), c.z()), maior ? 10 : 3, .2);
                }
                case "gojo_roxo_rastro", "gojo_roxo_200_rastro" -> {
                    // Fumaça grossa subindo do túnel enquanto ele esfria.
                    double raio = v.tipo.startsWith("gojo_roxo_200") ? 7 : 1.5;
                    for (int i = 0; i < qtd(3); i++) {
                        Vetor p = v.origem.mais(v.direcao.vezes(rnd.nextDouble() * v.forca));
                        if (!perto(p)) continue;
                        l.addParticle(ParticleTypes.LARGE_SMOKE, p.x() + (rnd.nextDouble() - .5) * raio,
                                p.y() + (rnd.nextDouble() - .5) * raio, p.z() + (rnd.nextDouble() - .5) * raio, 0, .04, 0);
                    }
                }
                case "gojo_vermelho_explosao" -> {
                    if (!perto(v.origem)) break;
                    // Fumaça e brasas pairando e subindo devagar.
                    Vec3 c = new Vec3(v.origem.x(), v.origem.y(), v.origem.z());
                    for (int i = 0; i < qtd(2); i++) {
                        l.addParticle(ParticleTypes.SMOKE, c.x + (rnd.nextDouble() - .5) * 2, c.y + (rnd.nextDouble() - .5),
                                c.z + (rnd.nextDouble() - .5) * 2, 0, .03, 0);
                    }
                    l.addParticle(ParticleTypes.SMALL_FLAME, c.x + (rnd.nextDouble() - .5) * 1.5, c.y + (rnd.nextDouble() - .5),
                            c.z + (rnd.nextDouble() - .5) * 1.5, 0, .02, 0);
                }
                default -> {
                }
            }
        }
    }
}
