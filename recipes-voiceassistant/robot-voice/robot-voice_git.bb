# 泰山派机器人助手语音链（va/：免提对话 ASR/LLM/TTS + 可选打断）
# 源 = 本地 src/voice-assistant（git 协议 file://，SRCREV 必须 rev-parse 直出）
SUMMARY = "GLM voice interaction for robot assistant (hands-free loop)"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "git://${TOPDIR}/../src/voice-assistant;protocol=file;nobranch=1"
SRCREV = "498c987ffa805f304f68c21387ef1fa7d36dfed5"

S = "${WORKDIR}/git"

inherit python3-dir

# 镜像装完整 python3 元包（含 modules 全部标准库）；
# numpy 供 --barge-in 的 NlmsAec（缺则自动降级普通播放）
RDEPENDS:${PN} = "python3"
RRECOMMENDS:${PN} = "python3-numpy \
    python3-sherpa-onnx \
    kws-model \
"

do_install() {
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}
    cp -r ${S}/va ${D}${PYTHON_SITEPACKAGES_DIR}/va
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/va/__pycache__

    install -d ${D}${bindir}
    cat > ${D}${bindir}/robot-voice <<'EOF'
#!/bin/sh
exec python3 -m va.board_main "$@"
EOF
    chmod 0755 ${D}${bindir}/robot-voice

    # 开机自启语音链；ZHIPU_API_KEY 等敏感配置放 /etc/robot-voice.env
    # （不随镜像分发，板上手写；占位 env 防止缺文件起不来）
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/robot-voice.service ${D}${systemd_system_unitdir}/
    install -d ${D}${sysconfdir}
    printf 'ZHIPU_API_KEY=PLACEHOLDER\n' > ${D}${sysconfdir}/robot-voice.env
}

inherit systemd
SYSTEMD_SERVICE:${PN} = "robot-voice.service"
# 语音链由 robot-ui 语音页门控（进页 start/退页 stop），不开机自启
SYSTEMD_AUTO_ENABLE = "disable"

FILES:${PN} = "${PYTHON_SITEPACKAGES_DIR}/va/ ${bindir}/robot-voice \
    ${systemd_system_unitdir}/robot-voice.service ${sysconfdir}/robot-voice.env"
