#version 150

uniform sampler2D SamplerSceneDepth;
uniform sampler2D SamplerSceneColor;
uniform mat4 U_InverseProjectionMatrix;
uniform vec4 ColorModulator;
uniform float GameTime;
uniform float Potencia;
uniform float Distorcao;
uniform vec4 HDRCor;

in vec3 posVista;
in vec3 normalVista;
in vec4 corVertice;

out vec4 fragColor;

float distanciaOlho(float ndcZ) {
    vec4 v = U_InverseProjectionMatrix * vec4(0.0, 0.0, ndcZ, 1.0);
    return -(v.z / v.w);
}

void main() {
    vec2 tela = gl_FragCoord.xy / vec2(textureSize(SamplerSceneDepth, 0));
    // Fresnel: borda acesa, centro quase transparente.
    float fresnel = pow(1.0 - abs(dot(normalize(normalVista), normalize(-posVista))), Potencia);
    // Interseção: acende onde a casca encosta no que está atrás.
    float cena = distanciaOlho(texture(SamplerSceneDepth, tela).r * 2.0 - 1.0);
    float propria = distanciaOlho(gl_FragCoord.z * 2.0 - 1.0);
    float corte = 1.0 - smoothstep(0.0, 0.6, cena - propria);
    // Distorção: puxa a cor da cena com uma onda que corre pela casca.
    float t = GameTime * 1200.0;
    vec2 desvio = normalize(normalVista).xy * Distorcao * (0.6 + 0.4 * sin(posVista.y * 6.0 + t * 4.0));
    vec3 atras = texture(SamplerSceneColor, tela + desvio).rgb;
    vec3 cor = atras + HDRCor.rgb * (fresnel * 0.8 + corte * 1.5) * corVertice.rgb * ColorModulator.rgb;
    // Só a borda e a linha de interseção cobrem a cena: o miolo fica transparente, para o brilho da
    // explosão que está dentro da casca (desenhado depois da captura da cena) continuar aparecendo.
    fragColor = vec4(cor, clamp(fresnel * 1.3 + corte, 0.0, 1.0));
}
