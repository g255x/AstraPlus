#version 150

uniform vec4 u_Color;
uniform vec4 u_BackgroundColor;
uniform vec2 u_Resolution;
uniform vec2 u_Mouse;
uniform float u_Scale;
uniform float u_Time;
uniform float mixFactor;

in vec2 texCoord;
out vec4 fragColor;

#define TAU 6.28318530718
#define MAX_ITER 5
#define INTENSITY 0.005

void main() {
    vec2 resolution = max(u_Resolution.xy, vec2(1.0));
    vec2 uv = texCoord - vec2(0.5);
    uv.x *= resolution.x / resolution.y;
    uv = (uv + u_Mouse) * u_Scale + vec2(0.5);
    vec2 p = mod(uv * TAU, TAU) - 250.0;

    vec2 iterPos = p;
    float value = 1.0;
    float timeOffset = u_Time * 0.5 + 23.0;
    float invMaxIter = 1.0 / float(MAX_ITER);

    for (int n = 0; n < MAX_ITER; n++) {
        float iterTime = timeOffset * (1.0 - (3.5 / float(n + 1)));
        iterPos = p + vec2(
            cos(iterTime - iterPos.x) + sin(iterTime + iterPos.y),
            sin(iterTime - iterPos.y) + cos(iterTime + iterPos.x)
        );

        float sinX = sin(iterPos.x + iterTime);
        float cosY = cos(iterPos.y + iterTime);
        float compX = p.x * INTENSITY / max(abs(sinX), 0.001);
        float compY = p.y * INTENSITY / max(abs(cosY), 0.001);
        // inversesqrt 是 GPU 原生快速指令，等价于 1.0 / sqrt(...)，结果完全一致
        value += inversesqrt(compX * compX + compY * compY);
    }

    value = 1.17 - pow(value * invMaxIter, 1.4);

    float pwr = value * value;
    pwr = pwr * pwr;
    pwr = pwr * pwr;

    vec3 baseColor = vec3(abs(pwr));
    vec3 enhancedColor = clamp(baseColor + vec3(0.0, 0.35, 0.5), 0.0, 1.0);
    float luminance = dot(enhancedColor, vec3(0.299, 0.587, 0.114));

    float alpha = clamp(luminance * clamp(mixFactor, 0.0, 1.0), 0.0, 1.0);
    vec3 background = mix(vec3(0.01, 0.02, 0.05), u_BackgroundColor.rgb, clamp(u_BackgroundColor.a, 0.0, 1.0));
    vec3 overlay = vec3(luminance) * clamp(u_Color.rgb, 0.0, 1.0) * 1.4;
    fragColor = vec4(mix(background, overlay, alpha), 1.0);
}
