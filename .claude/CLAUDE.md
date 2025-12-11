# Claude Code向け システム生成指示書

このプロジェクトで要件定義書からアプリケーションを生成する際の**必須ルール**です。

---

## 🎯 基本方針

**Docker環境を標準とし、ローカルのJava/Maven環境に依存しない構成にする**

開発者はDocker Desktopのみで開発可能な状態を保つこと。

---

## 📦 技術スタック

- **Backend**: Spring Boot 3.2.0, Java 17, PostgreSQL 16, Maven
- **Frontend**: Next.js 14, TypeScript
- **実行環境**: Docker Compose

---

## ⚙️ 必須構成

### Docker関連
- `backend/Dockerfile`: マルチステージビルド（maven:3.9-eclipse-temurin-17使用）
- `docker-compose.yml`: PostgreSQL + Backend、ヘルスチェック、ネットワーク設定
- `Makefile`: up/down/build/logs/test/db-reset/clean
- `.dockerignore`: target/, .idea/, .DS_Store等を除外

### pom.xml
- **Lombok**: 1.18.28（バージョン明示必須）
- **maven-compiler-plugin**: annotationProcessorPaths設定必須
- **Flyway**: flyway-coreのみ（flyway-database-postgresqlは不要）

### application.yml
- 環境変数で設定上書き可能（`${SPRING_DATASOURCE_URL:デフォルト値}`形式）

### README.md
- 前提条件: Docker Desktop + Git のみ
- クイックスタート: `make up` で起動
- ローカルJava/Mavenの説明は不要