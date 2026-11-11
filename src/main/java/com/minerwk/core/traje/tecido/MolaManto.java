package com.minerwk.core.traje.tecido;
/** Four damped angular springs, advanced at fixed 20 Hz, never by render calls. Units are radians. */
public final class MolaManto {
    public static final int SEGMENTOS=4;
    public record Entrada(double frente,double lado,double vertical,double giro,boolean noChao){}
    public record Pose(double x,double y,double z){}
    private final double[][] ang=new double[4][3],vel=new double[4][3],anterior=new double[4][3];
    private final double resposta;
    private double frenteAnterior,ladoAnterior;
    public MolaManto(){this(1);}
    public MolaManto(double resposta){if(!Double.isFinite(resposta)||resposta<=0)throw new IllegalArgumentException("Resposta inválida");this.resposta=Math.max(.5,Math.min(2,resposta));}
    public void reset(){frenteAnterior=ladoAnterior=0;for(int i=0;i<4;i++)for(int j=0;j<3;j++)ang[i][j]=vel[i][j]=anterior[i][j]=0;}
    private static double limitar(double n,double min,double max){return Math.max(min,Math.min(max,n));}
    public void tick(Entrada e){
        if(!Double.isFinite(e.frente+e.lado+e.vertical+e.giro)){reset();return;}
        double acelFrente=limitar(e.frente-frenteAnterior,-.35,.35),acelLado=limitar(e.lado-ladoAnterior,-.35,.35);
        frenteAnterior=e.frente;ladoAnterior=e.lado;
        // Signed local motion: strafe is not converted into forward speed.
        double pitch=limitar(-e.frente*1.15-acelFrente*.65+(e.noChao?0:-.025)+limitar(e.vertical*.1,-.045,.025),-.24,.024);
        double roll=limitar(-e.lado*.8-acelLado*.6-e.giro*.18,-.14,.14);
        double yaw=limitar(-e.lado*.32-e.giro*.35,-.13,.13);
        for(int i=0;i<4;i++){
            System.arraycopy(ang[i],0,anterior[i],0,3);
            double w=.55+i*.15,k=(84-i*13)*resposta,d=2*Math.sqrt(k)*.78;
            double[] alvo={Math.min(.005+i*.008,pitch*w),yaw*w,roll*w};
            for(int passo=0;passo<2;passo++)for(int j=0;j<3;j++){
                vel[i][j]+=(k*(alvo[j]-ang[i][j])-d*vel[i][j])*.025;
                ang[i][j]+=vel[i][j]*.025;
                double limitado=limitar(ang[i][j],j==0?-.30:-.16,j==0?.010+i*.011:.16);
                if(limitado!=ang[i][j])vel[i][j]=0;ang[i][j]=limitado;
            }
        }
    }
    public Pose amostrar(int segmento,double parcial){
        if(segmento<0||segmento>=4)throw new IllegalArgumentException("Segmento inválido");
        parcial=Double.isFinite(parcial)?limitar(parcial,0,1):0;
        return new Pose(lerp(segmento,0,parcial),lerp(segmento,1,parcial),lerp(segmento,2,parcial));
    }
    private double lerp(int i,int j,double t){return anterior[i][j]+(ang[i][j]-anterior[i][j])*t;}
}
