.PHONY: help build clean run test install docs package release

# Variables
PROJECT_NAME=transcript-to-exam-notes
JAR_FILE=target/transcript-pipeline.jar
MAIN_CLASS=com.transcript.pipeline.App

# Default target
help:
	@echo "$(PROJECT_NAME) - Transcript to Exam Notes Pipeline"
	@echo ""
	@echo "Available targets:"
	@echo "  make build       - Compile the project"
	@echo "  make clean       - Remove build artifacts"
	@echo "  make run         - Run the interactive CLI application"
	@echo "  make test        - Run unit tests"
	@echo "  make install     - Install the JAR to local Maven repository"
	@echo "  make package     - Build the executable JAR"
	@echo "  make docs        - Generate documentation"
	@echo "  make deps        - Show dependency tree"
	@echo "  make verify      - Run all checks"
	@echo "  make help        - Show this help message"
	@echo ""

# Build the project
build:
	@echo "Building $(PROJECT_NAME)..."
	mvn clean compile
	@echo "Build complete!"

# Remove build artifacts
clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	rm -rf logs/*
	@echo "Clean complete!"

# Run the application
run: package
	@echo "Starting $(PROJECT_NAME)..."
	java -jar $(JAR_FILE)

# Run tests
test:
	@echo "Running tests..."
	mvn test

# Install to local Maven repository
install: build
	@echo "Installing to local Maven repository..."
	mvn install

# Package the application
package:
	@echo "Packaging $(PROJECT_NAME)..."
	mvn clean package -DskipTests
	@echo "Package complete! JAR file: $(JAR_FILE)"

# Generate documentation
docs:
	@echo "Documentation files:"
	@echo "  - README.md (User guide)"
	@echo "  - USAGE_GUIDE.md (Step-by-step instructions)"
	@echo "  - TECHNICAL_IMPLEMENTATION_GUIDE.md (Developer guide)"
	@echo "  - API_INTEGRATION_GUIDE.md (API reference)"
	@ls -1 *.md

# Show dependency tree
deps:
	@echo "Dependency tree:"
	mvn dependency:tree

# Verify the project
verify:
	@echo "Verifying project..."
	mvn verify

# Format code (requires formatter plugin)
format:
	@echo "Formatting code..."
	mvn spotless:apply

# Create release package
release: clean package docs
	@echo "Creating release package..."
	@mkdir -p release
	cp $(JAR_FILE) release/
	cp README.md release/
	cp USAGE_GUIDE.md release/
	cp API_INTEGRATION_GUIDE.md release/
	cp LICENSE release/
	cp config/.env.example release/
	@echo "Release package created in release/ directory"

# Quick start (setup + build)
quickstart: build
	@echo "Quick start guide:"
	@echo "1. Copy .env.example to .env"
	@echo "   cp config/.env.example .env"
	@echo ""
	@echo "2. Edit .env with your API keys"
	@echo "   CLAUDE_API_KEY=your_key_here"
	@echo "   OPENAI_API_KEY=your_key_here"
	@echo ""
	@echo "3. Place transcripts in transcripts/ directory"
	@echo ""
	@echo "4. Run the application:"
	@echo "   make run"
	@echo ""

# Show Java version
version:
	@echo "Project: $(PROJECT_NAME)"
	@echo "Java version:"
	java -version
	@echo ""
	@echo "Maven version:"
	mvn --version

# Check configuration
config-check:
	@echo "Checking configuration..."
	@if [ -f .env ]; then \
		echo "✓ .env file found"; \
	else \
		echo "✗ .env file not found - copy from config/.env.example"; \
	fi
	@if [ -f .gitignore ]; then \
		echo "✓ .gitignore file found"; \
	else \
		echo "✗ .gitignore file not found"; \
	fi
	@if [ -d transcripts ]; then \
		echo "✓ transcripts directory exists"; \
	else \
		echo "! transcripts directory not found (will be created at runtime)"; \
	fi
	@if [ -d output ]; then \
		echo "✓ output directory exists"; \
	else \
		echo "! output directory not found (will be created at runtime)"; \
	fi

# Watch mode (requires watchexec)
watch:
	@echo "Starting watch mode..."
	watchexec -e java,pom.xml "make build"

# Run with debug logging
debug: package
	@echo "Running with debug logging..."
	java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG -jar $(JAR_FILE)

# Check for security vulnerabilities
audit:
	@echo "Running security audit..."
	mvn org.owasp:dependency-check-maven:check

# Generate test coverage
coverage:
	@echo "Generating test coverage..."
	mvn jacoco:report
	@echo "Coverage report: target/site/jacoco/index.html"

# Development setup
dev-setup: clean build config-check
	@echo "Development environment setup complete!"
	@echo "Next steps:"
	@echo "1. Edit .env with your API keys"
	@echo "2. Add sample transcripts to transcripts/ directory"
	@echo "3. Run: make run"

# Show commit log
log:
	@git log --oneline -10 || echo "Not a git repository"

# Show file statistics
stats:
	@echo "Code statistics:"
	@find src -name "*.java" -type f | xargs wc -l | tail -1
	@echo ""
	@echo "Total files:"
	@find src -name "*.java" -type f | wc -l

# List all available make targets
targets:
	@grep "^[a-z].*:" Makefile | sed 's/:.*$$//' | column

# Initialize project (first time setup)
init: clean build
	@echo "Project initialization..."
	@mkdir -p transcripts output/chunks output/summaries output/consolidated output/exam_materials logs
	@echo "✓ Directories created"
	@if [ ! -f .env ]; then \
		cp config/.env.example .env; \
		echo "✓ .env file created (please add your API keys)"; \
	fi
	@echo "✓ Project ready for use"
	@echo ""
	@echo "Next: Edit .env with your API keys and run 'make run'"
