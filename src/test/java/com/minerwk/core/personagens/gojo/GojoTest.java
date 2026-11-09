package com.minerwk.core.personagens.gojo;

import static org.junit.jupiter.api.Assertions.*;

import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.Efeito;
import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.kit.Vetor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class GojoTest {
    private static final EstadoDoJogador PARADO = new EstadoDoJogador(true, false, false);
    private static final EstadoDoJogador ANDANDO = new EstadoDoJogador(false, false, false);
    private static final EstadoDoJogador APANHOU = new EstadoDoJogador(true, true, false);

    /** Contexto falso: anota chamadas e manda os efeitos para o kit. */
    static final class Falso implements Contexto {
        KitDoJogador kit;
        final List<String> chamadas = new ArrayList<>();
        final List<Float> danos = new ArrayList<>();
        final List<Vetor> pontosDeDano = new ArrayList<>();
        Predicate<Vetor> colideEm = v -> false;
        /** Um alvo parado aqui (ou null): o danoUmaVez só o fere se estiver no raio. */
        Vetor alvoEm;
        int arrancados;
        boolean temRecurso = true;

        int conta(String nome) {
            return (int) chamadas.stream().filter(nome::equals).count();
        }

        public Vetor olhos() { return Vetor.ZERO; }
        public Vetor olhar() { return new Vetor(1, 0, 0); }
        public void puxar(Vetor c, double r, double f) { chamadas.add("puxar"); }
        public void empurrar(Vetor c, double r, double f) { chamadas.add("empurrar"); }
        public void danoEm(Vetor c, double r, float d) { chamadas.add("dano"); danos.add(d); pontosDeDano.add(c); }
        public boolean colide(Vetor de, Vetor ate) { return colideEm.test(ate); }
        public int danoUmaVez(Vetor c, double r, float d, java.util.Set<Object> atingidos) {
            chamadas.add("dano"); pontosDeDano.add(c);
            boolean pega = alvoEm == null || c.distancia(alvoEm) <= r;
            if (pega && atingidos.add("alvo")) { danos.add(d); return 1; }
            return 0;
        }
        public void arremessar(Vetor c, double r, double f, int congelar, Vetor eixo) { chamadas.add("arremessar"); }
        public int arrancarBlocos(Vetor c, double r, int maximo, float dureza) { chamadas.add("arrancar"); arrancados += maximo; return maximo; }
        public void acertou(float f) { chamadas.add("acertou"); }
        public void apagarBlocos(Vetor c, double r) { chamadas.add("apagar"); }
        public void paralisar(Vetor c, double r, int t) { chamadas.add("paralisar"); }
        public void infinito(int t) { chamadas.add("infinito"); }
        public void curar(float v) { chamadas.add("curar"); }
        public boolean gastarRecurso(double q) { return temRecurso; }
        public void criarEfeito(Efeito e) { kit.adicionarEfeito(e); }
        public void tremor(float f, int d, double r) { chamadas.add("tremor"); }
        public void visual(String tipo, Vetor o, Vetor d, float f, int dur) { chamadas.add("visual:" + tipo); }
        public void visualFim(String tipo, Vetor p) { chamadas.add("fim:" + tipo); }
        public void particulas(String tipo, Vetor p, int n) { chamadas.add("particulas"); }
    }

    private final Falso ctx = new Falso();
    private final KitDoJogador kit = new KitDoJogador(Gojo.criar());
    private long agora;

    GojoTest() {
        ctx.kit = kit;
    }

    private void ticks(int n) {
        for (int i = 0; i < n; i++) kit.tick(agora++, PARADO, ctx);
    }

    private void usar(Tecla tecla) {
        assertTrue(kit.apertar(tecla, agora), "tecla " + tecla + " deveria começar");
    }

    @Test
    void personagemComSeisTeclasEPassiva() {
        Personagem p = Gojo.criar();
        assertEquals("gojo", p.id());
        for (Tecla t : Tecla.values()) assertTrue(p.naTecla(t).isPresent(), "falta " + t);
        assertEquals(1, p.passivas().size());
    }

    @Test
    void energiaEncheConformeOMovimento() {
        var e = new EnergiaAmaldicoada();
        assertEquals(200, e.maximo());
        e.gastar(100);
        e.tick(PARADO);
        assertEquals(101, e.atual(), 1e-9);
        e.tick(ANDANDO);
        assertEquals(101.4, e.atual(), 1e-9);
        e.carregar(Double.NaN);
        assertEquals(0, e.atual());
        e.carregar(999);
        assertEquals(200, e.atual());
    }

    @Test
    void azulPuxa60VezesDaDano4VezesEArrancaAte40Blocos() {
        usar(Tecla.Z);
        ticks(100);
        assertEquals(60, ctx.conta("puxar"));
        assertEquals(4, ctx.conta("dano"));
        assertEquals(List.of(2f, 2f, 2f, 2f), ctx.danos);
        assertEquals(40, ctx.arrancados, "no máximo 40 blocos por campo");
        assertEquals(160, Gojo.criar().naTecla(Tecla.Z).orElseThrow().numeros().recarga());
    }

    @Test
    void vermelhoExplodeAoColidirComDanoPelaForca() {
        ctx.colideEm = v -> v.x() >= 6;
        usar(Tecla.X);
        ticks(5);
        kit.soltar(Tecla.X); // força 0: soltou no mínimo
        ticks(40);
        assertEquals(1, ctx.conta("dano"));
        assertEquals(6f, ctx.danos.get(0), 0.5);
        assertTrue(ctx.pontosDeDano.get(0).x() < 8);
        assertEquals(1, ctx.conta("arremessar"));
        assertEquals(1, ctx.conta("acertou"), "acertou alguém: a tela de quem bateu trava");
    }

    @Test
    void vermelhoSemColisaoExplodeNoLimiteComForcaMaxima() {
        usar(Tecla.X);
        ticks(90); // segurou até o máximo; a esfera leva 30 ticks
        assertEquals(1, ctx.conta("dano"));
        assertEquals(14f, ctx.danos.get(0), 1e-6);
        assertEquals(31, ctx.pontosDeDano.get(0).x(), 1e-6, "30 blocos a partir de 1 bloco à frente");
    }

    @Test
    void infinitoDura120TicksEAcabaSemEnergia() {
        usar(Tecla.C);
        ticks(200);
        assertEquals(120, ctx.conta("infinito"));

        var ctx2 = new Falso();
        var kit2 = new KitDoJogador(Gojo.criar());
        ctx2.kit = kit2;
        assertTrue(kit2.apertar(Tecla.C, 0));
        for (int i = 0; i < 10; i++) kit2.tick(i, PARADO, ctx2);
        int antes = ctx2.conta("infinito");
        ctx2.temRecurso = false;
        for (int i = 10; i < 30; i++) kit2.tick(i, PARADO, ctx2);
        assertEquals(antes, ctx2.conta("infinito"));
        assertTrue(kit2.efeitos().isEmpty());
    }

    @Test
    void roxoDa40PassosDeUmBlocoComDano30() {
        usar(Tecla.V);
        ticks(120);
        assertEquals(40, ctx.conta("apagar"));
        assertEquals(40, ctx.conta("dano"));
        assertTrue(ctx.danos.stream().allMatch(d -> d == 30f));
        assertEquals(40, ctx.pontosDeDano.get(39).x(), 1e-6);
    }

    @Test
    void roxoFereCadaAlvoUmaVezSo() {
        ctx.alvoEm = new Vetor(10, 0, 0); // fica uns 3 ticks dentro da massa
        usar(Tecla.V);
        ticks(120);
        assertEquals(List.of(30f), ctx.danos);
        assertEquals(1, ctx.conta("acertou"));
    }

    @Test
    void roxo200Da60PassosAte120Blocos() {
        usar(Tecla.R);
        ticks(200);
        assertEquals(60, ctx.conta("apagar"));
        assertTrue(ctx.danos.stream().allMatch(d -> d == 60f));
        assertEquals(120, ctx.pontosDeDano.get(59).x(), 1e-6);
    }

    @Test
    void roxo200CaiComGolpeForteNaCarga() {
        usar(Tecla.R);
        ticks(10);
        kit.levouDano(6f, agora, ctx);
        ticks(100);
        assertEquals(0, ctx.conta("apagar"));
    }

    @Test
    void roxo200PrecisaDeEnergiaCheia() {
        kit.recurso().gastar(1);
        assertFalse(kit.apertar(Tecla.R, agora));
    }

    @Test
    void dominioParalisa160Ticks() {
        usar(Tecla.G);
        ticks(250);
        assertEquals(160, ctx.conta("paralisar"));
    }

    @Test
    void tecnicaReversaCuraSoDepoisDe100TicksSemDano() {
        var p = new TecnicaReversa();
        for (int i = 0; i < 99; i++) p.tick(ctx, PARADO);
        assertEquals(0, ctx.conta("curar"));
        p.tick(ctx, PARADO); // 100º tick sem dano
        assertEquals(1, ctx.conta("curar"));
        for (int i = 0; i < 79; i++) p.tick(ctx, PARADO);
        assertEquals(1, ctx.conta("curar"));
        p.tick(ctx, PARADO);
        assertEquals(2, ctx.conta("curar"));
        p.tick(ctx, APANHOU);
        for (int i = 0; i < 99; i++) p.tick(ctx, PARADO);
        assertEquals(2, ctx.conta("curar"));
    }

    @Test
    void cadaGolpeMostraSeuVisualSemParticulasVanilla() {
        usar(Tecla.Z);
        ticks(60);
        usar(Tecla.X);
        ticks(80);
        usar(Tecla.C);
        ticks(150);
        assertEquals(1, ctx.conta("visual:gojo_azul"));
        assertEquals(1, ctx.conta("visual:gojo_vermelho_carga"));
        assertEquals(1, ctx.conta("visual:gojo_vermelho"));
        assertEquals(1, ctx.conta("fim:gojo_vermelho"), "a explosão sai pelo visualFim");
        assertEquals(1, ctx.conta("visual:gojo_infinito"));
        assertEquals(0, ctx.conta("particulas"));
    }

    @Test
    void roxoEDominioMostramCargaEDisparo() {
        usar(Tecla.V);
        ticks(100);
        assertEquals(1, ctx.conta("visual:gojo_roxo_carga"));
        assertEquals(1, ctx.conta("visual:gojo_roxo"));
        var ctx2 = new Falso();
        var kit2 = new KitDoJogador(Gojo.criar());
        ctx2.kit = kit2;
        kit2.apertar(Tecla.G, 0);
        for (int i = 0; i < 30; i++) kit2.tick(i, PARADO, ctx2);
        assertEquals(1, ctx2.conta("visual:gojo_vazio_infinito"));
    }

    @Test
    void cargaInterrompidaEncerraOVisualDaCarga() {
        usar(Tecla.R);
        ticks(10);
        assertEquals(1, ctx.conta("visual:gojo_roxo_200_carga"));
        kit.levouDano(6f, agora, ctx);
        assertEquals(1, ctx.conta("fim:gojo_roxo_200_carga"));
        ticks(100);
        assertEquals(0, ctx.conta("visual:gojo_roxo_200"));
    }

    @Test
    void infinitoSemEnergiaEncerraOVisual() {
        usar(Tecla.C);
        ticks(10);
        ctx.temRecurso = false;
        ticks(5);
        assertEquals(1, ctx.conta("fim:gojo_infinito"));
    }
}
