# 本地唤醒词模型：zipformer-zh-en-3M（k2-fsa，2025-12-20 版）
# 来源 = github release（**走 ghfast.top 代理可拉**，直连被墙）：
# https://github.com/k2-fsa/sherpa-onnx/releases/download/kws-models/
#   sherpa-onnx-kws-zipformer-zh-en-3M-2025-12-20.tar.bz2
# 板上 A/B 实测（2026-10-13）：同条件正常语速 4/4 全中且响应快，
# 旧 wenetspeech-3.3M（ModelScope pkufool）同条件命中明显慢/漏
# —— zh-en 版对正常语速连读更稳。decoder 仅 fp32（模型包未含 int8，
# 760KB 无所谓）；en.phone 不需要
SUMMARY = "Zipformer keyword-spotting model (zh-en 3M, 2025-12-20)"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRC_URI = "file://encoder-epoch-13-avg-2-chunk-16-left-64.int8.onnx \
           file://decoder-epoch-13-avg-2-chunk-16-left-64.onnx \
           file://joiner-epoch-13-avg-2-chunk-16-left-64.int8.onnx \
           file://tokens.txt"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${datadir}/kws-model
    # 显式列文件——曾用 ${WORKDIR}/*.onnx 通配，把旧版本 recipe 在
    # WORKDIR 残留的 wenetspeech 模型一锅端进镜像（增量构建不清理）
    install -m 0644 ${S}/encoder-epoch-13-avg-2-chunk-16-left-64.int8.onnx \
        ${S}/decoder-epoch-13-avg-2-chunk-16-left-64.onnx \
        ${S}/joiner-epoch-13-avg-2-chunk-16-left-64.int8.onnx \
        ${S}/tokens.txt ${D}${datadir}/kws-model/
}

FILES:${PN} = "${datadir}/kws-model/*"
