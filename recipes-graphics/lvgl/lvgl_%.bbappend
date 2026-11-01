# robot-ui renders XRGB8888 straight into rockchipdrmfb (32bpp) — no
# rotation/scaling anywhere, flush_cb is a plain row memcpy.
PACKAGECONFIG = ""

LVGL_CONFIG_LV_COLOR_DEPTH = "32"

# system malloc (see game-player lvgl bbappend for the pool-overflow saga)
do_configure:append() {
    sed -r \
        -e "s|^([[:space:]]*#define LV_USE_STDLIB_MALLOC[[:space:]]+).*|\1LV_STDLIB_CLIB|" \
        -i "${S}/lv_conf.h"
}

# default theme fonts for LV_SYMBOL_* glyphs (back arrow etc.)
# 注意：LV_DRAW_SW_ASM_NEON 的 lv_blend_neon.S 是 32 位 ARM 汇编
# （.fpu/.syntax/@注释，aarch64 GAS 直接报错）——游戏机层能用是因为
# RK3506B 是 32 位；本层目标是 aarch64，不开 NEON，纯 C 足够。
do_configure:append() {
    sed -r \
        -e "s|^([[:space:]]*#define LV_FONT_MONTSERRAT_16[[:space:]]).*|\1 1|" \
        -i "${S}/lv_conf.h"
}
