SUMMARY = "Robot audio mixer defaults for rk809 codec (TaishanPi)"
DESCRIPTION = "Applies known-good ALSA mixer settings (mic capture path, \
ADC gains, speaker playback path) at boot. Without this the rk809 codec \
defaults to MIC OFF / playback OFF after every boot."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://robot-audio-config.service \
           file://robot-audio-config.timer \
           file://robot-audio-defaults.sh \
           file://asound.conf"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${sbindir} ${D}${systemd_system_unitdir} ${D}${sysconfdir}
    install -m 0755 ${WORKDIR}/robot-audio-defaults.sh ${D}${sbindir}/robot-audio-defaults.sh
    install -m 0644 ${WORKDIR}/robot-audio-config.service ${D}${systemd_system_unitdir}/robot-audio-config.service
    install -m 0644 ${WORKDIR}/asound.conf ${D}${sysconfdir}/asound.conf
    install -m 0644 ${WORKDIR}/robot-audio-config.timer \
        ${D}${systemd_system_unitdir}/robot-audio-config.timer
}

RDEPENDS:${PN} = "alsa-utils-amixer"

inherit systemd

SYSTEMD_SERVICE:${PN} = "robot-audio-config.service robot-audio-config.timer"

pkg_postinst:${PN}() {
    if [ -n "$D" ]; then
        OPTS="--root=$D"
    fi
    systemctl $OPTS enable robot-audio-config.service
}
