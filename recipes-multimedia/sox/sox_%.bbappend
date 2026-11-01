# 机器人 TTS 链需 mp3 解码（edge-tts 兜底引擎输出 mp3）
# 拒绝 pulseaudio：DISTRO_FEATURES_BACKFILL 会把 pulseaudio 回填进
# DISTRO_FEATURES（bitbake.conf），sox 的 filter 会命中而拖出
# pulseaudio→avahi→bluez5→ofono→icu/cairo 全家桶（+1300 任务）
PACKAGECONFIG:append = " mad"
PACKAGECONFIG:remove = "pulseaudio"
