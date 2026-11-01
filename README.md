# meta-rk3566-robot-assistant

泰山派（RK3566）机器人助手 Yocto layer——大模型语音对话（本地 KWS 唤醒 →
ASR → LLM → TTS）、LVGL 表情 UI、相机实时预览、WiFi 开机自联网。

## 依赖

- poky（scarthgap / Yocto 5.0.x）
- meta-rockchip
- meta-openembedded（openembedded-layer）

## 构建

```bash
export TEMPLATECONF=$PWD/meta/meta-rk3566-robot-assistant/conf/templates/default
source poky/oe-init-build-env build-robot-assistant
bitbake rk3566-robot-assistant-image
```

产物为 update.img（RKFW 格式，Windows RKDevTool 线刷）。

## WiFi 凭证

真实 WiFi 配置不入库：

```bash
cd recipes-connectivity/robot-wifi-config/files/
cp wpa_supplicant-wlan0.conf wpa_supplicant-wlan0.conf.local
# 编辑 .local 填入真实 ssid/psk（已被 .gitignore 忽略）
```

recipe 检测到 `.local` 文件会自动优先使用，否则装占位模板。

## 注意

- `robot-voice` / `robot-ui` 等配方（recipe）的 SRCREV 指向**本地方源码仓**
  （含模型数据与厂商标定衍生文件，不公开）——公开克隆本仓库仅供
  layer 结构 / 集成方式参考，无法直接完整构建
- sherpa-onnx aarch64 wheel 与 KWS 唤醒词模型（openzipformer）体积原因
  直接收录在 files/ 下
- 硬件：泰山派 RK3566 + OV5695 摄像头 + AP6212A1 WiFi（2.4G）+ RK809 codec
