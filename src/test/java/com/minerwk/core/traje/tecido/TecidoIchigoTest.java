package com.minerwk.core.traje.tecido;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class TecidoIchigoTest {
    private static MolaManto.Entrada mov(double frente,double giro){return new MolaManto.Entrada(frente,0,0,giro,true);}
    @Test void ossosBatemComOModelo()throws Exception{
        var geo=Files.readString(Path.of("src/main/resources/assets/minerwk/geo/traje/ichigo_bankai.geo.json"));
        assertEquals(36+6+6+12+12+12+6+12+6,TecidoIchigo.OSSOS.size());
        // Os ossos mg_* são da forma do Mugetsu, que ainda não tem modelo aqui.
        for(var o:TecidoIchigo.OSSOS)assertTrue(o.startsWith("mg_")||geo.contains("\""+o+"\""),o);
    }
    @Test void correrLevantaACaudaParaTrasESemEsticar(){
        var t=new TecidoIchigo();
        for(int i=0;i<60;i++)t.tick(mov(.28,0));
        var p=t.pose("bk_capa_costas_1_0",0);t.impulso(.9);for(int i=0;i<8;i++)t.tick(mov(.28,0),.2,-.2);assertTrue(Double.isFinite(p.x()+p.y()+p.z()));
        assertTrue(t.erroAlongamento()<.08,"alongamento "+t.erroAlongamento());
        for(int i=0;i<200;i++)t.tick(mov(0,0));
        for(var o:TecidoIchigo.OSSOS){var q=t.pose(o,0);assertTrue(Math.abs(q.x())<.4&&Math.abs(q.y())<.4&&Math.abs(q.z())<.4,o);}
    }
    @Test void exportaParaPrevia()throws Exception{
        var t=new TecidoIchigo();var quadros=new ArrayList<Object>();
        t.impulso(.9);double antes=0;
        for(int i=0;i<160;i++){
            double frente=i<20?0:i<80?.28:i<100?.05:0,giro=i>=100&&i<112?.18:0;
            // Same arm swing as the preview draws (32 degrees while running).
            double braco=i>=20&&i<80?Math.sin(i*.55)*Math.toRadians(32):0,db=braco-antes;antes=braco;
            t.tick(mov(frente,giro),-db,db);var ossos=new LinkedHashMap<String,Object>();
            for(var o:TecidoIchigo.OSSOS){var q=t.pose(o,0);ossos.put(o,List.of(q.x(),q.y(),q.z()));}
            quadros.add(Map.of("fase",i<20?"REVELACAO (casaco abre)":i<80?"CORRENDO":i<100?"PARANDO":i<112?"GIRANDO":"PARADO","ossos",ossos));
        }
        Files.createDirectories(Path.of("build"));
        try(var w=Files.newBufferedWriter(Path.of("build/ichigo-bankai-cloth-samples.json"))){new Gson().toJson(quadros,w);}
    }
}
