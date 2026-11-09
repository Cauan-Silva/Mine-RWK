#version 150
uniform float EffectTime;
uniform float EffectMode;
uniform vec3 Viewer;
in vec4 vertexColor;
in vec2 energyUV;
out vec4 fragColor;
const float PI=3.14159265;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float noise(vec2 p){
    vec2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);
}
float fbm(vec2 p){return noise(p)*.57+noise(p*2.03+7.1)*.28+noise(p*4.07+13.7)*.15;}
vec3 sphereNormal(vec2 uv){
    float a=uv.x*2.0*PI,lat=(uv.y-.5)*PI;
    return vec3(cos(a)*cos(lat),sin(lat),sin(a)*cos(lat));
}
float hash3(vec3 p){return fract(sin(dot(p,vec3(127.1,311.7,74.7)))*43758.5453);}
float gojoNoise3(vec3 p){
    vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);
    float a=mix(mix(hash3(i),hash3(i+vec3(1,0,0)),f.x),mix(hash3(i+vec3(0,1,0)),hash3(i+vec3(1,1,0)),f.x),f.y);
    float b=mix(mix(hash3(i+vec3(0,0,1)),hash3(i+vec3(1,0,1)),f.x),mix(hash3(i+vec3(0,1,1)),hash3(i+vec3(1,1,1)),f.x),f.y);
    return mix(a,b,f.z);
}
float cloud3(vec3 p){return gojoNoise3(p)*.57+gojoNoise3(p*2.03+7.1)*.28+gojoNoise3(p*4.07+13.7)*.15;}
vec3 stars(vec2 uv,vec2 cells,float threshold,float time){
    vec2 p=uv*cells,id=floor(p),local=fract(p)-.5;
    id.x=mod(id.x,cells.x);
    float seed=hash(id),exist=step(threshold,seed);
    vec2 offset=vec2(hash(id+19.),hash(id+47.))*.5-.25;
    vec2 d=local-offset;
    float pixel=max(fwidth(p.x),fwidth(p.y));
    float size=max(.035+.055*hash(id+9.),pixel*.6);
    float dotStar=exp(-dot(d,d)/(size*size));
    float halo=exp(-dot(d,d)/(.045))*.035;
    float crossStar=(exp(-abs(d.x)*180.-abs(d.y)*13.)+exp(-abs(d.y)*180.-abs(d.x)*13.))*.2*step(.99,seed);
    vec3 tint=mix(vec3(.46,.64,1.),vec3(1.,.91,.83),hash(id+31.));
    return tint*(dotStar+halo+crossStar)*exist*(.85+.15*sin(time*.6+seed*31.));
}
vec3 limitlessSky(vec2 uv,float time){
    vec3 n=sphereNormal(uv);
    // Advect the large masses; secondary flow deforms their edges continuously.
    // Star coordinates below remain unrotated, so the sky does not spin as a flat layer.
    float angle=time*.045,c=cos(angle),s=sin(angle);
    vec3 advected=vec3(c*n.x-s*n.z,n.y,s*n.x+c*n.z);
    vec3 flow=vec3(sin(advected.y*3.+time*.17),
                   sin(advected.z*3.-time*.13),cos(advected.x*3.+time*.11))*.12;
    float cloud=cloud3((advected+flow)*4.+vec3(time*.045,-time*.025,0.));
    float detail=cloud3((advected+flow*1.5)*13.+vec3(-time*.09,time*.13,time*.07));
    float plane=n.y*.75+n.x*.28+n.z*.16;
    float band=exp(-pow((plane+.1+(cloud-.5)*.3)*5.,2.));
    float mass=cloud*.65+detail*.35;
    float clouds=smoothstep(.46,.65,mass);
    float wisps=smoothstep(.44,.51,mass)*(1.-smoothstep(.51,.58,mass));
    float holes=smoothstep(.28,.47,detail);
    clouds*=holes;
    vec3 color=vec3(.0006,.0012,.005);
    color+=mix(vec3(.015,.075,.2),vec3(.025,.18,.38),cloud)*band*(.3+detail*.7);
    color+=vec3(.05,.18,.34)*wisps*.55;
    color+=mix(vec3(.09,.31,.57),vec3(.82,.91,1.),pow(clouds,.65))*clouds;
    color+=vec3(.11,.22,.38)*pow(max(0.,detail-.45),2.)*band;
    color+=stars(uv,vec2(420.,210.),.975,time)*(.4+.6*(1.-clouds));
    color+=stars(uv,vec2(140.,70.),.987,time)*.65;
    return color;
}
void main(){
    vec2 uv=energyUV;
    float time=EffectTime,mode=EffectMode;
    vec3 color=vertexColor.rgb;
    float alpha=vertexColor.a;
    if(mode<2.5){
        vec3 n=sphereNormal(uv);
        float facing=abs(dot(n,normalize(Viewer)));
        float rim=pow(1.0-facing,1.6);
        vec2 p=vec2(n.x+n.z,n.y)*5.0;
        float flow=fbm(p+vec2(time*.9,-time*.6));
        float veins=pow(1.0-abs(sin(flow*18.0+time*2.0+n.y*9.0)),14.0);
        if(mode<.5){
            // Azul: volume escuro que absorve, borda ciano e fluxo espiral convergente.
            float spiral=pow(.5+.5*sin(atan(n.z,n.x)*7.0+n.y*18.0+time*4.0),10.0);
            color=mix(vec3(.001,.008,.025),vec3(.04,.5,1.0),rim*.9);
            color+=vec3(.1,.65,1.0)*(veins*.07+spiral*rim*.6);
        } else if(mode<1.5){
            // Vermelho: fonte compacta branca, halo carmesim e linhas que irradiam.
            float radial=pow(.5+.5*sin(atan(n.z,n.x)*11.0+n.y*7.0-time*8.0),12.0);
            color=mix(vec3(1.0,.025,.035),vec3(1.0,.88,.74),pow(facing,5.0));
            color+=vec3(.8,.12,.1)*(veins*.4+radial*rim*.6);
        } else {
            // Pearl-hot imaginary mass, violet turbulence and a crisp luminous boundary.
            float warp=fbm(p*1.1+vec2(time*.5,-time*.9));
            float fold=fbm(p*2.5+warp*3.+vec2(-time*.7,time*.4));
            float sheets=pow(.5+.5*sin(n.y*24.+warp*11.-time*4.),5.);
            float hot=pow(facing,2.8)*(.52+fold*.4);
            color=mix(vec3(.045,.002,.12),vec3(.52,.065,.94),warp*.8+rim*.25);
            color=mix(color,vec3(.94,.73,1.),hot);
            color+=vec3(.45,.2,.65)*sheets*(.12+rim*.2);
            float lip=exp(-pow((facing-.12)*16.,2.));
            color+=vec3(.68,.37,1.)*lip*.8;
            color+=vec3(.10,.22,.55)*pow(max(0.,sin(n.x*13.+warp*5.-time*2.)),10.)*rim*.18;
        }
    } else if(mode<3.5){
        // Infinito: discreto, mais visivel nos pontos de aproximacao.
        float wave=pow(.5+.5*cos(uv.x*42.0-time*3.0+sin(uv.y*17.0)),18.0);
        float edge=pow(max(0.0,sin(uv.y*PI)),2.0);
        color=vec3(.55,.8,1.0);alpha*=wave*edge;
    } else if(mode<4.5){
        // Vazio Infinito: estrelas estaveis, nebulosa lenta e linhas de informacao.
        vec2 sky=vec2(uv.x*2.0,uv.y);
        float cloud=fbm(sky*7.0+vec2(time*.015,0));
        vec2 grid=sky*110.0,id=floor(grid),p=fract(grid)-.5;
        float seed=hash(id);
        float star=(1.0-smoothstep(.02,.13,length(p)))*step(.94,seed);
        float band=exp(-pow((uv.y-.46+sin(uv.x*12.0)*.07)*13.0,2.0));
        color=vec3(.001,.002,.012)+vec3(.035,.075,.18)*cloud*band;
        color+=vec3(.65,.82,1.0)*star*(.8+.2*sin(time*.7+seed*19.0));
        color+=vec3(.18,.24,.46)*pow(max(0.0,cloud-.2),2.0)*band;
    } else if(mode<5.5){
        // Fitas: material compartilhado; cor e movimento definidos pela tecnica.
        float warp=(fbm(vec2(uv.x*2.0-time*1.6,uv.y*3.0))-.5)*.18;
        float d=abs(uv.y-.5+warp);
        float core=exp(-d*d*800.0),edge=exp(-d*d*35.0);
        float pulse=.55+.45*fbm(vec2(uv.x*5.0-time*4.0,uv.y*4.0));
        color=mix(color,color*.4+vec3(.65),core*.65);
        alpha*=edge*pulse*(1.0-smoothstep(.35,.5,abs(uv.y-.5)));
    } else if(mode<6.5){
        vec3 n=sphereNormal(uv);
        float rim=pow(1.0-abs(dot(n,normalize(Viewer))),3.5);
        color*=.7+fbm(vec2(n.x+n.z,n.y)*8.0+time*.2)*.3;
        alpha*=rim*.6;
    } else if(mode<7.5) {
        vec3 n=sphereNormal(uv);
        float f=fbm(vec2(n.x+n.z,n.y)*9.0+time*.05);
        color=vec3(.004,.005,.012)+vec3(.008,.012,.025)*pow(f,3.0);
    }
    if(mode>7.5&&mode<8.5){float d=abs(uv.y-.5);float core=exp(-d*d*170.0);color=mix(vertexColor.rgb,vec3(1.0,.91,1.0),core*.8);alpha=vertexColor.a*exp(-d*d*32.0);}
    if(mode>=8.5&&mode<9.5){
        float d=abs(uv.y-.5),noiseFlow=fbm(vec2(uv.x*8.-time*2.4,uv.y*5.));
        float broken=smoothstep(.22,.6,.5+.5*sin(uv.x*16.+noiseFlow*4.-time*3.));
        float edge=exp(-d*d*85.),core=exp(-d*d*580.);
        color=mix(vertexColor.rgb,vec3(.99,.9,1.),core*.9);
        alpha*=edge*broken*(.65+.35*noiseFlow);
    }
    if(mode>=9.5&&mode<10.5){
        vec3 n=sphereNormal(uv);
        float facing=abs(dot(n,normalize(Viewer)));
        float rim=pow(1.-facing,2.2);
        float warp=fbm(vec2(n.x+n.z,n.y)*7.+vec2(time*.9,-time*1.3));
        float lip=exp(-pow((facing-.13)*19.,2.));
        float streak=pow(.5+.5*sin(atan(n.z,n.x)*9.+n.y*17.+warp*6.+time*5.),12.);
        color=vec3(.001,.004,.014)+vec3(.015,.17,.48)*rim;
        color+=vec3(.09,.67,1.)*(lip*.95+streak*rim*.55);
        color+=vec3(.38,.85,1.)*pow(lip,3.)*.45;
    }
    if(mode>=10.5&&mode<11.5){
        float d=abs(uv.y-.5);
        float core=exp(-d*d*620.),edge=exp(-d*d*65.);
        float flow=.65+.35*fbm(vec2(uv.x*11.-time*3.,uv.y*3.));
        color=mix(vertexColor.rgb,vec3(.62,.92,1.),core*.8);
        alpha*=edge*flow;
    }
    if(mode>=11.5&&mode<12.5){
        vec3 n=sphereNormal(uv);
        float facing=abs(dot(n,normalize(Viewer)));
        float rim=pow(1.-facing,1.7);
        float flow=fbm(vec2(n.x+n.z,n.y)*8.+vec2(time*2.,-time*3.));
        float cracks=pow(.5+.5*sin(n.y*24.+flow*12.-time*13.),8.);
        color=mix(vec3(.72,.002,.025),vec3(1.,.92,.85),pow(facing,2.5));
        color+=vec3(.55,.055,.06)*cracks*rim;
        color+=vec3(.42,.09,.07)*exp(-pow((facing-.16)*14.,2.));
    }
    if(mode>=12.5&&mode<13.5){
        float d=abs(uv.y-.5),edge=exp(-d*d*55.),core=exp(-d*d*650.);
        float flow=.75+.25*fbm(vec2(uv.x*12.-time*12.,uv.y*4.));
        color=mix(vertexColor.rgb,vec3(1.,.91,.85),core*.92);
        alpha*=edge*flow;
    }
    if(mode>=13.5&&mode<14.5)color=limitlessSky(uv,time);
    if(mode>=14.5&&mode<15.5){
        vec3 n=sphereNormal(uv);float facing=abs(dot(n,normalize(Viewer)));
        float cloud=cloud3(n*9.+vec3(time*.006,0.,0.));
        float rim=pow(1.-facing,4.);
        color=vec3(.002,.003,.008)+vec3(.004,.007,.014)*cloud;
        color+=vec3(.025,.052,.1)*rim;
        float threads=pow(.5+.5*sin(cloud*34.+n.y*6.-time*.12),24.);
        color+=vec3(.007,.013,.025)*threads;
    }
    if(mode>=16.5&&mode<17.5){
        float d=abs(uv.y-.5),core=exp(-d*d*600.);
        color=mix(vertexColor.rgb*(1.2+core*.3),vec3(1.),core*.03);
        alpha*=exp(-d*d*65.);
    }
    if(mode>=17.5&&mode<18.5)color=limitlessSky(uv,time)*.55;
    if(mode>=18.5&&mode<19.5){
        float warp=(fbm(vec2(uv.x*5.-time*1.8,uv.y*3.))-.5)*.22;
        float d=abs(uv.y-.5+warp),core=exp(-d*d*650.);
        float flow=.6+.4*fbm(vec2(uv.x*9.-time*4.,uv.y*5.));
        color=mix(vertexColor.rgb,vec3(.78,.96,1.),core*.4);
        alpha*=exp(-d*d*65.)*flow;
    }
    if(mode>=19.5&&mode<20.5){
        float flow=fbm(vec2(uv.x*6.-time*1.8,uv.y*4.+time*.7));
        float d=abs(uv.y-.5+(flow-.5)*.3);
        float mist=smoothstep(.3,.7,flow)*exp(-d*d*25.);
        color=vertexColor.rgb*(.6+flow*.6);
        alpha*=mist;
    }
    if(mode>=20.5&&mode<21.5){
        float d=abs(uv.y-.5),core=exp(-d*d*500.);
        float flow=.8+.2*fbm(vec2(uv.x*14.+time*8.,uv.y*4.));
        color=mix(vertexColor.rgb*(1.15+core*.3),vec3(.94,.98,1.),core*.08);
        alpha*=exp(-d*d*65.)*flow;
    }
    if(mode>=21.5&&mode<23.5){
        vec3 n=sphereNormal(uv);float facing=abs(dot(n,normalize(Viewer))),rim=pow(1.-facing,1.5);
        float cloud=fbm(vec2(n.x+n.z,n.y)*10.+vec2(time*2.,-time*3.));
        float cracks=pow(1.-abs(sin(cloud*13.+n.y*24.-time*7.)),8.);
        color=mix(vec3(.32,.005,.65),vec3(.99,.9,1.),cracks*.7+rim*.3);
        if(mode<22.5)alpha*=(.15+.85*rim)*smoothstep(.25,.75,cloud);
        else color=mix(vec3(.92,.72,1.),vec3(1.,.98,1.),facing*.7+cracks*.3);
    }
    if(mode>=23.5&&mode<24.5){
        float d=abs(uv.y-.5),core=exp(-d*d*650.);
        float flow=.7+.3*fbm(vec2(uv.x*9.-time*9.,uv.y*4.));
        color=mix(vertexColor.rgb,vec3(.9,.98,1.),core*.45);
        alpha*=exp(-d*d*65.)*flow;
    }
    if(mode>=24.5&&mode<25.5){
        float d=abs(uv.y-.5),core=exp(-d*d*650.);
        float data=.35+.65*smoothstep(.35,.65,.5+.5*sin(uv.x*38.+time*12.));
        color=mix(vertexColor.rgb,vec3(.94,.98,1.),core*.35);
        alpha*=exp(-d*d*65.)*data;
    }
    if(mode>=39.5&&mode<40.5){
        // Vazio Infinito (rework 2026-11-08): fundo quase preto e limpo, estrelas nitidas cintilando.
        vec2 uvv=energyUV;
        float lat=abs(uvv.y-.5)*2.;
        color=mix(vec3(.012,.02,.06),vec3(.002,.003,.012),smoothstep(0.,.6,lat));
        vec2 g=vec2(uvv.x*220.,uvv.y*110.),cel=floor(g),f=fract(g)-.5;
        float h=hash(cel);
        if(h>.965){
            vec2 o=vec2(hash(cel+3.1),hash(cel+7.7))-.5;
            float dd=length(f-o*.6);
            float tw=.55+.45*sin(time*(1.5+h*4.)+h*40.);
            float star=exp(-dd*dd*120.)*tw*(.5+.5*fract(h*37.));
            color+=vec3(.85,.93,1.)*star*1.6;
        }
        alpha=vertexColor.a;
    }
    if(mode>=40.5&&mode<41.5){
        // Chao do Vazio Infinito: superficie solida escura e polida (sem estrelas), reflexo suave perto do centro
        // e ondas finas de "agua parada". uv.x = angulo (0..1), uv.y = .5 no centro ate 1 na borda.
        float r=clamp((energyUV.y-.5)*2.,0.,1.);
        color=mix(vec3(.05,.07,.11),vec3(.012,.016,.03),smoothstep(0.,1.,r));
        float onda=.5+.5*sin(r*90.-time*1.2);
        color+=vec3(.06,.09,.14)*pow(onda,18.)*(1.-r);
        color+=vec3(.10,.14,.22)*exp(-r*r*14.);
        alpha=vertexColor.a;
    }
    if(mode>=41.5&&mode<42.5){
        // Brilho (Mine-RWK): quadrado virado para a camera com queda radial suave, no lugar do bloom.
        float d=length(energyUV-vec2(.5))*2.;
        float g=pow(max(0.,1.-d),2.2)*.75+exp(-d*d*14.)*.5;
        color=vertexColor.rgb*(1.+exp(-d*d*30.)*.6);
        alpha=vertexColor.a*g;
    }
    if(alpha<.004) discard;
    fragColor=vec4(color,alpha);
}
