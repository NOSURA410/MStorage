# MStorage (MS)

Minecraft Paper / Purpur 向け
大規模サーバー対応型ストレージ圧縮プラグイン

## 対応バージョン

* Minecraft 1.21.11+
* Paper
* Purpur
* Java 21+

---

# 概要

MStorage (MS) は、

```text
大量アイテムを1スロットへ圧縮保存
```

できる高性能ストレージプラグインです。

特徴：

* Vanillaライク
* 大規模サーバー向けTPS対策
* PDCベース永続化
* Lore同期
* ホッパー負荷軽減
* レッドストーン物流ネットワーク
* AutoCollect
* Container mode
* 完全非コマンド運用

---

# 主な機能

## ストレージ化

```text
チェスト8個 + 中央アイテム
```

で作成。

保存対象：

* ブロック
* 素材
* 一般アイテム

保存禁止：

* 耐久値付きアイテム
* 他MS
* 一部特殊アイテム

---

# 保存容量

最大：

```text
100,000,000 LC
```

対応。

---

# 表示

## アイテム名

```text
MS: ダイヤモンド
```

## Lore

例：

```text
総数: 123456
LC: 35.7
Stacks: 1929
端数: 0
```

---

# AutoCollect

周囲アイテムを自動収納。

## 特徴

* Tick分散
* TPS監視
* Queue処理
* Storage除外
* 範囲限定

---

# Container Mode

チェスト/樽と連携。

## 機能

* コンテナへ収納
* コンテナから搬出
* ストレージ優先
* Inventory保護

---

# レッドストーン物流ネットワーク

最新版で追加。

## 特徴

```text
レッドストーン信号
↓
接続コンテナ探索
↓
対応MSへ自動搬送
```

---

# 対応信号

* ボタン
* レバー
* レッドストーンブロック
* 間接信号
* 直接信号

---

# ネットワーク仕様

## 最大連結数

```text
41LC
```

まで。

## 探索方式

```text
全方向BFS探索
```

* 上下左右前後
* ラージチェスト対応
* 樽対応

---

# 搬送仕様

## 通常アイテム

```text
通常アイテム
↓
対応MSへ収納
```

## MS → MS

```text
搬出元MSの半数のみ搬送
```

---

# TPS対策

## 大規模サーバー向け最適化

* Queueベース処理
* Cooldown
* Lock管理
* Lore更新Queue
* InventoryKey管理
* 同一Inventory多重実行防止
* 変更されたMSのみLore更新
* 信号時のみ動作
* 高速クロック対策
* 搬送元1LC制限
* 探索上限41LC

---

# 搬送演出

搬送成功時：

```text
配送先チェスト/樽が一瞬開閉
```

---

# 安全対策

## 増殖対策

* 自己搬送禁止
* 同一Inventory除外
* 同一物理チェスト除外
* PDC同期
* rollback対応

## 操作ロック

搬送中：

* GUI操作禁止
* ホッパー搬入禁止
* ホッパー搬出禁止

---

# データ管理

## 永続化

PersistentDataContainer (PDC)

## 保存内容

* 保存アイテム
* 保存数
* Auto状態
* モード
* Version
* Owner

---

# パフォーマンス設計

MStorage は：

```text
大量ホッパー常時稼働
```

を前提としない設計です。

代わりに：

```text
信号時のみ処理
```

でTPS負荷を大幅削減しています。

---

# 推奨用途

* 経済サーバー
* 長期運営サーバー
* 大規模生活サーバー
* 資材サーバー
* 工業系サーバー

---

# 非推奨

以下環境では追加調整推奨：

* 超高速レッドストーンクロック
* 数百人同時接続
* 超巨大物流網

---

# 今後予定

* Config強化
* GUI設定
* ログ機能
* 統計機能
* 管理者監視
* 搬送フィルタ
* 優先度制御

---

# ライセンス

Private / All Rights Reserved

---

# 開発環境

* IntelliJ IDEA
* Maven
* Java 21
* Paper API
* Purpur API

---

# GitHub

```text
Source code included
README included
Supported versions documented
Permissions documented
```
