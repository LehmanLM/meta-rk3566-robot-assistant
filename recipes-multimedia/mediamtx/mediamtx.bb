# mediamtx RTSP/HLS 流媒体服务器（官方 arm64 静态二进制）
# 二进制不入 git（37M，见 files/.gitignore）；来源：
# https://github.com/bluenviron/mediamtx/releases v1.9.0 linux_arm64
# cam-stream.service = robot-ui 相机页 fifo 的 ffmpeg 读者（-c copy 转 RTSP）
SUMMARY = "mediamtx streaming server + camera fifo reader"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://mediamtx \
           file://mediamtx.yml \
           file://mediamtx.service \
           file://cam-stream.service \
"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${bindir} ${D}${sysconfdir}
    install -m 0755 mediamtx ${D}${bindir}/mediamtx
    install -m 0644 mediamtx.yml ${D}${sysconfdir}/mediamtx.yml
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 mediamtx.service ${D}${systemd_system_unitdir}/
    install -m 0644 cam-stream.service ${D}${systemd_system_unitdir}/
}

inherit systemd

SYSTEMD_SERVICE:${PN} = "mediamtx.service cam-stream.service"

RDEPENDS:${PN} = "ffmpeg"

FILES:${PN} = "${bindir}/mediamtx ${sysconfdir}/mediamtx.yml"
