#!/bin/bash
###############################################################################
# API Key Tester Script
# Tests all configured API keys (Claude, OpenAI, Gemini) for validity
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}API Key Tester${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo -e "${RED}✗ Error: .env file not found!${NC}"
    echo ""
    echo "Please create a .env file with your API keys:"
    echo "  1. Copy the example: cp config/.env.example .env"
    echo "  2. Edit .env and add your API keys"
    echo ""
    exit 1
fi

echo -e "${GREEN}✓ Found .env file${NC}"
echo ""

# Check if JAR exists
JAR_FILE="target/transcript-pipeline.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo -e "${YELLOW}⚠ JAR file not found. Building project...${NC}"
    echo ""

    # Check if Maven is installed
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}✗ Error: Maven is not installed!${NC}"
        echo "Please install Maven and try again."
        exit 1
    fi

    # Build the project
    echo "Running: mvn clean package"
    mvn clean package -DskipTests

    if [ $? -ne 0 ]; then
        echo -e "${RED}✗ Build failed!${NC}"
        exit 1
    fi

    echo ""
    echo -e "${GREEN}✓ Build successful${NC}"
    echo ""
fi

# Run the API key tester
echo -e "${BLUE}Running API key tests...${NC}"
echo ""

java -cp "$JAR_FILE" com.transcript.pipeline.util.ApiKeyTester

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✓ API Key Test Completed Successfully${NC}"
    echo -e "${GREEN}========================================${NC}"
else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}✗ API Key Test Failed${NC}"
    echo -e "${RED}========================================${NC}"
    echo ""
    echo "Please check the error messages above and:"
    echo "  1. Verify your API keys are correct in .env file"
    echo "  2. Check your internet connection"
    echo "  3. Ensure you have API credits/quota available"
    echo ""
fi

exit $EXIT_CODE
