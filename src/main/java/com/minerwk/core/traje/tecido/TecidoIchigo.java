package com.minerwk.core.traje.tecido;

import java.util.*;

/**
 * O pano dos trajes do Ichigo, vindo do Minezin (mesmo autor). O casaco do Bankai é pano inteiro: as costas
 * são uma malha de partículas de COLUNAS_COSTAS x SEGMENTOS presa na cintura ({@link MalhaTecido}); os
 * lados, as quinas e as tiras abertas da frente seguem as colunas de fora dela, então a saia do casaco é
 * um pano só. As mangas largas (do Bankai, em segmentos, e do Shikai, uma peça) andam em molas angulares
 * amortecidas ({@link MolaManto}) e seguem o balanço dos braços. {@code impulso()} joga o casaco para fora
 * (a revelação do Bankai) e ele assenta sozinho.
 * Ossos: bk_capa_{costas|frente_esq|frente_dir|lado_esq|lado_dir|quina_esq|quina_dir}_{coluna}_{segmento},
 * bk_manga_{esq|dir}_{segmento} e manto_manga_{esq|dir}; os mg_* são da forma do Mugetsu.
 */
public final class TecidoIchigo {
    public static final int SEGMENTOS=6,COLUNAS_COSTAS=6,COLUNAS_FRENTE=1,SEGMENTOS_MANGA=3,COLUNAS_FAIXA=2;
    public static final List<String> OSSOS;
    static{
        var nomes=new ArrayList<String>();
        for(var painel:new String[]{"costas","frente_esq","frente_dir","lado_esq","lado_dir","quina_esq","quina_dir","quinaf_esq","quinaf_dir"})
            for(int c=0;c<(painel.equals("costas")?COLUNAS_COSTAS:COLUNAS_FRENTE);c++)for(int s=0;s<SEGMENTOS;s++)nomes.add("bk_capa_"+painel+"_"+c+"_"+s);
        for(var lado:new String[]{"esq","dir"})for(int s=0;s<SEGMENTOS_MANGA;s++)nomes.add("bk_manga_"+lado+"_"+s);
        // Mugetsu form: loose ends of the face bandage (mg_faixa_{coluna}_{segmento}) and the closed skirt front.
        for(int c=0;c<COLUNAS_FAIXA;c++)for(int s=0;s<SEGMENTOS;s++)nomes.add("mg_faixa_"+c+"_"+s);
        for(int s=0;s<SEGMENTOS;s++)nomes.add("mg_saia_frente_meio_"+s);
        OSSOS=List.copyOf(nomes);
    }
    private static final MolaManto.Pose ZERO=new MolaManto.Pose(0,0,0);
    private final MalhaTecido costas;
    private final MolaManto mangaEsq,mangaDir,faixaCabeca;
    public TecidoIchigo(){this(PerfilTecido.PADRAO);}
    /** perfil: weight, stiffness, drag, inertia and spring response of the outfit (PerfilTecido). */
    public TecidoIchigo(PerfilTecido perfil){
        costas=new MalhaTecido(COLUNAS_COSTAS,SEGMENTOS+1,1.46/16,1.85/16,perfil);
        mangaEsq=new MolaManto(1.5*perfil.mola());mangaDir=new MolaManto(1.45*perfil.mola());faixaCabeca=new MolaManto(1.7*perfil.mola());
    }
    private double tempo,impulso,agitacao,anteriorAgitacao,subida;
    public void reset(){costas.reset();for(var m:molas())m.reset();tempo=impulso=agitacao=anteriorAgitacao=subida=0;}
    private MolaManto[] molas(){return new MolaManto[]{mangaEsq,mangaDir,faixaCabeca};}
    /** Throws the coat outward for a few ticks (the Bankai reveal cut); decays by itself. */
    public void impulso(double forca){impulso=Math.max(impulso,forca);}
    public void tick(MolaManto.Entrada e){tick(e,0,0);}
    /** braco*: change of each arm's swing angle this tick (radians), so the sleeves trail the arms. */
    public void tick(MolaManto.Entrada e,double bracoEsq,double bracoDir){
        if(!Double.isFinite(e.frente()+e.lado()+e.vertical()+e.giro()+bracoEsq+bracoDir)){reset();return;}
        tempo++;var v=vento(tempo*1.2);double sopro=impulso*Math.sin(tempo*1.7);
        // Agitation follows speed (walking/running), turns, the reveal burst and falling; it drives the travelling ripple.
        double vel=Math.hypot(e.frente(),e.lado());anteriorAgitacao=agitacao;
        agitacao+=(Math.min(1.4,.12+vel*3.2+Math.abs(e.giro())*1.5+impulso+Math.max(0,-e.vertical())*2.5)-agitacao)*.25;
        subida+=(Math.max(-.2,Math.min(.6,-e.vertical()*2.2))-subida)*.3;
        double frente=e.frente()*1.6+v[0]*2.2-impulso*.6,lado=e.lado()*1.5+v[1]*2.2+sopro*.35,vertical=e.vertical()*1.4-impulso*.3;impulso*=.84;
        costas.tick(frente,lado,vertical,e.giro()+sopro*.2);
        // Bandage ends are light: they stream back hard and lift when falling.
        faixaCabeca.tick(new MolaManto.Entrada(frente*1.3-subida*.4,lado*1.2,vertical*1.3,e.giro()*1.2,e.noChao()));
        mangaEsq.tick(new MolaManto.Entrada(frente*.7+bracoEsq*.22,lado*.6,vertical,e.giro()*.6,e.noChao()));
        mangaDir.tick(new MolaManto.Entrada(frente*.7+bracoDir*.22,lado*.6,vertical,e.giro()*.6,e.noChao()));
    }
    /** Spring stage at a continuous position u in 0..3 (MolaManto has 4 stages). */
    private static MolaManto.Pose estagio(MolaManto m,double u,double parcial){
        int i=(int)Math.floor(Math.min(2.999,Math.max(0,u)));double f=u-i;var a=m.amostrar(i,parcial);var b=m.amostrar(i+1,parcial);
        return new MolaManto.Pose(a.x()+(b.x()-a.x())*f,a.y()+(b.y()-a.y())*f,a.z()+(b.z()-a.z())*f);
    }
    /** Parent-relative rotation delta (radians) for one coat bone; sampling never advances the physics. */
    public MolaManto.Pose pose(String osso,double parcial){
        double ag=anteriorAgitacao+(agitacao-anteriorAgitacao)*parcial,tt=tempo+parcial;
        if(osso.startsWith("manto_manga_")){
            // Shikai: a manga larga do shihakushō é uma peça só; segue a ponta da mola do lado dela.
            var m=osso.endsWith("_esq")?mangaEsq:mangaDir;int lado=m==mangaEsq?1:-1;var a=estagio(m,3,parcial);
            double on=onda(1,0,tt,ag,2,lado>0?.3:1.9);
            return new MolaManto.Pose(clamp(-a.x()*.9+on,.5),clamp(a.y()*.35,.2),clamp((a.z()*.6+Math.abs(a.x())*.2)*.9*lado+on*.4*lado,.3));
        }
        if(osso.startsWith("bk_manga_")){
            int s=Integer.parseInt(osso.substring(osso.lastIndexOf('_')+1));var m=osso.startsWith("bk_manga_esq_")?mangaEsq:mangaDir;int lado=m==mangaEsq?1:-1;
            var a=estagio(m,3.0*(s+1)/SEGMENTOS_MANGA,parcial);var b=estagio(m,3.0*s/SEGMENTOS_MANGA,parcial);double k=s==0?.8:1.4;
            double on=onda(s,0,tt,ag,SEGMENTOS_MANGA,lado>0?.3:1.9);
            return new MolaManto.Pose(clamp(-(a.x()-b.x())*k+on,.6),clamp((a.y()-b.y())*k*.4,.2),clamp(((a.z()-b.z())*.6+Math.abs(a.x()-b.x())*.2)*k*lado+on*.4*lado,.35));
        }
        if(osso.startsWith("mg_faixa_")){
            String[] q=osso.substring(9).split("_");int c=Integer.parseInt(q[0]),s=Integer.parseInt(q[1]);
            var a=estagio(faixaCabeca,3.0*(s+1)/SEGMENTOS,parcial);var b=estagio(faixaCabeca,3.0*s/SEGMENTOS,parcial);double k=s==0?.9:1.5+.1*s;
            double on=onda(s,c+11,tt*1.4,ag*1.6+.25,SEGMENTOS,c*1.3),abre=c==0?-1:1;
            return new MolaManto.Pose(clamp(-(s==0?a.x():a.x()-b.x())*k+on-subida*(s==0?.6:.15),.75),clamp((a.y()-b.y())*k*.5,.25),clamp(((a.z()-b.z())*.8)*k+abre*(.04+ag*.05)+on*.4*abre,.4));
        }
        if(osso.startsWith("mg_saia_frente_meio_")){
            // One piece with the sides (user): the closed front follows the mean of the back cloth's two outer columns,
            // the same ones the front strips and sides follow, so the whole skirt moves together.
            int s=Integer.parseInt(osso.substring(osso.lastIndexOf('_')+1));var r0=costas.rotacao(0,s,parcial);var r1=costas.rotacao(COLUNAS_COSTAS-1,s,parcial);
            double on=onda(s,3,tt,ag,SEGMENTOS,1.4);
            return new MolaManto.Pose((r0.x()+r1.x())/2+on-subida*(s==0?.5:.12),-(r0.y()+r1.y())*.25,-(r0.z()+r1.z())*.3);
        }
        if(!osso.startsWith("bk_capa_"))return ZERO;
        String[] p=osso.substring(8).split("_");int s=Integer.parseInt(p[p.length-1]),c=Integer.parseInt(p[p.length-2]);
        if(osso.startsWith("bk_capa_costas_")){var r=costas.rotacao(c,s,parcial);double on=onda(s,c,tt,ag,SEGMENTOS,0);
            return new MolaManto.Pose(r.x()+on-subida*(s==0?.5:.12),-r.y()+on*.15*(c-2.5)/2.5,-r.z()+onda(s,c,tt*1.3,ag,SEGMENTOS,2.1)*.35);}
        int lado=osso.contains("_esq_")?1:-1;
        // Side panels are one piece with the back (user): each follows the outer column of the back cloth, segment by
        // segment (left side = last column, x>0), so the corner moves as a single fabric.
        // Front strips, front corners, sides and back corners all follow the back cloth (user: everything joined).
        // Front strips and front corners take exactly the side's pose (user: the front must follow the side; every
        // limit or outward sweep of its own broke the sheet into flaps). The legs are kept out by the surface
        // collision (SuperficieTecido), which slides the cloth around their outer face.
        if(osso.startsWith("bk_capa_frente_")||osso.startsWith("bk_capa_quinaf_")||osso.startsWith("bk_capa_lado_")||osso.startsWith("bk_capa_quina"))return lateral(lado,s,parcial,tt,ag);
        return ZERO;
    }
    /** Side panel pose: follows the outer back column segment by segment (left side = last column). */
    private MolaManto.Pose lateral(int lado,int s,double parcial,double tt,double ag){
        int col=lado>0?COLUNAS_COSTAS-1:0;var r=costas.rotacao(col,s,parcial);double on=onda(s,col,tt,ag,SEGMENTOS,0);
        return new MolaManto.Pose(r.x()+on-subida*(s==0?.5:.12),-r.y()*.5,-r.z()*.6+lado*(Math.abs(r.x())*.2+subida*.15+on*.3));
    }
    /** Travelling ripple: runs from the waist to the hem (phase grows with the segment), each column offset, stronger
     *  on lower segments and with agitation; a slower swell plus a fast flutter so it never looks like one sine. */
    static double onda(int s,int c,double t,double agitacao,int segmentos,double fase){
        double u=(s+1.0)/segmentos,amp=agitacao*(.03+.09*u*u);
        return amp*(Math.sin(t*.55-s*.9+c*.7+fase)+.45*Math.sin(t*1.7-s*1.6+c*1.3+fase*2));
    }
    /** Brisa leve que nunca para: duas ondas lentas somadas, para a frente e para o lado. */
    static double[] vento(double t){
        return new double[]{.035*Math.sin(t*.09)+.02*Math.sin(t*.23+1.3)+.012*Math.sin(t*.61+.4),.025*Math.sin(t*.07+.5)+.015*Math.sin(t*.19+2.1)};
    }
    public double erroAlongamento(){return costas.erroAlongamento();}
    private static double clamp(double v,double max){return Math.max(-max,Math.min(max,v));}
}
