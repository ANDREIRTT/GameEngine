#version 300 es
precision highp float;

uniform mat4 uMVPMatrix;

in vec4 aPosition;

void main() {
    gl_Position = uMVPMatrix * aPosition;
}