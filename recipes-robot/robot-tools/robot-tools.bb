SUMMARY = "Robot assistant board-side debug tools"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

DEPENDS = "rockchip-mpp"

SRC_URI = "file://iqfeed.c \
           file://camcat.c \
           file://mpenc.c \
"

S = "${WORKDIR}"

do_compile() {
	${CC} ${CFLAGS} -I${STAGING_INCDIR}/rockchip ${LDFLAGS} -o iqfeed iqfeed.c
	${CC} ${CFLAGS} -I${STAGING_INCDIR}/rockchip ${LDFLAGS} -o camcat camcat.c
	${CC} ${CFLAGS} -I${STAGING_INCDIR}/rockchip ${LDFLAGS} -o mpenc mpenc.c -lrockchip_mpp
}

do_install() {
	install -d ${D}${bindir}
	install -m 0755 iqfeed ${D}${bindir}/iqfeed
	install -m 0755 camcat ${D}${bindir}/camcat
	install -m 0755 mpenc ${D}${bindir}/mpenc
}

FILES:${PN} = "${bindir}/iqfeed ${bindir}/camcat ${bindir}/mpenc"
