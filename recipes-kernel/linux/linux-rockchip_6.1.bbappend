# 泰山派 RK3566 内核：本地 src/kernel 仓库（vendor 6.1）
# SRCREV 必须跟随 src/kernel 的 commit 更新（含泰山派 dts）
# 注意：本 bbappend 覆盖 SRC_URI/SRCREV/KERNEL_DEVICETREE，
# robot build 的 bblayers 不含 meta-game-player，二者互不影响。
#
# 内核配置真相（2026-10-17）：本 recipe 用 kernel.bbclass（非 kernel-yocto），
# .cfg 片段不生效！实际生效 = src/kernel/arch/arm64/configs/defconfig
# （KBUILD_DEFCONFIG 默认名 "defconfig"，ATK SDK 带入，一直就是它在起作用）。
# 改内核配置必须直接改该 defconfig 并提交 src/kernel。

SRC_URI = "git://${TOPDIR}/../src/kernel;protocol=file;nobranch=1;"

# 508c4c07: defconfig: brcmfmac 内建回归（build33 误留 SDK 的 AP6XXX/BCMDHD）
# a8670ac4: 3.1" MIPI DSI screen + fbcon, drop DEBUG_INFO (boot part size)
# f6a67a8: 3.1" MIPI DSI screen (ST7701S + my_touch + GP7101 bl)
# d438d134: arm64: dts: tspi: fix i2s1 trcm property for capture
# （29329df39: 初版泰山派 dts 移植）
SRCREV = "508c4c073f5b8c82ab60c158d869ecfddb662264"

KERNEL_DEVICETREE = "rockchip/tspi-rk3566-user-v10-linux.dtb"

# 覆盖 meta-rockchip 的 auto-patch 目录（本层不放 patch）
PATCHPATH := "${THISDIR}/linux-rockchip_6.1"
