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