# タスク管理ツール 要件定義書（詳細版 v0.1）

最終更新: 2025-08-27 / 作成: 齊藤様提供ドラフトを基に詳細化（AI草案）

---

## 0\. 本ドキュメントの位置づけ

* 目的: 既存ドラフトを実装可能なレベルに**詳細化**し、Phase1着手の合意形成・見積り・実装計画に使える状態にする。  
* スコープ: Phase1（ガント \+ 取込・編集）を中心に、Phase2以降は方向性を示す。未確定事項はTBDとして**決めること一覧**に集約。

---

## 1\. 案件概要（更新）

### 1.1 背景・目的

* スプレッドシートで管理しているタスク/進捗を、**ガントチャート**で可視化し、**画面上で編集・管理**可能にする。  
* **短期立上げ（Phase1）** → **複数人利用/アカウント管理（Phase2+）** の段階拡張。

### 1.2 前提・制約

* 対象端末/ブラウザ: **Chrome 最新安定版**  
* データ保管先: **PostgreSQL（オンプレ）**  
* 技術/フレームワーク: **Next.js（フロント）**, **Spring Boot（API）**  
* リリーススコープ: **Phase1 \= ガント \+ 取込・編集**  
* セキュリティ強化（認証/権限）は **Phase2+**

### 1.3 成果物

* 稼働アプリ（Dev/Stg/Prod）  
* API/DB スキーマ, 運用Runbook, 移行手順, 受入テスト観点

---

## 2\. ユースケース

### 2.1 Phase1

* **UC-01**: ガントで**プロジェクト/タスクの期間・進捗・依存**を閲覧  
* **UC-02**: ガント上で**タスクの追加/期間変更/進捗更新/依存設定**  
* **UC-03**: **CSV/Excel 取込**で初期データまたは差分を登録  
* **UC-04**: **簡易タスク一覧**で表編集（名称/日付/進捗/状態）  
* **UC-05**: \*\*設定（最小）\*\*で稼働日/休日・表示粒度を変更  
* **UC-06（任意）**: CSV エクスポート

### 2.2 Phase2 以降

* **UC-07**: 複数ユーザー同時編集（競合の基本対策）  
* **UC-08**: アカウント管理（ユーザー/ロール/監査ログ）  
* **UC-09**: カレンダー（月/週/日）表示、外部カレンダー連携の基礎

---

## 3\. 画面/機能要件（詳細）

### 3.1 一覧

| ID | 画面/機能 | フェーズ | 主要操作 | 主要表示項目 |
| :---- | :---- | :---- | :---- | :---- |
| UI-01 | ガントチャート | P1 | タスク追加/編集/ドラッグ&ドロップ/依存線作成/ズーム（日/週/月）/スクロール/フィルタ | プロジェクト/タスク名、開始/終了、期間、進捗(%)、状態、依存、マイルストン、カレンダー（稼働日） |
| UI-02 | タスク一覧（簡易） | P1 | インライン編集、並び替え、フィルタ、ページング | タスクID/コード、名称、開始/終了、進捗、状態、親子、担当(Phase2+) |
| UI-03 | データ取込 | P1 | ファイル選択、項目マッピング、**ドライラン検証**、登録実行、エラーレポートDL | 取込結果（新規/更新/スキップ/エラー件数） |
| UI-04 | 設定（最小） | P1 | 稼働日/休日設定、祝日カレンダー読込（手動）、ズーム既定値 | 稼働曜日、個別休日、開始週、ガント表示既定 |
| UI-05 | ログイン/ユーザー管理 | P2+ | 認証/ロール/アカウント状態/パスワード方針 | ユーザー一覧、ロール、最終ログイン |
| UI-06 | カレンダー表示 | P2+ | 月/週/日、タスク連動、ドラッグ編集 | 予定/タスク、All-day、色分け |

空状態: 初回は「取込を促すカード」を表示。サンプルCSVをDL可。

### 3.2 ガント詳細要件（UI-01）

* **表示粒度**: 日/週/月（切替）。P1は週/月中心、日表示は任意。  
    
* **操作**:  
    
  * **期間変更**: バー端をドラッグで開始/終了変更。稼働日外は自動スキップ。  
  * **移動**: バーをドラッグで開始日シフト。依存関係により**制約警告**。  
  * **追加**: 空白行または+ボタン。最低限「名前/開始/終了」。  
  * **進捗更新**: バー内スライダー or 一覧の%編集。  
  * **依存設定**: バー端からドラッグで\*\*FS（Finish→Start）\*\*をP1の既定。SS/FF/SFはP2候補。  
  * **マイルストン**: 期間0日のダイヤ表示（任意）。


* **バリデーション/制約**:  
    
  * 開始 ≤ 終了、依存の循環禁止、日付フォーマット検証、進捗は0–100。  
  * 稼働日/休日の補正（設定に従い土日/祝を除外）。


* **フィルタ/検索**（任意）: 状態（planned/in\_progress/done/blocked）、タグ、テキスト。  
    
* **アクセシビリティ**: キーボード操作（↑↓行移動、←→日移動、Enter編集）。  
    
* **パフォーマンス**: 100タスクで初回描画≤1s、操作遅延≤100ms（目安）。

### 3.3 タスク一覧（UI-02）

* インライン編集: 名前、開始/終了、進捗、状態。  
* 階層（親子）表示はP1任意（折りたたみ対応はP2）。  
* ソート/フィルタ/簡易検索。

### 3.4 取込（UI-03）

* **対応形式**: CSV（UTF-8/BOM可）, XLSX  
* **サイズ上限**: 10MB（TBD）  
* **マッピング**: ユーザーが列を**Task/Project**項目へ割当。**テンプレ保存**可（任意）。  
* **ドライラン**: 書込前に検証。件数と詳細エラーを表示。  
* **重複判定**: `project_id + task_code`（推奨）でupsert。task\_code未提供時は新規採番。  
* **エラー処理**: 行単位でスキップし、**原因**（例: 日付不正、ID不在、循環依存）をCSVでDL。

### 3.5 設定（UI-04）

* 稼働曜日（例: 月–金）  
* 個別休日（手動追加）  
* 表示既定（ズーム、開始週）  
* （任意）日本の祝日CSVインポート

---

## 4\. 外部IF（詳細）

* **IF-01 取込（IN）**: UI/REST両方（後述API）。  
* **IF-02 エクスポート（OUT, 任意）**: 一覧/ガントのCSV出力。  
* **IF-03 カレンダー（P2+）**: iCal/ICS or Google/Microsoft 基礎連携（片方向→双方向）。  
* **IF-04 認証連携（P2+）**: 社内ID基盤/SSO（SAML/OIDC, TBD）。

---

## 5\. データモデル（詳細）

型はPostgreSQL想定。`NOT NULL`/制約/索引を明示。

### 5.1 テーブル定義

* **project**  
    
  * `id` BIGSERIAL PK  
  * `name` VARCHAR(200) NOT NULL  
  * `start_date` DATE, `end_date` DATE  
  * `status` VARCHAR(32) DEFAULT 'active' CHECK (status IN ('active','archived'))  
  * `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()  
  * `updated_at` TIMESTAMPTZ NOT NULL DEFAULT now()  
  * **INDEX**: `idx_project_name`


* **task**  
    
  * `id` BIGSERIAL PK  
  * `project_id` BIGINT NOT NULL FK→project(id) ON DELETE CASCADE  
  * `task_code` VARCHAR(64) NULL  \-- 取込の安定キー（推奨一意/プロジェクト内）  
  * `name` VARCHAR(255) NOT NULL  
  * `assignee` VARCHAR(120) NULL  \-- P2でUser参照に変更想定  
  * `start_date` DATE NOT NULL  
  * `end_date` DATE NOT NULL  
  * `progress` SMALLINT NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100\)  
  * `status` VARCHAR(32) NOT NULL DEFAULT 'planned' CHECK (status IN ('planned','in\_progress','done','blocked','on\_hold'))  
  * `parent_task_id` BIGINT NULL FK→task(id)  
  * `is_milestone` BOOLEAN NOT NULL DEFAULT FALSE  
  * `notes` TEXT NULL  
  * `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()  
  * `updated_at` TIMESTAMPTZ NOT NULL DEFAULT now()  
  * **UNIQUE**: (`project_id`,`task_code`)  \-- NULLは許容  
  * **INDEX**: `idx_task_project`, `idx_task_parent`, `idx_task_dates`


* **task\_dependency**  
    
  * `id` BIGSERIAL PK  
  * `task_id` BIGINT NOT NULL FK→task(id) ON DELETE CASCADE  
  * `predecessor_task_id` BIGINT NOT NULL FK→task(id) ON DELETE CASCADE  
  * `type` VARCHAR(8) NOT NULL DEFAULT 'FS' CHECK (type IN ('FS','SS','FF','SF'))  \-- P1はFSのみ使用  
  * **UNIQUE**: (`task_id`,`predecessor_task_id`)  
  * **CHECK**: `task_id <> predecessor_task_id`


* **import\_job**  
    
  * `id` BIGSERIAL PK  
  * `source_type` VARCHAR(16) NOT NULL CHECK (source\_type IN ('CSV','Excel'))  
  * `status` VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','DRY\_RUN','SUCCESS','PARTIAL','FAILED'))  
  * `executed_at` TIMESTAMPTZ NOT NULL DEFAULT now()  
  * `summary` JSONB NOT NULL DEFAULT '{}'  \-- 件数, 警告など  
  * `error_report_path` TEXT NULL  \-- エラーファイル保存先


* **user**（P2+）  
    
  * `id` BIGSERIAL PK  
  * `name` VARCHAR(120) NOT NULL  
  * `email` VARCHAR(255) NOT NULL UNIQUE  
  * `role` VARCHAR(32) NOT NULL CHECK (role IN ('admin','editor','viewer'))  
  * `is_active` BOOLEAN NOT NULL DEFAULT TRUE


* **calendar\_event**（P2+）  
    
  * `id` BIGSERIAL PK  
  * `project_id` BIGINT NULL, `task_id` BIGINT NULL  \-- どちらか必須  
  * `start` TIMESTAMPTZ NOT NULL, `end` TIMESTAMPTZ NOT NULL, `all_day` BOOLEAN NOT NULL DEFAULT FALSE

### 5.2 ドメイン制約

* 依存の循環を禁止（保存時に検出）。  
* `start_date <= end_date`、マイルストンは `start_date = end_date`。  
* 稼働日計算は**設定**に基づき非稼働日をスキップ。

---

## 6\. 取込/エクスポート仕様（詳細）

### 6.1 推奨CSVカラム（例）

| 列名（例） | マッピング先 | 必須 | 型/例 | 備考 |
| :---- | :---- | :---- | :---- | :---- |
| project\_name | project.name | 必須(初回) | 文字列 | 既存プロジェクトが無ければ新規作成（任意） |
| task\_code | task.task\_code | 任意 | 文字列 | 安定キー。未指定は新規採番 |
| task\_name | task.name | 必須 | 文字列 |  |
| start\_date | task.start\_date | 必須 | YYYY-MM-DD |  |
| end\_date | task.end\_date | 必須 | YYYY-MM-DD |  |
| progress | task.progress | 任意 | 0–100 |  |
| status | task.status | 任意 | planned 等 | ドロップダウン対応 |
| parent\_code | task.parent\_task\_id | 任意 | 文字列 | `task_code`参照で階層構成 |
| predecessors | task\_dependency | 任意 | 例: `T-001;T-005` | `task_code`を;区切りで参照 |
| is\_milestone | task.is\_milestone | 任意 | TRUE/FALSE |  |
| notes | task.notes | 任意 | 文字列 |  |

### 6.2 取込動作

* **Upsert**: (`project`,`task_code`)で一致 → 更新。無ければ新規。  
* **依存**: 2パス処理（タスク登録後に依存解決）。  
* **検証**: 形式/型/参照整合性/循環検出。警告（休日に開始等）は許容し自動補正またはユーザー選択。  
* **ロールバック**: 致命的エラー時は全体ロールバック or 行単位適用（設定TBD）。

### 6.3 エクスポート（任意）

* 表示中フィルタを反映したCSV出力。UTF-8。日付/進捗/状態を標準化。

---

## 7\. API 設計（Phase1）

すべて JSON / HTTPS。URLは例。認可はP1最小（後述）。

### 7.1 エンドポイント

* `GET /api/projects` / `POST /api/projects`  
* `GET /api/projects/{id}` / `PATCH /api/projects/{id}` / `DELETE`(任意)  
* `GET /api/projects/{id}/tasks?from=&to=` 期間フィルタ/ページング  
* `POST /api/projects/{id}/tasks`（単体作成）  
* `PATCH /api/tasks/{id}`（部分更新: 日付/進捗/状態/親子）  
* `POST /api/tasks/{id}/dependencies` / `DELETE /api/tasks/{id}/dependencies/{depId}`  
* `POST /api/import-jobs`（ファイル+マッピング）  
* `GET /api/import-jobs/{id}`（結果/ログ取得）

### 7.2 共通レスポンス

```json
{
  "data": {},
  "meta": {"requestId": "...", "timestamp": "..."},
  "errors": []
}
```

### 7.3 バリデーション & エラーコード

* 400: フォーマット不正、循環依存、日付逆転  
* 409: 重複キー衝突（task\_code）  
* 413: ファイルサイズ超過  
* 422: ビジネスルール違反（依存が休日を跨ぐ等、警告→補正可）

---

## 8\. セキュリティ/認証（段階適用）

### Phase1（最小）

* **ネットワーク制御**: 社内NW/VPN、**IP許可リスト**  
* **接続**: HTTPS（内部CA/TLS終端）  
* **アプリ認証**: **Basic Auth もしくは リバースプロキシの認証**（TBD）  
* **権限**: 単一/少人数でフル操作、監査は簡易アクセスログ

### Phase2+

* **認証**: SSO（SAML/OIDC）  
* **権限**: ロール（admin/editor/viewer）  
* **監査**: 重要操作の監査ログ（作成/更新/削除/取込）保存  
* **パスワード方針**: SSO基準に準拠

---

## 9\. 非機能要件（詳細）

### 9.1 性能

* ガント初期描画: 100タスク ≤1s（平均）  
* 一覧/検索: ≤1s  
* API スループット（目安）: 30req/s（P1）  
* レスポンス目標: p95 ≤300ms（主要API）

### 9.2 可用性・運用

* 監視: アプリ/DBの死活、HTTPレイテンシ、エラーレート  
* ログ: アクセス/アプリ（構造化JSON） 保持 90日（TBD）  
* バックアップ: DB日次スナップショット \+ 取込原本ファイル保全  
* リストア手順: Stgで四半期ごと演習（TBD）

### 9.3 信頼性

* 例外ハンドリングとユーザー通知（トースト/モーダル）  
* 取込は**冪等**（同一ジョブの再実行で結果不変）

### 9.4 セキュリティ

* 入力検証/XSS/CSRF対策、依存ライブラリの定期更新

---

## 10\. アーキテクチャ/設計

### 10.1 構成

* **Frontend**: Next.js \+ TypeScript。ガントUIライブラリは**選定TBD**。  
* **Backend**: Spring Boot（REST）。DBアクセスは JPA or MyBatis（TBD）。  
* **DB**: PostgreSQL（オンプレ）。マイグレーションは **Flyway** 推奨。  
* **ファイル**: 取込原本はNAS等に保全（パスをimport\_jobに記録）。

### 10.2 ガントUI候補（比較観点）

* 候補例: OSS系 / 商用コンポーネント  
* 比較軸: **操作性（ドラッグ/依存線）**, **性能**, **ライセンス/コスト**, **アクセシビリティ**, **React互換性**, **印刷/エクスポート**  
* **決定はTBD**（PoCで比較）

### 10.3 日付/タイムゾーン

* すべて **日付（DATE）基準**で管理。UIはローカルTZ表示。  
* 稼働日/休日の演算はサーバー側で統一ロジック。

---

## 11\. 環境/デプロイ

* 環境: **Dev / Stg / Prod**  
* 配布: コンテナ（Docker）想定。オンプレK8s or VM（TBD）。  
* CI/CD: Git（main/dev）、ビルド（Maven/Gradle, Node）、テスト、デプロイ（TBD）  
* 設定: 12factorに準拠。秘密情報はVault等（TBD）。

---

## 12\. 品質保証/テスト

### 12.1 受入観点（Phase1）

* 100タスクのCSVを取り込み、**1秒以内**でガント表示できる。  
* バーのドラッグで**開始/終了**が正しく反映される。  
* **FS依存**を設定すると、前タスクの終了より前に後続を開始できない。  
* 稼働日設定で**土日除外**が反映される。  
* 取込ドライランで**エラーレポート**がDLできる。

### 12.2 テスト種別

* 単体/結合/E2E（主要操作シナリオ）/性能/回帰/セキュリティ簡易

---

## 13\. データ移行

* 移行方法: 初回**CSV/Excel取込**  
* クリーニング: 日付/依存/重複（task\_code）  
* 検証: 行数一致、代表タスクのハンドチェック

---

## 14\. ログ/監査/計測

* 操作ログ（主要操作: 追加/更新/削除/取込）※P1はアプリログにINFO出力、P2で監査テーブル化  
* メトリクス: API応答時間、取込件数、失敗率

---

## 15\. リリース計画（案）

* **P0: PoC（2週間）** ガントUI候補比較 \+ 性能測定  
* **P1: MVP（4–6週間）** ガント/一覧/取込/設定  
* **P1.1（任意）** エクスポート/細かなUX改善  
* **P2** 認証/複数人/カレンダー

---

## 16\. 用語/状態定義

* **状態**: planned / in\_progress / done / blocked / on\_hold  
* **進捗(%)**: タスク完了比率（0–100）  
* **マイルストン**: 期間0日、チェックポイント

---

## 17\. 決めること一覧（TBD/候補/提案）

優先度高い順。担当/期限は調整用に空欄。

| \# | トピック | 現状 | 候補/提案 | フェーズ | 推奨 | 担当 | 期限 |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | **ガントUIライブラリ** | 未選定 | OSS系/商用をPoC比較（操作/性能/React互換/コスト） | P1 | **PoCで決定** |  |  |
| 2 | **認証方式（P1）** | 最小 | **IP許可 \+ Basic/Proxy認証** or 無認証(社内NW限定) | P1 | **IP許可+Basic** |  |  |
| 3 | **DBアクセス方式** | 未決 | JPA or MyBatis | P1 | **JPA** |  |  |
| 4 | **タスク安定キー** | 未決 | `task_code`の運用（ユニーク/プロジェクト内） | P1 | **採用** |  |  |
| 5 | **取込エラーの適用単位** | 未決 | 全体ロールバック vs 行単位適用 | P1 | **行単位** |  |  |
| 6 | **稼働日/休日データ** | 未決 | 手動管理 or 祝日CSV読込 | P1 | **手動 \+ CSV取込** |  |  |
| 7 | **FS以外の依存** | 未決 | SS/FF/SFの扱い | P2 | **FSのみ(P1)** |  |  |
| 8 | **エクスポート実装** | 任意 | CSVのみ or 画像/PDFは後続 | P1.1 | **CSVのみ** |  |  |
| 9 | **ファイル保管先** | 未決 | NAS/オブジェクトストレージ/DB BLOB | P1 | **NAS** |  |  |
| 10 | **CI/CD** | 未決 | Jenkins/GitHub Actions 等 | P1 | **既存基盤** |  |  |
| 11 | **コンテナ基盤** | 未決 | K8s/VM直載せ | P1 | **既存標準に合わせる** |  |  |
| 12 | **ログ保持期間** | TBD | 30/90/180日 | P1 | **90日** |  |  |
| 13 | **バックアップ方式** | TBD | DBスナップ \+ WAL, 原本保全 | P1 | **日次+週次検証** |  |  |
| 14 | **同時編集対策** | 未決 | 楽観ロック/ロック/差分マージ | P2 | **楽観ロック** |  |  |
| 15 | **監査ログ粒度** | 未決 | 取込/CRUD/設定変更 | P2 | **CRUD+取込** |  |  |
| 16 | **API認可** | 未決 | トークン/セッション/リバプロ委譲 | P2 | **SSO後にロール付与** |  |  |
| 17 | **一覧の階層UI** | 未決 | 展開/折畳み/検索連動 | P1.1 | **折畳み対応** |  |  |
| 18 | **日表示の有無** | 未決 | P1で含むか | P1 | **任意（性能見て判断）** |  |  |
| 19 | **印刷/画像出力** | 未決 | スクショ支援 or 専用出力 | 後続 | **後続** |  |  |
| 20 | **リリース方式** | 未決 | 段階公開/一括切替 | P1 | **段階公開** |  |  |

---

## 18\. リスクと対応

| リスク | 影響 | 対応策 |
| :---- | :---- | :---- |
| ガントUIの描画性能不足 | UX低下/再選定コスト | 早期PoC/100–300タスクで負荷確認、仮説検証 |
| 取込データ品質 | エラー多発/移行停滞 | ドライラン/エラーレポート/テンプレ提供/サンプル検証 |
| 稼働日/休日の相違 | 期間ずれ | 設定UIの明確化/計算ロジックの一元化 |
| Phase2の要件拡大 | スケジュール延伸 | P1と分離しMVP死守/バックログ管理 |

---

## 19\. 参考フロー（主要操作）

1. 取込テンプレDL → 既存表の列合わせ → UIでドライラン → エラー修正 → 本適用  
2. ガントでドラッグ編集 → API保存 → 即時反映 → 依存警告が出た場合は補正  
3. 稼働日変更 → 全タスク自動再計算（任意: 影響プレビュー）

---

## 20\. 付録: APIスキーマ（例）

```
Task:
  id: number
  projectId: number
  taskCode: string
  name: string
  startDate: string  # YYYY-MM-DD
  endDate: string
  progress: number   # 0-100
  status: string     # planned/in_progress/done/blocked/on_hold
  parentTaskId: number | null
  isMilestone: boolean
  notes: string | null
```

---

以上。Phase1の見積り/実装に必要な粒度までブレークダウン済み。未決事項は「決めること一覧」を基にPoC/レビューで確定させる。  
