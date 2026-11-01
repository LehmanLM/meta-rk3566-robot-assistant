# libc headers 用本地 src/kernel（同游戏机模式，避免 GitHub）
# SRCREV 跟随内核 dts commit（uapi 与内核树一致）
SRC_URI = "git://${TOPDIR}/../src/kernel;protocol=file;nobranch=1;"
SRCREV = "29329df392c9237f7c21523b2af8fe9bc625169d"
