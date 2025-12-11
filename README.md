# タスク管理ツール (Task Management Tool)

ガントチャート機能を備えたタスク管理ツール - Phase 1 MVP

## 前提条件

開発・実行には以下のみが必要です：

- **Docker Desktop** (最新版推奨)
- **Git**

ローカルのJava/Maven/Node.js環境は**不要**です。すべてDocker内で実行されます。

## クイックスタート

### 1. リポジトリのクローン

```bash
git clone <repository-url>
cd planmode-mpf3-task-management
```

### 2. アプリケーションの起動

```bash
make up
```

初回起動時は、Dockerイメージのビルドに数分かかります。

### 3. アクセス

サービスが起動したら、以下のURLでアクセスできます：

- **フロントエンド**: http://localhost:3000
- **バックエンドAPI**: http://localhost:8080
- **データベース**: localhost:5432

### 4. 動作確認

ブラウザで http://localhost:3000 を開き、画面が表示されることを確認してください。

## 利用可能なコマンド

### サービス管理

```bash
make up        # サービスを起動
make down      # サービスを停止
make build     # Dockerイメージを再ビルド
make logs      # ログを表示（リアルタイム）
```

### 開発・テスト

```bash
make test      # バックエンドのテストを実行
```

### データベース操作

```bash
make db-reset  # データベースをリセット（全データ削除）
make clean     # サービス停止 + ボリューム削除
```

### ヘルプ

```bash
make help      # 利用可能なコマンド一覧を表示
```

## プロジェクト構成

```
planmode-mpf3-task-management/
├── backend/              # Spring Boot 3.2.0 + Java 17
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/taskmanagement/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/  # Flywayマイグレーション
│   │   └── test/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/             # Next.js 14 + TypeScript
│   ├── src/
│   │   ├── app/         # App Router
│   │   ├── components/  # React コンポーネント
│   │   ├── lib/         # API クライアント、型定義
│   │   └── hooks/       # カスタムフック
│   ├── package.json
│   └── Dockerfile
├── docker-compose.yml    # サービス定義
├── Makefile             # 便利コマンド
└── README.md
```

## 技術スタック

### バックエンド
- Spring Boot 3.2.0
- Java 17
- PostgreSQL 16
- Maven
- JPA (Hibernate)
- Flyway (DBマイグレーション)
- Lombok 1.18.28

### フロントエンド
- Next.js 14 (App Router)
- TypeScript
- TOAST UI Gantt (ガントチャートライブラリ)
- Axios (HTTP クライアント)
- Zustand (状態管理)

### インフラ
- Docker + Docker Compose
- PostgreSQL 16

## Phase 1 機能

- ✅ **ガントチャート**: タスクの期間・進捗・依存関係を可視化
  - ドラッグ&ドロップでタスク編集
  - FS (Finish-to-Start) 依存関係
  - 日/週/月表示の切り替え
- ✅ **タスク一覧**: インライン編集可能な表形式ビュー
- ✅ **CSV/Excelインポート**: ドライラン検証、エラーレポート
- ✅ **設定**: 稼働日/休日の設定

## データベーススキーマ

### プロジェクト (project)
- プロジェクトの基本情報（名前、期間、ステータス）

### タスク (task)
- タスクの詳細（名前、期間、進捗、ステータス、マイルストン）
- プロジェクトへの紐付け
- 親子関係（階層構造）

### タスク依存関係 (task_dependency)
- タスク間の依存関係（FS/SS/FF/SF）
- 循環依存の防止

### インポートジョブ (import_job)
- CSV/Excelインポートの履歴とエラーレポート

詳細は `backend/src/main/resources/db/migration/` のSQLファイルを参照してください。

## API エンドポイント

### プロジェクト
- `GET /api/projects` - プロジェクト一覧
- `POST /api/projects` - プロジェクト作成
- `GET /api/projects/{id}` - プロジェクト詳細
- `PATCH /api/projects/{id}` - プロジェクト更新

### タスク
- `GET /api/projects/{projectId}/tasks` - タスク一覧（フィルタ可）
- `POST /api/projects/{projectId}/tasks` - タスク作成
- `PATCH /api/tasks/{id}` - タスク更新
- `DELETE /api/tasks/{id}` - タスク削除

### 依存関係
- `POST /api/tasks/{taskId}/dependencies` - 依存関係作成
- `DELETE /api/tasks/{taskId}/dependencies/{dependencyId}` - 依存関係削除

### インポート
- `POST /api/import-jobs?dryRun=true/false` - CSV/Excelインポート
- `GET /api/import-jobs/{id}` - インポート結果取得
- `GET /api/import-jobs/{id}/errors` - エラーレポートダウンロード

## トラブルシューティング

### サービスが起動しない

1. Docker Desktopが起動していることを確認
2. ポート競合を確認（3000, 8080, 5432が使用可能か）
3. ログを確認: `make logs`

### データベースエラー

```bash
make db-reset  # データベースをリセット
```

### ビルドエラー

```bash
make clean     # クリーンアップ
make build     # 再ビルド
make up        # 起動
```

### コンテナの状態確認

```bash
docker-compose ps
```

## 開発

### ホットリロード（開発モード）

開発時のホットリロードには `docker-compose.override.yml` を作成：

```yaml
version: '3.8'
services:
  backend:
    volumes:
      - ./backend/src:/app/src

  frontend:
    command: npm run dev
    volumes:
      - ./frontend/src:/app/src
```

### テストの実行

```bash
# バックエンドのテスト
make test

# 個別のテストクラス
docker-compose exec backend mvn test -Dtest=ProjectServiceTest
```

## ライセンス

このプロジェクトで使用しているオープンソースライブラリ：
- Spring Boot - Apache License 2.0
- Next.js - MIT License
- TOAST UI Gantt - MIT License
- PostgreSQL - PostgreSQL License

## サポート

問題が発生した場合は、Issueを作成してください。

---

**Phase 1 完了目標**: 10週間（2025年）
