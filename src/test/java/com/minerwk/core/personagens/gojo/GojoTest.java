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
        com.minerwk.core.maestria.Melhorias melhorias = com.minerwk.core.maestria.Melhorias.NENHUMA;
        public com.minerwk.core.maestria.Melhorias melhorias() { return melhorias; }

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
        final List<double[]> barreiras = new ArrayList<>();
        final List<Double> raiosApagados = new ArrayList<>();
        final List<double[]> cascas = new ArrayList<>();
        Vetor centroCasca;
        public void barreira(Vetor c, double r, double chao, int t) { chamadas.add("barreira"); barreiras.add(new double[] {r, chao, t}); }
        public void voo(int t) { chamadas.add("voo"); }
        /** Alvo que o R marca (ou null) e onde ele está. */
        Object alvoMarcado;
        Vetor posAlvo;
        public Object marcarAlvo(double alcance, double largura) { return alvoMarcado; }
        public Vetor posicaoDe(Object alvo) { return posAlvo; }
        final List<Vetor> suspensos = new ArrayList<>();
        public void suspender(Object alvo, Vetor ponto, int t) { chamadas.add("suspender"); suspensos.add(ponto); }
        public void ferir(Object alvo, float d) { chamadas.add("ferir"); danos.add(d); }
        public void apagarCasca(Vetor c, double de, double ate) { chamadas.add("casca"); cascas.add(new double[] {de, ate}); centroCasca = c; }
        public void cutscene(String id) { chamadas.add("cutscene:" + id); }
        public void cutscene(String id, Vetor origem, Vetor direcao) { chamadas.add("cutscene:" + id); }
        public void cutsceneParaAlvo(Object alvo, String id, Vetor origem, Vetor direcao, int passados) { chamadas.add("cutsceneAlvo:" + id); }
        public void encerrarCutscene() { chamadas.add("encerrarCutscene"); }
        public void descida(String anim, int t) { chamadas.add("descida:" + anim); }
        public void apresentacao(String anim, String efeito, String som) { if (!anim.isEmpty()) chamadas.add("anim:" + anim); }
        public void pararAnimacao() { chamadas.add("pararAnimacao"); }
        public void apagarBlocos(Vetor c, double r) { chamadas.add("apagar"); raiosApagados.add(r); }
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
    private final KitDoJogador kit = desperto();
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
        var kit2 = desperto();
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
    void roxo200SemAlvoExplodeOndeMiraSemFeixe() {
        usar(Tecla.R);
        ticks(300);
        assertEquals(0, ctx.conta("apagar"), "o R não lança feixe");
        assertEquals(0, ctx.conta("ferir"));
        assertEquals(1, ctx.conta("visual:gojo_roxo_maximo_esferas"));
        assertEquals(1, ctx.conta("anim:" + Gojo.ANIM_R_SEM_ALVO), "abre as mãos e lança as esferas");
        assertEquals(1, ctx.conta("cutscene:" + CutsceneRoxoMaximo.idSemAlvo(RoxoMaximo.ALCANCE)));
        assertEquals(0, ctx.conta("cutsceneAlvo:" + CutsceneRoxoMaximo.idSemAlvo(RoxoMaximo.ALCANCE)));
        assertEquals(0, ctx.conta("anim:" + Gojo.ANIM_R_UPPERCUT));
        assertExplosaoInteira(new Vetor(RoxoMaximo.ALCANCE, 0, 0));
    }

    @Test
    void roxo200ComAlvoAvancaDaUppercutSobeEExplodeAtrasDoGojo() {
        ctx.alvoMarcado = "alvo";
        ctx.posAlvo = new Vetor(31.5, 0, 0);
        int a = RoxoMaximo.avanco(31.5);
        usar(Tecla.R);
        ticks(300);
        assertEquals(0, ctx.conta("apagar"));
        assertEquals(1, ctx.conta("visual:gojo_roxo_maximo"));
        assertEquals(1, ctx.conta("anim:" + Gojo.ANIM_R_UPPERCUT), "a animação do uppercut toca no soco");
        assertEquals(1, ctx.conta("cutscene:" + CutsceneRoxoMaximo.id(a)), "a parte 2 vai para o Gojo no disparo");
        assertEquals(1, ctx.conta("cutsceneAlvo:" + CutsceneRoxoMaximo.id(a)), "e para o alvo no uppercut");
        assertEquals(1, ctx.conta("descida:" + Gojo.ANIM_R_DESCIDA),
                "no fim, a descida com a pose (o servidor só toca a pose se ele estiver no ar)");
        assertEquals(List.of(RoxoMaximo.DANO_UPPERCUT, RoxoMaximo.DANO), ctx.danos,
                "uppercut e depois a explosão, uma vez só (o alvo não leva de novo pela casca)");
        assertEquals(RoxoMaximo.inicioExplosao(a) - a, ctx.conta("suspender"), "segurado no ar até explodir");
        // O falso não anda (como um servidor atrasado em relação ao cliente, que é quem move o Gojo): a
        // fusão fica 4 atrás de onde o Gojo está pela conta do movimento, 30 à frente e 25 acima.
        assertExplosaoInteira(new Vetor(31.5 - RoxoMaximo.PARADA - RoxoMaximo.ATRAS, RoxoMaximo.SUBIDA, 0));
    }

    @Test
    void uppercutPuxaOAlvoParaOndeOGojoBateMesmoSeEleAndou() {
        ctx.alvoMarcado = "alvo";
        ctx.posAlvo = new Vetor(31.5, 0, 0);
        usar(Tecla.R);
        ticks(85); // disparou; o avanço (10 ticks) ainda não acabou
        ctx.posAlvo = new Vetor(36, 0, 0); // o alvo correu
        ticks(300);
        assertFalse(ctx.suspensos.isEmpty());
        for (Vetor p : ctx.suspensos) assertEquals(31.5, p.x(), 1e-9, "segurado onde o Gojo bate, não onde correu");
    }

    /** As cascas cobrem de 0 ao raio 40 sem buraco nem repetição, e a explosão aparece uma vez. */
    private void assertExplosaoInteira(Vetor centro) {
        assertEquals(1, ctx.conta("visual:gojo_roxo_explosao"));
        assertEquals(centro, ctx.centroCasca);
        double r = 0;
        for (double[] c : ctx.cascas) {
            assertEquals(r, c[0], 1e-9);
            assertTrue(c[1] > c[0]);
            r = c[1];
        }
        assertEquals(RoxoMaximo.RAIO_EXPLOSAO, r, 1e-9);
        assertEquals(RoxoMaximo.TICKS_EXPLOSAO, ctx.cascas.size());
    }

    @Test
    void roxo200VoaNaCarga() {
        usar(Tecla.R);
        ticks(1);
        assertEquals(1, ctx.conta("voo"));
    }

    @Test
    void roxo200CaiComGolpeForteNaCarga() {
        usar(Tecla.R);
        ticks(10);
        kit.levouDano(6f, agora, ctx);
        ticks(100);
        assertEquals(0, ctx.conta("casca"));
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
    void roxo200AbreACutsceneNaCargaEOCorteAEncerra() {
        kit.recurso().carregar(200);
        usar(Tecla.R);
        ticks(1);
        assertEquals(1, ctx.conta("cutscene:" + CutsceneRoxo200.ID));
        kit.levouDano(10f, agora, ctx);
        assertEquals(1, ctx.conta("fim:gojo_roxo_200_carga"), "o corte da carga encerra a cutscene no cliente");
    }

    @Test
    void cadaGolpeTocaASuaAnimacaoNoComecoDaCarga() {
        kit.recurso().carregar(10000);
        java.util.Map<Tecla, String> ids = java.util.Map.of(Tecla.Z, "gojo_azul", Tecla.X, "gojo_vermelho",
                Tecla.C, "gojo_infinito", Tecla.V, "gojo_roxo", Tecla.G, "gojo_vazio_infinito", Tecla.R, "gojo_roxo_200");
        for (var e : ids.entrySet()) {
            KitDoJogador k = desperto();
            ctx.kit = k;
            k.recurso().carregar(10000);
            ctx.chamadas.clear();
            assertTrue(k.apertar(e.getKey(), agora));
            k.tick(agora, PARADO, ctx);
            assertEquals(1, ctx.conta("anim:" + e.getValue()), "animação de " + e.getKey() + " no 1º tick da carga");
        }
    }

    @Test
    void vermelhoSoltoCedoPulaParaAPoseDoDisparo() {
        usar(Tecla.X);
        ticks(5);
        kit.soltar(Tecla.X);
        ticks(3);
        assertEquals(1, ctx.conta("anim:gojo_vermelho_disparo"));
    }

    @Test
    void descartarDepoisDoDisparoParaAAnimacaoUmaVez() {
        usar(Tecla.Z);
        ticks(10); // já disparou; o campo do Azul segue como efeito
        kit.descartar(ctx);
        kit.descartar(ctx); // morto: o descartar roda todo tick, mas só avisa uma vez
        assertEquals(1, ctx.conta("pararAnimacao"));
    }

    @Test
    void descartarNoMeioDoRAcabaACutscene() {
        usar(Tecla.R);
        ticks(85); // disparou: a sequência está no ar
        kit.descartar(ctx);
        assertEquals(1, ctx.conta("encerrarCutscene"));
    }

    @Test
    void cargaCortadaParaAAnimacao() {
        kit.recurso().carregar(200);
        usar(Tecla.R);
        ticks(1);
        kit.levouDano(10f, agora, ctx);
        assertEquals(1, ctx.conta("pararAnimacao"));
    }

    @Test
    void dominioCriaBarreiraComChaoNosPes() {
        usar(Tecla.G);
        ticks(20);
        assertEquals(1, ctx.conta("barreira"));
        double[] b = ctx.barreiras.get(0);
        assertEquals(12, b[0], 1e-9);
        assertEquals(-Gojo.ALTURA_OLHOS, b[1], 1e-9, "os olhos estão em 0: o chão fica nos pés");
        assertEquals(160, b[2], 1e-9);
    }

    @Test
    void roxoEDominioMostramCargaEDisparo() {
        usar(Tecla.V);
        ticks(100);
        assertEquals(1, ctx.conta("visual:gojo_roxo_carga"));
        assertEquals(1, ctx.conta("visual:gojo_roxo"));
        var ctx2 = new Falso();
        var kit2 = desperto();
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

    private static com.minerwk.core.maestria.Melhorias comprou(String no, int nivel) {
        return new com.minerwk.core.maestria.Melhorias(ArvoreGojo.ARVORE, java.util.Map.of(no, nivel));
    }

    @Test
    void reservaEFluxoDaArvoreMudamAEnergia() {
        var e = new EnergiaAmaldicoada();
        e.melhorias(comprou(ArvoreGojo.ENERGIA_RESERVA, 3));
        assertEquals(260, e.maximo(), 1e-9);
        assertEquals(200, e.atual(), 1e-9, "comprar a reserva não enche a barra");
        e.carregar(999);
        assertEquals(260, e.atual(), 1e-9);
        e.melhorias(comprou(ArvoreGojo.ENERGIA_FLUXO, 2));
        assertEquals(200, e.maximo(), 1e-9);
        assertEquals(200, e.atual(), 1e-9, "sem a reserva, o que passava do máximo some");
        e.gastar(100);
        e.tick(PARADO);
        assertEquals(101.2, e.atual(), 1e-9);
    }

    @Test
    void tecnicaReversaDaArvoreCuraMaisVezes() {
        var comArvore = new Falso();
        comArvore.melhorias = comprou(ArvoreGojo.TECNICA_REVERSA, 3);
        var p = new TecnicaReversa();
        // Intervalo de 80 com -30%: 56 ticks.
        for (int i = 0; i < 100 + 56; i++) p.tick(comArvore, PARADO);
        assertEquals(2, comArvore.conta("curar"));
    }

    @Test
    void arvoreDoGojoVemComOPersonagem() {
        assertSame(ArvoreGojo.ARVORE, Gojo.criar().arvore());
    }

    @Test
    void melhoriasDaArvoreChegamNosGolpes() {
        var falso = new Falso();
        falso.melhorias = new com.minerwk.core.maestria.Melhorias(ArvoreGojo.ARVORE, java.util.Map.of(
                ArvoreGojo.INFINITO_DURACAO, 2, ArvoreGojo.DOMINIO_DURACAO, 3, ArvoreGojo.VERMELHO_FORCA, 3));
        KitDoJogador k = desperto();
        falso.kit = k;
        new Gojo.Infinito().aoDisparar(falso, 1f);
        assertEquals(156, k.efeitos().get(0).duracao(), "120 ticks com +30%");
        new Gojo.VazioInfinito().aoDisparar(falso, 1f);
        assertEquals(208, k.efeitos().get(1).duracao(), "160 ticks com +30%");
        assertEquals(208, falso.barreiras.get(0)[2], 1e-9, "a barreira dura o mesmo que o domínio");
        var esfera = new Gojo.EsferaVermelha(Vetor.ZERO, new Vetor(1, 0, 0), 1f, 1.24f);
        esfera.aoAcabar(falso);
        assertEquals(14 * 1.24f, falso.danos.get(0), 1e-4);
    }

    /** Um kit do Gojo já desperto: os testes dos golpes não são sobre o trancamento. */
    private static KitDoJogador desperto() {
        KitDoJogador k = new KitDoJogador(Gojo.criar());
        k.definirDesperto(true);
        return k;
    }

    @Test
    void gojoComecaSoComAzulEInfinito() {
        KitDoJogador k = new KitDoJogador(Gojo.criar());
        assertFalse(k.trancada(Tecla.Z));
        assertFalse(k.trancada(Tecla.C));
        for (Tecla t : new Tecla[] {Tecla.X, Tecla.V, Tecla.G, Tecla.R}) assertTrue(k.trancada(t), t + " deveria exigir o despertar");
        assertTrue(Gojo.criar().trancas().passivas(), "a Técnica Reversa exige o despertar");
    }
}
