#!/bin/bash

# egs-engine CLI 一键构建测试脚本
# 用法: ./build-and-test.sh [测试命令参数]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 路径配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EGS_ENGINE_DIR="$SCRIPT_DIR"
KANGO_DIR="$(dirname "$SCRIPT_DIR")/kango"
JAR_SOURCE="$EGS_ENGINE_DIR/app/build/libs/app-all.jar"
JAR_TARGET="$KANGO_DIR/app-all.jar"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}  egs-engine CLI 一键构建测试脚本${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# 步骤1: 清理旧构建
#echo -e "${YELLOW}[1/4] 清理旧构建...${NC}"
#cd "$EGS_ENGINE_DIR"
#./gradlew clean --quiet 2>/dev/null || ./gradlew clean
#echo -e "${GREEN}✓ 清理完成${NC}"
#echo ""

# 步骤2: 构建 shadow jar
echo -e "${YELLOW}[2/4] 构建 egs-engine shadow jar...${NC}"
./gradlew shadowJar --quiet 2>/dev/null || ./gradlew :app:shadowJar
if [ ! -f "$JAR_SOURCE" ]; then
    echo -e "${RED}✗ 构建失败: 未找到 $JAR_SOURCE${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 构建完成: $JAR_SOURCE${NC}"
echo ""

# 步骤3: 复制 jar 到 kango 目录
echo -e "${YELLOW}[3/4] 复制 jar 到 kango 目录...${NC}"
if [ ! -d "$KANGO_DIR" ]; then
    echo -e "${RED}✗ 目标目录不存在: $KANGO_DIR${NC}"
    exit 1
fi
cp "$JAR_SOURCE" "$JAR_TARGET"
echo -e "${GREEN}✓ 复制完成: $JAR_TARGET${NC}"
echo ""

# 步骤4: 运行 CLI 测试命令
echo -e "${YELLOW}[4/4] 运行 CLI 测试命令...${NC}"
echo -e "${YELLOW}----------------------------------------${NC}"
cd "$KANGO_DIR"

# 如果有参数，使用参数运行；否则运行默认测试命令
if [ $# -eq 0 ]; then
    # 默认测试命令 - 显示帮助
    echo -e "${YELLOW}运行: java -jar app-all.jar --help${NC}"
    java -jar app-all.jar --help
else
    # 使用用户提供的参数
    echo -e "${YELLOW}运行: java -jar app-all.jar $@${NC}"
    java -jar app-all.jar "$@"
fi

echo -e "${YELLOW}----------------------------------------${NC}"
echo -e "${GREEN}✓ CLI 测试完成${NC}"
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  构建和测试全部完成！${NC}"
echo -e "${GREEN}========================================${NC}"
