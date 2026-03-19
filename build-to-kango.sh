#!/bin/bash

# egs-engine 一键构建并复制到 kango 目录
# 用法: ./build-to-kango.sh

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
echo -e "${YELLOW}  egs-engine 一键构建脚本${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# 检查 kango 目录是否存在
if [ ! -d "$KANGO_DIR" ]; then
    echo -e "${RED}✗ 错误: kango 目录不存在${NC}"
    echo -e "${YELLOW}  预期路径: $KANGO_DIR${NC}"
    exit 1
fi

# 步骤1: 构建 shadow jar
echo -e "${YELLOW}[1/2] 正在构建 egs-engine...${NC}"
cd "$EGS_ENGINE_DIR"
./gradlew :app:shadowJar --quiet 2>/dev/null || ./gradlew :app:shadowJar

if [ ! -f "$JAR_SOURCE" ]; then
    echo -e "${RED}✗ 构建失败: 未找到 $JAR_SOURCE${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 构建完成${NC}"
echo ""

# 步骤2: 复制 jar 到 kango 目录
echo -e "${YELLOW}[2/2] 复制 jar 到 kango 目录...${NC}"
cp "$JAR_SOURCE" "$JAR_TARGET"
echo -e "${GREEN}✓ 复制完成${NC}"
echo -e "${YELLOW}  源文件: $JAR_SOURCE${NC}"
echo -e "${YELLOW}  目标文件: $JAR_TARGET${NC}"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  构建完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}现在你可以在 kango 目录下运行:${NC}"
echo -e "  ${GREEN}java -jar app-all.jar --help${NC}"
echo ""
