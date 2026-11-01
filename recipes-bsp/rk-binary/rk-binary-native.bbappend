# rk-binary（boot_merger/trust_merger 等打包工具）本地源（同游戏机模式）
SRC_URI = " \
    git://${TOPDIR}/../prebuilt/rkbin;protocol=file;branch=master;name=rkbin \
    git://${TOPDIR}/../prebuilt/rk-tools;protocol=file;branch=master;name=tools;destsuffix=git/extra \
"

SRCREV_rkbin = "58aa4f038ddd89bf7f4c0efae3e57d11c67ad8ad"
SRCREV_tools = "1cfe8c7ea7477cb93fba0b3149e4e4f778f5a1c5"
