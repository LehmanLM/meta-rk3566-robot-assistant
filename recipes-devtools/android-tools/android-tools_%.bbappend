# adbd only: trim adb/fastboot/ext4_utils/mkbootimg to cut build deps and size.
FILESEXTRAPATHS:prepend := "${THISDIR}/android-tools:"

TOOLS = "adbd"

SRC_URI += "file://99-adbd-always.conf"

do_install:append() {
	install -d ${D}${systemd_unitdir}/system/android-tools-adbd.service.d
	install -m0644 ${WORKDIR}/99-adbd-always.conf \
		${D}${systemd_unitdir}/system/android-tools-adbd.service.d/
}

FILES:${PN}-adbd += "${systemd_unitdir}/system/android-tools-adbd.service.d/"
