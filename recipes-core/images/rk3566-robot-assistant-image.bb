# RK3566 机器人助手系统镜像（SD 卡启动，不动 eMMC 出厂 Android）
# 产出：wic 镜像（idblock + uboot + trust + boot + ext4 rootfs），
# dd 到 TF 卡插入板上卡槽即启动

require recipes-core/images/core-image-minimal.bb

DESCRIPTION = "RK3566 robot assistant image (TaishanPi, SD card boot)"

# boot 分区固定 64MiB（本层 wic/robot-gptdisk.wks.in），不再随内核体积顶格
WKS_FILE = "robot-gptdisk.wks.in"

IMAGE_FEATURES += "ssh-server-openssh"

# 首启验证集：串口/shell 已由 debug-tweaks 覆盖；WiFi/音频工具供板上联调
# WiFi：实测 AP6212A1（BCM43430A1），固件由 rkwifibt-firmware bbappend
# 补装 brcm/brcmfmac43430-sdio.*；robot-wifi-config 开机自动联网
IMAGE_INSTALL += " \
    android-tools-adbd \
    android-tools-conf-rockchip \
    wpa-supplicant \
    robot-wifi-config \
    alsa-utils \
    rkwifibt-firmware \
    robot-voice \
    robot-audio-config \
    robot-ui \
    python3 \
    python3-numpy \
    python3-edge-tts \
    python3-sherpa-onnx \
    kws-model \
    sox \
    libgpiod \
    libgpiod-tools \
"
