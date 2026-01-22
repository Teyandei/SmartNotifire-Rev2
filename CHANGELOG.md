## CL-0003 無効通知の整理 2026-01-22

### 現象

- 追加ボタンクリックによる通知ログ表示で、アプリ名に変換できず、パッケージ名＋デフォルトアイコンのレコードがある。

### 原因

- PackageManager.NameNotFoundException例外が発生するも、パッケージ名とデフォルトアイコンを表示するコードになっていた。
- 以下の点が直接原因と考えられる。
  1. プリインストール系、一時的なメーカーアプリ、disabled / hidden 状態のアプリは、getApplicationInfo(packageName, 0) が 例外を投げる or null相当 になることがある
  2. 表示時点でアンインストール/無効化されて、後から変換が失敗
  3. 別ユーザー（Work profile / サブユーザー）由来の通知
  4. パッケージは見えるがリソース参照が失敗（まれに Resources.NotFoundException 系）

### 対処
- PackageManager.NameNotFoundException例外が発生した場合は、通知ログの該当行削除依頼をNotificationLogAdapterに追加
- NotificationLogAdapter->MainFragment->MainViewModel->NotificationLogRepository->NotificationDaoで該当行を削除
- Ruleに既にある無効なレコードに対してenabledをオフにする処理を追加。アプリ名に無効であることを表示。（削除判断はユーザーに任せる）
- その他クリティカルバグの修正

## CL-0002 プライバシーポリシー変更 - 2026-01-20

### 変更内容
- Google Play Storeのデフォルト言語が`ja-JP`のため、プライバシーポリシーを日本語版リンクに変更。よって下記の修正をした。
  1. 日本語プライバシーポリシーに英語版へのリンクを文末に追加。
  2. 英語版プライバシーポリシーに日本語版へのリンクを文末に追加
- フィーチャーグラフィックを追加


## CL-0001 Internal Test Start - 2026-01-19

### 変更内容
- ソースコード内のクラス・メソッドに KDoc コメントを追加
- 追加ボタンで表示する通知ログリストに✕(閉じる)ボタンを追加。
- 追加ボタンでダブルタップする新しいルールの重複タイトル自動生成をカウントから、50回ループによるナンバリングに変更。
- 同上において、ナンバリングの表示形式を`(n)`から`-#nn`に変更。
- MainViewModel内の日本語出力部分を国際化対応。
  1. MainViewModelにsealed interface AddRuleFromLogEventを追加
  2. MainViewModelでは文言を生成せず、AddRuleFromLogEvent として結果のみを通知（StateFlowを公開）
  3. MainFragmentで購読して、string.xmlによる国際対応メッセージに変換してSnackBarに出力