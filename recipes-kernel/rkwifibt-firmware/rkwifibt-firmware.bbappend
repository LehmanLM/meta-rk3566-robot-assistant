# rkwifibt-firmware：源从 GitHub（JeffyCN mirrors rkwifibt 分支，本机连不上）
# 改为本地 src/rkwifibt（自 lckfb SDK external/rkwifibt 克隆）
# SRCREV 同 meta-rockchip 原始指向，历史可达
SRC_URI = "git://${TOPDIR}/../src/rkwifibt;protocol=file;nobranch=1;"

# 泰山派实测 WiFi 模块 = AP6212A1（BCM43430A1，2.4G only）
# brcmfmac 按 chip id 请求 brcm/brcmfmac43430-sdio.{bin,txt}，
# 原 recipe 只平铺安装 rk 命名固件，这里补装标准命名到 brcm/
# 注意：A1 芯片必须配 nvram_ap6212a.txt（非 A 版 nvram 实测无 RF 输出，扫描全 -96dBm）
do_install:append() {
	install -d ${D}${nonarch_base_libdir}/firmware/brcm/
	install -m 0644 ${S}/firmware/broadcom/AP6212A1/wifi/fw_bcm43438a1.bin \
		${D}${nonarch_base_libdir}/firmware/brcm/brcmfmac43430-sdio.bin
	install -m 0644 ${S}/firmware/broadcom/AP6212A1/wifi/nvram_ap6212a.txt \
		${D}${nonarch_base_libdir}/firmware/brcm/brcmfmac43430-sdio.txt
}
