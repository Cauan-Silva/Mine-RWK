package com.minerwk.client.vfx;

import com.minerwk.core.kit.Vetor;
import com.minerwk.client.camera.CutsceneCliente;
import com.minerwk.core.personagens.cid.AtomicFinal;
import com.minerwk.core.personagens.cid.CutsceneAtomic;
import com.minerwk.core.personagens.cid.SistemaSolar;
import com.minerwk.core.vfx.FormasCid;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Os visuais do Cid: o slime é tinta preta, opaca, e a magia é azul-violeta e soma luz. Cor chapada com luz
 * por vértice, sem textura. Os tempos e as formas vêm de {@code core/vfx/FormasCid}; o servidor manda cada
 * visual como {@code MostrarVisual} com o tipo começando em {@code cid_}. Tudo relativo à câmera.
 */
final class RenderVisuaisCid {
    private static final int TINTA = 0x0B0912, EBANO = 0x1B1626, MAGIA = 0x7A5CFF, CLARO = 0xCBBEFF, ROXO = 0x5B2FA8;
    /** O miolo da luz atômica, quase branco. */
    private static final int BRANCO = 0xF4F0FF;
    private static final Vec3 CIMA = new Vec3(0, 1, 0);
    private static final double ALCANCE = 128;

    private static BufferBuilder buffer;

    private RenderVisuaisCid() {
    }

    static boolean eDoCid(String tipo) {
        return tipo.startsWith("cid_");
    }

    /** Desenha {@code v}; {@code t} é a idade em ticks. */
    static void desenhar(Vec3 camera, float parcial, VisuaisAtivos.Visual v, double t) {
        Vec3 o = new Vec3(v.origem.x() - camera.x, v.origem.y() - camera.y, v.origem.z() - camera.z);
        // A luz no céu é vista de longe: quem a recebe está a até 96 blocos do ponto, e ela fica bem acima.
        if (o.lengthSqr() > ALCANCE * ALCANCE && !v.tipo.equals("cid_atomic_luz")) return;
        // Visual encerrado antes da hora (o slime que bateu, o Overdrive desligado): some em poucos ticks.
        float sai = v.fimEm < 0 ? 1 : (float) Math.max(0, 1 - (t - (v.fimEm - v.inicio)) / VisuaisAtivos.SAIDA);
        try {
            switch (v.tipo) {
                case "cid_redemoinho" -> redemoinho(camera, parcial, v, o, t);
                case "cid_pos_imagem" -> posImagem(v, o, t);
                case "cid_slime" -> slime(camera, v, o, t, sai);
                case "cid_correntes" -> correntes(v, o, t);
                case "cid_overdrive" -> overdrive(camera, parcial, v, o, t, sai);
                case "cid_traje" -> traje(camera, parcial, v, o, t);
                case "cid_espada_carga" -> cargaDaEspada(camera, parcial, v, o, t, sai);
                case "cid_espada_atomica" -> corteAtomico(v, o, t);
                case "cid_recovery_carga" -> cargaDoRecovery(v, o, t, sai);
                case "cid_recovery" -> recovery(v, o, t);
                case "cid_atomic_soco" -> soco(v, o, t);
                case "cid_atomic_risco" -> risco(o, t);
                case "cid_atomic_aura" -> aura(camera, parcial, v, o, t, sai);
                case "cid_atomic" -> atomic(o, t);
                case "cid_atomic_luz" -> luzNoCeu(o, t);
                case "cid_atomic_onda" -> ondaNoChao(o, t);
                default -> {
                }
            }
        } finally {
            if (buffer != null) {
                // Um desenho falhou no meio do lote: joga fora o que estava pela metade.
                MeshData resto = buffer.build();
                if (resto != null) resto.close();
                buffer = null;
                Pincel.restaurar();
            }
        }
    }

    // ---------------------------------------------------------------- lotes e primitivas

    private static void abrir(boolean aditivo) {
        RenderSystem.enableBlend();
        if (aditivo) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    private static void fechar() {
        MeshData malha = buffer.build();
        buffer = null;
        if (malha != null) BufferUploader.drawWithShader(malha);
        Pincel.restaurar();
    }

    private static void ponto(Vec3 p, int cor, float alpha) {
        buffer.addVertex((float) p.x, (float) p.y, (float) p.z)
                .setColor((cor >> 16) & 255, (cor >> 8) & 255, cor & 255, (int) (Mth.clamp(alpha, 0, 1) * 255));
    }

    private static void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int cor, float alpha) {
        ponto(a, cor, alpha);
        ponto(b, cor, alpha);
        ponto(c, cor, alpha);
        ponto(d, cor, alpha);
    }

    private static int luz(int cor, double l) {
        int r = (int) Math.min(255, ((cor >> 16) & 255) * l), g = (int) Math.min(255, ((cor >> 8) & 255) * l);
        int b = (int) Math.min(255, (cor & 255) * l);
        return r << 16 | g << 8 | b;
    }

    /** Fita de {@code a} a {@code b} virada para a câmera (que está na origem). */
    private static void fita(Vec3 a, Vec3 b, double largura, int cor, float alphaA, float alphaB) {
        Vec3 l = b.subtract(a).cross(a.add(b).scale(.5));
        if (l.lengthSqr() < 1e-12) return;
        l = l.normalize().scale(largura / 2);
        ponto(a.subtract(l), cor, alphaA);
        ponto(a.add(l), cor, alphaA);
        ponto(b.add(l), cor, alphaB);
        ponto(b.subtract(l), cor, alphaB);
    }

    /** Brilho redondo virado para a câmera: cheio no centro, zero na borda. */
    private static void halo(Vec3 c, double raio, int cor, float alpha) {
        if (alpha <= .004f || c.lengthSqr() < 1e-8) return;
        Vec3 ver = c.normalize();
        Vec3 x = ver.cross(CIMA);
        x = (x.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : x.normalize()).scale(raio);
        Vec3 y = x.cross(ver);
        int n = 14;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            ponto(c, cor, alpha);
            ponto(c.add(x.scale(Math.cos(a0))).add(y.scale(Math.sin(a0))), cor, 0);
            ponto(c.add(x.scale(Math.cos(a1))).add(y.scale(Math.sin(a1))), cor, 0);
            ponto(c, cor, alpha);
        }
    }

    /** Anel deitado: {@code dentro} e {@code fora} são as opacidades das duas beiradas. */
    private static void anel(Vec3 c, double r0, double r1, int cor, float dentro, float fora) {
        int n = 32;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            Vec3 u0 = new Vec3(Math.cos(a0), 0, Math.sin(a0)), u1 = new Vec3(Math.cos(a1), 0, Math.sin(a1));
            ponto(c.add(u0.scale(r0)), cor, dentro);
            ponto(c.add(u0.scale(r1)), cor, fora);
            ponto(c.add(u1.scale(r1)), cor, fora);
            ponto(c.add(u1.scale(r0)), cor, dentro);
        }
    }

    /** Caixa alinhada ao mundo, com cada face num tom (parece sólida sem textura). */
    private static void caixa(Vec3 c, double hx, double hy, double hz, int cor, float alpha) {
        Vec3 a = c.add(-hx, -hy, -hz), b = c.add(hx, -hy, -hz), d = c.add(hx, -hy, hz), e = c.add(-hx, -hy, hz);
        Vec3 f = c.add(-hx, hy, -hz), g = c.add(hx, hy, -hz), h = c.add(hx, hy, hz), i = c.add(-hx, hy, hz);
        quad(f, g, h, i, luz(cor, 1.6), alpha);
        quad(a, b, d, e, luz(cor, .6), alpha);
        quad(a, b, g, f, cor, alpha);
        quad(e, d, h, i, cor, alpha);
        quad(a, e, i, f, luz(cor, .8), alpha);
        quad(b, d, h, g, luz(cor, .8), alpha);
    }

    /** Parede de cilindro em pé, de {@code y0} a {@code y1}, com a opacidade de cada beirada. */
    private static void parede(Vec3 c, double raio, double y0, double y1, int cor, float embaixo, float emCima) {
        int n = 32;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            Vec3 u0 = new Vec3(Math.cos(a0) * raio, 0, Math.sin(a0) * raio), u1 = new Vec3(Math.cos(a1) * raio, 0, Math.sin(a1) * raio);
            ponto(c.add(u0).add(0, y0, 0), cor, embaixo);
            ponto(c.add(u0).add(0, y1, 0), cor, emCima);
            ponto(c.add(u1).add(0, y1, 0), cor, emCima);
            ponto(c.add(u1).add(0, y0, 0), cor, embaixo);
        }
    }

    private static double acaso(double n) {
        double x = Math.sin(n * 127.1 + 311.7) * 43758.5453;
        return x - Math.floor(x);
    }

    private static Vec3 vec(Vetor v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    /** Onde o dono do visual está agora, relativo à câmera (ou {@code reserva} se ele não estiver no mundo). */
    private static Vec3 pes(Vec3 camera, float parcial, VisuaisAtivos.Visual v, Vec3 reserva) {
        Entity e = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(v.dono);
        return e == null ? reserva : e.getPosition(parcial).subtract(camera);
    }

    private static boolean euEmPrimeiraPessoa(VisuaisAtivos.Visual v) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getId() == v.dono && mc.options.getCameraType().isFirstPerson();
    }

    // ---------------------------------------------------------------- Z: Redemoinho de Ébano

    /** Três cortes em volta dele: a espada de slime esticada varre um arco preto, com o fio de magia na ponta. */
    private static void redemoinho(Vec3 camera, float parcial, VisuaisAtivos.Visual v, Vec3 o, double t) {
        Vec3 c = pes(camera, parcial, v, o.add(0, -1.62, 0)).add(0, 1.0, 0);
        Vec3 d = vec(v.direcao);
        double frente = Math.atan2(d.z, d.x), raio = v.forca;
        int n = 26;
        for (int corte = 0; corte < FormasCid.CORTES; corte++) {
            double idade = t - corte * FormasCid.ENTRE_CORTES;
            float vida = (float) FormasCid.corte(idade);
            if (vida <= .01f) continue;
            // Cada corte gira para um lado e vem num plano um pouco tombado.
            double sentido = corte % 2 == 0 ? 1 : -1, tombo = FormasCid.TOMBO * sentido;
            double cabeca = frente + sentido * FormasCid.angulo(idade) + corte * 2.1;
            Vec3[] fora = new Vec3[n + 1], dentro = new Vec3[n + 1];
            float[] alpha = new float[n + 1];
            for (int i = 0; i <= n; i++) {
                double u = i / (double) n, a = cabeca - sentido * FormasCid.CAUDA * u;
                Vec3 rumo = new Vec3(Math.cos(a), Math.sin(a - frente) * tombo, Math.sin(a));
                fora[i] = c.add(rumo.scale(raio));
                dentro[i] = c.add(rumo.scale(raio * (.5 + .3 * u)));
                alpha[i] = vida * (float) Math.pow(1 - u, 1.4);
            }
            abrir(false);
            for (int i = 0; i < n; i++) {
                ponto(dentro[i], TINTA, 0);
                ponto(fora[i], TINTA, alpha[i] * .92f);
                ponto(fora[i + 1], TINTA, alpha[i + 1] * .92f);
                ponto(dentro[i + 1], TINTA, 0);
            }
            fechar();
            abrir(true);
            for (int i = 0; i < n; i++) {
                fita(fora[i], fora[i + 1], .09, CLARO, alpha[i], alpha[i + 1]);
                fita(fora[i], fora[i + 1], .5, MAGIA, alpha[i] * .4f, alpha[i + 1] * .4f);
            }
            halo(fora[0], .7, MAGIA, vida * .6f);
            fechar();
        }
        // A poeira de magia que o giro levanta no chão.
        float chao = (float) Math.max(0, 1 - t / (FormasCid.CORTES * FormasCid.ENTRE_CORTES + 4.0));
        abrir(true);
        Vec3 solo = c.add(0, -.95, 0);
        anel(solo, raio * .25, raio * (.5 + .5 * (1 - chao)), ROXO, 0, chao * .35f);
        fechar();
    }

    // ---------------------------------------------------------------- X: Pós-imagem

    /** O vulto que fica onde ele estava, tremendo e se desfazendo, e os riscos da partida. */
    private static void posImagem(VisuaisAtivos.Visual v, Vec3 o, double t) {
        float vulto = (float) FormasCid.vulto(t, v.duracao);
        Vec3 d = vec(v.direcao);
        Vec3 lado = d.cross(CIMA);
        lado = lado.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : lado.normalize();
        if (vulto > .01f) {
            // O corpo em três caixas (pernas, tronco com o sobretudo, cabeça com o capuz), que tremem de lado.
            double treme = (acaso(Math.floor(t / 2)) - .5) * .06 * (1 + t / v.duracao);
            Vec3 base = o.add(lado.scale(treme)).add(0, -1.62, 0);
            abrir(false);
            caixa(base.add(0, .4, 0), .24, .4, .2, TINTA, vulto * .85f);
            caixa(base.add(0, 1.1, 0), .32, .36, .22, EBANO, vulto * .85f);
            caixa(base.add(0, 1.72, 0), .26, .26, .26, TINTA, vulto * .85f);
            fechar();
            abrir(true);
            // A magia escorre do vulto para cima enquanto ele se desfaz.
            for (int k = 0; k < 9; k++) {
                double fase = (t * .05 + acaso(k)) % 1, a = Math.PI * 2 * acaso(k + 9);
                Vec3 p = base.add(Math.cos(a) * .34, .2 + fase * 1.9, Math.sin(a) * .34);
                fita(p, p.add(0, .28, 0), .05, MAGIA, 0, vulto * (float) Math.sin(Math.PI * fase) * .9f);
            }
            halo(base.add(0, 1.1, 0), 1.1, MAGIA, vulto * .22f);
            fechar();
        }
        // Os riscos da partida, nos primeiros ticks.
        float risco = (float) Math.max(0, 1 - t / FormasCid.RISCOS);
        if (risco <= .01f) return;
        double cabeca = Math.min(1, t / 2), cauda = Mth.clamp((t - 2) / 5, 0, 1);
        abrir(false);
        riscos(o, d, lado, cabeca, cauda, .12, TINTA, risco * .8f);
        fechar();
        abrir(true);
        riscos(o, d, lado, cabeca, cauda, .05, MAGIA, risco * .85f);
        fechar();
    }

    private static void riscos(Vec3 o, Vec3 d, Vec3 lado, double cabeca, double cauda, double largura, int cor, float alpha) {
        Vec3 caminho = d.scale(FormasCid.CAMINHO);
        for (int k = 0; k < 14; k++) {
            double x = (acaso(k) - .5) * 1.1, y = -1.5 + acaso(k + 20) * 1.8;
            double s0 = Math.min(1, cauda + .15 * acaso(k + 40)), s1 = Math.max(s0, cabeca - .1 * acaso(k + 50));
            Vec3 base = o.add(lado.scale(x)).add(0, y, 0);
            fita(base.add(caminho.scale(s0)), base.add(caminho.scale(s1)), largura, cor, 0, alpha);
        }
    }

    // ---------------------------------------------------------------- C: Prisão de Correntes Negras

    /** O pedaço de slime que voa: um miolo preto que ondula, gotas ficando para trás e o respingo onde bate. */
    private static void slime(Vec3 camera, VisuaisAtivos.Visual v, Vec3 o, double t, float sai) {
        Vec3 d = vec(v.direcao);
        if (d.lengthSqr() < 1e-8) return;
        d = d.normalize();
        Vec3 p = o.add(d.scale(FormasCid.voo(t)));
        boolean bateu = v.pontoFim != null;
        if (bateu) p = new Vec3(v.pontoFim.x() - camera.x, v.pontoFim.y() - camera.y, v.pontoFim.z() - camera.z);
        if (bateu) {
            // O respingo: gotas que abrem em volta do ponto e caem.
            double e = 1 - sai;
            abrir(false);
            for (int k = 0; k < 12; k++) {
                double a = Math.PI * 2 * acaso(k), b = (acaso(k + 5) - .3) * 1.4, r = e * (.5 + acaso(k + 11));
                double lado = .09 * sai + .02;
                caixa(p.add(Math.cos(a) * r, b * e - e * e * .5, Math.sin(a) * r), lado, lado, lado, TINTA, sai);
            }
            fechar();
            abrir(true);
            halo(p, .9 + e, MAGIA, sai * .5f);
            fechar();
            return;
        }
        // Não bateu em nada: some no fim do alcance.
        float voa = (float) FormasCid.slime(t);
        if (voa <= .01f) return;
        abrir(false);
        double pulso = .04 * Math.sin(t * 1.9);
        caixa(p, .26 + pulso, .26 - pulso, .26 + pulso, TINTA, voa);
        for (int k = 1; k <= 6; k++) {
            // As gotas da cauda: mais para trás, menores e um pouco fora do eixo.
            double atras = k * .34, lado = .2 * (1 - k / 7.0);
            Vec3 q = p.subtract(d.scale(atras)).add((acaso(k + Math.floor(t)) - .5) * .22, (acaso(k + 3) - .5) * .22 - k * .012, 0);
            if (q.subtract(o).dot(d) < 0) continue;
            caixa(q, lado, lado, lado, k % 2 == 0 ? EBANO : TINTA, voa * (1 - k / 8f));
        }
        fechar();
        abrir(true);
        halo(p, .7, MAGIA, voa * .45f);
        fita(p.subtract(d.scale(2.2)), p, .1, MAGIA, 0, voa * .6f);
        fechar();
    }

    /** As correntes: sobem do chão em volta do alvo, cruzam nele e o seguram até soltarem. */
    private static void correntes(VisuaisAtivos.Visual v, Vec3 o, double t) {
        // O servidor manda o meio do corpo do alvo.
        Vec3 chao = o.add(0, -FormasCid.MEIO_DO_ALVO, 0);
        float vida = (float) FormasCid.correntes(t, v.duracao);
        if (vida <= .01f) return;
        double cresce = Mth.clamp(t / FormasCid.FECHAR, 0, 1);
        abrir(false);
        // A poça de slime de onde elas saem.
        anel(chao.add(0, .04, 0), 0, FormasCid.RAIO_DA_PRISAO * (.4 + .6 * cresce), TINTA, vida * .7f, vida * .25f);
        for (int k = 0; k < FormasCid.CORRENTES; k++) {
            double a = Math.PI * 2 * k / FormasCid.CORRENTES + .4;
            Vec3 pe = chao.add(Math.cos(a) * FormasCid.RAIO_DA_PRISAO, .05, Math.sin(a) * FormasCid.RAIO_DA_PRISAO);
            // Cada uma passa pelo corpo e prende do outro lado, numa altura diferente.
            Vec3 ponta = chao.add(-Math.cos(a) * .45, .45 + 1.25 * k / (FormasCid.CORRENTES - 1.0), -Math.sin(a) * .45);
            elos(pe, pe.add(ponta.subtract(pe).scale(cresce)), vida, t + k);
        }
        fechar();
        abrir(true);
        for (int k = 0; k < FormasCid.CORRENTES; k++) {
            double a = Math.PI * 2 * k / FormasCid.CORRENTES + .4;
            Vec3 pe = chao.add(Math.cos(a) * FormasCid.RAIO_DA_PRISAO, .05, Math.sin(a) * FormasCid.RAIO_DA_PRISAO);
            Vec3 ponta = chao.add(-Math.cos(a) * .45, .45 + 1.25 * k / (FormasCid.CORRENTES - 1.0), -Math.sin(a) * .45);
            fita(pe, pe.add(ponta.subtract(pe).scale(cresce)), .16, MAGIA, vida * .1f, vida * .3f);
        }
        // O estalo de quando elas fecham.
        float estalo = (float) Math.max(0, 1 - Math.abs(t - FormasCid.FECHAR) / 3);
        halo(o, 1.6, CLARO, estalo * .7f);
        fechar();
    }

    /** Uma corrente de {@code a} a {@code b}: elos pretos, um deitado e um de pé, que tremem esticados. */
    private static void elos(Vec3 a, Vec3 b, float alpha, double semente) {
        Vec3 d = b.subtract(a);
        int n = Math.max(1, (int) (d.length() / .22));
        for (int i = 0; i < n; i++) {
            Vec3 p = a.add(d.scale((i + .5) / n)).add(0, Math.sin(semente * 1.7 + i * .9) * .012, 0);
            if (i % 2 == 0) caixa(p, .085, .04, .085, TINTA, alpha);
            else caixa(p, .04, .085, .04, EBANO, alpha);
        }
    }

    // ---------------------------------------------------------------- Passiva: o traje de slime

    /** O traje amortece um golpe forte: o slime salta do corpo em gotas e um clarão de magia marca o momento. */
    private static void traje(Vec3 camera, float parcial, VisuaisAtivos.Visual v, Vec3 o, double t) {
        float vida = (float) Math.max(0, 1 - t / FormasCid.TRAJE);
        if (vida <= .01f || euEmPrimeiraPessoa(v)) return;
        Vec3 meio = pes(camera, parcial, v, o.add(0, -1.62, 0)).add(0, 1.0, 0);
        double e = 1 - vida;
        abrir(false);
        for (int k = 0; k < 14; k++) {
            double a = Math.PI * 2 * acaso(k), sobe = (acaso(k + 5) - .5) * 1.4, r = .45 + e * (.5 + .6 * acaso(k + 11));
            double lado = .07 * vida + .015;
            caixa(meio.add(Math.cos(a) * r, sobe - e * e * .5, Math.sin(a) * r), lado, lado, lado, TINTA, vida);
        }
        fechar();
        abrir(true);
        halo(meio, 1.2 + e, MAGIA, vida * .4f);
        fechar();
    }

    // ---------------------------------------------------------------- G: Overdrive

    /** A magia transbordando: o estouro ao ligar, fios azul-violeta subindo em volta do corpo e o anel nos pés. */
    private static void overdrive(Vec3 camera, float parcial, VisuaisAtivos.Visual v, Vec3 o, double t, float sai) {
        Vec3 pes = pes(camera, parcial, v, o.add(0, -1.62, 0));
        float vida = sai * (float) FormasCid.aura(t, v.duracao);
        if (vida <= .01f) return;
        boolean fios = !euEmPrimeiraPessoa(v);
        // Por baixo da luz, os mesmos fios em roxo opaco: de dia a luz sozinha some contra o céu.
        if (fios) {
            abrir(false);
            for (int k = 0; k < FormasCid.FIOS; k++) fio(pes, k, t, .11, ROXO, vida * .75f);
            fechar();
        }
        abrir(true);
        // O estouro: um anel que abre no chão nos primeiros ticks.
        float estouro = (float) Math.max(0, 1 - t / FormasCid.ESTOURO);
        if (estouro > 0) {
            double r = FormasCid.raioDoEstouro(t);
            anel(pes.add(0, .06, 0), r * .7, r, MAGIA, 0, estouro * .8f);
            halo(pes.add(0, 1, 0), 2.2, CLARO, estouro * .5f);
        }
        double pulso = .5 + .5 * Math.sin(t * .35);
        anel(pes.add(0, .05, 0), .35, .85 + .1 * pulso, MAGIA, 0, vida * (.35f + .2f * (float) pulso));
        // Em primeira pessoa os fios passariam na frente da câmera: fica só o anel.
        if (fios) {
            for (int k = 0; k < FormasCid.FIOS; k++) fio(pes, k, t, .06, k % 3 == 0 ? CLARO : MAGIA, vida);
            halo(pes.add(0, 1.0, 0), 1.4, MAGIA, vida * .22f);
        }
        fechar();
    }

    /** Um fio da aura: sobe em espiral em volta do corpo, some no alto e recomeça. */
    private static void fio(Vec3 pes, int k, double t, double largura, int cor, float alpha) {
        double fase = FormasCid.fio(k, t), a = Math.PI * 2 * acaso(k) + t * .04, r = .42 + .2 * acaso(k + 7);
        Vec3 p = pes.add(Math.cos(a) * r, fase * 2.1, Math.sin(a) * r);
        Vec3 q = pes.add(Math.cos(a + .25) * r * .9, fase * 2.1 + .6, Math.sin(a + .25) * r * .9);
        fita(p, q, largura, cor, 0, alpha * (float) Math.sin(Math.PI * fase));
    }

    // ---------------------------------------------------------------- V: Espada Atômica

    /** A carga: a magia corre do chão e do ar para a lâmina, que acende. */
    private static void cargaDaEspada(Vec3 camera, float parcial, VisuaisAtivos.Visual v, Vec3 o, double t, float sai) {
        Vec3 pes = pes(camera, parcial, v, o.add(0, -1.62, 0));
        float junta = sai * (float) FormasCid.cargaDaEspada(t);
        float vida = sai * (float) Math.min(1, t / 2);
        if (vida <= .01f) return;
        Vec3 d = vec(v.direcao);
        Vec3 frente = new Vec3(d.x, 0, d.z);
        frente = frente.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : frente.normalize();
        // A lâmina, erguida ao lado do corpo.
        Vec3 lamina = pes.add(frente.scale(.35)).add(frente.cross(CIMA).scale(.45)).add(0, 1.5, 0);
        abrir(true);
        // O anel no chão fecha em direção aos pés conforme a carga junta.
        double r = 3.2 - 2.4 * junta;
        anel(pes.add(0, .05, 0), r * .8, r, MAGIA, 0, vida * .6f);
        if (!euEmPrimeiraPessoa(v)) {
            for (int k = 0; k < 16; k++) {
                // Cada risco nasce longe e corre para a lâmina.
                double fase = (t / 7 + acaso(k)) % 1, a = Math.PI * 2 * acaso(k + 30), sobe = acaso(k + 60) * 2.2;
                Vec3 longe = pes.add(Math.cos(a) * 3, sobe, Math.sin(a) * 3);
                Vec3 p0 = longe.add(lamina.subtract(longe).scale(fase)), p1 = longe.add(lamina.subtract(longe).scale(Math.min(1, fase + .22)));
                fita(p0, p1, .05, k % 3 == 0 ? CLARO : MAGIA, 0, vida * (float) Math.sin(Math.PI * fase));
            }
            fita(lamina.add(0, -.5, 0), lamina.add(0, .9, 0), .1 + .1 * junta, BRANCO, vida * .3f, junta);
            halo(lamina.add(0, .3, 0), .5 + 1.1 * junta, MAGIA, .25f * vida + .55f * junta);
            halo(lamina.add(0, .3, 0), .25 + .4 * junta, BRANCO, junta * .8f);
        }
        fechar();
    }

    /** O corte: uma meia-lua de luz que sai da lâmina e voa deitada até o alcance, com tinta por baixo. */
    private static void corteAtomico(VisuaisAtivos.Visual v, Vec3 o, double t) {
        float vida = (float) FormasCid.corteAtomico(t);
        if (vida <= .01f) return;
        Vec3 d = vec(v.direcao);
        // Olhando reto para cima ou para baixo não há frente: o corte (e o dano) dá a volta toda.
        boolean semFrente = d.x * d.x + d.z * d.z < 1e-8;
        double frente = Math.atan2(d.z, d.x), meio = semFrente ? Math.PI : Math.toRadians(FormasCid.ARCO_DO_CORTE / 2);
        double raio = FormasCid.raioDoCorte(t), cauda = Math.max(.6, raio * .55);
        // Um pouco abaixo dos olhos: na altura do peito, onde a espada passa.
        Vec3 c = o.add(0, -.45, 0);
        int n = 28;
        Vec3[] fora = new Vec3[n + 1], dentro = new Vec3[n + 1];
        float[] alpha = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            double u = i / (double) n, a = frente - meio + 2 * meio * u;
            Vec3 rumo = new Vec3(Math.cos(a), 0, Math.sin(a));
            // A meia-lua é mais grossa no meio e afina nas pontas.
            double gordo = Math.sin(Math.PI * u);
            fora[i] = c.add(rumo.scale(raio));
            dentro[i] = c.add(rumo.scale(raio - cauda * (.25 + .75 * gordo)));
            alpha[i] = vida * (float) (.35 + .65 * gordo);
        }
        abrir(false);
        for (int i = 0; i < n; i++) {
            ponto(dentro[i], TINTA, 0);
            ponto(fora[i], TINTA, alpha[i] * .55f);
            ponto(fora[i + 1], TINTA, alpha[i + 1] * .55f);
            ponto(dentro[i + 1], TINTA, 0);
            // Por baixo da luz, o fio em roxo opaco: de dia a luz sozinha some contra o céu e a grama.
            fita(fora[i], fora[i + 1], .7, ROXO, alpha[i] * .8f, alpha[i + 1] * .8f);
        }
        fechar();
        abrir(true);
        for (int i = 0; i < n; i++) {
            ponto(dentro[i], MAGIA, 0);
            ponto(fora[i], MAGIA, alpha[i] * .75f);
            ponto(fora[i + 1], MAGIA, alpha[i + 1] * .75f);
            ponto(dentro[i + 1], MAGIA, 0);
            fita(fora[i], fora[i + 1], 1.5, MAGIA, alpha[i] * .5f, alpha[i + 1] * .5f);
            fita(fora[i], fora[i + 1], .32, BRANCO, alpha[i], alpha[i + 1]);
        }
        // O clarão na lâmina, no instante do corte.
        float clarao = (float) Math.max(0, 1 - t / 3);
        halo(c.add(new Vec3(d.x, 0, d.z).scale(1.2)), 2.4, CLARO, clarao * .8f);
        // Faíscas que ficam para trás da frente do corte.
        for (int k = 0; k < 18; k++) {
            double a = frente - meio + 2 * meio * acaso(k), r = raio * (.35 + .6 * acaso(k + 20));
            Vec3 p = c.add(Math.cos(a) * r, (acaso(k + 40) - .5) * .5, Math.sin(a) * r);
            fita(p, p.add(Math.cos(a) * .7, 0, Math.sin(a) * .7), .05, CLARO, 0, vida * .8f);
        }
        fechar();
    }

    // ---------------------------------------------------------------- Agachado + R: Recovery Atomic

    /** A carga: a espada cravada acende, o círculo abre no chão até o raio da cura e a luz sobe devagar. */
    private static void cargaDoRecovery(VisuaisAtivos.Visual v, Vec3 o, double t, float sai) {
        float vida = sai * (float) Math.min(1, t / 4);
        if (vida <= .01f) return;
        double u = Mth.clamp(t / FormasCid.CARGA_DO_RECOVERY, 0, 1), r = FormasCid.circulo(t);
        Vec3 chao = o.add(0, .06, 0);
        boolean primeira = euEmPrimeiraPessoa(v);
        // Por baixo da luz, o círculo em roxo opaco: de dia a luz sozinha some contra a grama.
        abrir(false);
        anel(chao, r - .5, r, ROXO, vida * .25f, vida * .85f);
        anel(chao, 0, r - .5, ROXO, vida * .1f, vida * (.1f + .15f * (float) u));
        anel(chao, 1.2, 1.6, ROXO, vida * .8f, vida * .8f);
        fechar();
        abrir(true);
        // O círculo que cresce, e dois anéis fixos em volta da espada.
        anel(chao, r - .35, r, MAGIA, 0, vida * .8f);
        anel(chao, r - .08, r, BRANCO, vida * .6f, vida * .6f);
        anel(chao, 0, r, MAGIA, vida * .04f, vida * (.05f + .1f * (float) u));
        anel(chao, 1.3, 1.5, CLARO, vida * .7f, vida * .7f);
        for (int k = 0; k < 8; k++) {
            // Os raios do selo, girando devagar.
            double a = Math.PI * 2 * k / 8 + t * .02;
            Vec3 p0 = chao.add(Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5), p1 = chao.add(Math.cos(a) * Math.min(r, 3.2), 0, Math.sin(a) * Math.min(r, 3.2));
            ponto(p0.add(Math.cos(a + 1.57) * .04, 0, Math.sin(a + 1.57) * .04), CLARO, vida * .6f);
            ponto(p0.subtract(Math.cos(a + 1.57) * .04, 0, Math.sin(a + 1.57) * .04), CLARO, vida * .6f);
            ponto(p1.subtract(Math.cos(a + 1.57) * .04, 0, Math.sin(a + 1.57) * .04), CLARO, 0);
            ponto(p1.add(Math.cos(a + 1.57) * .04, 0, Math.sin(a + 1.57) * .04), CLARO, 0);
        }
        // A luz que sobe do chão dentro do círculo, mais densa perto do fim.
        for (int k = 0; k < 60; k++) {
            double raioK = Math.sqrt(acaso(k)) * FormasCid.RAIO_DO_RECOVERY;
            if (raioK > r) continue;
            double a = Math.PI * 2 * acaso(k + 100), fase = (t / (26 + 20 * acaso(k + 200)) + acaso(k + 300)) % 1;
            Vec3 p = chao.add(Math.cos(a) * raioK, fase * (1.5 + 2.5 * u), Math.sin(a) * raioK);
            fita(p, p.add(0, .5 + .7 * u, 0), .1, k % 4 == 0 ? BRANCO : CLARO, 0, vida * (float) Math.sin(Math.PI * fase));
        }
        if (!primeira) {
            // A espada cravada à frente dele: um fio de luz que cresce para cima.
            fita(chao, chao.add(0, 1.2 + 5 * u * u, 0), .12 + .2 * u, BRANCO, vida, 0);
            halo(chao.add(0, .9, 0), 1 + 1.6 * u, MAGIA, vida * (.25f + .35f * (float) u));
        }
        fechar();
    }

    /** A coluna de luz: o pilar no Cid, a onda que corre pelo chão até o raio da cura e a luz subindo em tudo. */
    private static void recovery(VisuaisAtivos.Visual v, Vec3 o, double t) {
        float vida = (float) FormasCid.coluna(t);
        if (vida <= .01f) return;
        double raio = FormasCid.RAIO_DO_RECOVERY, onda = FormasCid.onda(t), u = t / FormasCid.COLUNA;
        Vec3 chao = o.add(0, .06, 0);
        boolean primeira = euEmPrimeiraPessoa(v);
        abrir(true);
        // O chão inteiro do círculo aceso, e a borda.
        anel(chao, 0, raio, MAGIA, vida * .1f, vida * .16f);
        anel(chao, raio - .3, raio, CLARO, 0, vida * .8f);
        // A onda de cura.
        float frente = (float) Math.max(0, 1 - t / (FormasCid.ONDA + 4.0));
        anel(chao, onda * .75, onda, BRANCO, 0, frente);
        parede(chao, onda, 0, 1.6, CLARO, frente * .5f, 0);
        // A parede de luz na borda: baixa, para não tapar quem está dentro.
        parede(chao, raio, 0, 5 + 3 * u, MAGIA, vida * .3f, 0);
        // O pilar no meio. Em primeira pessoa ele taparia a tela: fica só o resto.
        if (!primeira) {
            double grosso = 1.5 * (1 - .5 * u);
            parede(chao, grosso, 0, 40, MAGIA, vida * .55f, 0);
            parede(chao, grosso * .55, 0, 40, BRANCO, vida * .7f, 0);
            halo(chao.add(0, 1.2, 0), 4.5, CLARO, vida * .5f);
        }
        // A luz subindo por todo o círculo.
        for (int k = 0; k < 110; k++) {
            double raioK = Math.sqrt(acaso(k)) * raio, a = Math.PI * 2 * acaso(k + 100);
            if (raioK > onda + .5 || (primeira && raioK < 1.2)) continue;
            double fase = (t / (14 + 12 * acaso(k + 200)) + acaso(k + 300)) % 1;
            Vec3 p = chao.add(Math.cos(a) * raioK, fase * 6, Math.sin(a) * raioK);
            fita(p, p.add(0, 1.1, 0), .07, k % 3 == 0 ? BRANCO : CLARO, 0, vida * (float) Math.sin(Math.PI * fase));
        }
        fechar();
    }

    // ---------------------------------------------------------------- R: I Am Atomic final

    /** Aro de luz num plano qualquer (o anel deitado só serve para o chão): {@code e1} e {@code e2} são os eixos dele. */
    private static void aro(Vec3 c, Vec3 e1, Vec3 e2, double raio, double largura, int cor, float alpha) {
        int n = 28;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            fita(c.add(e1.scale(Math.cos(a0) * raio)).add(e2.scale(Math.sin(a0) * raio)),
                    c.add(e1.scale(Math.cos(a1) * raio)).add(e2.scale(Math.sin(a1) * raio)), largura, cor, alpha, alpha);
        }
    }

    /**
     * Um soco. Nos ticks da parada de impacto, o clarão seco no ponto do golpe; depois a onda de choque abre
     * em aro, atravessada ao arremesso, e o rastro de luz segue o alvo. {@code forca} diz o tipo: 0 no mundo
     * (para cima), 1 no espaço (para a frente), 2 o arremesso final e 3 o da passagem.
     */
    private static void soco(VisuaisAtivos.Visual v, Vec3 o, double t) {
        int tipo = Math.round(v.forca);
        boolean forte = tipo >= 2;
        double tam = forte ? 1.8 : 1;
        Vec3 d = vec(v.direcao);
        Vec3 lado = d.cross(CIMA);
        lado = lado.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : lado.normalize();
        // Para onde o alvo é arremessado, e os dois eixos do aro atravessado a esse rumo.
        Vec3 rumo = tipo == 0 || tipo == 3 ? CIMA : d;
        Vec3 e1 = lado, e2 = rumo == CIMA ? d : CIMA;
        double u = Math.min(1, t / AtomicFinal.ENTRE_SOCOS);
        float some = (float) (1 - u);
        abrir(true);
        // A parada: tudo branco no ponto, sem se mexer.
        if (t < AtomicFinal.PARADA) {
            float p = (float) (1 - t / (AtomicFinal.PARADA + 1));
            halo(o, 3.2 * tam, BRANCO, p);
            halo(o, 5.5 * tam, CLARO, p * .7f);
            halo(o, 9 * tam, MAGIA, p * .4f);
            for (int k = 0; k < 8; k++) {
                double a = Math.PI * k / 4 + acaso(k + v.inicio);
                Vec3 r = e1.scale(Math.cos(a)).add(e2.scale(Math.sin(a)));
                fita(o.subtract(r.scale(4.5 * tam)), o.add(r.scale(4.5 * tam)), .09 * tam, BRANCO, p * .8f, p * .8f);
            }
        }
        // O arremesso: a onda abre e o rastro segue o alvo.
        double s = Math.max(0, t - AtomicFinal.PARADA) / (AtomicFinal.ENTRE_SOCOS - AtomicFinal.PARADA);
        if (s > 0) {
            double abre = 1 - (1 - s) * (1 - s);
            float f = (float) (1 - s);
            aro(o, e1, e2, (1 + 5 * abre) * tam, (.6 - .4 * abre) * tam, MAGIA, f * .9f);
            aro(o.add(rumo.scale(1.5 * abre * tam)), e1, e2, (.6 + 4 * abre) * tam, .4 * tam, MAGIA, f * .7f);
            aro(o.add(rumo.scale(3.5 * abre * tam)), e1, e2, (.4 + 2.2 * abre) * tam, .25 * tam, BRANCO, f * .6f);
            double vai = (rumo == CIMA ? AtomicFinal.SUBIDA : forte ? AtomicFinal.ARREMESSO : AtomicFinal.EMPURRAO) * abre;
            if (tipo == 3) vai = 60 * abre;
            fita(o, o.add(rumo.scale(vai)), .9 * tam * (1 - s), MAGIA, 0, f * .8f);
            fita(o, o.add(rumo.scale(vai)), .3 * tam * (1 - s), BRANCO, 0, f);
        }
        halo(o, .5 + 1.6 * u, MAGIA, some * .8f);
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * (k + acaso(k + v.inicio)) / 12;
            Vec3 r = e1.scale(Math.cos(a)).add(e2.scale(Math.sin(a)));
            fita(o.add(r.scale(.3 + 1.2 * u * tam)), o.add(r.scale(.8 + 3.4 * u * tam)), .1, CLARO, some, 0);
        }
        fechar();
    }

    /** O risco roxo que fica quando os dois somem para cima: uma linha de luz até o céu, que afina e some. */
    private static void risco(Vec3 o, double t) {
        double u = Math.min(1, t / AtomicFinal.TICKS_DO_RISCO);
        float some = (float) ((1 - u) * (1 - u));
        Vec3 topo = o.add(0, 160, 0);
        abrir(true);
        fita(o, topo, 2.2 * (1 - u) + .2, ROXO, some * .8f, 0);
        fita(o, topo, .9 * (1 - u) + .08, MAGIA, some, 0);
        fita(o, topo, .3 * (1 - u) + .03, BRANCO, some, 0);
        halo(o, 3.5 * (1 - u * .5), MAGIA, some * .7f);
        fechar();
    }

    /**
     * As palavras: a magia roxa cresce em volta dele, parada, enquanto ele diz as duas primeiras; na carga ela
     * é toda sugada para a espada, erguida acima da cabeça, e na terceira palavra só resta o ponto de luz lá.
     */
    private static void aura(Vec3 camera, float parcial, VisuaisAtivos.Visual v, Vec3 o, double t, float sai) {
        double c = AtomicFinal.carga(AtomicFinal.FALA + t);
        double g = AtomicFinal.aura(AtomicFinal.FALA + t) * (1 - c);
        Vec3 pes = pes(camera, parcial, v, o);
        Vec3 meio = pes.add(0, 1, 0), espada = pes.add(0, 3.1, 0);
        boolean primeira = euEmPrimeiraPessoa(v);
        abrir(true);
        if (!primeira) {
            halo(meio, 1.4 + 4.5 * g, ROXO, sai * (float) (.3 + .4 * g) * (float) (1 - c));
            halo(meio, .9 + 2.2 * g, MAGIA, sai * (float) (.3 + .45 * g) * (float) (1 - c));
            halo(meio, .4 + .9 * g, BRANCO, sai * (float) (.15 + .5 * g * g) * (float) (1 - c));
        }
        anel(pes.add(0, -.2, 0), .3 + 2.5 * g, .8 + 3.4 * g, MAGIA, 0, sai * (float) ((.25 + .5 * g) * (1 - c)));
        // A magia sobe em fios, cada vez mais altos e mais longe dele.
        for (int k = 0; k < 34; k++) {
            double raio = (primeira ? 1.3 : .45) + (1.2 + 2.2 * g) * acaso(k), a = Math.PI * 2 * acaso(k + 50) + t * .03;
            double fase = (t / (9 + 8 * acaso(k + 100)) + acaso(k + 150)) % 1;
            double alto = (2 + 5 * g) * fase;
            Vec3 p = pes.add(Math.cos(a) * raio, alto - .3, Math.sin(a) * raio);
            float f = sai * (float) (Math.sin(fase * Math.PI) * (.35 + .55 * g) * (1 - c));
            fita(p, p.add(0, .5 + 1.3 * g, 0), .06 + .05 * g, k % 3 == 0 ? CLARO : MAGIA, f, 0);
        }
        if (c > 0) {
            // A carga: riscos de todo lado correm para a ponta da espada, cada vez mais curtos e rápidos.
            double desde = AtomicFinal.FALA + t - AtomicFinal.CARGA;
            boolean cheia = c >= 1;
            if (!cheia) {
                for (int k = 0; k < 46; k++) {
                    double y = 2 * acaso(k + 300) - 1, giro = Math.PI * 2 * acaso(k + 340), h = Math.sqrt(1 - y * y);
                    Vec3 r = new Vec3(h * Math.cos(giro), y, h * Math.sin(giro));
                    double fase = (desde / (5 + 5 * acaso(k + 380)) + acaso(k + 420)) % 1;
                    double longe = (9 - 5 * c) * (1 - fase) + .2;
                    float f = sai * (float) (Math.sin(fase * Math.PI) * (.4 + .6 * c));
                    fita(espada.add(r.scale(longe + 1.4)), espada.add(r.scale(longe)), .07, k % 3 == 0 ? BRANCO : MAGIA, 0, f);
                }
                aro(espada, new Vec3(1, 0, 0), new Vec3(0, 0, 1), 6 * (1 - c) + .3, .12, CLARO, sai * (float) c * .7f);
            }
            // O ponto de luz na espada: cresce com a carga e, cheia, treme quieto até o estouro.
            double pulso = cheia ? 1 + .12 * Math.sin(desde * 1.9) : c;
            halo(espada, 2.6 * pulso, ROXO, sai * (float) (.6 * c));
            halo(espada, 1.3 * pulso, MAGIA, sai * (float) (.9 * c));
            halo(espada, .55 * pulso, BRANCO, sai * (float) c);
            if (cheia) {
                fita(espada.add(-7, 0, 0), espada.add(7, 0, 0), .05, BRANCO, 0, 0);
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI * k / 4 + desde * .02;
                    Vec3 r = new Vec3(Math.cos(a), Math.sin(a), 0);
                    fita(espada, espada.add(r.scale(3.5 * pulso)), .05, CLARO, sai * .8f, 0);
                    fita(espada, espada.subtract(r.scale(3.5 * pulso)), .05, CLARO, sai * .8f, 0);
                }
            }
        }
        fechar();
    }

    /**
     * O Atomic: um ponto branco que estoura numa esfera de luz roxa; ela cresce devagar, cada vez mais depressa,
     * até engolir a câmera, com ondas de choque saindo dela uma atrás da outra. Os dois ficam dentro.
     */
    private static void atomic(Vec3 o, double t) {
        float a = (float) AtomicFinal.clarao(t);
        // Com a cutscene, a câmera recua até o sistema solar: a esfera passa a ser o ponto roxo do céu. Sem ela,
        // a esfera dá o pulso final no lugar do ponto, junto com o som.
        boolean naCena = CutsceneCliente.emCurso(CutsceneAtomic.CLARAO);
        if (naCena) a *= (float) SistemaSolar.esferaNoMundo(t);
        if (a <= .004f) return;
        double raio = RAIO_DO_ATOMIC * (.05 + .95 * AtomicFinal.raioDoClarao(t)) * (naCena ? 1 : SistemaSolar.repiqueDaEsfera(t));
        float pulso = naCena ? 0 : (float) SistemaSolar.estouroDoPulso(t);
        abrir(true);
        // O estouro: nos primeiros ticks tudo é branco em volta do ponto.
        if (t < 8) {
            float e = (float) (1 - t / 8);
            halo(o, 60, BRANCO, e);
            halo(o, 130, CLARO, e * .7f);
            for (int k = 0; k < 6; k++) {
                double g = Math.PI * k / 6;
                Vec3 r = new Vec3(Math.cos(g), Math.sin(g) * .25, Math.sin(g));
                fita(o.subtract(r.scale(220 * e)), o.add(r.scale(220 * e)), 1.2, BRANCO, e, e);
            }
        }
        if (pulso > .004f) {
            halo(o, raio * .8, BRANCO, pulso);
            halo(o, raio * 2.4, CLARO, pulso * .6f);
            anel(o, raio * (1.2 + (1 - pulso) * 1.5), raio * (1.3 + (1 - pulso) * 1.6), CLARO, 0, pulso * .7f);
        }
        halo(o, raio * 1.9, ROXO, a * .5f);
        halo(o, raio * 1.3, ROXO, a);
        halo(o, raio * 1.1, MAGIA, a);
        halo(o, raio * .9, MAGIA, a);
        halo(o, raio * .7, CLARO, a);
        halo(o, raio * .45, BRANCO, a);
        // As ondas de choque, deitadas, uma a cada 14 ticks, e os raios que saem do meio.
        for (int k = 0; k < 4; k++) {
            double idade = t - k * 14;
            if (idade <= 0) continue;
            double onda = raio * 1.1 + idade * 3.2;
            float f = a * (float) Math.max(0, 1 - idade / 40) * .6f;
            anel(o, onda * .94, onda, CLARO, 0, f);
        }
        for (int k = 0; k < 36; k++) {
            double y = 2 * acaso(k) - 1, giro = Math.PI * 2 * acaso(k + 40), h = Math.sqrt(1 - y * y);
            Vec3 r = new Vec3(h * Math.cos(giro), y, h * Math.sin(giro));
            double longe = raio * (1.3 + 1.4 * acaso(k + 80)) + 6;
            fita(o.add(r.scale(raio * .5)), o.add(r.scale(longe)), raio * .04 + .15, k % 2 == 0 ? CLARO : MAGIA, a * .5f, 0);
        }
        fechar();
    }

    /**
     * A onda de choque no chão, na volta: um aro de luz e poeira que abre a partir do ponto de partida, com
     * riscos de poeira subindo atrás dele. Só visual: nada quebra.
     */
    private static void ondaNoChao(Vec3 o, double t) {
        float f = (float) AtomicFinal.onda(t);
        if (f <= .004f) return;
        double raio = AtomicFinal.RAIO_DA_ONDA * 1.5 * AtomicFinal.raioDaOnda(t);
        Vec3 chao = o.add(0, .08, 0);
        abrir(true);
        anel(chao, raio * .8, raio, CLARO, 0, f * .9f);
        anel(chao, raio, raio + 1.2, MAGIA, f * .9f, 0);
        parede(chao, raio, 0, 1.6 * f + .3, CLARO, f * .55f, 0);
        if (t < 4) halo(o.add(0, 1, 0), 6, BRANCO, (float) (1 - t / 4) * .8f);
        fechar();
        abrir(false);
        for (int k = 0; k < 40; k++) {
            double a = Math.PI * 2 * (k + acaso(k + 500)) / 40, r = raio * (.55 + .45 * acaso(k + 540));
            double alto = (.6 + 2.2 * acaso(k + 580)) * Math.sin(Math.PI * Math.min(1, t / AtomicFinal.TICKS_DA_ONDA));
            Vec3 p = chao.add(Math.cos(a) * r, 0, Math.sin(a) * r);
            fita(p, p.add(Math.cos(a) * .6, alto, Math.sin(a) * .6), .5, 0x8C8698, f * .45f, 0);
        }
        fechar();
    }

    private static final double RAIO_DO_ATOMIC = 200;

    /** No mundo de onde saíram: uma luz azul-violeta bem no alto, que acende de uma vez e some devagar. */
    private static void luzNoCeu(Vec3 o, double t) {
        float a = (float) AtomicFinal.luz(t);
        if (a <= .004f) return;
        Vec3 alto = o.add(0, 140, 0);
        double abre = 1 + t / AtomicFinal.TICKS_DA_LUZ;
        abrir(true);
        halo(alto, 150 * abre, ROXO, a * .5f);
        halo(alto, 85 * abre, MAGIA, a * .75f);
        halo(alto, 38, CLARO, a * .9f);
        halo(alto, 14, BRANCO, a);
        anel(alto, 60 * abre + 2 * t, 64 * abre + 2 * t, CLARO, 0, a * .5f);
        anel(alto, 20 * abre + 1.2 * t, 23 * abre + 1.2 * t, MAGIA, 0, a * .5f);
        // A coluna de luz que desce até o ponto de onde eles saíram, afinando.
        fita(alto, o, 5 * a + .3, MAGIA, a * .5f, 0);
        fita(alto, o, 1.5 * a + .1, BRANCO, a * .6f, 0);
        fechar();
    }
}
