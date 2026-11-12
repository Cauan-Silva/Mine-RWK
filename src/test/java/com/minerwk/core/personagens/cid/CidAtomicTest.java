package com.minerwk.core.personagens.cid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minerwk.core.kit.Contexto;
import com.minerwk.core.kit.Efeito;
import com.minerwk.core.kit.EstadoDoJogador;
import com.minerwk.core.kit.KitDoJogador;
import com.minerwk.core.kit.Tecla;
import com.minerwk.core.kit.Vetor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** O R do Cid, o I Am Atomic final: a linha do tempo e o que o golpe pede ao jogo em cada momento. */
class CidAtomicTest {
    private static final EstadoDoJogador PARADO = new EstadoDoJogador(true, false, false);
    private static final Vetor ESPACO = new Vetor(1000, 128, 1000);

    final class Falso implements Contexto {
        /** O alvo na mira (null = ninguém) e se ele ainda vive. */
        Object naMira = "alvo";
        boolean vivo = true;
        Vetor centroDoAlvo = new Vetor(3, -.7, 0);
        final List<Float> feridas = new ArrayList<>();
        final List<Integer> ticksDasFeridas = new ArrayList<>();
        final List<Vetor> pesDoCid = new ArrayList<>(), centros = new ArrayList<>();
        final List<String> visuais = new ArrayList<>(), cutscenes = new ArrayList<>(), animacoes = new ArrayList<>();
        final List<String> naOrigem = new ArrayList<>();
        final List<Float> tiposDeSoco = new ArrayList<>();
        int tremores, empurroes;
        int viagens, fins, idas, cutscenesEncerradas;
        double devolvido;
        boolean noEspaco;

        public Vetor olhos() { return new Vetor(0, 0, 0); }
        public Vetor pes() { return new Vetor(0, -1.62, 0); }
        public Vetor olhar() { return new Vetor(1, 0, 0); }
        public Object marcarAlvo(double alcance, double largura) {
            assertEquals(6, alcance);
            return naMira;
        }
        public Vetor posicaoDe(Object alvo) { return vivo ? centroDoAlvo : null; }
        public boolean alvoVivo(Object alvo) { return vivo; }
        public void ferir(Object alvo, float dano) { feridas.add(dano); ticksDasFeridas.add((int) agora - 1); }
        public boolean viagem(Object alvo) { viagens++; return true; }
        public void pontosDaViagem(Object alvo, Vetor pes, Vetor centro, Vetor encarar) { pesDoCid.add(pes); centros.add(centro); }
        public Vetor aoEspaco(Object alvo) { idas++; noEspaco = true; return ESPACO; }
        public void fimDaViagem() { fins++; noEspaco = false; }
        public void visualNaOrigem(String tipo, float forca, int duracao, double raio) {
            naOrigem.add(tipo);
            assertEquals(96, raio);
            assertEquals(AtomicFinal.TICKS_DA_LUZ, duracao);
        }
        public void devolverRecurso(double q) { devolvido += q; kit.recurso().carregar(kit.recurso().atual() + q); }
        public void criarEfeito(Efeito e) { kit.adicionarEfeito(e); }
        public boolean efeitoAtivo(Class<? extends Efeito> tipo) { return kit.efeitos().stream().anyMatch(tipo::isInstance); }
        public double recurso() { return kit.recurso().atual(); }
        public void recarga(String habilidade, int ticks) { kit.recargas().iniciar(habilidade, agora, ticks); }
        public void apresentacao(String animacao, String efeito, String som) { if (!animacao.isEmpty()) animacoes.add(animacao); }
        public void visual(String tipo, Vetor origem, Vetor direcao, float forca, int duracao) {
            visuais.add(tipo);
            if (tipo.equals(Cid.Atomic.SOCO)) tiposDeSoco.add(forca);
            assertTrue(duracao > 0, tipo);
        }
        public void cutscene(String id, Vetor origem, Vetor direcao) { cutscenes.add(id); }
        public void encerrarCutscene() { cutscenesEncerradas++; }
        public void tremor(float forca, int duracao, double raio) { tremores++; }
        public void empurrar(Vetor centro, double raio, double forca) {
            empurroes++;
            assertEquals(pes(), centro, "a onda sai do ponto de partida");
        }
    }

    private final KitDoJogador kit = new KitDoJogador(Cid.criar());
    private final Falso ctx = new Falso();
    private long agora;

    private void ticks(int n) {
        for (int i = 0; i < n; i++) kit.tick(agora++, PARADO, ctx);
    }

    /** Aperta o R e anda até o primeiro soco; devolve o tick dele. */
    private long comecar() {
        kit.definirDesperto(true);
        assertTrue(kit.apertar(Tecla.R, agora), "desperto e com 100 de Magia, o R começa");
        while (ctx.feridas.isEmpty()) {
            ticks(1);
            assertTrue(agora < 40, "o primeiro soco não saiu");
        }
        return agora - 1;
    }

    @Test
    void aLinhaDoTempoTemQuatroSocosEmCadaLado() {
        List<Integer> socos = new ArrayList<>();
        for (int t = 0; t < AtomicFinal.FIM; t++) if (AtomicFinal.soco(t)) socos.add(t);
        assertEquals(List.of(0, 12, 24, 36, 64, 76, 88, 100), socos, "a passagem (48) não fere");
        assertEquals(48, AtomicFinal.PASSAGEM);
        assertEquals(112, AtomicFinal.FALA);
        assertEquals(168, AtomicFinal.CARGA, "duas palavras e os silêncios");
        assertEquals(222, AtomicFinal.ATOMIC, "a carga e a terceira palavra");
        assertEquals(292, AtomicFinal.VOLTA);
        assertTrue(AtomicFinal.FIM >= 300, "o golpe dura pelo menos 15 s");
        assertTrue(AtomicFinal.FIM >= AtomicFinal.ATOMIC + AtomicFinal.TICKS_DA_LUZ, "o golpe cobre a luz no céu");
    }

    @Test
    void cadaSocoSobeOAlvoDozeBlocosEOCidSurgeAoLado() {
        assertEquals(0, AtomicFinal.alturaDoAlvo(0));
        assertEquals(0, AtomicFinal.alturaDoAlvo(AtomicFinal.PARADA), 1e-9, "a parada de impacto: 3 ticks sem sair do lugar");
        assertTrue(AtomicFinal.alturaDoAlvo(AtomicFinal.PARADA + 2) > 5, "e sai de uma vez");
        assertEquals(12, AtomicFinal.alturaDoAlvo(10), 1e-9, "7 ticks para subir");
        assertEquals(12, AtomicFinal.alturaDoAlvo(12), 1e-9);
        assertEquals(12, AtomicFinal.alturaDoAlvo(15), 1e-9, "parado de novo no segundo soco");
        assertEquals(48, AtomicFinal.alturaDoAlvo(47), 1e-9);
        assertEquals(48, AtomicFinal.alturaDoAlvo(400), 1e-9);
        for (int t = 1; t < 48; t++) assertTrue(AtomicFinal.alturaDoAlvo(t) >= AtomicFinal.alturaDoAlvo(t - 1));
        assertEquals(0, AtomicFinal.alturaDoCid(0), "o primeiro soco é no chão");
        assertEquals(0, AtomicFinal.alturaDoCid(8));
        assertEquals(12, AtomicFinal.alturaDoCid(9), "surge 3 ticks antes do segundo soco");
        assertEquals(36, AtomicFinal.alturaDoCid(36));
        assertEquals(48, AtomicFinal.alturaDoCid(45));
        Vetor pes = new Vetor(0, 64, 0), alvo = new Vetor(3, 64.9, 0), rumo = new Vetor(1, 0, 0);
        assertEquals(pes, AtomicFinal.cidNaSubida(pes, alvo, rumo, 0));
        Vetor aoLado = AtomicFinal.cidNaSubida(pes, alvo, rumo, 12);
        assertEquals(76, aoLado.y(), 1e-9, "os pés na altura dos pés do alvo");
        assertEquals(3 - AtomicFinal.AO_LADO, aoLado.x(), 1e-9, "do lado de onde ele veio");
    }

    @Test
    void noEspacoCadaSocoEmpurraOAlvoEOCidVaiAtras() {
        assertEquals(0, AtomicFinal.empurraoDoAlvo(64));
        assertEquals(0, AtomicFinal.empurraoDoAlvo(67), 1e-9, "a parada de impacto");
        assertEquals(2.5, AtomicFinal.empurraoDoAlvo(74), 1e-9);
        assertEquals(7.5, AtomicFinal.empurraoDoAlvo(100), 1e-9, "três empurrões antes do último soco");
        assertEquals(16.5, AtomicFinal.empurraoDoAlvo(300), 1e-9, "o último arremessa 9 blocos");
        assertEquals(0, AtomicFinal.avancoDoCid(64));
        assertEquals(2.5, AtomicFinal.avancoDoCid(73));
        assertEquals(7.5, AtomicFinal.avancoDoCid(100));
        assertEquals(7.5, AtomicFinal.avancoDoCid(300), "depois do último ele fica");
        Vetor rumo = new Vetor(0, 0, 1);
        double antes = AtomicFinal.alvoNoEspaco(ESPACO, rumo, 100).z() - AtomicFinal.cidNoEspaco(ESPACO, rumo, 100).z();
        assertEquals(AtomicFinal.AO_LADO, antes, 1e-9, "em cada soco os dois estão frente a frente");
        double entre = AtomicFinal.alvoNoEspaco(ESPACO, rumo, 200).z() - AtomicFinal.cidNoEspaco(ESPACO, rumo, 200).z();
        assertEquals(AtomicFinal.AO_LADO + 9, entre, 1e-9, "nas palavras o alvo está longe");
    }

    @Test
    void asTresPalavrasOClaraoEALuz() {
        assertEquals(-1, AtomicFinal.palavra(111));
        assertEquals(0, AtomicFinal.palavra(112));
        assertEquals(-1, AtomicFinal.palavra(136), "um silêncio entre as palavras");
        assertEquals(1, AtomicFinal.palavra(140));
        assertEquals(-1, AtomicFinal.palavra(180), "a carga é muda");
        assertEquals(2, AtomicFinal.palavra(198));
        assertEquals(2, AtomicFinal.palavra(221));
        assertEquals(-1, AtomicFinal.palavra(222));
        assertEquals(0, AtomicFinal.aura(112));
        assertEquals(1, AtomicFinal.aura(168));
        assertEquals(0, AtomicFinal.carga(168));
        assertEquals(1, AtomicFinal.carga(198), "a magia toda na espada antes da terceira palavra");
        assertTrue(AtomicFinal.raioDoClarao(20) < .2, "a esfera nasce pequena e cresce devagar");
        assertEquals(1, AtomicFinal.onda(0));
        assertEquals(0, AtomicFinal.onda(AtomicFinal.TICKS_DA_ONDA));
        assertEquals(1, AtomicFinal.raioDaOnda(AtomicFinal.TICKS_DA_ONDA));
        assertEquals(0, AtomicFinal.raioDoClarao(0));
        assertEquals(1, AtomicFinal.raioDoClarao(AtomicFinal.TICKS_DO_CLARAO));
        assertEquals(0, AtomicFinal.branco(0), "primeiro o clarão roxo, depois o branco");
        assertEquals(1, AtomicFinal.clarao(10), "o clarão roxo cheio");
        assertEquals(0, AtomicFinal.branco(20), "enquanto o roxo cresce a tela não clareia");
        assertEquals(0, AtomicFinal.clarao(AtomicFinal.SOME_O_CLARAO), "o roxo some");
        assertEquals(1, AtomicFinal.branco(AtomicFinal.SOME_O_CLARAO), "e só então a tela está branca");
        assertEquals(1, AtomicFinal.branco(AtomicFinal.TICKS_DO_CLARAO), "a volta acontece por baixo do branco");
        assertEquals(0, AtomicFinal.branco(AtomicFinal.FIM_DO_BRANCO));
        assertEquals(1, AtomicFinal.lampejo(0), "a passagem também é coberta");
        assertEquals(0, AtomicFinal.lampejo(AtomicFinal.TICKS_DO_LAMPEJO));
        assertEquals(1, AtomicFinal.luz(20));
        assertEquals(0, AtomicFinal.luz(AtomicFinal.TICKS_DA_LUZ));
    }

    @Test
    void semAlvoNadaAconteceENadaEGasto() {
        kit.definirDesperto(true);
        ctx.naMira = null;
        assertTrue(kit.apertar(Tecla.R, agora));
        ticks(20);
        assertEquals(100, kit.recurso().atual(), 1e-6, "a Magia volta inteira");
        assertEquals(100, ctx.devolvido);
        assertTrue(kit.recargas().pronta(Cid.ATOMIC, agora), "sem recarga");
        assertTrue(kit.recargas().pronta(Cid.RECOVERY, agora));
        assertEquals(0, ctx.viagens);
        assertTrue(kit.efeitos().isEmpty());
        assertTrue(ctx.feridas.isEmpty() && ctx.visuais.isEmpty() && ctx.cutscenes.isEmpty());
    }

    @Test
    void precisaDeCemDeMagiaEDoDespertar() {
        assertFalse(kit.apertar(Tecla.R, agora), "trancado até o despertar");
        kit.definirDesperto(true);
        kit.recurso().carregar(99);
        assertFalse(kit.apertar(Tecla.R, agora));
        kit.recurso().carregar(100);
        assertTrue(kit.apertar(Tecla.R, agora));
    }

    @Test
    void aSequenciaInteira() {
        long t0 = comecar();
        assertEquals(1, ctx.viagens);
        assertTrue(kit.recurso().atual() < 5, "custa os 100 de Magia");
        assertTrue(ctx.cutscenes.contains(CutsceneAtomic.SUBIDA));
        ticks(AtomicFinal.PASSAGEM - 1);
        assertEquals(4, ctx.feridas.size(), "quatro socos no mundo");
        assertEquals(0, ctx.idas);
        ticks(1);
        assertEquals(1, ctx.idas, "no quinto soco os dois saem do mundo");
        assertTrue(ctx.visuais.contains(Cid.Atomic.RISCO), "quem ficou embaixo vê o risco roxo");
        assertTrue(ctx.cutscenes.contains(CutsceneAtomic.ESPACO));
        ticks(AtomicFinal.FALA - AtomicFinal.PASSAGEM);
        assertEquals(8, ctx.feridas.size(), "mais quatro no espaço");
        for (int i = 0; i < 8; i++) assertEquals(2f, ctx.feridas.get(i));
        assertEquals(List.of(0, 12, 24, 36, 64, 76, 88, 100), ctx.ticksDasFeridas.stream().map(t -> (int) (t - t0)).toList());
        assertEquals(List.of(0f, 0f, 0f, 0f, 3f, 1f, 1f, 1f, 2f), ctx.tiposDeSoco, "o da passagem não fere, e o último é o arremesso");
        assertTrue(ctx.tremores >= 9, "cada soco treme a tela");
        assertTrue(ctx.visuais.contains(Cid.Atomic.AURA));
        ticks(AtomicFinal.TICKS_DA_FALA - 1);
        assertEquals(8, ctx.feridas.size(), "as palavras e a carga, parados");
        assertTrue(ctx.naOrigem.isEmpty());
        ticks(1);
        assertEquals(44f, ctx.feridas.get(8), "o Atomic");
        assertEquals(List.of(Cid.Atomic.LUZ), ctx.naOrigem, "o mundo de onde saíram vê a luz no céu");
        assertTrue(ctx.visuais.contains(Cid.ATOMIC));
        assertTrue(ctx.cutscenes.contains(CutsceneAtomic.CLARAO));
        assertEquals(0, ctx.fins);
        ticks(AtomicFinal.TICKS_DO_CLARAO);
        assertEquals(1, ctx.fins, "voltam quando o clarão some");
        assertFalse(ctx.visuais.contains(Cid.Atomic.ONDA), "a onda espera o branco abrir");
        ticks(AtomicFinal.ATRASO_DA_ONDA);
        assertTrue(ctx.visuais.contains(Cid.Atomic.ONDA), "a onda de choque no chão");
        assertEquals(1, ctx.empurroes, "que empurra quem está perto");
        assertTrue(ctx.cutscenes.contains(CutsceneAtomic.CEU));
        ticks(AtomicFinal.DEPOIS_DA_VOLTA + 3);
        assertTrue(kit.efeitos().isEmpty());
        assertEquals(1, ctx.fins, "e só uma vez");
        assertEquals(9, ctx.feridas.size());
        assertFalse(kit.recargas().pronta(Cid.ATOMIC, t0 + 1790));
        assertTrue(kit.recargas().pronta(Cid.ATOMIC, t0 + 1802), "recarga de 90 s");
        assertFalse(kit.recargas().pronta(Cid.RECOVERY, t0 + 1790), "que o Recovery Atomic divide");
    }

    @Test
    void osPontosDaSubidaEDoEspaco() {
        comecar();
        ticks(AtomicFinal.PASSAGEM - 1);
        Vetor ultimoCentro = ctx.centros.get(ctx.centros.size() - 1), ultimoPe = ctx.pesDoCid.get(ctx.pesDoCid.size() - 1);
        assertEquals(ctx.centroDoAlvo.y() + 48, ultimoCentro.y(), 1e-6, "quatro socos, 48 blocos");
        assertEquals(ultimoCentro.y() - .9, ultimoPe.y(), 1e-6, "o Cid está na altura dele");
        ticks(40);
        Vetor noEspaco = ctx.centros.get(ctx.centros.size() - 1);
        assertTrue(noEspaco.x() > ESPACO.x(), "no espaço os pontos saem de onde o servidor os pôs");
        assertNotNull(ctx.pesDoCid.get(ctx.pesDoCid.size() - 1));
    }

    @Test
    void seOAlvoMorreNaSubidaASequenciaPulaParaOAtomic() {
        long t0 = comecar();
        ticks(8);
        ctx.vivo = false;
        ticks(2);
        assertEquals(1, ctx.idas, "o Cid sobe sozinho");
        assertEquals(List.of(Cid.Atomic.LUZ), ctx.naOrigem, "e o clarão acontece na hora");
        assertTrue(ctx.cutscenes.contains(CutsceneAtomic.CLARAO));
        int socos = ctx.feridas.size();
        ticks(AtomicFinal.TICKS_DO_CLARAO + 1);
        assertEquals(1, ctx.fins);
        assertTrue(agora - t0 < AtomicFinal.ATOMIC, "não espera os socos que faltavam");
        ticks(AtomicFinal.DEPOIS_DA_VOLTA + 5);
        assertTrue(kit.efeitos().isEmpty());
        assertTrue(ctx.feridas.size() <= socos + 1);
    }

    @Test
    void morrerOuSairNoMeioDevolveOsDois() {
        comecar();
        ticks(AtomicFinal.PASSAGEM + 10);
        assertTrue(ctx.noEspaco);
        kit.descartar(ctx);
        assertEquals(1, ctx.fins);
        assertTrue(ctx.cutscenesEncerradas > 0);
        assertFalse(ctx.noEspaco);
    }

    @Test
    void noOverdriveOsSocosEOAtomicFeremMais() {
        kit.definirDesperto(true);
        assertTrue(kit.apertar(Tecla.G, agora));
        ticks(10);
        kit.recurso().carregar(100);
        assertTrue(kit.apertar(Tecla.R, agora));
        ticks(AtomicFinal.ATOMIC + 10);
        assertEquals(2.6f, ctx.feridas.get(0), 1e-5);
        assertEquals(57.2f, ctx.feridas.get(ctx.feridas.size() - 1), 1e-4, "o bônus é o do disparo, mesmo com o Overdrive já acabado");
    }

    @Test
    void asCutscenesCobremCadaTrecho() {
        assertTrue(CutsceneAtomic.daSubida().duracao() >= AtomicFinal.PASSAGEM);
        assertTrue(CutsceneAtomic.doEspaco().duracao() >= AtomicFinal.ATOMIC - AtomicFinal.PASSAGEM);
        assertTrue(CutsceneAtomic.doClarao().duracao() >= AtomicFinal.TICKS_DO_CLARAO);
        assertEquals(AtomicFinal.DEPOIS_DA_VOLTA, CutsceneAtomic.doCeu().duracao());
        assertEquals(3, CutsceneAtomic.PALAVRAS.size());
        assertEquals(AtomicFinal.FALA - AtomicFinal.PASSAGEM, CutsceneAtomic.PALAVRAS.get(0).inicio(), "as palavras contam da chegada");
        assertTrue(CutsceneAtomic.PALAVRAS.get(1).fim() <= AtomicFinal.CARGA - AtomicFinal.PASSAGEM, "a carga é muda");
        assertEquals(AtomicFinal.ATOMIC - AtomicFinal.PASSAGEM, CutsceneAtomic.PALAVRAS.get(2).fim(), "a terceira vai até o estouro");
    }
}
