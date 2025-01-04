#version 300 es
precision highp float;

layout (location = 0) in float vSegment;
uniform mat4 uMVPMatrix;
uniform int uNumSegments;

uniform float uRadius;
uniform vec2 uCirclePosition;

uniform mat3 uTextureTransform;

out vec2 vFragmentPosition;
out vec2 vTextureCoordinate;

void main() {
    float theta = 2.0 * 3.1415926 * vSegment / float(uNumSegments);
    float x = uRadius * cos(theta) + uCirclePosition.x;
    float y = uRadius * sin(theta) + uCirclePosition.y;
    vFragmentPosition = vec2(x, y);

    vec2 polarTexCoord = vec2(cos(theta) * 0.5 + 0.5, sin(theta) * 0.5 + 0.5);
    vec3 transformedTexCoord = uTextureTransform * vec3(polarTexCoord, 1.0);
    vTextureCoordinate = transformedTexCoord.xy;

    gl_Position = uMVPMatrix * vec4(x, y, 0.0, 1.0);
}
