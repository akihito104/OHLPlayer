OHLPlayer
===

OHLPlayerはAndroidで動作する頭外音像定位 (Out-of-Head sound Localization) デモアプリです。メディア再生エンジンに [ExoPlayer](https://github.com/google/ExoPlayer) を使用しています。作者が自分で使うために作成したものですが、頭外音像定位(バイノーラルなど)に興味をお持ちの方に体験していただけるようOSSとして公開します。

## インストール

```
git clone https://github.com/akihito104/OHLPlayer
cd OHLPlayer
./gradlew installDebug
```

## 動作環境

- Android 4.1+
    - Nexus5X (Android 7.1.2) で動作することを確認しています
- 外部ストレージの読取権限が必要
    - Music, Moviesフォルダ以下のファイルを再生するのに必要です
    - 現在はローカルのファイルにのみ対応しています

## 頭外音像定位とは

頭外音像定位はヘッドホンやイヤホンを用いた高臨場感再生技術の一つで、ヘッドホンやイヤホンで音を聞いているにも関わらず、あたかも目の前にあるスピーカーから音が聞こえてくるように感じさせる技術です。このアプリでは予め測定しておいた作者の頭部伝達関数 (Head-Related Transfer Function) をリアルタイムに音に畳み込んで頭外定位を実現しようとしています。

ヘッドホンやイヤホンでの普段の聞こえ方と、頭外定位の聞こえ方とを比べて体験できるよう、効果のON/OFFを操作するスイッチをプレーヤー画面に配置しています。

## HRTFのインパルス応答の仕様

- サンプリング周波数: 44100Hz, 48000Hz
- ビット長: 64bit (実数型)
- 帯域: 20-20000Hz
- 方向: 受聴者正面の方位角を0度とした時、左30度、右30度
- 距離: 受聴者の頭部中心から1m

## LICENSE

Apache License Version 2.0
