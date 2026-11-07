package com.reactgram.liquidglass;

/**
 * AGSL source of the glass shader.
 *
 * Refraction follows the existing Telegram Android shader (rounded-rect SDF, bevel normal, Snell refraction).
 * The edge highlight, saturation and matte are modelled on the iOS Liquid Glass layers
 * (LiquidGlassShapes.metal: rim line of constant width along the shape edge, saturation multiply, white matte).
 * No Metal code is copied; the formulation is written for AGSL.
 */
final class GlassShaders {
    private GlassShaders() {}

    static final String LIQUID_GLASS =
        "uniform shader img;\n" +
        "uniform float2 resolution;\n" +
        "uniform float2 center;\n" +
        "uniform float2 size;\n" +
        "uniform float4 radius;\n" +
        "uniform float thickness;\n" +
        "uniform float refract_index;\n" +
        "uniform float refract_intensity;\n" +
        "uniform float4 foreground_color_premultiplied;\n" +
        "uniform float rim_width;\n" +
        "uniform float rim_strength;\n" +
        "uniform float2 light_dir;\n" +
        "uniform float saturation;\n" +
        "uniform float matte;\n" +
        "\n" +
        "float sdfRect(float2 p, float4 r) {\n" +
        "  r.xy = (p.x > 0.0) ? r.xy : r.zw;\n" +
        "  r.x  = (p.y > 0.0) ? r.x  : r.y;\n" +
        "  float2 q = abs(p) - size + r.x;\n" +
        "  return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r.x;\n" +
        "}\n" +
        "\n" +
        "half4 srcOver(half4 src, half4 dst) {\n" +
        "  return half4(src.rgb + dst.rgb * (1.0 - src.a), src.a + (1.0 - src.a) * dst.a);\n" +
        "}\n" +
        "\n" +
        "half4 main(float2 fragCoord) {\n" +
        "  float2 p = fragCoord - center;\n" +
        "  float sd = sdfRect(p, radius);\n" +
        "  float2 uv = fragCoord;\n" +
        "  float rim = 0.0;\n" +
        "  float lift = 0.0;\n" +
        "  if (sd < 0.0) {\n" +
        "    float sdX = sdfRect(p + float2(1.0, 0.0), radius);\n" +
        "    float sdY = sdfRect(p + float2(0.0, 1.0), radius);\n" +
        "    float2 grad = float2(sdX - sd, sdY - sd);\n" +
        "\n" +
        "    float n_cos = max(thickness + sd, 0.0) / thickness;\n" +
        "    float n_cos2 = n_cos * n_cos;\n" +
        "    float n_sin = sqrt(1.0 - n_cos2);\n" +
        "    float3 normal = normalize(float3(grad * n_cos, n_sin));\n" +
        "\n" +
        "    float3 refract_vec = refract(float3(0.0, 0.0, -1.0), normal, 1.0 / refract_index);\n" +
        "    float h = sd < -thickness ? thickness : sqrt(sd * (-2.0 * thickness - sd));\n" +
        "    float refract_length = (h + 8.0 * thickness) / -refract_vec.z;\n" +
        "    uv += refract_vec.xy * refract_length * refract_intensity;\n" +
        "\n" +
        "    // Edge highlight: strongest on the edge, fading over rim_width; stronger on the edges facing\n" +
        "    // towards and away from the light (opposite edges glint, like the iOS glass).\n" +
        "    float edge = clamp(1.0 + sd / max(rim_width, 0.001), 0.0, 1.0);\n" +
        "    float g = length(grad);\n" +
        "    float facing = g > 0.0001 ? abs(dot(grad / g, normalize(light_dir))) : 0.0;\n" +
        "    rim = rim_strength * edge * edge * (0.35 + 0.65 * facing * facing);\n" +
        "    // Inner glow: the bevel lifts the backdrop slightly towards white.\n" +
        "    lift = matte + 0.12 * rim_strength * (1.0 - n_cos);\n" +
        "  }\n" +
        "\n" +
        "  half4 c = img.eval(uv);\n" +
        "  if (sd < 0.0) {\n" +
        "    half luma = dot(c.rgb, half3(0.299, 0.587, 0.114));\n" +
        "    c.rgb = mix(half3(luma), c.rgb, half(1.0 + saturation));\n" +
        "    c.rgb += half(lift) * (c.a - c.rgb);\n" +
        "  }\n" +
        "  half4 res = srcOver(half4(foreground_color_premultiplied), c);\n" +
        "  if (rim > 0.0) {\n" +
        "    res = srcOver(half4(half(rim)), res);\n" +
        "  }\n" +
        "  return res;\n" +
        "}\n";
}
