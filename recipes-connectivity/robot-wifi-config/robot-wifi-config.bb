# 机器人助手 WiFi 开机自联网：
# wpa_supplicant@wlan0（关联）+ systemd-networkd（DHCP）+ systemd-resolved（DNS）
# 说明：三个 unit 属别的包（wpa-supplicant/systemd），不能走 SYSTEMD_SERVICE
# （systemd bbclass 校验要求单元随本 recipe 装出），
# 用 rootfs 期离线 systemctl enable
SUMMARY = "Robot assistant WiFi auto-connect configuration"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# 真实 WiFi 凭证不入 git：files/wpa_supplicant-wlan0.conf.local 存在则
# 优先使用（已 gitignore），否则用占位模板 wpa_supplicant-wlan0.conf
WIFI_CONF = "${@'wpa_supplicant-wlan0.conf.local' \
	if os.path.exists(d.getVar('THISDIR') + '/files/wpa_supplicant-wlan0.conf.local') \
	else 'wpa_supplicant-wlan0.conf'}"

SRC_URI = "file://${WIFI_CONF} \
           file://80-wlan0.network \
           file://timesyncd.conf \
           file://robot-wifi-tune.service \
           file://wifi-tune.sh \
"

S = "${WORKDIR}"

do_install() {
	install -d ${D}${sysconfdir}/wpa_supplicant
	install -m 0600 ${WORKDIR}/${WIFI_CONF} \
		${D}${sysconfdir}/wpa_supplicant/wpa_supplicant-wlan0.conf
	install -d ${D}${sysconfdir}/systemd/network
	install -m 0644 ${WORKDIR}/80-wlan0.network \
		${D}${sysconfdir}/systemd/network/80-wlan0.network
	# NTP 对时（板无 RTC 电池，时钟漂移会导致 TLS 证书校验失败）
	# drop-in 方式覆盖，避免与 systemd-conf 的 timesyncd.conf 冲突
	install -d ${D}${sysconfdir}/systemd/timesyncd.conf.d
	install -m 0644 ${WORKDIR}/timesyncd.conf \
		${D}${sysconfdir}/systemd/timesyncd.conf.d/10-ntp.conf
	# 关 WiFi 省电（BCM43430 省电模式会周期性掉链）
	install -d ${D}${sbindir} ${D}${systemd_system_unitdir}
	install -m 0755 ${WORKDIR}/wifi-tune.sh ${D}${sbindir}/robot-wifi-tune.sh
	install -m 0644 ${WORKDIR}/robot-wifi-tune.service \
		${D}${systemd_system_unitdir}/robot-wifi-tune.service
}

pkg_postinst:${PN}() {
	if [ -n "$D" ]; then
		OPTS="--root=$D"
	else
		OPTS=""
	fi
	systemctl $OPTS enable wpa_supplicant@wlan0.service
	systemctl $OPTS enable systemd-networkd.service
	systemctl $OPTS enable systemd-resolved.service
	systemctl $OPTS enable systemd-timesyncd.service
}

FILES:${PN} = "${sysconfdir}/wpa_supplicant/wpa_supplicant-wlan0.conf \
               ${sysconfdir}/systemd/network/80-wlan0.network \
               ${sysconfdir}/systemd/timesyncd.conf.d/10-ntp.conf \
               ${sbindir}/robot-wifi-tune.sh \
               ${systemd_system_unitdir}/robot-wifi-tune.service \
"

inherit systemd

# 自有单元，可以也必须声明给 bbclass 校验
SYSTEMD_SERVICE:${PN} = "robot-wifi-tune.service"

RDEPENDS:${PN} = "wpa-supplicant systemd iw"
