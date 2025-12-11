.PHONY: up down build logs test db-reset clean help

help:
	@echo "Task Management Tool - Makefile Commands"
	@echo ""
	@echo "Available commands:"
	@echo "  make up        - Start all services (PostgreSQL, backend, frontend)"
	@echo "  make down      - Stop all services"
	@echo "  make build     - Build all Docker images"
	@echo "  make logs      - Show logs from all services (follow mode)"
	@echo "  make test      - Run backend tests"
	@echo "  make db-reset  - Reset database (WARNING: deletes all data)"
	@echo "  make clean     - Stop services and remove volumes"

up:
	docker-compose up -d
	@echo ""
	@echo "✅ Services started!"
	@echo "   Frontend: http://localhost:3000"
	@echo "   Backend:  http://localhost:8080"
	@echo "   Database: localhost:5432"

down:
	docker-compose down

build:
	docker-compose build

logs:
	docker-compose logs -f

test:
	docker-compose exec backend mvn test

db-reset:
	@echo "⚠️  WARNING: This will delete all data!"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker-compose down -v; \
		docker-compose up -d; \
		echo "✅ Database reset complete"; \
	else \
		echo "❌ Cancelled"; \
	fi

clean:
	docker-compose down -v
	docker system prune -f
	@echo "✅ Cleanup complete"
