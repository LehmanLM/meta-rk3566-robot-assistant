# sherpa-onnx（本地唤醒词 KWS 引擎）+ onnxruntime C 库
# 预编译 aarch64 方案：PyPI wheel 本地化（本机连不上 github，
# 无法取 k2-fsa 官方 linux-aarch64 tarball；PyPI 可达）
#
# 依赖拼接（2026-10-13 实测）：
# - wheel 不带 libonnxruntime.so（动态依赖），且链接的是 k2-fsa
#   fork 的符号版本 VERS_1.28.2；PyPI 无 1.28.2 → 用 1.31.0 的
#   libonnxruntime + do_install 内补丁把 verneed 的版本字符串与
#   vna_hash 一起改到 1.31.0（字符串与 hash 必须同步改，
#   只改字符串 ld.so 仍报 version not found）
# - 运行库：libstdc++ / libgcc（RDEPENDS）；libasound 已随 wheel
#   （sherpa_onnx.libs/，auditwheel rpath）
SUMMARY = "sherpa-onnx streaming KWS engine (prebuilt aarch64 wheels)"
LICENSE = "Apache-2.0 & MIT"
LIC_FILES_CHKSUM = "file://${WORKDIR}/LICENSE.apache-2.0;md5=3b83ef96387f14655fc854ddc3c6bd57 \
                    file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://sherpa_onnx-1.13.8-cp312-aarch64.whl \
           file://libonnxruntime.so.1.31.0 \
           file://LICENSE.apache-2.0"

S = "${WORKDIR}/sources"
UNPACKDIR = "${WORKDIR}/sources"

inherit python3-dir

do_install() {
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}
    python3 -m zipfile -e ${WORKDIR}/sherpa_onnx-1.13.8-cp312-aarch64.whl \
        ${D}${PYTHON_SITEPACKAGES_DIR}/

    # verneed 补丁：VERS_1.28.2 → VERS_1.31.0（字符串 + vna_hash）
    python3 - <<'PYEOF'
import glob, struct

sos = glob.glob("${D}${PYTHON_SITEPACKAGES_DIR}/sherpa_onnx/lib/_sherpa_onnx*.so")
assert len(sos) == 1, sos
p = sos[0]
d = bytearray(open(p, 'rb').read())

def elf_hash(name):
    h = 0
    for c in name:
        h = (h << 4) + c
        g = h & 0xF0000000
        if g:
            h ^= g >> 24
        h &= ~g & 0xFFFFFFFF
    return h

assert d.count(b'VERS_1.28.2') == 1
d = d.replace(b'VERS_1.28.2', b'VERS_1.31.0')

e_shoff = struct.unpack_from('<Q', d, 0x28)[0]
e_shentsize = struct.unpack_from('<H', d, 0x3A)[0]
e_shnum = struct.unpack_from('<H', d, 0x3C)[0]

def sh(i):
    o = e_shoff + i * e_shentsize
    vals = struct.unpack_from('<IIQQQQIIQQ', d, o)
    return dict(off=vals[4], size=vals[5], link=vals[6], typ=vals[1])

patched = 0
for i in range(e_shnum):
    s = sh(i)
    if s['typ'] != 0x6ffffffe:
        continue
    dynstr = sh(s['link'])
    o, end = s['off'], s['off'] + s['size']
    while o < end:
        _, cnt, _, aux, nxt = struct.unpack_from('<HHIII', d, o)
        ao = o + aux
        for _ in range(cnt):
            if ao + 8 > end:
                break
            _, _, _, name = struct.unpack_from('<IHHI', d, ao)
            so = dynstr['off'] + name
            try:
                e = d.index(b'\0', so)
                nstr = bytes(d[so:e])
            except ValueError:
                nstr = b''
            if nstr == b'VERS_1.31.0':
                struct.pack_into('<I', d, ao, elf_hash(b'VERS_1.31.0'))
                patched += 1
            ao += 8
        if nxt == 0:
            break
        o += nxt
assert patched == 1, patched
open(p, 'wb').write(d)
print("verneed patched")
PYEOF

    install -d ${D}${libdir}
    install -m 0755 ${WORKDIR}/libonnxruntime.so.1.31.0 ${D}${libdir}/
    ln -s libonnxruntime.so.1.31.0 ${D}${libdir}/libonnxruntime.so
}

# 无版本号 libonnxruntime.so 软链会被 PN-dev 的默认 FILES 抢走
# （PACKAGES 里 -dev 排在 PN 前）→ 拆 libs 子包并前置
PACKAGES =+ "${PN}-libs"
FILES:${PN}-libs = "${libdir}/libonnxruntime*"
FILES:${PN} = "${PYTHON_SITEPACKAGES_DIR}/sherpa_onnx*"

# 预编译 aarch64 通用二进制：放行架构/ldflags/dev-so/已剥离检查
INSANE_SKIP:${PN} += "arch ldflags dev-so already-stripped file-rdeps"
INSANE_SKIP:${PN}-libs += "arch ldflags dev-so already-stripped"
# libonnxruntime.so.1.31.0 主版本 1：无 soname 缺失问题（有 soname）
RDEPENDS:${PN} += "libstdc++ libgcc ${PN}-libs"
