# Pin xserver-xorg SRCREV（meta-rockchip 用 AUTOREV，解析期需联网查 GitHub，
# 本机 git 直连 github 不通会导致 bitbake -p 失败）。固定为分支 21.1.13 头，
# robot 镜像不含 X，仅保证解析通过；源码 fetch 留给需要时的网络环境。
SRCREV = "10176418b54300446a342b643294181a5fb76d17"
