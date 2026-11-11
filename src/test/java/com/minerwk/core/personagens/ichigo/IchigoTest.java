package com.minerwk.core.personagens.ichigo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.kit.Arma;
import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.Efeito;
import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.Forma;
import com.minerwk.core.kit.Habilidade;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Personagem;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.kit.Vetor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** O kit do Ichigo: os golpes do Shikai e do Bankai, a troca de forma e o ataque básico da Zangetsu. */
class IchigoTest {
    private static final EstadoDoJogador PARADO = new EstadoDoJogador(true, false, false);

    /** Contexto falso: anota o que os golpes pedem e liga forma, efeitos, recurso e recargas ao kit. */
    final class Falso implements Contexto {
        final List<String> chamadas = new ArrayList<>();
        final List<Float> danos = new ArrayList<>();
        final List<Vetor> pontosDeDano = new ArrayList<>();
        final List<double[]> areas = new ArrayList<>();
        Predicate<Vetor> paredeEm = v -> false;
        /** Um inimigo parado aqui (ou null = ninguém). */
        Vetor inimigoEm;
        Object alvoMarcado;
        Vetor posAlvo;
        Arma arma;
        Vetor direcaoDoPasso;
        double distanciaDoPasso;
        Vetor encarado;

        public Vetor olhos() { return Vetor.ZERO; }
        public Vetor olhar() { return new Vetor(1, 0, 0); }
        public boolean colideBloco(Vetor de, Vetor ate) { return paredeEm.test(ate); }
        public int danoUmaVez(Vetor c, double r, float d, Set<Object> atingidos) {
            pontosDeDano.add(c);
            if (inimigoEm != null && c.distancia(inimigoEm) <= r && atingidos.add("inimigo")) { danos.add(d); return 1; }
            return 0;
        }
        public void danoEm(Vetor c, double r, float d) { chamadas.add("dano"); areas.add(new double[] {r, d}); }
        public void empurrar(Vetor c, double r, double f) { chamadas.add("empurrar"); areas.add(new double[] {r, f}); }
        public int desfazerProjeteis(Vetor c, double r) { chamadas.add("projeteis"); areas.add(new double[] {r}); return 0; }
        public Vetor deslocar(Vetor direcao, double distancia, Vetor encarar) {
            chamadas.add("deslocar");
            direcaoDoPasso = direcao;
            distanciaDoPasso = distancia;
            encarado = encarar;
            return direcao.vezes(distancia);
        }
        public Object marcarAlvo(double alcance, double largura) { return alvoMarcado; }
        public Vetor posicaoDe(Object alvo) { return posAlvo; }
        public void arma(Arma a) { arma = a; }
        public String forma() { return kit.forma(); }
        public boolean mudarForma(String forma) { return kit.mudarForma(forma, this); }
        public void criarEfeito(Efeito e) { kit.adicionarEfeito(e); }
        public double recurso() { return kit.recurso().atual(); }
        public void recarga(String habilidade, int ticks) { kit.recargas().iniciar(habilidade, agora, ticks); }
    }

    private final Personagem ichigo = Ichigo.criar();
    private final KitDoJogador kit = new KitDoJogador(ichigo);
    private final Falso ctx = new Falso();
    private long agora;

    private void ticks(int n) {
        for (int i = 0; i < n; i++) kit.tick(agora++, PARADO, ctx);
    }

    private void usar(Tecla tecla) {
        assertTrue(kit.apertar(tecla, agora), "tecla " + tecla + " deveria começar");
    }

    /** Entra no Bankai e espera o golpe acabar, com o reiatsu cheio de novo. */
    private void entrarNoBankai() {
        usar(Tecla.G);
        ticks(45);
        assertEquals(Reiatsu.BANKAI, kit.forma());
        kit.recurso().carregar(Reiatsu.MAXIMO);
    }

    @Test
    void shikaiTemCincoGolpesEOBankaiQuatro() {
        assertEquals("ichigo", ichigo.id());
        assertEquals(Set.of(Tecla.Z, Tecla.X, Tecla.C, Tecla.V, Tecla.G), ichigo.habilidades().keySet());
        Forma bankai = ichigo.transformacoes().get(0);
        assertEquals(Reiatsu.BANKAI, bankai.id());
        assertEquals(Set.of(Tecla.Z, Tecla.X, Tecla.C, Tecla.G), bankai.habilidades().keySet());
        Set<String> ids = new HashSet<>();
        for (Habilidade h : ichigo.todasAsHabilidades()) assertTrue(ids.add(h.id()), "id repetido: " + h.id());
        assertEquals(9, ids.size());
        assertTrue(kit.recurso() instanceof Reiatsu);
    }

    @Test
    void getsugaSeguradoFereMaisEAndaVinteEQuatroBlocos() {
        ctx.inimigoEm = new Vetor(20, 0, 0);
        usar(Tecla.Z);
        ticks(30); // carga cheia (10 ticks) e o voo inteiro
        assertEquals(List.of(12f), ctx.danos);
        double maisLonge = ctx.pontosDeDano.stream().mapToDouble(Vetor::x).max().orElse(0);
        assertEquals(25, maisLonge, 1e-6, "sai a 1 bloco dos olhos e anda 24");
        assertEquals(Reiatsu.MAXIMO - 20, kit.recurso().atual(), 5);
        assertFalse(kit.recargas().pronta("ichigo_getsuga", agora));
    }

    @Test
    void getsugaSoltoCedoFereOito() {
        ctx.inimigoEm = new Vetor(6, 0, 0);
        usar(Tecla.Z);
        ticks(1);
        kit.soltar(Tecla.Z);
        ticks(20);
        assertEquals(List.of(8f), ctx.danos);
    }

    @Test
    void getsugaAtravessaOInimigoFerindoUmaVezEParaNaParede() {
        ctx.inimigoEm = new Vetor(5, 0, 0);
        ctx.paredeEm = v -> v.x() > 12;
        usar(Tecla.Z);
        ticks(30);
        assertEquals(1, ctx.danos.size(), "uma vez só, mesmo passando por cima dele vários ticks");
        double maisLonge = ctx.pontosDeDano.stream().mapToDouble(Vetor::x).max().orElse(0);
        assertTrue(maisLonge > 5 && maisLonge <= 12, "passou do inimigo e parou na parede, foi até " + maisLonge);
        assertTrue(kit.efeitos().isEmpty());
    }

    @Test
    void getsugaComParedeColadaNaoNasceDoOutroLado() {
        ctx.paredeEm = v -> v.x() > .5;
        ctx.inimigoEm = new Vetor(2, 0, 0);
        usar(Tecla.Z);
        ticks(30);
        assertTrue(ctx.danos.isEmpty(), "o inimigo atrás da parede não é atingido");
        assertTrue(kit.efeitos().isEmpty());
    }

    @Test
    void shunpoDoShikaiAvancaOitoBlocosNaMira() {
        usar(Tecla.X);
        ticks(3);
        assertEquals(new Vetor(1, 0, 0), ctx.direcaoDoPasso);
        assertEquals(8, ctx.distanciaDoPasso);
        assertNull(ctx.encarado);
    }

    @Test
    void getsugaNoChaoFereEmpurraEDesfazProjeteis() {
        usar(Tecla.C);
        ticks(7);
        assertTrue(ctx.chamadas.isEmpty(), "o gesto leva 8 ticks");
        ticks(1);
        assertEquals(List.of("dano", "empurrar", "projeteis"), ctx.chamadas);
        assertEquals(5, ctx.areas.get(0)[0]);
        assertEquals(6, ctx.areas.get(0)[1]);
        assertEquals(5, ctx.areas.get(1)[0]);
        assertEquals(5, ctx.areas.get(2)[0]);
    }

    @Test
    void arremessoFereNaIdaEDeixaSemEspadaPorDezesseisTicks() {
        ctx.inimigoEm = new Vetor(6, 0, 0);
        usar(Tecla.V);
        ticks(6);
        assertEquals(Arma.MAO, ctx.arma, "a espada saiu da mão no disparo");
        for (int i = 0; i < 15; i++) {
            ticks(1);
            assertEquals(Arma.MAO, ctx.arma, "tick " + i + " com a espada fora");
            assertFalse(kit.apertar(Tecla.Z, agora), "sem outros golpes com a espada fora");
        }
        ticks(2);
        assertEquals(Ichigo.ZANGETSU, ctx.arma, "a espada voltou");
        assertEquals(List.of(10f), ctx.danos, "fere na ida, uma vez");
        double maisLonge = ctx.pontosDeDano.stream().mapToDouble(Vetor::x).max().orElse(0);
        assertEquals(10, maisLonge, 1e-6);
        assertTrue(kit.apertar(Tecla.Z, agora));
    }

    @Test
    void ataqueBasicoEDaZangetsuEmCadaForma() {
        ticks(1);
        assertEquals(Ichigo.ZANGETSU, ctx.arma);
        assertEquals(7, Ichigo.ZANGETSU.dano());
        entrarNoBankai();
        ticks(1);
        assertEquals(Ichigo.TENSA_ZANGETSU, ctx.arma);
        assertEquals(6, Ichigo.TENSA_ZANGETSU.dano());
        assertTrue(Ichigo.TENSA_ZANGETSU.velocidade() > Ichigo.ZANGETSU.velocidade(), "o Bankai bate mais rápido");
    }

    @Test
    void bankaiLevaTrintaTicksEmpurraSemFerirETrocaAForma() {
        usar(Tecla.G);
        ticks(29);
        assertEquals(Forma.BASE, kit.forma());
        ticks(1);
        assertEquals(Reiatsu.BANKAI, kit.forma());
        assertEquals(List.of("empurrar"), ctx.chamadas);
        assertEquals(6, ctx.areas.get(0)[0]);
        assertTrue(ctx.danos.isEmpty());
        assertEquals(Reiatsu.MAXIMO - 50, kit.recurso().atual(), 1e-6);
        assertEquals("ichigo_getsuga_negro", kit.habilidade(Tecla.Z).orElseThrow().id());
    }

    @Test
    void getsugaNegroSaiSemCargaEAndaVinteEOitoBlocos() {
        entrarNoBankai();
        ctx.inimigoEm = new Vetor(25, 0, 0);
        usar(Tecla.Z);
        ticks(2);
        assertFalse(kit.efeitos().isEmpty(), "sai em 2 ticks");
        ticks(20);
        assertEquals(List.of(10f), ctx.danos);
        double maisLonge = ctx.pontosDeDano.stream().mapToDouble(Vetor::x).max().orElse(0);
        assertEquals(29, maisLonge, 1e-6);
    }

    @Test
    void shunpoDoBankaiSemAlvoAvancaCatorzeBlocos() {
        entrarNoBankai();
        usar(Tecla.X);
        ticks(3);
        assertEquals(14, ctx.distanciaDoPasso);
        assertNull(ctx.encarado);
    }

    @Test
    void shunpoDoBankaiComAlvoSurgeAtrasDeleEOEncara() {
        entrarNoBankai();
        ctx.alvoMarcado = "alvo";
        ctx.posAlvo = new Vetor(9, 0, 0);
        usar(Tecla.X);
        ticks(3);
        Vetor destino = ctx.direcaoDoPasso.vezes(ctx.distanciaDoPasso);
        assertEquals(9 + Ichigo.ATRAS_DO_ALVO, destino.x(), 1e-6, "passa do alvo");
        assertEquals(0, destino.z(), 1e-6);
        assertEquals(ctx.posAlvo, ctx.encarado);
    }

    @Test
    void sairDoBankaiVoltaAoShikaiEPoeOGEmRecargaDeCincoSegundos() {
        entrarNoBankai();
        usar(Tecla.G);
        ticks(10);
        assertEquals(Forma.BASE, kit.forma());
        assertFalse(kit.apertar(Tecla.G, agora), "o Bankai não volta na hora");
        long faltam = kit.recargas().restante(Ichigo.ENTRAR_NO_BANKAI, agora);
        assertTrue(faltam > 80 && faltam <= 100, "faltam " + faltam);
    }

    @Test
    void semReiatsuOBankaiAcabaSozinhoEOGFicaDezSegundosEmRecarga() {
        entrarNoBankai();
        kit.recurso().carregar(Reiatsu.DRENO * 3);
        ticks(2);
        assertEquals(Reiatsu.BANKAI, kit.forma());
        ticks(2);
        assertEquals(Forma.BASE, kit.forma());
        assertEquals(Ichigo.ZANGETSU, ctx.arma);
        long faltam = kit.recargas().restante(Ichigo.ENTRAR_NO_BANKAI, agora);
        assertTrue(faltam > 190 && faltam <= 200, "faltam " + faltam);
    }

    @Test
    void getsugaNaLaminaDeixaOAtaqueBasicoMaisForteELongoPorSeisSegundos() {
        entrarNoBankai();
        usar(Tecla.C);
        ticks(4);
        assertEquals(Ichigo.TENSA_CARREGADA, ctx.arma);
        assertEquals(Ichigo.TENSA_ZANGETSU.dano() + 6, Ichigo.TENSA_CARREGADA.dano());
        assertEquals(5, Ichigo.TENSA_CARREGADA.alcance());
        kit.recurso().carregar(Reiatsu.MAXIMO);
        ticks(118);
        assertEquals(Ichigo.TENSA_CARREGADA, ctx.arma, "ainda dentro dos 6 s");
        ticks(3);
        assertEquals(Ichigo.TENSA_ZANGETSU, ctx.arma);
    }

    @Test
    void sairDoBankaiAcabaComALaminaCarregada() {
        entrarNoBankai();
        usar(Tecla.C);
        ticks(12);
        assertEquals(Ichigo.TENSA_CARREGADA, ctx.arma);
        usar(Tecla.G);
        ticks(10);
        assertEquals(Forma.BASE, kit.forma());
        assertEquals(Ichigo.ZANGETSU, ctx.arma);
        assertTrue(kit.efeitos().isEmpty());
    }
}
