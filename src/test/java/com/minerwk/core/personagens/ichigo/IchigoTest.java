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
        /** O inimigo mais próximo que a forma Hollow acha (ou null). */
        Object proximo;
        final List<Float> feridos = new ArrayList<>(), vidas = new ArrayList<>(), curas = new ArrayList<>();
        public Object inimigoProximo(double raio) { return proximo; }
        public void ferir(Object alvo, float dano) { feridos.add(dano); }
        public void vida(float v) { vidas.add(v); }
        public void curar(float v) { curas.add(v); }
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
        public com.minerwk.core.maestria.Melhorias melhorias() { return kit.melhorias(); }
        public String forma() { return kit.forma(); }
        public boolean mudarForma(String forma) { return kit.mudarForma(forma, this); }
        public void criarEfeito(Efeito e) { kit.adicionarEfeito(e); }
        public double recurso() { return kit.recurso().atual(); }
        public void recarga(String habilidade, int ticks) { kit.recargas().iniciar(habilidade, agora, ticks); }
        final List<Float> danosDiretos = new ArrayList<>();
        final List<Vetor> pontosDiretos = new ArrayList<>();
        public int danoDiretoUmaVez(Vetor c, double r, float d, Set<Object> atingidos) {
            pontosDiretos.add(c);
            if (inimigoEm != null && c.distancia(inimigoEm) <= r && atingidos.add("inimigo")) { danosDiretos.add(d); return 1; }
            return 0;
        }
        public boolean gastarRecurso(double q) {
            if (!kit.recurso().pode(q)) return false;
            kit.recurso().gastar(q);
            return true;
        }
        public void sinalAoRecurso(String sinal, int ticks) { kit.recurso().sinal(sinal, ticks); }
        public boolean efeitoAtivo(Class<? extends Efeito> tipo) { return kit.efeitos().stream().anyMatch(tipo::isInstance); }
        public String golpeEmAndamento() { return kit.execucao().map(e -> e.habilidade().id()).orElse(""); }
        final List<String> cutscenes = new ArrayList<>();
        public void cutscene(String id) { cutscenes.add(id); }
        public void encerrarCutscene() { cutscenes.add("fim"); }
        final List<String> animacoes = new ArrayList<>(), visuais = new ArrayList<>(), fins = new ArrayList<>();
        int paradas;
        Vetor rumoDoVisual;
        public void apresentacao(String animacao, String efeito, String som) { if (!animacao.isEmpty()) animacoes.add(animacao); }
        public void pararAnimacao() { paradas++; }
        public void visual(String tipo, Vetor origem, Vetor direcao, float forca, int duracao) {
            visuais.add(tipo);
            rumoDoVisual = direcao;
            assertTrue(duracao > 0, tipo);
        }
        public void visualFim(String tipo, Vetor ponto) { fins.add(tipo); }
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
    void shikaiTemCincoGolpesEOBankaiSeis() {
        assertEquals("ichigo", ichigo.id());
        assertEquals(Set.of(Tecla.Z, Tecla.X, Tecla.C, Tecla.V, Tecla.G), ichigo.habilidades().keySet());
        Forma bankai = ichigo.transformacoes().get(0);
        assertEquals(Reiatsu.BANKAI, bankai.id());
        assertEquals(Set.of(Tecla.Z, Tecla.X, Tecla.C, Tecla.V, Tecla.G, Tecla.R), bankai.habilidades().keySet());
        Set<String> ids = new HashSet<>();
        for (Habilidade h : ichigo.todasAsHabilidades()) assertTrue(ids.add(h.id()), "id repetido: " + h.id());
        assertEquals(11, ids.size());
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
        assertEquals(Reiatsu.MAXIMO - 12, kit.recurso().atual(), 5);
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
        assertEquals(Reiatsu.MAXIMO - 40, kit.recurso().atual(), 1e-6);
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

    @Test
    void getsugaMostraACargaOCorteEAMeiaLua() {
        usar(Tecla.Z);
        ticks(30);
        assertEquals(List.of("ichigo_getsuga", "ichigo_getsuga_disparo"), ctx.animacoes);
        assertEquals(List.of("ichigo_carga", "ichigo_getsuga"), ctx.visuais);
        assertEquals(List.of("ichigo_carga"), ctx.fins, "a carga some no disparo; a meia-lua foi até o fim");
    }

    @Test
    void getsugaQueBateEmBlocoEncerraOVisualDaMeiaLua() {
        ctx.paredeEm = v -> v.x() > 6;
        usar(Tecla.Z);
        ticks(30);
        assertTrue(ctx.fins.contains("ichigo_getsuga"));
    }

    @Test
    void cargaInterrompidaApagaOReiatsuEParaAAnimacao() {
        usar(Tecla.Z);
        ticks(3);
        kit.atordoar(agora, ctx);
        assertEquals(List.of("ichigo_carga"), ctx.visuais);
        assertEquals(List.of("ichigo_carga"), ctx.fins);
        assertEquals(1, ctx.paradas);
        assertEquals(List.of("ichigo_getsuga"), ctx.animacoes, "o corte não saiu");
    }

    @Test
    void shunpoMostraOCaminhoPercorrido() {
        usar(Tecla.X);
        ticks(6);
        assertEquals(List.of("ichigo_shunpo"), ctx.visuais);
        // O contexto falso não anda: o caminho é o que o jogador andou de verdade, aqui nada.
        assertEquals(Vetor.ZERO, ctx.rumoDoVisual);
    }

    @Test
    void bankaiMostraACargaOPilarEASaida() {
        entrarNoBankai();
        assertEquals(List.of("ichigo_bankai_carga", "ichigo_bankai"), ctx.visuais);
        assertEquals(List.of("ichigo_bankai_carga"), ctx.fins);
        assertEquals(0, ctx.paradas, "trocar de forma no disparo não corta a animação da liberação");
        usar(Tecla.C);
        ticks(12);
        usar(Tecla.G);
        ticks(10);
        assertTrue(ctx.visuais.containsAll(List.of("ichigo_lamina", "ichigo_sair")));
        assertTrue(ctx.fins.contains("ichigo_lamina"), "a lâmina carregada apaga ao sair do Bankai");
    }

    @Test
    void todaAnimacaoPedidaExisteNoArquivo() throws Exception {
        Tecla[] shikai = {Tecla.Z, Tecla.X, Tecla.C, Tecla.V}, bankai = {Tecla.Z, Tecla.X, Tecla.C, Tecla.G};
        for (Tecla tecla : shikai) {
            kit.recurso().carregar(Reiatsu.MAXIMO);
            usar(tecla);
            ticks(40);
        }
        entrarNoBankai();
        for (Tecla tecla : bankai) {
            kit.recurso().carregar(Reiatsu.MAXIMO);
            usar(tecla);
            ticks(20);
        }
        String arquivo = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/assets/minerwk/player_animations/ichigo.json"));
        assertEquals(10, ctx.animacoes.size(), ctx.animacoes.toString());
        for (String animacao : ctx.animacoes) {
            assertTrue(arquivo.contains("\"" + animacao + "\": {"), "falta a animação " + animacao);
        }
    }

    // --- Parte 6: máscara Hollow, Mugetsu e a perda dos poderes ---

    /** No Bankai, já desperto (a máscara e o Mugetsu são trancados), com o reiatsu cheio. */
    private void comNos(java.util.Map<String, Integer> nos) {
        kit.definirMelhorias(new com.minerwk.core.maestria.Melhorias(ArvoreIchigo.ARVORE, nos));
    }

    @Test
    void aArvoreTemODespertarNoMeioESoNosDeHabilidadesQueExistem() {
        var arvore = ichigo.arvore();
        assertEquals(13, arvore.nos().size());
        var despertar = arvore.no(ArvoreIchigo.DESPERTAR).orElseThrow();
        assertEquals(com.minerwk.core.maestria.No.Tipo.DESPERTAR, despertar.tipo());
        assertEquals(20, despertar.nivelMinimo());
        assertEquals(5, despertar.custoDoProximo(0));
        Set<String> ids = new HashSet<>();
        for (Habilidade h : ichigo.todasAsHabilidades()) ids.add(h.id());
        for (var no : arvore.nos()) {
            if (!no.alvo().isEmpty()) assertTrue(ids.contains(no.alvo()), "alvo desconhecido: " + no.alvo());
        }
        assertEquals(6, arvore.nos().stream().filter(n -> n.depoisDoDespertar()).count());
    }

    @Test
    void osNosDeForcaAumentamOGetsugaEOGetsugaNegro() {
        comNos(java.util.Map.of(ArvoreIchigo.GETSUGA_FORCA, 3, ArvoreIchigo.NEGRO_FORCA, 2));
        ctx.inimigoEm = new Vetor(6, 0, 0);
        usar(Tecla.Z);
        ticks(30);
        assertEquals(12f * 1.24f, ctx.danos.get(0), 1e-4);
        ctx.danos.clear();
        entrarNoBankai();
        usar(Tecla.Z);
        ticks(20);
        assertEquals(10f * 1.16f, ctx.danos.get(0), 1e-4);
    }

    @Test
    void oNoDaMascaraAFazDurarMais() {
        comNos(java.util.Map.of(ArvoreIchigo.MASCARA_DURACAO, 3, ArvoreIchigo.LAMINA_DURACAO, 3));
        bankaiDesperto();
        usar(Tecla.V);
        ticks(Ichigo.MascaraHollow.GESTO + 2);
        Efeito mascara = kit.efeitos().stream().filter(e -> e instanceof Ichigo.MascaraAtiva).findFirst().orElseThrow();
        assertEquals(286, mascara.duracao(), "220 ticks e mais 30%");
        ticks(20);
        usar(Tecla.C);
        ticks(8);
        Efeito lamina = kit.efeitos().stream().filter(e -> e instanceof Ichigo.LaminaCarregada).findFirst().orElseThrow();
        assertEquals(156, lamina.duracao(), "120 ticks e mais 30%");
    }

    @Test
    void oNoDosPoderesOsTrazDeVoltaMaisCedo() {
        comNos(java.util.Map.of(ArvoreIchigo.PODERES_VOLTA, 3));
        bankaiDesperto();
        mugetsuInteiro();
        Efeito sem = kit.efeitos().stream().filter(e -> e instanceof Ichigo.SemPoderes).findFirst().orElseThrow();
        assertEquals(840, sem.duracao(), "60 s menos 30%");
    }

    @Test
    void noMeioDaLutaContraOZangetsuAMascaraSeFormaSemCustoEJaNoBankai() {
        double antes = kit.recurso().atual();
        Ichigo.vestirAMascara(ctx);
        assertEquals(Reiatsu.BANKAI, kit.forma(), "do Shikai ele vai direto ao Bankai");
        assertTrue(kit.efeitos().stream().anyMatch(e -> e instanceof Ichigo.MascaraAtiva));
        assertTrue(ctx.visuais.contains("ichigo_mascara"));
        assertEquals(antes, kit.recurso().atual(), 1e-9, "sem custo");
    }

    private void bankaiDesperto() {
        kit.definirDesperto(true);
        entrarNoBankai();
    }

    @Test
    void mascaraEMugetsuSaoTrancadosAteODespertar() {
        assertEquals(Set.of(Ichigo.MASCARA, Ichigo.MUGETSU), ichigo.trancas().habilidades());
        assertFalse(ichigo.trancas().passivas(), "a espada na mão não é trancada");
        entrarNoBankai();
        assertTrue(kit.trancada(Tecla.V) && kit.trancada(Tecla.R));
        assertFalse(kit.apertar(Tecla.V, agora));
        assertFalse(kit.apertar(Tecla.R, agora));
        assertFalse(kit.trancada(Tecla.Z), "o resto do Bankai é livre");
        kit.definirDesperto(true);
        assertTrue(kit.apertar(Tecla.V, agora));
    }

    @Test
    void mascaraDuraOnzeSegundosEParaODrenoDoBankai() {
        bankaiDesperto();
        usar(Tecla.V);
        ticks(Ichigo.MascaraHollow.GESTO);
        assertTrue(ctx.visuais.contains("ichigo_mascara"), "a máscara aparece no rosto");
        assertTrue(ctx.efeitoAtivo(Ichigo.MascaraAtiva.class));
        double comMascara = kit.recurso().atual();
        assertEquals(Reiatsu.MAXIMO - 40, comMascara, 1, "custa 40 (mais o que o Bankai drenou durante o gesto)");
        ticks(200);
        assertEquals(comMascara, kit.recurso().atual(), 1e-6, "com a máscara o Bankai não drena");
        assertTrue(ctx.efeitoAtivo(Ichigo.MascaraAtiva.class), "ainda dentro dos 11 s");
        ticks(25);
        assertFalse(ctx.efeitoAtivo(Ichigo.MascaraAtiva.class), "a máscara se desfaz aos 11 s");
        assertEquals(220, Ichigo.MascaraAtiva.DURACAO);
        double antes = kit.recurso().atual();
        ticks(20);
        assertEquals(antes - 1, kit.recurso().atual(), 1e-6, "sem a máscara o dreno volta: 1 por segundo");
        assertEquals(Reiatsu.BANKAI, kit.forma(), "a máscara acabar não tira do Bankai");
        assertFalse(kit.recargas().pronta(Ichigo.MASCARA, agora));
        assertTrue(kit.recargas().restante(Ichigo.MASCARA, agora) > 500, "recarga de 40 s contada do uso");
    }

    @Test
    void mascaraDeixaTodoDanoTrintaPorCentoMaior() {
        bankaiDesperto();
        ticks(1);
        assertEquals(Ichigo.TENSA_ZANGETSU, ctx.arma);
        usar(Tecla.V);
        ticks(Ichigo.MascaraHollow.GESTO + 2);
        assertEquals(Ichigo.TENSA_ZANGETSU.dano() * 1.3f, ctx.arma.dano(), 1e-4, "o ataque básico também");
        assertEquals(Ichigo.TENSA_ZANGETSU.alcance(), ctx.arma.alcance());
        ticks(8); // o resto do gesto da máscara
        ctx.inimigoEm = new Vetor(10, 0, 0);
        usar(Tecla.Z);
        ticks(12);
        assertEquals(List.of(13f), ctx.danos, "Getsuga negro de 10 vira 13");
        // A lâmina carregada por cima da máscara: os dois bônus valem.
        kit.recargas().limpar();
        usar(Tecla.C);
        ticks(6);
        assertEquals(Ichigo.TENSA_CARREGADA.dano() * 1.3f, ctx.arma.dano(), 1e-4);
        assertEquals(Ichigo.TENSA_CARREGADA.alcance(), ctx.arma.alcance());
    }

    @Test
    void comAMascaraOGetsugaNegroFicaCinquentaPorCentoMaisLargo() {
        bankaiDesperto();
        // 2 blocos para o lado: fora do corte normal (meia largura 1,5), dentro do alargado (2,25).
        ctx.inimigoEm = new Vetor(10, 0, 2);
        usar(Tecla.Z);
        ticks(14);
        assertTrue(ctx.danos.isEmpty(), "sem máscara não alcança");
        kit.recargas().limpar();
        usar(Tecla.V);
        ticks(Ichigo.MascaraHollow.GESTO + 8);
        usar(Tecla.Z);
        ticks(14);
        assertEquals(1, ctx.danos.size(), "com a máscara alcança");
    }

    @Test
    void sairDoBankaiDesfazAMascaraEODrenoVoltaAoNormal() {
        bankaiDesperto();
        usar(Tecla.V);
        ticks(Ichigo.MascaraHollow.GESTO + 10);
        usar(Tecla.G);
        ticks(10);
        assertEquals(Forma.BASE, kit.forma());
        assertFalse(ctx.efeitoAtivo(Ichigo.MascaraAtiva.class));
        assertTrue(ctx.fins.contains("ichigo_mascara"), "a máscara some do rosto na hora");
        assertEquals(Ichigo.ZANGETSU, ctx.arma);
        // De volta ao Bankai depois, o dreno é o normal (a pausa da máscara não sobrou).
        kit.recargas().limpar();
        kit.recurso().carregar(Reiatsu.MAXIMO);
        usar(Tecla.G);
        ticks(45);
        double antes = kit.recurso().atual();
        ticks(20);
        assertEquals(antes - 1, kit.recurso().atual(), 1e-6);
    }

    @Test
    void mugetsuPedeCemDeReiatsu() {
        bankaiDesperto();
        kit.recurso().carregar(99);
        assertFalse(kit.apertar(Tecla.R, agora));
        kit.recurso().carregar(100);
        assertTrue(kit.apertar(Tecla.R, agora));
    }

    /** Usa o Mugetsu e espera a carga (a cutscene), o disparo e o véu inteiro. */
    private void mugetsuInteiro() {
        usar(Tecla.R);
        ticks(Ichigo.Mugetsu.CARGA + Ichigo.Veu.TICKS + 12);
    }

    @Test
    void mugetsuFereQuarentaSemArmaduraNumVeuFinoQueAndaCentoECinquenta() {
        bankaiDesperto();
        ctx.inimigoEm = new Vetor(140, 0, 1);
        mugetsuInteiro();
        assertEquals(List.of(40f), ctx.danosDiretos, "40 de dano, uma vez, ignorando armadura");
        assertTrue(ctx.danos.isEmpty(), "nada pelo dano comum");
        double maisLonge = ctx.pontosDiretos.stream().mapToDouble(Vetor::x).max().orElse(0);
        assertEquals(150, maisLonge, 1e-6, "o véu avança 150 blocos");
        assertEquals(1.5, Ichigo.Veu.MEIA_LARGURA, "3 blocos de largura");
        assertTrue(ctx.visuais.contains("ichigo_mugetsu_carga") && ctx.visuais.contains("ichigo_mugetsu"));
        assertTrue(ctx.cutscenes.contains("ichigo_mugetsu"), "a carga é a cutscene");
    }

    @Test
    void mugetsuNaoAlcancaQuemEstaForaDoVeu() {
        bankaiDesperto();
        ctx.inimigoEm = new Vetor(20, 0, 3);
        mugetsuInteiro();
        assertTrue(ctx.danosDiretos.isEmpty(), "3 blocos para o lado: fora dos 3 de largura");
    }

    @Test
    void mugetsuAlcancaQuemEstaNoAltoMasNaoAcimaDoVeu() {
        bankaiDesperto();
        ctx.inimigoEm = new Vetor(60, 31, 0);
        mugetsuInteiro();
        assertEquals(List.of(40f), ctx.danosDiretos, "o véu tem 40 de altura");
    }

    @Test
    void mugetsuNaoFereAcimaDosQuarentaDeAltura() {
        bankaiDesperto();
        ctx.inimigoEm = new Vetor(60, 44, 0);
        mugetsuInteiro();
        assertTrue(ctx.danosDiretos.isEmpty());
    }

    @Test
    void mugetsuNaoPassaDosCentoECinquentaBlocos() {
        bankaiDesperto();
        ctx.inimigoEm = new Vetor(156, 0, 0);
        mugetsuInteiro();
        assertTrue(ctx.danosDiretos.isEmpty());
    }

    @Test
    void mugetsuAtravessaParedesENaoParaEmBloco() {
        bankaiDesperto();
        ctx.paredeEm = v -> v.x() > 10;
        ctx.inimigoEm = new Vetor(30, 0, 0);
        mugetsuInteiro();
        assertEquals(List.of(40f), ctx.danosDiretos);
    }

    @Test
    void duranteACargaDoMugetsuOReiatsuNaoEscorreEEleGastaTudoNoCorte() {
        bankaiDesperto();
        usar(Tecla.R);
        ticks(Ichigo.Mugetsu.CARGA - 1);
        assertEquals(Reiatsu.MAXIMO, kit.recurso().atual(), 1e-6, "a carga dura 6 s: sem pausa, o Bankai drenaria 12");
        assertEquals(Reiatsu.BANKAI, kit.forma());
        ticks(3);
        assertEquals(0, kit.recurso().atual(), 1e-9, "gasta todo o reiatsu, não só os 100");
    }

    @Test
    void depoisDoMugetsuOIchigoPerdeOsPoderesPorSessentaSegundos() {
        bankaiDesperto();
        mugetsuInteiro();
        assertEquals(Forma.BASE, kit.forma(), "volta ao Shikai");
        ticks(1);
        assertEquals(Ichigo.ZANGETSU, ctx.arma, "só o ataque básico");
        assertEquals(1200, Ichigo.SEM_PODERES);
        for (Tecla t : List.of(Tecla.Z, Tecla.X, Tecla.C, Tecla.V, Tecla.G)) {
            assertFalse(kit.apertar(t, agora), "sem golpes: " + t);
            String id = kit.habilidade(t).orElseThrow().id();
            long falta = kit.recargas().restante(id, agora);
            assertTrue(falta > 1100 && falta <= 1200, "o HUD mostra o tempo que falta em " + id + ": " + falta);
        }
        ticks(600);
        assertEquals(0, kit.recurso().atual(), 1e-9, "sem reiatsu: nem regenera");
        assertFalse(kit.apertar(Tecla.Z, agora));
        ticks(620);
        assertTrue(kit.recurso().atual() > 0, "passados os 60 s o reiatsu volta a subir");
        kit.recurso().carregar(Reiatsu.MAXIMO);
        assertTrue(kit.apertar(Tecla.Z, agora), "e os golpes voltam");
    }

    @Test
    void mugetsuTemRecargaDeCentoEOitentaSegundosContadaDoGolpe() {
        bankaiDesperto();
        usar(Tecla.R);
        ticks(Ichigo.Mugetsu.CARGA + 1);
        long falta = kit.recargas().restante(Ichigo.MUGETSU, agora);
        assertTrue(falta > 3590 && falta <= 3600, "180 s a partir do corte, faltam " + falta);
    }

    @Test
    void comReiatsuExatoOBankaiNaoAcabaNoMeioDaCargaDoMugetsu() {
        bankaiDesperto();
        kit.recurso().carregar(100);
        ctx.inimigoEm = new Vetor(10, 0, 0);
        mugetsuInteiro();
        assertEquals(List.of(40f), ctx.danosDiretos, "o corte sai");
        // Saiu do Bankai pelo Mugetsu (60 s), não por falta de reiatsu (10 s).
        assertTrue(kit.recargas().restante(Ichigo.ENTRAR_NO_BANKAI, agora) > 1000);
    }

    @Test
    void golpeForteNaCargaCortaOMugetsuSemGastarNada() {
        bankaiDesperto();
        usar(Tecla.R);
        ticks(40);
        kit.levouDano(6f, agora, ctx);
        ticks(Ichigo.Mugetsu.CARGA + 40);
        assertTrue(ctx.danosDiretos.isEmpty(), "o corte não sai");
        assertEquals(Reiatsu.BANKAI, kit.forma(), "continua no Bankai, com os poderes");
        assertTrue(kit.recurso().atual() > 150, "não gastou os 100 nem o resto; só o dreno voltou");
        assertTrue(ctx.fins.contains("ichigo_mugetsu_carga"), "o visual da carga some");
        assertTrue(ctx.cutscenes.contains("fim"), "e a câmera volta ao jogador");
        ticks(1);
        assertEquals(Ichigo.TENSA_ZANGETSU, ctx.arma);
    }

    @Test
    void golpeFracoNaoCortaOMugetsu() {
        bankaiDesperto();
        ctx.inimigoEm = new Vetor(10, 0, 0);
        usar(Tecla.R);
        ticks(40);
        kit.levouDano(5.9f, agora, ctx);
        ticks(Ichigo.Mugetsu.CARGA + Ichigo.Veu.TICKS);
        assertEquals(List.of(40f), ctx.danosDiretos);
    }

    @Test
    void mugetsuComAMascaraFereMaisEAMascaraAcabaComOsPoderes() {
        bankaiDesperto();
        usar(Tecla.V);
        ticks(Ichigo.MascaraHollow.GESTO + 8);
        kit.recurso().carregar(Reiatsu.MAXIMO);
        ctx.inimigoEm = new Vetor(10, 0, 0);
        mugetsuInteiro();
        assertEquals(List.of(52f), ctx.danosDiretos, "40 com os 30% da máscara");
        assertFalse(ctx.efeitoAtivo(Ichigo.MascaraAtiva.class), "fora do Bankai a máscara se desfaz");
    }

    @Test
    void aPerdaEAVoltaDosPoderesAparecemOndeOIchigoEstiver() {
        bankaiDesperto();
        mugetsuInteiro();
        assertTrue(ctx.efeitoAtivo(Ichigo.SemPoderes.class));
        ticks(Ichigo.SemPoderes.AVISO);
        assertTrue(ctx.visuais.contains("ichigo_poderes_perdidos"), "depois do corte, o reiatsu vai embora");
        assertFalse(ctx.visuais.contains("ichigo_poderes_voltam"));
        ticks(Ichigo.SEM_PODERES);
        assertTrue(ctx.visuais.contains("ichigo_poderes_voltam"), "aos 60 s os poderes voltam");
        assertFalse(ctx.efeitoAtivo(Ichigo.SemPoderes.class));
        assertEquals(1, ctx.visuais.stream().filter("ichigo_poderes_voltam"::equals).count());
    }

    @Test
    void aCutsceneDoMugetsuTemATransformacaoEOSilencioAntesDoCorte() {
        assertTrue(0 < Ichigo.Mugetsu.TRANSFORMA && Ichigo.Mugetsu.TRANSFORMA < Ichigo.Mugetsu.SILENCIO);
        assertTrue(Ichigo.Mugetsu.SILENCIO < Ichigo.Mugetsu.CARGA);
        assertTrue(Ichigo.Mugetsu.CARGA - Ichigo.Mugetsu.SILENCIO >= 20, "ao menos 1 s de silêncio");
    }

    @Test
    void aMascaraEOMugetsuTocamAsSuasAnimacoes() throws Exception {
        bankaiDesperto();
        usar(Tecla.V);
        ticks(Ichigo.MascaraHollow.GESTO + 8);
        kit.recurso().carregar(Reiatsu.MAXIMO);
        usar(Tecla.R);
        ticks(4);
        assertTrue(ctx.animacoes.contains("ichigo_mascara") && ctx.animacoes.contains("ichigo_mugetsu"), ctx.animacoes.toString());
        String arquivo = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/assets/minerwk/player_animations/ichigo.json"));
        assertTrue(arquivo.contains("\"ichigo_mascara\"") && arquivo.contains("\"ichigo_mugetsu\""));
        // A do Mugetsu cobre a carga inteira e o corte: 6 s e mais o gesto final.
        var anim = com.google.gson.JsonParser.parseString(arquivo).getAsJsonObject().getAsJsonObject("animations");
        assertTrue(anim.getAsJsonObject("ichigo_mugetsu").get("animation_length").getAsDouble() >= Ichigo.Mugetsu.CARGA / 20.0 + .5);
    }

    // --- A forma Hollow completa ---

    private void virarHollow() {
        bankaiDesperto();
        Ichigo.virarHollow(ctx);
    }

    @Test
    void hollowSoTomaOCorpoNoBankaiDespertoComAPassivaProntaEForaDaArena() {
        assertTrue(Ichigo.podeVirarHollow(Reiatsu.BANKAI, true, true, false));
        assertFalse(Ichigo.podeVirarHollow(Forma.BASE, true, true, false), "no Shikai o golpe mata");
        assertFalse(Ichigo.podeVirarHollow(Reiatsu.BANKAI, false, true, false), "antes de vencer o Zangetsu");
        assertFalse(Ichigo.podeVirarHollow(Reiatsu.BANKAI, true, false, false), "em recarga");
        assertFalse(Ichigo.podeVirarHollow(Reiatsu.BANKAI, true, true, true), "na arena do boss");
        assertFalse(Ichigo.podeVirarHollow(Reiatsu.HOLLOW, true, true, false), "já está na forma");
    }

    @Test
    void golpeFatalNoBankaiViraHollowComOnzeDeVidaERecargaDeCincoMinutos() {
        virarHollow();
        assertEquals(Reiatsu.HOLLOW, kit.forma());
        assertEquals(List.of(1f), ctx.vidas, "fica com 1 de vida");
        assertEquals(List.of(10f), ctx.curas, "e cura 10");
        assertEquals(6000, kit.recargas().restante(Ichigo.PASSIVA_HOLLOW, agora));
        ticks(1);
        assertTrue(ctx.visuais.contains("ichigo_hollow"));
        assertTrue(kit.efeitos().stream().anyMatch(e -> e instanceof Ichigo.FormaHollow));
    }

    @Test
    void naFormaHollowAsTeclasEOAtaqueBasicoNaoRespondemEOReiatsuNaoMuda() {
        virarHollow();
        double reiatsu = kit.recurso().atual();
        ticks(60);
        for (Tecla tecla : Tecla.values()) assertFalse(kit.apertar(tecla, agora), "tecla " + tecla);
        assertEquals(0f, ctx.arma.dano(), "o clique não fere");
        assertEquals(reiatsu, kit.recurso().atual(), 1e-9);
        assertEquals(Reiatsu.HOLLOW, kit.forma(), "sem reiatsu drenando, a forma não cai antes da hora");
    }

    @Test
    void hollowDaGarraEmQuemEstaAteQuatroBlocos() {
        ctx.proximo = "alvo";
        ctx.posAlvo = new Vetor(3, 0, 0);
        virarHollow();
        ticks(Ichigo.FormaHollow.PRIMEIRO_ATO + 1);
        assertEquals(List.of(8f), ctx.feridos);
        assertTrue(ctx.visuais.contains("ichigo_hollow_garra"));
        assertEquals(new Vetor(3, 0, 0), ctx.encarado, "vira para o alvo");
        ticks(Ichigo.FormaHollow.INTERVALO);
        assertEquals(List.of(8f, 8f), ctx.feridos, "uma a cada 30 ticks");
    }

    @Test
    void hollowUsaOSonidoParaChegarEmQuemEstaLonge() {
        ctx.proximo = "alvo";
        ctx.posAlvo = new Vetor(15, 0, 0);
        virarHollow();
        ticks(Ichigo.FormaHollow.PRIMEIRO_ATO + 1);
        assertTrue(ctx.feridos.isEmpty(), "longe demais para a garra");
        assertEquals(15 - Ichigo.FormaHollow.JUNTO_DO_ALVO, ctx.distanciaDoPasso, 1e-9);
        assertEquals(1, ctx.direcaoDoPasso.x(), 1e-9);
        assertTrue(ctx.visuais.contains("ichigo_hollow_sonido"));
    }

    @Test
    void hollowSemInimigoPorPertoSoEspera() {
        virarHollow();
        ticks(100);
        assertTrue(ctx.feridos.isEmpty());
        assertFalse(ctx.chamadas.contains("deslocar"));
    }

    @Test
    void aosSeisSegundosOHollowDisparaOCeroDeDezoitoDeDano() {
        ctx.proximo = "alvo";
        ctx.posAlvo = new Vetor(12, .55, 0);
        ctx.inimigoEm = new Vetor(12, .55, 0);
        virarHollow();
        ticks(Ichigo.FormaHollow.CERO_EM + 1);
        assertTrue(ctx.visuais.contains("ichigo_hollow_cero_carga"));
        assertFalse(ctx.danos.contains(18f), "a carga leva 20 ticks");
        ticks(Ichigo.FormaHollow.CERO_CARGA);
        assertTrue(ctx.visuais.contains("ichigo_hollow_cero"));
        assertEquals(List.of(18f), ctx.danos);
        double maisLonge = ctx.pontosDeDano.stream().mapToDouble(Vetor::x).max().orElse(0);
        assertEquals(24, maisLonge, 1, "o feixe tem 24 blocos");
    }

    @Test
    void oCeroParaNaParede() {
        ctx.proximo = "alvo";
        ctx.posAlvo = new Vetor(12, .55, 0);
        ctx.inimigoEm = new Vetor(12, .55, 0);
        ctx.paredeEm = v -> v.x() > 8;
        virarHollow();
        ticks(Ichigo.FormaHollow.CERO_EM + Ichigo.FormaHollow.CERO_CARGA + 1);
        assertTrue(ctx.danos.isEmpty(), "o alvo está atrás da parede");
    }

    @Test
    void aoAcabarOHollowVoltaAoShikaiComSeisDeVidaEOBankaiEmRecarga() {
        virarHollow();
        ticks(Ichigo.FormaHollow.DURACAO + 1);
        assertEquals(Forma.BASE, kit.forma());
        assertEquals(6f, ctx.vidas.get(ctx.vidas.size() - 1));
        assertTrue(ctx.fins.contains("ichigo_hollow"));
        long falta = kit.recargas().restante(Ichigo.ENTRAR_NO_BANKAI, agora);
        assertTrue(falta > 390 && falta <= 400, "G em recarga de 20 s: " + falta);
        assertFalse(kit.efeitos().stream().anyMatch(e -> e instanceof Ichigo.FormaHollow));
    }

    @Test
    void formaHollowSemOEfeitoAcabaComOMesmoCusto() {
        // Quem saiu do jogo ou trocou de dimensão no meio da forma volta sem o efeito, que não é salvo.
        kit.mudarForma(Reiatsu.HOLLOW, ctx);
        ticks(1);
        assertEquals(Forma.BASE, kit.forma());
        assertEquals(List.of(6f), ctx.vidas, "6 de vida, como no fim normal");
        assertFalse(kit.recargas().pronta(Ichigo.ENTRAR_NO_BANKAI, agora), "e o G em recarga");
        assertTrue(ctx.fins.contains("ichigo_hollow"), "o visual da forma é encerrado");
    }

    @Test
    void aFormaDuraDuzentosTicksComSeisGarrasEUmCeroSo() {
        ctx.proximo = "alvo";
        ctx.posAlvo = new Vetor(3, 0, 0);
        virarHollow();
        ticks(Ichigo.FormaHollow.DURACAO - 1);
        assertEquals(Reiatsu.HOLLOW, kit.forma(), "no tick 199 ainda é o Hollow");
        // Garras aos 15, 45, 75, 105, 165 e 195: a de 135 cairia em cima do Cero.
        assertEquals(6, ctx.feridos.size());
        assertEquals(1, ctx.visuais.stream().filter("ichigo_hollow_cero"::equals).count());
        assertEquals(1, ctx.visuais.stream().filter("ichigo_hollow_cero_carga"::equals).count());
        ticks(1);
        assertEquals(Forma.BASE, kit.forma(), "e acaba no 200");
    }

    @Test
    void oCeroPegaQuemEstaColado() {
        ctx.proximo = "alvo";
        ctx.posAlvo = new Vetor(.5, .55, 0);
        ctx.inimigoEm = new Vetor(.5, .55, 0);
        virarHollow();
        ticks(Ichigo.FormaHollow.CERO_EM + Ichigo.FormaHollow.CERO_CARGA + 1);
        assertEquals(List.of(18f), ctx.danos);
    }

    // --- Balanceamento (12/11): o Ichigo gasta menos reiatsu ---

    @Test
    void custosDoReiatsuDepoisDoTeste() {
        java.util.Map<String, Double> custos = new java.util.HashMap<>();
        for (Habilidade h : ichigo.todasAsHabilidades()) custos.put(h.id(), h.numeros().custo());
        assertEquals(java.util.Map.ofEntries(
                java.util.Map.entry("ichigo_getsuga", 12.0), java.util.Map.entry("ichigo_shunpo", 10.0),
                java.util.Map.entry("ichigo_getsuga_chao", 20.0), java.util.Map.entry("ichigo_arremesso", 12.0),
                java.util.Map.entry(Ichigo.ENTRAR_NO_BANKAI, 40.0), java.util.Map.entry("ichigo_getsuga_negro", 10.0),
                java.util.Map.entry("ichigo_shunpo_bankai", 6.0), java.util.Map.entry("ichigo_getsuga_lamina", 20.0),
                java.util.Map.entry("ichigo_sair_bankai", 0.0), java.util.Map.entry(Ichigo.MASCARA, 40.0),
                java.util.Map.entry(Ichigo.MUGETSU, 100.0)), custos);
        assertEquals(1.0 / 20, Reiatsu.DRENO, 1e-12, "o Bankai drena 1 por segundo");
        assertEquals(2.5 / 20, Reiatsu.GANHO, 1e-12);
        assertEquals(5.0 / 20, Reiatsu.GANHO_CALMO, 1e-12);
    }
}
