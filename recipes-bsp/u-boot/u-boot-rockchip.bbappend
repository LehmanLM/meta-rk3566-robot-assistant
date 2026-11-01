# 泰山派 RK3566 u-boot：复用游戏机的本地 src/u-boot（JeffyCN 2017.09 全量树，
# 含 rk3568_defconfig）+ prebuilt/rkbin（DDR v1.23 / bl31 / miniloader）
# 与游戏机的差异仅 LOADER_INI：RK3566 板必须用 RK3566MINIALL.ini
# （make.sh 默认按 chip 挑 RK3568MINIALL.ini → 加载 rk3568 DDR bin 会失败）

FILESEXTRAPATHS:prepend := "${THISDIR}/u-boot-rockchip:"

SRC_URI = " \
    git://${TOPDIR}/../src/u-boot;protocol=file;branch=master; \
    git://${TOPDIR}/../prebuilt/rkbin;protocol=file;branch=master;name=rkbin;destsuffix=rkbin; \
"

SRCREV = "c41fd9619952d49769affecfb9686ecd01f4fcda"
SRCREV_rkbin = "58aa4f038ddd89bf7f4c0efae3e57d11c67ad8ad"

do_configure:append() {
    sed -i '/^CONFIG_LOADER_INI=/d' ${B}/.config
    echo 'CONFIG_LOADER_INI="RK3566MINIALL.ini"' >> ${B}/.config
}
