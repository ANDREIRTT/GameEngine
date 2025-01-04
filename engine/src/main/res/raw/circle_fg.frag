#version 300 es
precision highp float;

uniform float uRadius;
uniform vec2 uCirclePosition;
uniform float uCircleBorderWidth;

uniform sampler2D uCircleTexture;

in vec2 vFragmentPosition;
in vec2 vTextureCoordinate;

out vec4 vFragmentColor;

void main() {
    float dist = distance(vFragmentPosition, uCirclePosition);
    float outerRadius = uRadius;
    float innerRadius = outerRadius - uCircleBorderWidth;

    if (dist < innerRadius) {
        vFragmentColor = texture(uCircleTexture, vTextureCoordinate);
    } else if (dist < outerRadius) {
        vFragmentColor = vec4(1.0, 1.0, 1.0, 1.0);
    } else {
        discard;
    }
}