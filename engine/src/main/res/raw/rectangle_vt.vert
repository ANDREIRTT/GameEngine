#version 300 es
precision mediump float;
uniform mat4 uMVPMatrix;

in vec4 aPosition;
in vec2 aTexCoord;

out vec2 vTexCoord;

uniform float uRotationAngle;
uniform vec2 uScale;


mat3 translate(float tx, float ty) {
    return mat3(
    1.0, 0.0, 0.0,
    0.0, 1.0, 0.0,
    tx, ty, 1.0
    );
}

mat3 rotate(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat3(
    c, -s, 0.0,
    s, c, 0.0,
    0.0, 0.0, 1.0
    );
}

mat3 scale(vec2 s) {
    return mat3(
    s.x, 0.0, 0.0,
    0.0, s.y, 0.0,
    0.0, 0.0, 1.0
    );
}

void main() {
    gl_Position = uMVPMatrix * aPosition;
    vec2 texCoord = vec2(aTexCoord.x, (1.0 - (aTexCoord.y)));

    mat3 transform = translate(0.5, 0.5) *
    rotate(uRotationAngle) *
    scale(uScale) *
    translate(-0.5, -0.5);
    vTexCoord = (transform * vec3(texCoord, 1.0)).xy;
}
