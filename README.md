# 📣 Smart Notifire Rev2

## 🧠 このアプリについて

スマートフォンの多くのアプリは「通知音」で通知を知らせます。しかし通知音だけでは、**どのアプリから・どんな内容の通知なのか**は分かりにくいことがほとんどです。

**Smart Notifire** は、

* アプリ名
* 必要に応じて通知タイトルの一部

といったユーザーが指定した条件に一致した通知が届いたとき、 **あらかじめ登録した文章を音声で読み上げる**アプリです。

画面を見なくても、通知の概要を耳で把握できる。それがこのアプリの目的です。

※ マナーモード時は音声案内をしません。

---

## 🧩 動作の仕組み

1. アプリ起動後、通知をモニタし、必要最小限<sup>※1</sup>の情報をアプリ内に記録します。
2. 記録された通知をもとに、ユーザーは音声案内を行うアプリを決めます。また必要に応じて通知タイトルの一部に一致する条件も指定できます。
3. 決めたアプリの通知を受信した場合、**通知音の約3秒後**に音声案内を行います。

通知音と音声が重ならないため、落ち着いて内容を把握できます。

<sup>※1</sup> 保存する通知情報をご覧ください。

---

## ⚠️ 通知の取り扱いについて

通知には、個人情報や機密情報が含まれる場合があります。 そのため **Smart Notifire** では、次の方針を厳守しています。

### 📁 保存する通知情報

保存するのは、通知を識別するための最小限の情報のみです。

* アプリのパッケージ名
* アプリ名(パッケージ名から可読可能な名称に変換したもの。例：Gmail, YouTube)
* 通知チャンネルID

タイトルやメッセージ内容を含む上記以外の情報は保存しません。

### 🚫 取り扱えない通知

このアプリは、記録した通知ログから音声案内ルールを作成します。  
よって、通知ログ記録時にアプリ名<sup>※2</sup>が取得できない通知は使用しません。

<sup>※2</sup> パッケージ名から可読できるアプリの名称です。

### ✔️ 必要な権限

本アプリを利用するには、次の権限が必要です。

1. 通知へのアクセス権限
2. 通知の表示に関する権限

権限が付与されていない場合、本アプリは動作しません。

---

## 🔤 使い方

詳しい使い方は GitHub Pages のドキュメントをご覧ください。

👉 [Smart Notifire Rev2 使い方ガイド](https://teyandei.github.io/SmartNotifire-Rev2/ja/)

---

## 🧭 設計書との用語について

本 README では、設計書（SmartNotifire-Rev2.md）と比べて **ユーザーに分かりやすい表現**を優先しています。

* 「通知検出ルール」 → 「音声案内ルール」
* 「NotificationLog」 → 「通知ログ」

といった形で、意味は同じまま言葉を調整しています。

---

## 📂 ディレクトリ構成（抜粋）

```
SmartNotifire-Rev2
├─ app/
│  └─ src/main/java/com/example/smartnotifier
│     ├─ data/        # データ層（Room / DataStore）
│     ├─ core/        # アプリ基盤
│     └─ ui/          # 画面・ViewModel
├─ docs/
│  └─ design/
│     └─ SmartNotifire-Rev2.md
├─ README.md
└─ LICENSE
```

---

## 🪪 ライセンス

本プロジェクトは個人開発アプリ **Smart Notifire Rev2** です。 ライセンスの詳細は、リポジトリ内の `LICENSE` ファイルをご確認ください。

---

# 🌍 English Version

## 📣 About This App

Many smartphone apps notify users using notification sounds.  
However, notification sounds alone often make it difficult to understand **which app sent the notification**.

**Smart Notifire** is an app that reads out a pre-registered voice message when a notification matches user-defined conditions such as:

-   App name

-   _If necessary_, part of the notification title


This allows you to understand the overview of a notification **by sound only**, without looking at the screen.

_Voice guidance is not played while the device is in Silent or Vibrate mode._

----------

## 🧩 How It Works

1.  After the app starts, it monitors notifications and records only the **minimum required information** within the app. <sup>*1</sup>

2.  Based on the recorded notifications, users select which apps should trigger voice guidance.  
    If needed, conditions matching part of the notification title can also be specified.

3.  When a notification from a selected app is received, a voice message is spoken **approximately 3 seconds after** the notification sound.


This prevents notification sounds and voice guidance from overlapping, allowing calm and clear listening.

<sup>*1</sup> See _Stored Notification Information_ below.

----------

## ⚠️ Notification Handling Policy

Notifications may contain personal or sensitive information.  
For this reason, **Smart Notifire** strictly follows the policies below.

### 📁 Stored Notification Information

Only the minimum information required to identify notifications is stored:

-   App package name

-   App name (a human-readable name resolved from the package name, e.g. Gmail, YouTube)

-   Notification channel ID


No other information, including notification titles or message contents, is stored.

----------

### 🚫 Unsupported Notifications

This app creates voice guidance settings based on recorded notification logs.  
Therefore, notifications for which the app name <sup>*2</sup> cannot be obtained at the time of logging are not supported.

<sup>*2</sup> The app name refers to a human-readable application name resolved from the package name.

----------

### ✔️ Required Permissions

The following permissions are required to use this app:

1.  Notification access permission

2.  Permission related to notification display


If these permissions are not granted, this app will not function.

----------

## 🔤 How to Use

For detailed usage instructions, please see the GitHub Pages documentation.

👉 [Smart Notifire Rev2 User Guide](https://teyandei.github.io/SmartNotifire-Rev2/)

----------

## 🧭 Terminology Notes

In this README, user-friendly terms are prioritized over strict design document terminology.

For example:

-   “Notification Detection Rule” → “Voice Guidance Rule”

-   “NotificationLog” → “Notification Log”


The meanings remain the same.

----------

## 📂 Directory Structure (Excerpt)

```
SmartNotifire-Rev2  
├─ app/  
│  └─ src/main/java/com/example/smartnotifier  
│     ├─ data/ # Data layer (Room / DataStore)   
│     ├─ core/ # Application core  
│     └─ ui/   # UI and ViewModels  
├─ docs/  
│  └─ design/  
│     └─ SmartNotifire-Rev2.md  
├─ README.md  
└─ LICENSE
```  

----------

## 🪪 License

**Smart Notifire Rev2** is a personal development project.  
Please refer to the `LICENSE` file in this repository for license details.
