package com.minerwk.core.traje.tecido;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MalhaTecidoTest {
    @Test void fixaOmbrosMantemComprimentosEColisao(){
        var m=new MalhaTecido(5,5,.15,.34375);
        for(int i=0;i<1200;i++){
            m.tick(Math.sin(i*.03)*.3,Math.cos(i*.04)*.2,Math.sin(i*.1)*.5,Math.sin(i*.09)*.15);
            for(int x=0;x<5;x++){
                assertEquals(new MalhaTecido.Ponto((x-2)*.15,0,0),m.ponto(x,0,1));
                for(int y=1;y<5;y++){var p=m.ponto(x,y,1);assertTrue(Double.isFinite(p.x()+p.y()+p.z()));assertTrue(p.z()>=0&&p.y()<=-.025);}
            }
            assertTrue(m.erroAlongamento()<.14,"Alongamento excessivo: "+m.erroAlongamento());
        }
    }
    @Test void frenteAbreLateralDeslocaEReNaoAtravessaTronco(){
        var f=new MalhaTecido(5,5,.15,.34375);var e=new MalhaTecido(5,5,.15,.34375);var d=new MalhaTecido(5,5,.15,.34375);var re=new MalhaTecido(5,5,.15,.34375);
        for(int i=0;i<300;i++){f.tick(.25,0,0,0);e.tick(0,-.2,0,0);d.tick(0,.2,0,0);re.tick(-.25,0,0,0);}
        assertTrue(f.ponto(2,4,1).z()>.25);assertTrue(e.ponto(2,4,1).x()>.01);assertTrue(d.ponto(2,4,1).x()<-.01);assertTrue(Math.abs(e.ponto(2,4,1).x())<.6&&Math.abs(d.ponto(2,4,1).x())<.6);assertEquals(0,re.ponto(2,4,1).z(),.02);
    }
    @Test void interpolacaoNaoSimulaEEntradaInvalidaReinicia(){
        var a=new MalhaTecido(5,5,.15,.34375);var b=new MalhaTecido(5,5,.15,.34375);
        for(int t=0;t<120;t++){a.tick(.2,.1,0,.1);b.tick(.2,.1,0,.1);for(int f=0;f<25;f++)for(int x=0;x<5;x++)for(int s=0;s<4;s++)a.rotacao(x,s,f/25d);}
        for(int x=0;x<5;x++)for(int y=0;y<5;y++)assertEquals(a.ponto(x,y,1),b.ponto(x,y,1));
        a.tick(Double.NaN,0,0,0);assertEquals(new MalhaTecido.Ponto(0,-1.375,0),a.ponto(2,4,1));
        assertThrows(IllegalArgumentException.class,()->new MalhaTecido(1,5,.1,.1));
    }
    @Test void rotacoesRelativasReconstroemCadaDirecaoDaMalha(){
        var m=new MalhaTecido(5,5,.15,.34375);for(int i=0;i<70;i++)m.tick(.12,.2,0,.08);
        for(int x=0;x<5;x++){
            double[][] world={{1,0,0},{0,1,0},{0,0,1}};
            for(int y=0;y<4;y++){
                var r=m.rotacao(x,y,1);double cx=Math.cos(r.x()),sx=Math.sin(r.x()),cy=Math.cos(r.y()),sy=Math.sin(r.y()),cz=Math.cos(r.z()),sz=Math.sin(r.z());
                double[][] local={{cz*cy,cz*sy*sx-sz*cx,cz*sy*cx+sz*sx},{sz*cy,sz*sy*sx+cz*cx,sz*sy*cx-cz*sx},{-sy,cy*sx,cy*cx}},product=new double[3][3];
                for(int i=0;i<3;i++)for(int j=0;j<3;j++)for(int k=0;k<3;k++)product[i][j]+=world[i][k]*local[k][j];world=product;
                var a=m.ponto(x,y,1);var b=m.ponto(x,y+1,1);double[] d={b.x()-a.x(),b.y()-a.y(),b.z()-a.z()};double n=Math.sqrt(d[0]*d[0]+d[1]*d[1]+d[2]*d[2]);for(int i=0;i<3;i++)assertEquals(d[i]/n,-world[i][1],1e-8);
            }
        }
    }
    @Test void quedaVerticalElevaEDobraSemPrecisarDeMovimentoHorizontal(){
        var m=new MalhaTecido(5,5,.15,.34375);
        for(int i=0;i<35;i++)m.tick(0,0,-.65,0);
        var durante=m.ponto(2,4,1);assertTrue(durante.z()>.25);assertTrue(durante.y()>-1.1,"A bainha deve subir no fluxo de ar da queda");
        for(int i=0;i<300;i++)m.tick(0,0,0,0);
        assertEquals(-1.375,m.ponto(2,4,1).y(),.03);assertTrue(m.ponto(2,4,1).z()<.08);
    }
}
