#version 300 es
precision mediump float;
uniform mat4 uMVPMatrix;

in vec4 aPosition;
in vec2 aTexCoord;

out vec2 vTexCoord;
void main() {
    gl_Position = uMVPMatrix * aPosition;
    vTexCoord = vec2(aTexCoord.x, (1.0 - (aTexCoord.y)));
}