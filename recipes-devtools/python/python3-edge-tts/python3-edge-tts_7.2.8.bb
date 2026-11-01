# edge-tts（微软免费 TTS 接口封装）：glm-tts 无额度时的兜底引擎
# sdist 本地化入 files/（避免构建期访问 pypi）
SUMMARY = "Microsoft Edge TTS Python package (free TTS fallback)"
LICENSE = "LGPL-3.0-only & MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=970e1dee884af469fb66c11e645b0dab"

SRC_URI = "file://edge_tts-${PV}.tar.gz \
           file://0001-tts-endpoint-api.msedgeservices.com.patch"
SRC_URI[sha256sum] = "fcf185a0d527a0d2d003f9d5841facc1d5e0e7b3b88d5df9c32990402c6b8cd0"

S = "${WORKDIR}/edge_tts-${PV}"

inherit setuptools3

RDEPENDS:${PN} = "python3-aiohttp python3-certifi python3-tabulate \
    python3-typing-extensions python3-asyncio python3-core"

FILES:${PN} = "${PYTHON_SITEPACKAGES_DIR}/edge_tts/ \
    ${PYTHON_SITEPACKAGES_DIR}/edge_tts-*.dist-info/ \
    ${PYTHON_SITEPACKAGES_DIR}/edge_playback/ \
    ${bindir}/edge-tts \
    ${bindir}/edge-playback"
