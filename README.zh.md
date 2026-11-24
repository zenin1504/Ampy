# Ampy - 电池信息查看工具（PlusPlusBattery 分支）

简体中文 | [繁體中文](./README.zh-Hant.md) | [English](./README.md)

## 简介

`Ampy` 是一款主要面向小米 / Redmi / POCO 设备的 Android 7.0+（API 24+）轻量电池信息显示应用。logcat 解析优先适配小米，一加 / 欧加、摩托罗拉及通用格式作为后备。它提供实时电池状态监测，并在设备概览中显示电池设计容量。

## 特性

- **实时电池信息**：紧凑网格显示电量、温度、状态、健康、循环计数和电压；电流充电时为正、放电时为负；功率图表默认展开并保持全宽以便阅读。
- **重新设计的设置页**：设置项按卡片分组，配有图标和清晰标签，刷新间隔弹窗也更简洁。
- **常驻通知**：特殊用途前台服务配合精确闹钟看门狗，可在待机或厂商杀进程后恢复锁屏通知；建议按提示允许精确闹钟。
- **设备与电池概览**：显示设备型号、Android 版本、电池类型，以及来自设备电源配置的电池设计容量（可用时，后备读取 logcat）。
- **原生优化**：三个支持 16 KB 页面对齐的 C++ 库分别负责批量解析 logcat、在保留峰值的前提下对长时间功率图表降采样，以及以每样本 O(1) 的开销维护滚动功率统计。
- **历史循环次数记录**: 自动或手动记录当天的循环次数并记录在本地Room数据库中，用户可在历史页查看，以及导出到csv文件。
- **实时电池监控**: 在悬浮窗或通知栏显示实时电池信息。可以选择需要显示的条目。

## 实验性功能
- **从 logcat 获取电池信息**：优先解析小米日志，欧加、摩托罗拉及通用格式作为后备。Android 会限制应用可读取的日志，实际可用性因设备和系统而异。

## 截图

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_1.jpg" width="200"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_2.jpg" width="200"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_3.jpg" width="200"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_4.jpg" width="200"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_5.jpg" width="200"/>
</p>

## 下载

[<img src="https://f-droid.org/badge/get-it-on-zh-hans.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/en/packages/com.dijia1124.plusplusbattery/)

或从[Github Release](https://github.com/zenin1504/Ampy/releases)页下载APK

## 安装

1. 小米 / Redmi / POCO 设备建议使用较新的 HyperOS 或 MIUI；一加 / 欧加设备使用较新的 ColorOS 或 OxygenOS 也作为后备支持。
2. 前往Release页面下载并安装APK文件。
3. 运行APP。
4. （可选）为了让通知持续刷新，请将 Ampy 的电池优化设为「无限制」，并在 HyperOS/MIUI 上开启「自启动」权限；一加 / 欧加后备设备请在 ColorOS 上开启「自启动」权限。请勿在系统设置中强制停止 Ampy，Android 不允许应用在被强制停止后自行启动。

## 鸣谢

感谢 [@shminer](https://github.com/shminer) 提供对fcc&soh偏移量相关的内核源码、算法和思路。

## 贡献者
<a href="https://github.com/dijia1124/plusplusbattery/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=dijia1124/plusplusbattery" />
</a>

## 开源许可

本项目采用 MIT 许可证。详情请参阅项目中的 `LICENSE` 文件。
