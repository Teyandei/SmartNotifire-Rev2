---  
title: LINEで友達からのメッセージを音声案内  
layout: default  
---  
# <span style="color: white; background-color: green; padding-left:8px; padding-right: 8px;">LINE</span>で友達からのメッセージを音声案内

LINEのトークで友達からメッセージが届くと通知が送信されます。  
そのときの通知チャンネル名は**メッセージ通知**です。  
そして、通知タイトルは友達の名前<sup>※1</sup>となります。

> ※1 2026年5月現在のLINEの仕組みです。

これを利用して、**特定の友達からのメッセージ通知に音声案内**をつけてみましょう。  

## 🎯 ゴール 

友達の名前を「明」（あきら）さんとしましょう。

## <span style="color: blue; ">明さんからLINEのメッセージが届いたら、「明さんからLINEが届きました」と音声案内する。</span>

## 🌱 *STEP 1* LINEの音声案内ルールを追加する。

1. <span style="color:white; background-color: purple; padding-left:8px; padding-right: 8px;">＋追加</span>ボタンをタップしてLINEの通知を探します。

    <img src="../images/ja/tips_line_add_rule.png" title="Add Rule" alt="ルール追加" style="width:clamp(30%,calc(60000px - 10000vw),80vw);height:auto;max-width:100%;">

2. <span style="background-color: white; "><img src="../icons/add_24.svg" width="16" style="vertical-align: middle; " alt="追加ボタン"></span> をタップしてLINEのルールを追加します。

## 🌷 *STEP 2* 音声案内ルールを編集する  

- 下の画面のとおりに設定します。

    <img src="../images/ja/tips_line_edit_rule.png" title="Add Rule" alt="ルール編集" style="width:clamp(30%,calc(60000px - 10000vw),80vw);height:auto;max-width:100%;">

    > ポイント1  
    > 友達に「明」を含む名前の人がいると、その人も音声案内します。  
    > 明さんだけが対象になるように、[詳細条件](./setting_search_advanced%20.md)(⚙️)で「明」さんだけに絞ることができます。(例：「正明」さんを含まないなど)
    
    > ポイント2  
    > 音声メッセージでは、名前をひらがなで「あきら」とします。  
    > デバイスによっては「明」は「めい」・「あき」と読むかもしれません。
  
以上で設定は終了です。友達からのLINEを待ちましょう📱

[先頭ページ](./index.md)へ
