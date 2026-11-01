#!/bin/sh
# 关闭 wlan0 WiFi 省电（BCM43430/AP6212 省电模式与部分路由器配合
# 会导致载波丢失掉链——2026-10-12 实测上连 11 秒后掉、2 分钟后重连）
i=0
while ! [ -d /sys/class/net/wlan0 ]; do
    i=$((i + 1))
    [ $i -ge 40 ] && echo "robot-wifi: wlan0 not present" >&2 && exit 1
    sleep 0.5
done
# 接口出现后稍等驱动就绪
sleep 2
iw dev wlan0 set power_save off || true
