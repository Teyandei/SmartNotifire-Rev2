# 📣 Smart Notifire Rev2

## 🧠 このアプリについて

スマートフォンの多くのアプリは「通知音」で通知を知らせます。しかし通知音だけでは、**どのアプリから・どんな内容の通知なのか**は分かりにくいことがほとんどです。

**Smart Notifire** は、

* アプリ名
* 通知タイトルの一部又は全てのタイトル

といったユーザーが指定した条件に一致した通知が届いたとき、 **あらかじめ登録した文章を音声で読み上げる**アプリです。

画面を見なくても、通知の概要を耳で把握できる。それがこのアプリの目的です。

※ マナーモード時は音声案内をしません。

---

## 🧩 動作の仕組み

1. アプリ起動後、通知をモニタし、必要最小限の情報だけをアプリ内に記録します。
2. 記録された通知をもとに、ユーザーは「音声案内ルール」を作成できます。
3. 通知がルールに一致した場合、**通知音の約3秒後**に音声案内を行います。

通知音と音声が重ならないため、落ち着いて内容を把握できます。

---

## ⚠️ 通知の取り扱いについて

通知には、個人情報や機密情報が含まれる場合があります。 そのため **Smart Notifire** では、次の方針を厳守しています。

### 📁 保存する通知情報

保存するのは、通知を識別するための最小限の情報のみです。

* アプリのパッケージ名
* アプリ名
* 通知チャンネルID
* 通知タイトル

本文やメッセージ内容そのものは保存しません。

### 🚫 取り扱えない通知

このアプリは、記録した通知ログから音声案内ルールを作成します。  
よって、通知ログ記録時にアプリ名^＊^が取得できない通知は使用しません。

^＊^ パッケージ名から可読できるアプリの名称です。

### ✔️ 必要な権限

本アプリを利用するには、次の権限が必要です。

1. 通知へのアクセス権限
2. 通知の表示権限

権限が付与されていない場合、機能は動作しません。

---

## 🔤 使い方

### 初期状態  

![初期画面](./docs/pictures/ja/initialScreen.png)

インストール直後は、**動作確認用の音声案内ルールが1件**登録されています。 このルールを使って、すべての機能を確認できます。

---

### 🔔 通知確認  

![通知確認画面](./docs/pictures/ja/ntf_cheack.png)

画面下部の「通知」ボタンをタップすると、 このアプリ自身がテスト通知を送信します。

* 初回は通知権限の許可画面が表示されます
* 許可後、通知音 → 音声案内の順で再生されます

---

### 🏛️ 音声案内ルール  

![音声案内ルール](./docs/pictures/ja/rule_useage.png)

通知を音声化するためのルール一覧画面です。

#### 🔑 ルールの有効化 ①

スイッチを ON にすると、そのルールが有効になり、ルールが一致した通知に対して、音声案内が行われます。

---

#### 🔎 検索タイトル

通知タイトルに含まれるキーワードを指定します。
1. キーワードを指定したときは、通知タイトルの部分一致。
2. キーワードが空欄のときは、全ての通知タイトル。

---

#### 🎤 音声メッセージ

読み上げたい文章を入力します。 TTS（端末の音声読み上げ機能）を使用します。
音声メッセージを指定しない場合は、「＜アプリ名＞から通知が届きました。」と音声読み上げを行います。

---

#### 📑 ルールのコピー ②

既存ルールをコピーして、新しいルールを作成できます。

* 検索タイトルは重複しないよう、自動で番号が付与されます

---

#### 🗑️ ルールの削除 ③

選択したルールを削除します。 削除前には確認ダイアログが表示されます。

---

#### ▶️ 音声メッセージの再生 ④

登録した音声メッセージを、その場で再生できます。

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

Most smartphone apps notify users using sounds.  
However, notification sounds alone often make it difficult to understand **which app sent the notification** and **what it is about**.

**Smart Notifire** is an app that reads out a pre-registered voice message when a notification matches user-defined conditions such as:

- App name
- Part of, or the entire, notification title

This allows you to understand the notification contents **by sound only**, without looking at the screen.

*Voice guidance is not played while the device is in Silent or Vibrate mode.*

---

## 🧩 How It Works

1. After the app starts, it monitors notifications and records only the minimum information required.
2. Based on the recorded notification logs, users can create *voice guidance rules*.
3. When a notification matches a rule, a voice message is spoken **approximately 3 seconds after** the notification sound.

This prevents the notification sound and voice guidance from overlapping, allowing calm and clear listening.

---

## ⚠️ Notification Handling Policy

Notifications may contain personal or sensitive information.  
For this reason, **Smart Notifire** strictly follows the policies below.

### 📁 Stored Notification Information

Only the minimum information necessary to identify notifications is stored:

- App package name
- App name
- Notification channel ID
- Notification title

The notification message body itself is **never stored**.

---

### 🚫 Unsupported Notifications

This app creates voice guidance rules based on recorded notification logs.  
Therefore, notifications for which the app name* cannot be obtained at the time of logging are not supported.

\* The app name refers to a human-readable application name resolved from the package name.

---

### ✔️ Required Permissions

The following permissions are required to use this app:

1. Notification access permission
2. Notification display permission

If these permissions are not granted, the app will not function.

---

## 🔤 How to Use

### Initial State

![Initial Screen](./docs/pictures/initialScreen.png)

Immediately after installation, **one test voice guidance rule** is registered by default.  
You can use this rule to verify that all features are working correctly.

---

### 🔔 Notification Check

![Notification Check](./docs/pictures/ntf_cheack.png)

Tap the **Notification** button at the bottom of the screen to send a test notification.

- On first use, a notification permission request screen will appear
- After permission is granted, the notification sound plays first, followed by the voice guidance

---

### 🏛️ Voice Guidance Rules

![Voice Guidance Rules](./docs/pictures/rule_useage.png)

This screen displays the list of rules used to convert notifications into voice messages.

---

#### 🔑 Enable Rule ①

Turning the switch ON activates the rule.  
When a notification matches the rule conditions, voice guidance is played.

---

#### 🔎 Search Title

Specify keywords included in the notification title.

1. When a keyword is specified, partial matching is applied.
2. When the field is empty, **all notification titles** match.

---

#### 🎤 Voice Message

Enter the text you want to be read aloud.  
Text-to-Speech (TTS) provided by the device is used.

If no voice message is specified, the app will read:

> “A notification has arrived from \<App Name\>.”

---

#### 📑 Copy Rule ②

You can copy an existing rule to create a new one.

- A number is automatically appended to the search title to avoid duplication

---

#### 🗑️ Delete Rule ③

Deletes the selected rule.

- A confirmation dialog is shown before deletion

---

#### ▶️ Play Voice Message ④

Plays the registered voice message immediately for confirmation.

---

## 🧭 Terminology Notes

In this README, user-friendly terminology is prioritized over strict design document terms.

For example:

- “Notification Detection Rule” → “Voice Guidance Rule”
- “NotificationLog” → “Notification Log”

The meanings remain the same.

---

## 📂 Directory Structure (Excerpt)

```
SmartNotifire-Rev2
├─ app/
│ └─ src/main/java/com/example/smartnotifier
│ ├─ data/ # Data layer (Room / DataStore)
│ ├─ core/ # Application core
│ └─ ui/ # UI and ViewModels
├─ docs/
│ └─ design/
│ └─ SmartNotifire-Rev2.md
├─ README.md
└─ LICENSE

```
---

## 🪪 License

**Smart Notifire Rev2** is a personal development project.  
Please refer to the `LICENSE` file in this repository for license details.