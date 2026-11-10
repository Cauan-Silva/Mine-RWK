#version 150

// Impact frame do uppercut do R: preto e branco de alto contraste, contornos marcados e um toque de roxo.
uniform sampler2D DiffuseSampler;
uniform float Forca;
uniform float Inverte;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float luz(vec2 uv) {
    return dot(texture(DiffuseSampler, uv).rgb, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec3 cor = texture(DiffuseSampler, texCoord).rgb;
    float l = luz(texCoord);
    float gx = luz(texCoord + vec2(oneTexel.x, 0.0)) - luz(texCoord - vec2(oneTexel.x, 0.0));
    float gy = luz(texCoord + vec2(0.0, oneTexel.y)) - luz(texCoord - vec2(0.0, oneTexel.y));
    float borda = clamp(length(vec2(gx, gy)) * 5.0, 0.0, 1.0);
    // Corte seco em dois tons; os contornos ficam do tom do fundo para marcar as silhuetas.
    float v = smoothstep(0.36, 0.46, l);
    v = mix(v, 1.0 - v, Inverte);
    v = mix(v, 1.0 - v, borda * 0.9);
    vec3 escuro = vec3(0.06, 0.015, 0.13);
    vec3 claro = vec3(1.0, 0.95, 1.0);
    fragColor = vec4(mix(cor, mix(escuro, claro, v), Forca), 1.0);
}
