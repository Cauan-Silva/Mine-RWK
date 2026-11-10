"""Motor de SFX em camadas, portado do Minezin (mesmo autor) para o Mine-RWK.
Layered SFX engine for the sound rework (user: sounds were synthetic/weak, out of sync, missing).
Tools only: numpy, soundfile. Original procedural audio, no sampled media.

Each sound is built from layers, the way game SFX are designed:
  transiente  - the first 5-30 ms: a click/crack that tells the ear "something hit" (where sync is felt)
  corpo       - the body: band-limited noise and tones with pitch envelopes (the character of the skill)
  sub         - a saturated low sine sweep under the impact (weight; felt more than heard)
  cauda       - a synthetic room/space reverb tail (size and distance)
then glued with a soft compressor and a peak limiter. Every sound is exported in VARIANTES slightly different takes
(pitch +-4%, other noise seed) listed together in sounds.json, so Minecraft picks one at random and repeated casts
never sound identical.

Timing: a sound starts on the server tick of the event it belongs to (the same tick the VFX/packet uses), and its
own internal hits are placed at the VFX phase times (ticks / 20), so sound and picture stay locked together.
"""
from pathlib import Path
import hashlib,json
import numpy as np
import soundfile as sf

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/minerwk'
RATE=48000;VARIANTES=3
_AMOSTRAS={}
TICK=1/20

class Som:
    """A mono buffer of `duracao` seconds with layer helpers; every helper returns an array of the same length."""
    def __init__(self,duracao,semente):
        self.n=max(1,round(duracao*RATE));self.dur=self.n/RATE;self.t=np.arange(self.n)/RATE
        self.rng=np.random.default_rng(semente);self.freq=np.fft.rfftfreq(self.n,1/RATE)
    # ---------- recorded layers (CC0, art/audio/cc0, sources in art/audio/cc0/fontes.json) ----------
    def amostra(self,nome,ini=0.,ganho=1.,tom=1.,inverter=False,corte=None,fade=.008):
        """A CC0 recording (path relative to art/audio/cc0, or a list to pick one at random per take) placed at `ini`
        seconds: mono, resampled to RATE and pitched by `tom` (2 = an octave up, also twice as short), optionally
        reversed and cut to `corte` seconds, peak-normalised and scaled by `ganho`."""
        if isinstance(nome,(list,tuple)):nome=nome[int(self.rng.integers(len(nome)))]
        if nome not in _AMOSTRAS:
            d,sr=sf.read(ROOT/'art/audio/cc0'/nome,dtype='float32',always_2d=True);_AMOSTRAS[nome]=(d.mean(axis=1),sr)
        d,sr=_AMOSTRAS[nome];passo=sr/RATE*tom;idx=np.arange(0,len(d)-1,passo);x=np.interp(idx,np.arange(len(d)),d)
        if inverter:x=x[::-1]
        if corte:x=x[:int(corte*RATE)]
        k=max(1,int(fade*RATE));x=x.copy();x[:k]*=np.linspace(0,1,k);x[-k:]*=np.linspace(1,0,k)
        x=x/max(1e-9,np.max(np.abs(x)));out=np.zeros(self.n);i=int(ini*RATE)
        if i<self.n:m=min(len(x),self.n-i);out[i:i+m]=x[:m]
        return ganho*out
    # ---------- sources ----------
    def ruido(self,lo=20,hi=16000,cor=0.):
        """Band-limited noise: lo/hi edges (4th/8th order), cor tilts it darker (pink ~.5, brown ~1)."""
        w=self.rng.standard_normal(self.n);f=self.freq
        g=(1+f/80)**(-cor)/(1+(f/hi)**8)*(1-1/(1+(f/max(lo,1))**4))
        x=np.fft.irfft(np.fft.rfft(w)*g,n=self.n);return x/max(1e-9,np.std(x))
    def tom(self,f0,f1=None,curva=1.,ini=0.,fim=None,harmonicos=(1,),pesos=None,vibrato=0.,vib_hz=5.):
        """Tone gliding f0->f1 between ini and fim (curve exponent), with harmonics and optional vibrato."""
        f1=f0 if f1 is None else f1;fim=self.dur if fim is None else fim
        q=np.clip((self.t-ini)/max(1e-6,fim-ini),0,1);f=f0+(f1-f0)*q**curva
        if vibrato:f=f*(1+vibrato*np.sin(2*np.pi*vib_hz*self.t))
        fase=2*np.pi*np.cumsum(f)/RATE;pesos=pesos or [1/(i+1) for i in range(len(harmonicos))]
        return sum(p*np.sin(h*fase) for h,p in zip(harmonicos,pesos))/sum(pesos)
    def sub(self,ini,f0=60,f1=28,queda=3.,drive=2.5):
        """Saturated low sweep starting at `ini` (the weight under an impact)."""
        q=np.maximum(0,self.t-ini);f=f1+(f0-f1)*np.exp(-q*6);x=np.sin(2*np.pi*np.cumsum(f)/RATE)
        return (self.t>=ini)*np.tanh(drive*x)/np.tanh(drive)*np.exp(-q*queda)
    def transiente(self,ini,brilho=6000,duracao=.012,forca=1.):
        """A crack: a very short noise burst plus a click, at `ini` (seconds)."""
        q=self.t-ini;m=(q>=0)&(q<duracao*6)
        x=np.zeros(self.n);x[m]=self.rng.standard_normal(m.sum())*np.exp(-q[m]/duracao)
        x=self._passa_alta(x,brilho*.15)
        k=int(ini*RATE)
        if 0<=k<self.n-64:x[k:k+64]+=np.hanning(128)[64:]*1.5
        return forca*x/max(1e-9,np.max(np.abs(x)))
    def estalos(self,ini,fim,densidade=40,brilho=5000,forca=1.):
        """Crackle of sparks: random tiny clicks between ini and fim (density per second)."""
        x=np.zeros(self.n);k=int(densidade*(fim-ini))
        for s in self.rng.uniform(ini,fim,max(0,k)):
            i=int(s*RATE);L=int(RATE*self.rng.uniform(.002,.008))
            if i+L<self.n:x[i:i+L]+=self.rng.uniform(-1,1)*self.rng.standard_normal(L)*np.exp(-np.arange(L)/(L/4))
        return forca*self._passa_alta(x,brilho)
    # ---------- shaping ----------
    def env(self,ini,ataque,queda,sustento=0.):
        """Envelope: starts at ini, linear attack, exponential decay towards `sustento`."""
        q=self.t-ini;a=np.clip(q/max(ataque,1e-4),0,1);d=np.exp(-np.maximum(0,q-ataque)*queda)
        return (q>=0)*a*(sustento+(1-sustento)*d)
    def crescer(self,ini,fim,curva=2.):
        """Swell from 0 at ini to 1 at fim (curve exponent), held after."""
        return np.clip((self.t-ini)/max(1e-6,fim-ini),0,1)**curva
    def janela(self,ini,fim,borda=.03):
        """1 between ini and fim with soft edges."""
        a=np.clip((self.t-ini)/borda,0,1);b=np.clip((fim-self.t)/borda,0,1);return a*b
    def girar(self,x,hz=3.,prof=.6):
        """Rotating amplitude/brightness wobble (a vortex or swirl)."""
        return x*(1-prof/2+prof/2*np.sin(2*np.pi*hz*self.t+np.sin(2*np.pi*hz*.37*self.t)))
    def _passa_alta(self,x,fc):
        X=np.fft.rfft(x);X*=1-1/(1+(self.freq/max(fc,1))**4);return np.fft.irfft(X,n=self.n)
    def passa_baixa(self,x,fc):
        X=np.fft.rfft(x);X/=1+(self.freq/max(fc,1))**4;return np.fft.irfft(X,n=self.n)
    def reverb(self,x,tamanho=1.2,mistura=.3,brilho=6000):
        """Synthetic space: early reflections + a dark exponentially decaying noise tail of `tamanho` seconds."""
        L=int(RATE*tamanho);ir=self.rng.standard_normal(L)*np.exp(-np.arange(L)/(RATE*tamanho/6.9))
        ir=np.convolve(ir,np.ones(8)/8,'same');F=np.fft.rfftfreq(L,1/RATE);ir=np.fft.irfft(np.fft.rfft(ir)/(1+(F/brilho)**2),n=L)
        for d,g in [(.011,.5),(.019,.35),(.031,.25),(.047,.18)]:ir[int(d*RATE)]+=g*len(ir)**.5*.02
        ir/=np.sqrt(np.sum(ir**2))+1e-9;N=1<<int(np.ceil(np.log2(self.n+L)))
        y=np.fft.irfft(np.fft.rfft(x,N)*np.fft.rfft(ir,N),n=N)[:self.n]
        return (1-mistura)*x+mistura*y*np.std(x)/max(1e-9,np.std(y))

def finalizar(x,pico=.6):
    """Glue (soft compressor), fades and a peak limit. Mono keeps Minecraft's 3D attenuation."""
    n=len(x);t=np.arange(n)/RATE
    # Compress only what is above the threshold (keeps the hit louder than its tail: punch), then soft clip.
    x=x/max(1e-9,np.max(np.abs(x)))
    env=np.sqrt(np.convolve(x**2,np.ones(480)/480,'same'))+1e-6;ganho=np.where(env>.45,(.45/env)**.5,1.)
    x=np.tanh(1.4*x*ganho)
    x*=np.minimum(1,t/.004)*np.minimum(1,(t[-1]-t)/.05+1e-9)
    return (x*min(1,pico/max(1e-9,np.max(np.abs(x))))).astype('float32')

def variantes(sintese,nome,duracao):
    """VARIANTES takes of one sound: a different seed each and a small pitch shift (resampling)."""
    base=7001+sum((i+1)*ord(c) for i,c in enumerate(nome));out=[]
    for v in range(VARIANTES):
        x=sintese(Som(duracao,base+97*v));fator=[1,1.04,.96][v%3]
        if fator!=1:
            idx=np.arange(0,len(x)-1,fator);x=np.interp(idx,np.arange(len(x)),x).astype('float32')
        out.append(finalizar(x))
    return out

def escrever(sons,sintese,rotulo,preview=None,idiomas=True,pasta_sons='gojo',marcos=None):
    """sons: name -> (duration s, attenuation distance, pt subtitle, en subtitle). Writes the variants, updates only
    these entries in sounds.json, the lang files and the manifest; optional preview WAV with every take in a row."""
    pasta=ASSETS/'sounds'/pasta_sons;pasta.mkdir(parents=True,exist_ok=True)
    sj=json.loads((ASSETS/'sounds.json').read_text(encoding='utf-8'))
    mp=ROOT/f'ferramentas/sfx_{pasta_sons}_manifesto.json';man=json.loads(mp.read_text(encoding='utf-8')) if mp.exists() else {}
    mix=[]
    for nome,(dur,dist,pt,en) in sons.items():
        lista=[]
        for v,x in enumerate(variantes(lambda s:sintese(nome,s),nome,dur)):
            arq=nome if v==0 else f'{nome}_{v+1}';p=pasta/(arq+'.ogg')
            with sf.SoundFile(p,mode='w',samplerate=RATE,channels=1,format='OGG',subtype='VORBIS') as e:
                for i in range(0,len(x),4096):e.write(x[i:i+4096])
            d,sr=sf.read(p);assert sr==RATE and d.ndim==1 and np.isfinite(d).all() and np.max(np.abs(d))<.99,(arq,float(np.max(np.abs(d))))
            lista.append({'name':f'minerwk:{pasta_sons}/'+arq,'attenuation_distance':dist})
            man[arq]={'duration':round(len(d)/sr,4),'sample_rate':sr,'channels':1,'peak':round(float(np.max(np.abs(d))),5),'rms':round(float(np.sqrt(np.mean(d**2))),5),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
            if v==0:man[arq]['ticks']=round(dur*20);man[arq]['marcos']=dict((marcos or {}).get(nome,{}))
            mix.extend([x,np.zeros(int(RATE*.35),dtype='float32')])
        sj[nome]={'subtitle':'subtitles.minerwk.'+nome,'sounds':lista}
    (ASSETS/'sounds.json').write_bytes((json.dumps(sj,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
    for lang,i in ([('pt_br',2),('en_us',3)] if idiomas else []):  # idiomas=False: legendas ficam a cargo de quem chama
        p=ASSETS/f'lang/{lang}.json';d=json.loads(p.read_text(encoding='utf-8'));d.update({'subtitles.minerwk.'+n:v[i] for n,v in sons.items()})
        p.write_bytes((json.dumps(d,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
    mp.parent.mkdir(parents=True,exist_ok=True);mp.write_bytes((json.dumps(man,indent=2)+'\n').encode('utf-8'))
    if preview:p=Path(preview);p.parent.mkdir(parents=True,exist_ok=True);sf.write(p,np.concatenate(mix),RATE,subtype='PCM_16')
    print(rotulo,len(sons),'sons x',VARIANTES,'variacoes, Vorbis mono com picos limitados.')
