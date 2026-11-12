package com.minerwk.core.traje.tecido;

import java.util.ArrayList;
import java.util.List;

/** Position based cloth. Metres/seconds; fixed 20Hz input with three solver substeps.
 * Top row is pinned. The local z>=0 back plane is an approximate torso collider.
 * This class has no Minecraft or renderer dependencies and can serve other garments.
 */
public final class MalhaTecido {
    public record Ponto(double x,double y,double z){}
    public record Rotacao(double x,double y,double z){}
    private record Ligacao(int a,int b,double comprimento,double rigidez){}
    private final int colunas,linhas;
    private final double passoX,passoY;
    private final double[][] atual,verlet,anterior;
    private final List<Ligacao> ligacoes=new ArrayList<>();
    private double frenteAnterior,ladoAnterior,verticalAnterior,tempo;
    private final PerfilTecido perfil;
    public MalhaTecido(int colunas,int linhas,double passoX,double passoY){this(colunas,linhas,passoX,passoY,PerfilTecido.PADRAO);}
    /** perfil: weight, stiffness, drag and inertia of this garment (assets/arcanecontrol/tecido/<estilo>.json). */
    public MalhaTecido(int colunas,int linhas,double passoX,double passoY,PerfilTecido perfil){
        this.perfil=perfil==null?PerfilTecido.PADRAO:perfil;
        if(colunas<2||colunas>16||linhas<2||linhas>24||!Double.isFinite(passoX+passoY)||passoX<=0||passoY<=0)throw new IllegalArgumentException("Dimensoes de tecido invalidas");
        this.colunas=colunas;this.linhas=linhas;this.passoX=passoX;this.passoY=passoY;
        atual=new double[colunas*linhas][3];verlet=new double[colunas*linhas][3];anterior=new double[colunas*linhas][3];
        for(int y=0;y<linhas;y++)for(int x=0;x<colunas;x++){
            if(x+1<colunas)ligar(x,y,x+1,y,1);
            if(y+1<linhas)ligar(x,y,x,y+1,1);
            if(x+1<colunas&&y+1<linhas){ligar(x,y,x+1,y+1,.18);ligar(x+1,y,x,y+1,.18);}
            if(y+2<linhas)ligar(x,y,x,y+2,.12);
        }
        reset();
    }
    private static void girar(double[] p,double c,double s){double x=p[0],z=p[2];p[0]=c*x+s*z;p[2]=-s*x+c*z;}
    private int id(int x,int y){return y*colunas+x;}
    private void ligar(int x,int y,int xx,int yy,double k){ligacoes.add(new Ligacao(id(x,y),id(xx,yy),Math.hypot((xx-x)*passoX,(yy-y)*passoY),k));}
    public void reset(){
        frenteAnterior=ladoAnterior=verticalAnterior=tempo=0;
        for(int y=0;y<linhas;y++)for(int x=0;x<colunas;x++){
            int i=id(x,y);atual[i][0]=(x-(colunas-1)*.5)*passoX;atual[i][1]=-y*passoY;atual[i][2]=0;
            System.arraycopy(atual[i],0,verlet[i],0,3);System.arraycopy(atual[i],0,anterior[i],0,3);
        }
    }
    public void tick(double frente,double lado,double vertical,double giro){
        if(!Double.isFinite(frente+lado+vertical+giro)){reset();return;}
        frente=clamp(frente,-.6,.6);lado=clamp(lado,-.6,.6);vertical=clamp(vertical,-2.5,1.5);giro=clamp(giro,-.25,.25);
        double ax=(-lado*35-clamp((lado-ladoAnterior)*90,-18,18))*perfil.inercia(),az=(frente*28+clamp((frente-frenteAnterior)*90,-18,18))*perfil.inercia();
        double queda=Math.max(0,-vertical),sustentacao=clamp(-vertical*21-(vertical-verticalAnterior)*38,-16,7.5);
        frenteAnterior=frente;ladoAnterior=lado;verticalAnterior=vertical;
        for(int i=0;i<atual.length;i++)System.arraycopy(atual[i],0,anterior[i],0,3);
        double dt=1d/60,arrasto=Math.exp(-2.8*perfil.arrasto()*dt),c=Math.cos(-giro/3),s=Math.sin(-giro/3);
        for(int sub=0;sub<3;sub++){
            for(int i=colunas;i<atual.length;i++){
                // Transport previous/current positions into the turning body's local frame.
                girar(atual[i],c,s);girar(verlet[i],c,s);
                double[] p=atual[i],v=verlet[i];
                double dx=clamp((p[0]-v[0])*arrasto+ax*dt*dt,-.10,.10);
                double dy=clamp((p[1]-v[1])*arrasto+(-9.81*perfil.peso()+sustentacao)*dt*dt,-.10,.10);
                double pressao=az+queda*12*(.7+.3*Math.sin(tempo*7+(i/colunas)*.9+(i%colunas)*.6))+Math.abs(lado)*12*(.75+.25*Math.sin(tempo*6+(i%colunas)*.8+(i/colunas)*.45));
                double dz=clamp((p[2]-v[2])*arrasto+pressao*dt*dt,-.10,.10);
                System.arraycopy(p,0,v,0,3);p[0]+=dx;p[1]+=dy;p[2]+=dz;
            }
            tempo+=dt;
            for(int it=0;it<8;it++){
                for(var link:ligacoes){
                    double[] a=atual[link.a],b=atual[link.b];double x=b[0]-a[0],y=b[1]-a[1],z=b[2]-a[2];double d=Math.sqrt(x*x+y*y+z*z);if(d<1e-10)continue;
                    double wa=link.a<colunas?0:1,wb=link.b<colunas?0:1;if(wa+wb==0)continue;
                    double q=(d-link.comprimento)/d*link.rigidez*perfil.rigidez()/(wa+wb);
                    for(int k=0;k<3;k++){double delta=(b[k]-a[k])*q;a[k]+=delta*wa;b[k]-=delta*wb;}
                }
                for(int i=colunas;i<atual.length;i++){
                    // Back-plane collision plus a ceiling prevents folds passing through shoulders.
                    atual[i][2]=Math.max(0,atual[i][2]);atual[i][1]=Math.min(-.025,atual[i][1]);
                }
            }
        }
    }
    public Ponto ponto(int x,int y,double parcial){
        if(x<0||x>=colunas||y<0||y>=linhas)throw new IllegalArgumentException("Particula invalida");
        double t=Double.isFinite(parcial)?clamp(parcial,0,1):0;int i=id(x,y);
        if(t==0)return new Ponto(anterior[i][0],anterior[i][1],anterior[i][2]);
        if(t==1)return new Ponto(atual[i][0],atual[i][1],atual[i][2]);
        return new Ponto(anterior[i][0]+(atual[i][0]-anterior[i][0])*t,anterior[i][1]+(atual[i][1]-anterior[i][1])*t,anterior[i][2]+(atual[i][2]-anterior[i][2])*t);
    }
    private static double[] normalizar(double[] v){double n=Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);return n<1e-10?new double[]{1,0,0}:new double[]{v[0]/n,v[1]/n,v[2]/n};}
    private static double[] cruzar(double[] a,double[] b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
    private double[][] orientacao(int x,int y,double t){
        Ponto a=ponto(x,y,t),b=ponto(x,y+1,t),left=ponto(Math.max(0,x-1),y+1,t),right=ponto(Math.min(colunas-1,x+1),y+1,t);
        double[] up=normalizar(new double[]{a.x-b.x,a.y-b.y,a.z-b.z});
        double[] across={right.x-left.x,right.y-left.y,right.z-left.z};
        double dot=across[0]*up[0]+across[1]*up[1]+across[2]*up[2];for(int i=0;i<3;i++)across[i]-=dot*up[i];
        double[] ex=normalizar(across),ez=normalizar(cruzar(ex,up));ex=normalizar(cruzar(up,ez));
        return new double[][]{{ex[0],up[0],ez[0]},{ex[1],up[1],ez[1]},{ex[2],up[2],ez[2]}};
    }
    /** Exact parent-relative ZYX rotation for a linked bone strip. Sampling never advances physics. */
    public Rotacao rotacao(int x,int segmento,double t){
        double[][] world=orientacao(x,segmento,t);if(segmento>0){double[][] parent=orientacao(x,segmento-1,t),r=new double[3][3];for(int i=0;i<3;i++)for(int j=0;j<3;j++)for(int k=0;k<3;k++)r[i][j]+=parent[k][i]*world[k][j];world=r;}
        return new Rotacao(Math.atan2(world[2][1],world[2][2]),Math.asin(clamp(-world[2][0],-1,1)),Math.atan2(world[1][0],world[0][0]));
    }
    public double erroAlongamento(){double max=0;for(var l:ligacoes)if(l.rigidez==1){double soma=0;for(int k=0;k<3;k++){double v=atual[l.a][k]-atual[l.b][k];soma+=v*v;}max=Math.max(max,Math.abs(Math.sqrt(soma)/l.comprimento-1));}return max;}
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
}
