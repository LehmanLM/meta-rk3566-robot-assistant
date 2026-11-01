#!/bin/sh
# rk809-codec 默认音频路由（泰山派机器人助手）
# 声卡0 = rockchiprk809co；值经 2026-10-12/13 板上扫参实测：
#   numid=13 Capture MIC Path = 1 (Main Mic)   —— 默认 0=MIC OFF，采集全零
#   numid=16 ADC Capture Volume = 255/255       —— 160 及以下近静音（-44dB 级）
#   numid=18 ADC PGA Gain = 8/15                —— 关键：≥12 引入巨量宽带自噪声
#     （15: 底噪~1930 → 8: ~250，信噪比净赚 ~9dB；KWS 命中率 0%→80%+）
#   numid=17 MIC Boost Gain = 3/3
#   numid=12 Playback Path = 2 (SPK)            —— 默认 0=OFF，喇叭无声
#   numid=15 DAC Playback Volume = 240/240      —— 默认 203 偏小；板上实测
#     240 以上写不进（Invalid argument），203→240 约 +3.5dB
i=0
while ! amixer -c 0 cget numid=13 >/dev/null 2>&1; do
    i=$((i + 1))
    [ $i -ge 20 ] && echo "robot-audio: card0 not ready" >&2 && exit 1
    sleep 0.5
done
amixer -c 0 cset numid=13 1  >/dev/null
amixer -c 0 cset numid=16 255 >/dev/null
amixer -c 0 cset numid=18 8   >/dev/null
amixer -c 0 cset numid=17 3   >/dev/null
amixer -c 0 cset numid=12 2   >/dev/null
amixer -c 0 cset numid=15 240 >/dev/null
