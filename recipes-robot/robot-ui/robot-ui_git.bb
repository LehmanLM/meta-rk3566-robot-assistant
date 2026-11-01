# 机器人助手 LVGL UI（3.1 寸 480x800 fb0 + evdev 触摸）
# 源 = 本地 src/robot-ui（git 协议 file://，SRCREV 必须 rev-parse 直出）
# va/board_main.py 通过 /tmp/robot-ui.fifo 推送状态与对话文本
SUMMARY = "LVGL UI for robot assistant (480x800, voice + camera launcher)"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "git://${TOPDIR}/../src/robot-ui;protocol=file;nobranch=1"
SRC_URI += "file://sfx"
# sfx/*.wav = Kenney CC0（interface-sounds/digital-audio 精选，ffmpeg 24k mono）
SRCREV = "daa249257427a3c23d5fbc1a981538eb4807fe3e"

S = "${WORKDIR}/git"

DEPENDS = "lvgl rockchip-mpp"

do_compile() {
    ${CC} ${CFLAGS} -pthread ${LDFLAGS} \
        -I${STAGING_INCDIR}/lvgl -I${STAGING_INCDIR}/rockchip -I${S} \
        robot-ui.c lv_font_cn20.c -o robot-ui -llvgl -lm -lrockchip_mpp
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 robot-ui ${D}${bindir}/robot-ui
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/robot-ui.service \
        ${D}${systemd_system_unitdir}/robot-ui.service
    install -d ${D}${datadir}/robot-ui/sfx
    install -m 0644 ${WORKDIR}/sfx/*.wav ${D}${datadir}/robot-ui/sfx/
}

FILES:${PN} += "${datadir}/robot-ui/sfx"

inherit systemd

SYSTEMD_SERVICE:${PN} = "robot-ui.service"
SYSTEMD_AUTO_ENABLE = "enable"
