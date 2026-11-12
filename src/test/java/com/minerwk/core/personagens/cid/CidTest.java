package com.minerwk.core.personagens.cid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.kit.Arma;
import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.Efeito;
import com.minerwk.core.kit.EstadoDoJogador;
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

/**
 * O kit do Cid: Redemoinho de Ébano, Pós-imagem, Prisão de Correntes Negras, Overdrive e o traje de slime; e,
 * depois do despertar, a Espada Atômica e o Recovery Atomic.
 */
class CidTest {
    private static final EstadoDoJogador PARADO = new EstadoDoJogador(true, false, false);

    /** Contexto falso: anota o que os golpes pedem e liga efeitos, recurso e recargas ao kit. */
    final class Falso implements Contexto {
        /** Cada corte em área: {raio, dano}. */
        final List<double[]> areas = new ArrayList<>();
        final List<Float> danos = new ArrayList<>();
        final List<Vetor> pontosDeDano = new ArrayList<>();
        Predicate<Vetor> paredeEm = v -> false;
        Vetor inimigoEm;
        /** Onde o alvo acertado está agora (null = sumiu). */
        Vetor posAlvo;
        Arma arma;
        double distanciaDoPasso;
        Vetor chamarizEm;
        int chamarizTicks;
        /** Cada vez que as correntes fecharam: os ticks. */
        final List<Integer> paralisias = new ArrayList<>();
        /** Quantos cortes em área passaram por cima da proteção do corte anterior. */
        int seguidos;
        final List<int[]> velocidades = new ArrayList<>();
        final List<Float> recuos = new ArrayList<>();
        final List<String> visuais = new ArrayList<>(), fins = new ArrayList<>(), animacoes = new ArrayList<>();
        /** Cada corte em arco: {alcance, graus, altura, dano}. */
        final List<double[]> arcos = new ArrayList<>();
        /** Cada vez que o jogador foi preso ao chão (true) ou solto (false). */
        final List<Boolean> firmes = new ArrayList<>();
        /** Cada cura em volta: o raio. */
        final List<Double> restauros = new ArrayList<>();
        int paradas;

        public Vetor olhos() { return Vetor.ZERO; }
        public Vetor olhar() { return new Vetor(1, 0, 0); }
        public boolean colideBloco(Vetor de, Vetor ate) { return paredeEm.test(ate); }
        public void danoEmArea(double raio, float dano) { areas.add(new double[] {raio, dano}); }
        public int danoUmaVez(Vetor c, double r, float d, Set<Object> atingidos) {
            pontosDeDano.add(c);
            if (inimigoEm != null && c.distancia(inimigoEm) <= r && atingidos.add("inimigo")) { danos.add(d); return 1; }
            return 0;
        }
        public Vetor deslocar(Vetor direcao, double distancia, Vetor encarar) {
            distanciaDoPasso = distancia;
            return direcao.vezes(distancia);
        }
        public void chamariz(Vetor posicao, int ticks) { chamarizEm = posicao; chamarizTicks = ticks; }
        public Vetor posicaoDe(Object alvo) { return "inimigo".equals(alvo) ? posAlvo : null; }
        public void paralisarAlvo(Object alvo, int t) { if (posicaoDe(alvo) != null) paralisias.add(t); }
        public void danoSeguidoEmArea(double raio, float dano) { seguidos++; danoEmArea(raio, dano); }
        public void velocidade(int nivel, int ticks) { velocidades.add(new int[] {nivel, ticks}); }
        public void recuo(float dano) { recuos.add(dano); }
        public void arma(Arma a) { arma = a; }
        public void criarEfeito(Efeito e) { kit.adicionarEfeito(e); }
        public void encerrarEfeito(Class<? extends Efeito> tipo) { kit.encerrarEfeitos(tipo, this); }
        public boolean efeitoAtivo(Class<? extends Efeito> tipo) { return kit.efeitos().stream().anyMatch(tipo::isInstance); }
        public double recurso() { return kit.recurso().atual(); }
        public void recarga(String habilidade, int ticks) { kit.recargas().iniciar(habilidade, agora, ticks); }
        public void sinalAoRecurso(String sinal, int ticks) { kit.recurso().sinal(sinal, ticks); }
        public void apresentacao(String animacao, String efeito, String som) { if (!animacao.isEmpty()) animacoes.add(animacao); }
        public void visual(String tipo, Vetor origem, Vetor direcao, float forca, int duracao) {
            visuais.add(tipo);
            assertTrue(duracao > 0, tipo);
        }
        public void visualFim(String tipo, Vetor ponto) { fins.add(tipo); }
        public int danoEmArco(Vetor origem, Vetor direcao, double alcance, double graus, double altura, float dano) {
            arcos.add(new double[] {alcance, graus, altura, dano});
            return inimigoEm == null ? 0 : 1;
        }
        public void firmar(boolean preso) { firmes.add(preso); }
        public int restaurar(double raio) { restauros.add(raio); return 1; }
        public void pararAnimacao() { paradas++; }
    }

    private final Personagem cid = Cid.criar();
    private final KitDoJogador kit = new KitDoJogador(cid);
    private final Falso ctx = new Falso();
    private long agora;

    private void ticks(int n) {
        for (int i = 0; i < n; i++) kit.tick(agora++, PARADO, ctx);
    }

    private void usar(Tecla tecla) {
        assertTrue(kit.apertar(tecla, agora), "tecla " + tecla + " deveria começar");
    }

    @Test
    void oKitTemSeisTeclasEAMagia() {
        assertEquals("cid", cid.id());
        assertEquals(Set.of(Tecla.Z, Tecla.X, Tecla.C, Tecla.G, Tecla.V, Tecla.R), cid.habilidades().keySet());
        assertEquals(7, cid.todasAsHabilidades().size(), "as seis teclas e o Recovery Atomic, a variação agachada do R");
        Set<String> ids = new HashSet<>();
        for (Habilidade h : cid.todasAsHabilidades()) assertTrue(ids.add(h.id()) && h.id().startsWith("cid_"), h.id());
        assertTrue(kit.recurso() instanceof Magia);
        assertTrue(cid.transformacoes().isEmpty(), "o Cid tem uma forma só");
    }

    @Test
    void aEspadaDeSlimeEOAtaqueBasico() {
        ticks(1);
        assertEquals(new Arma(5, 1.6f, 3.5f), ctx.arma);
    }

    @Test
    void redemoinhoDaTresCortesDeQuatroEmRaioCinco() {
        usar(Tecla.Z);
        ticks(3);
        assertEquals(85, kit.recurso().atual(), 2, "custa 15, cobrados no disparo");
        ticks(27);
        assertEquals(3, ctx.areas.size());
        for (double[] a : ctx.areas) {
            assertEquals(5, a[0]);
            assertEquals(4, a[1], 1e-6);
        }
        assertFalse(kit.recargas().pronta("cid_redemoinho", agora));
        assertTrue(kit.recargas().pronta("cid_redemoinho", agora + 80));
        assertEquals(2, ctx.seguidos, "só o segundo e o terceiro corte passam por cima da proteção do anterior");
        assertTrue(ctx.visuais.contains("cid_redemoinho"));
        assertTrue(ctx.animacoes.contains("cid_redemoinho"));
    }

    @Test
    void osTresCortesCabemEmDozeTicks() {
        usar(Tecla.Z);
        ticks(2); // a carga
        assertEquals(0, ctx.areas.size());
        ticks(1);
        assertEquals(1, ctx.areas.size(), "o primeiro corte sai logo depois do disparo");
        ticks(12);
        assertEquals(3, ctx.areas.size());
    }

    @Test
    void posImagemAvancaNoveBlocosEDeixaOChamarizNoPontoDePartida() {
        usar(Tecla.X);
        ticks(10);
        assertEquals(9, ctx.distanciaDoPasso);
        assertEquals(new Vetor(0, -1.62, 0), ctx.chamarizEm, "a imagem fica nos pés de onde ele saiu");
        assertEquals(40, ctx.chamarizTicks);
        assertTrue(ctx.visuais.contains("cid_pos_imagem"));
        assertEquals(90, kit.recurso().atual(), 5);
        assertTrue(kit.recargas().pronta("cid_pos_imagem", agora + 120));
        assertFalse(kit.recargas().pronta("cid_pos_imagem", agora + 100));
    }

    @Test
    void prisaoFereSeisNoPrimeiroEPrendeUmSegundoDepois() {
        ctx.inimigoEm = new Vetor(8, 0, 0);
        ctx.posAlvo = new Vetor(9, 0, 0);
        usar(Tecla.C);
        ticks(12);
        assertEquals(List.of(6f), ctx.danos);
        assertTrue(ctx.paralisias.isEmpty(), "as correntes só fecham 1 s depois do acerto");
        ticks(20);
        assertEquals(List.of(50), ctx.paralisias, "prende só o alvo acertado, por 2,5 s");
        assertTrue(ctx.visuais.contains("cid_correntes"));
        ticks(10);
        assertTrue(kit.efeitos().isEmpty());
    }

    @Test
    void prisaoParaNoPrimeiroInimigoENaoPassaDeDezesseisBlocos() {
        ctx.inimigoEm = new Vetor(5, 0, 0);
        ctx.posAlvo = ctx.inimigoEm;
        usar(Tecla.C);
        ticks(40);
        double maisLonge = ctx.pontosDeDano.stream().mapToDouble(Vetor::x).max().orElse(0);
        assertTrue(maisLonge < 7, "o slime some ao acertar, foi até " + maisLonge);
        assertTrue(ctx.fins.contains("cid_slime"));
    }

    @Test
    void semInimigoOSlimeAndaDezesseisBlocosESome() {
        usar(Tecla.C);
        ticks(40);
        double maisLonge = ctx.pontosDeDano.stream().mapToDouble(Vetor::x).max().orElse(0);
        assertEquals(17, maisLonge, 1e-6, "sai a 1 bloco dos olhos e anda 16");
        assertTrue(kit.efeitos().isEmpty());
        assertTrue(ctx.danos.isEmpty());
    }

    @Test
    void prisaoParaNaParede() {
        ctx.paredeEm = v -> v.x() > 6;
        ctx.inimigoEm = new Vetor(10, 0, 0);
        usar(Tecla.C);
        ticks(40);
        assertTrue(ctx.danos.isEmpty());
        assertTrue(ctx.fins.contains("cid_slime"));
    }

    @Test
    void seOAlvoSumiuAsCorrentesNaoPrendemNinguem() {
        ctx.inimigoEm = new Vetor(4, 0, 0);
        ctx.posAlvo = null;
        usar(Tecla.C);
        ticks(40);
        assertEquals(1, ctx.danos.size());
        assertTrue(ctx.paralisias.isEmpty());
    }

    @Test
    void overdriveDuraOitoSegundosDaVelocidadeEParaAMagia() {
        kit.recurso().carregar(50);
        usar(Tecla.G);
        ticks(3);
        assertTrue(ctx.efeitoAtivo(Cid.EmOverdrive.class));
        double aoLigar = kit.recurso().atual();
        ticks(40);
        assertFalse(ctx.velocidades.isEmpty());
        assertEquals(2, ctx.velocidades.get(0)[0], "Velocidade II");
        assertEquals(aoLigar, kit.recurso().atual(), 1e-9, "no Overdrive a Magia não volta");
        ticks(1);
        assertEquals(new Arma(6.5f, 1.6f, 3.5f), ctx.arma, "+30% no ataque básico");
        ticks(170);
        assertFalse(ctx.efeitoAtivo(Cid.EmOverdrive.class));
        assertEquals(List.of(4f), ctx.recuos, "levou até o fim: 4 de recuo");
        assertTrue(kit.recurso().atual() > aoLigar, "acabou: a Magia volta a subir");
        assertFalse(kit.recargas().pronta(Cid.OVERDRIVE, agora));
        ticks(1);
        assertEquals(new Arma(5, 1.6f, 3.5f), ctx.arma);
    }

    @Test
    void aRecargaDoOverdriveContaDoFim() {
        usar(Tecla.G);
        ticks(165);
        assertFalse(ctx.efeitoAtivo(Cid.EmOverdrive.class));
        long fim = agora;
        assertFalse(kit.recargas().pronta(Cid.OVERDRIVE, fim + 480));
        assertTrue(kit.recargas().pronta(Cid.OVERDRIVE, fim + 500));
    }

    @Test
    void gDeNovoDesligaOOverdriveSemRecuo() {
        usar(Tecla.G);
        ticks(40);
        usar(Tecla.G);
        ticks(10);
        assertFalse(ctx.efeitoAtivo(Cid.EmOverdrive.class));
        assertTrue(ctx.recuos.isEmpty());
        assertFalse(kit.recargas().pronta(Cid.OVERDRIVE, agora), "desligar também põe em recarga");
        assertTrue(ctx.fins.contains("cid_overdrive"));
    }

    @Test
    void gApertadoNoUltimoInstanteNaoEmendaOutroOverdrive() {
        usar(Tecla.G);
        // Até o Overdrive estar para acabar: o efeito começa no tick seguinte ao disparo.
        while (kit.efeitos().stream().noneMatch(Cid.EmOverdrive.class::isInstance)) ticks(1);
        ticks(Cid.Overdrive.DURACAO - 2);
        assertTrue(ctx.efeitoAtivo(Cid.EmOverdrive.class));
        usar(Tecla.G); // ainda ligado e sem recarga: o aperto é aceito, e o efeito acaba durante a carga dele
        ticks(12);
        assertFalse(ctx.efeitoAtivo(Cid.EmOverdrive.class), "não pode ligar outro em seguida");
        assertFalse(kit.recargas().pronta(Cid.OVERDRIVE, agora + 400), "e a recarga de 25 s continua valendo");
        assertEquals(List.of(4f), ctx.recuos);
    }

    @Test
    void morrerNoOverdrivePoeARecargaESoltaAMagia() {
        kit.recurso().carregar(20);
        usar(Tecla.G);
        ticks(30);
        double antes = kit.recurso().atual();
        kit.descartar(ctx);
        assertFalse(kit.recargas().pronta(Cid.OVERDRIVE, agora + 400));
        assertTrue(ctx.recuos.isEmpty(), "quem morre não paga o recuo");
        ticks(20);
        assertTrue(kit.recurso().atual() > antes, "a Magia volta a subir");
    }

    @Test
    void noOverdriveOsGolpesFeremTrintaPorCentoAMais() {
        usar(Tecla.G);
        ticks(10);
        usar(Tecla.Z);
        ticks(30);
        assertEquals(3, ctx.areas.size());
        assertEquals(5.2, ctx.areas.get(0)[1], 1e-5);
        ctx.inimigoEm = new Vetor(4, 0, 0);
        usar(Tecla.C);
        ticks(12);
        assertEquals(7.8f, ctx.danos.get(0), 1e-5);
    }

    @Test
    void oTrajeDeSlimeCortaPelaMetadeSoOGolpeForteEComAPassivaPronta() {
        assertEquals(5f, Cid.amortecer(10f, true));
        assertEquals(4f, Cid.amortecer(8f, true));
        assertEquals(7.9f, Cid.amortecer(7.9f, true), "golpe fraco passa inteiro e não gasta o traje");
        assertEquals(10f, Cid.amortecer(10f, false));
        assertTrue(Cid.gastaOTraje(8f, true));
        assertFalse(Cid.gastaOTraje(7.9f, true));
        assertFalse(Cid.gastaOTraje(20f, false));
        assertEquals(240, Cid.RECARGA_DO_TRAJE);
    }

    @Test
    void aEspadaAtomicaEOsAtomicSoValemDepoisDoDespertar() {
        assertEquals(Set.of(Cid.ESPADA_ATOMICA, Cid.RECOVERY, Cid.ATOMIC), cid.trancas().habilidades());
        assertFalse(cid.trancas().passivas(), "a espada e o traje de slime valem desde o começo");
        assertFalse(kit.apertar(Tecla.V, agora));
        assertFalse(kit.apertar(Tecla.R, agora, true));
        kit.definirDesperto(true);
        assertTrue(kit.apertar(Tecla.V, agora));
    }

    @Test
    void espadaAtomicaCarregaDozeTicksECortaEmArco() {
        kit.definirDesperto(true);
        ctx.inimigoEm = new Vetor(6, 0, 0);
        usar(Tecla.V);
        ticks(11);
        assertTrue(ctx.arcos.isEmpty(), "12 ticks de carga: dispara no décimo segundo");
        assertTrue(ctx.visuais.contains("cid_espada_carga"));
        assertTrue(ctx.animacoes.contains("cid_espada_atomica"));
        ticks(1);
        long disparo = agora - 1;
        assertEquals(1, ctx.arcos.size(), "um corte só");
        double[] a = ctx.arcos.get(0);
        assertEquals(12, a[0], "até 12 blocos");
        assertEquals(120, a[1], "num arco de 120 graus");
        assertEquals(16, a[3], 1e-6);
        assertTrue(ctx.visuais.contains("cid_espada_atomica"));
        assertEquals(70, kit.recurso().atual(), 4, "custa 30");
        ticks(30);
        assertEquals(1, ctx.arcos.size());
        assertFalse(kit.recargas().pronta(Cid.ESPADA_ATOMICA, disparo + 279));
        assertTrue(kit.recargas().pronta(Cid.ESPADA_ATOMICA, disparo + 280), "recarga de 14 s");
    }

    @Test
    void espadaAtomicaInterrompidaDesfazACarga() {
        kit.definirDesperto(true);
        usar(Tecla.V);
        ticks(5);
        kit.atordoar(agora, ctx);
        assertTrue(ctx.fins.contains("cid_espada_carga"));
        ticks(20);
        assertTrue(ctx.arcos.isEmpty());
    }

    @Test
    void noOverdriveAEspadaAtomicaTambemFereMais() {
        kit.definirDesperto(true);
        usar(Tecla.G);
        ticks(10);
        usar(Tecla.V);
        ticks(16);
        assertEquals(20.8, ctx.arcos.get(0)[3], 1e-5);
    }

    @Test
    void recoveryAtomicCarregaTresSegundosParadoECuraEmRaioDezesseis() {
        kit.definirDesperto(true);
        assertTrue(kit.apertar(Tecla.R, agora, true));
        ticks(1);
        assertEquals(List.of(true), ctx.firmes, "cravou a espada: fica preso ao chão");
        assertTrue(ctx.visuais.contains("cid_recovery_carga"));
        assertTrue(ctx.animacoes.contains(Cid.RECOVERY));
        ticks(58);
        assertTrue(ctx.restauros.isEmpty(), "3 s de carga: dispara no tick 60");
        ticks(2);
        assertEquals(List.of(16.0), ctx.restauros);
        assertEquals(List.of(true, false), ctx.firmes, "solta no disparo");
        assertTrue(ctx.visuais.contains(Cid.RECOVERY));
        assertEquals(40, kit.recurso().atual(), 2, "custa 60");
        assertTrue(ctx.arcos.isEmpty() && ctx.areas.isEmpty() && ctx.danos.isEmpty(), "não fere ninguém");
    }

    @Test
    void recoveryDivideARecargaDeNoventaSegundosComOR() {
        kit.definirDesperto(true);
        assertTrue(kit.apertar(Tecla.R, agora, true));
        while (ctx.restauros.isEmpty()) ticks(1);
        long disparo = agora - 1;
        assertFalse(kit.recargas().pronta(Cid.RECOVERY, disparo + 1799));
        assertTrue(kit.recargas().pronta(Cid.RECOVERY, disparo + 1800));
        // O contexto falso já está um tick adiante quando o golpe põe a recarga do R.
        assertFalse(kit.recargas().pronta(Cid.ATOMIC, disparo + 1799));
        assertTrue(kit.recargas().pronta(Cid.ATOMIC, disparo + 1801));
    }

    @Test
    void golpeDeOitoOuMaisCancelaORecovery() {
        kit.definirDesperto(true);
        assertTrue(kit.apertar(Tecla.R, agora, true));
        ticks(20);
        kit.levouDano(7.9f, agora, ctx);
        assertTrue(kit.execucao().isPresent(), "golpe fraco não cancela");
        kit.levouDano(8f, agora, ctx);
        assertTrue(kit.execucao().isEmpty());
        assertEquals(List.of(true, false), ctx.firmes, "cancelado: ele se solta");
        assertTrue(ctx.fins.contains("cid_recovery_carga"));
        assertTrue(ctx.paradas > 0);
        ticks(60);
        assertTrue(ctx.restauros.isEmpty());
        assertEquals(100, kit.recurso().atual(), 1e-6, "cancelado não custa nada");
        assertTrue(kit.recargas().pronta(Cid.ATOMIC, agora), "e a recarga do R não corre");
    }

    @Test
    void morrerCarregandoORecoverySoltaOJogador() {
        kit.definirDesperto(true);
        assertTrue(kit.apertar(Tecla.R, agora, true));
        ticks(20);
        kit.descartar(ctx);
        assertEquals(List.of(true, false), ctx.firmes);
    }

    @Test
    void oRDePeAindaNaoComeca() {
        kit.definirDesperto(true);
        assertFalse(kit.apertar(Tecla.R, agora), "o I Am Atomic final entra na parte 6");
    }
}
