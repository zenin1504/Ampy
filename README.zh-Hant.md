# Ampy - 裝置電池資訊檢視器（PlusPlusBattery 分支）

[简体中文](./README.zh.md) | 繁體中文 | [English](./README.md)

## 簡介

`Ampy` 是一款主要針對小米 / Redmi / POCO 裝置的 Android 7.0+（API 24+）輕量電池資訊工具。Logcat 解析優先适配小米，OnePlus/OPlus、摩托羅拉及通用格式作為後備。它提供即時電池狀態監測，並在裝置概覽中顯示電池設計容量。

## 功能

- **即時電池資訊**：緊湊網格顯示電量、溫度、狀態、健康度、循環次數與電壓；電流充電時為正、放電時為負；功率圖表預設展開並保持全寬以便閱讀。
- **重新設計的設定頁**：設定項目依卡片分組，搭配圖示與清楚標籤，更新間隔對話框也更簡潔。
- **常駐通知**：特殊用途前景服務搭配精確鬧鐘看門狗，可在待機或廠商殺掉處理程序後還原鎖屏通知；建議依提示允許精確鬧鐘。
- **裝置與電池概覽**：顯示裝置型號、Android 版本、電池類型，以及來自裝置電源設定的電池設計容量（可用時，後備讀取 logcat）。
- **原生最佳化**：三個支援 16 KB 頁面對齊的 C++ 程式庫分別負責批次解析 logcat、在保留峰值的前提下對長時間功率圖表降取樣，以及以每樣本 O(1) 成本維護滾動功率統計。
- **循環次數歷史記錄**：自動或手動記錄每日的電池循環次數，並使用 Room 資料庫儲存於本機。使用者可在歷史頁面檢視，並匯出為 CSV 檔案。
- **即時電池監控**：在可自訂的懸浮視窗或通知區域中顯示電池指標。可選擇要顯示的項目。

## 實驗性功能
- **從 Logcat 獲取電池資料**：優先解析小米日誌，OPlus/OnePlus、摩托羅拉及通用格式作為後備。Android 會限制應用程式可讀取的日誌，實際可用性因裝置與系統而異。

## 螢幕截圖

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_1.jpg" width="200"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_2.jpg" width="200"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_3.jpg" width="200"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_4.jpg" width="200"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_5.jpg" width="200"/>
</p>

## 下載

[<img src="https://f-droid.org/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/en/packages/com.dijia1124.plusplusbattery/)

或從 [Github Release](https://github.com/zenin1504/Ampy/releases) 下載 APK

## 安裝

1. 小米 / Redmi / POCO 裝置建議使用較新的 HyperOS 或 MIUI；OnePlus/OPlus 裝置使用較新的 ColorOS 或 OxygenOS 也作為後備支援。
2. 前往 Release 頁面下載並安裝 APK。
3. 啟動應用程式。
4. （可選）為了讓通知持續更新，請將 Ampy 的電池最佳化設為「無限制」，並在 HyperOS/MIUI 上啟用「自動啟動」權限；OnePlus/OPlus 後備裝置請在 ColorOS 上啟用「自動啟動」權限。請勿在系統設定中強制停止 Ampy，Android 不允許應用程式在被強制停止後自行啟動。

## 鳴謝

特別感謝 [@shminer](https://github.com/shminer) 提供核心原始碼、演算法邏輯以及有關 FCC 和 SOH 補償的見解。

## 貢獻者
<a href="https://github.com/dijia1124/plusplusbattery/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=dijia1124/plusplusbattery" />
</a>

## 授權條款

本專案採用 MIT 授權條款。詳情請參閱 `LICENSE` 檔案。
