#version 330 core

#moj_import <photon:particle.glsl>

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 posVista;
out vec3 normalVista;
out vec4 corVertice;

// Protótipo da Parte 0: casca da explosão com fresnel, interseção e distorção.
void main() {
    ParticleData data = getParticleData();
    vec4 vista = ModelViewMat * vec4(data.Position, 1.0);
    gl_Position = ProjMat * vista;
    posVista = vista.xyz;
    normalVista = normalize(mat3(ModelViewMat) * data.Normal);
    corVertice = data.Color;
}
