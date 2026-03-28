#!/bin/bash
# test-compose-page.sh - 测试 create page 命令并展示结果

set -e

echo "=========================================="
echo "  Compose Migration - Page Test"
echo "=========================================="
echo ""

# 创建临时测试目录
TEST_DIR="/tmp/egs-compose-test-page-$(date +%s)"
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"

echo "[1/6] 创建测试项目..."
mkdir -p .egs
mkdir -p feature/base
mkdir -p feature/common

cat > .egs/config.json << EOF
{
  "projectName": "test-compose",
  "projectType": "ANDROID",
  "rootPath": "$TEST_DIR",
  "basePackage": "com.example",
  "conventionPluginId": "com.example.convention.feature",
  "moduleStructure": {
    "layers": ["data", "domain", "presentation"],
    "hasRes": true
  },
  "baseClasses": []
}
EOF

cat > settings.gradle.kts << 'EOF'
rootProject.name = "test-compose"
include(
    ":feature:base",
    ":feature:common"
)
EOF

echo "[2/6] 先创建模块 home..."
java -jar /Volumes/Expend/JavaProject/egs-group/kango/app-all.jar create module home --project "$TEST_DIR" > /dev/null 2>&1 || true

echo "[3/6] 运行: egs create page home Profile..."
echo ""

# 运行egs命令
java -jar /Volumes/Expend/JavaProject/egs-group/kango/app-all.jar create page --module home --name Profile --project "$TEST_DIR" || true

echo ""
echo "[4/6] 生成的文件结构:"
echo "=========================================="
if command -v tree &> /dev/null; then
    tree -L 7 feature/home/src/main/
else
    find feature/home/src/main/ -type f | sort
fi

echo ""
echo "[5/6] 关键文件内容:"
echo "=========================================="

echo ""
echo ">>> ProfileScreen.kt (Compose Screen) <<<"
echo "------------------------------------------"
if [ -f "feature/home/src/main/kotlin/com/example/feature/home/presentation/screen/ProfileScreen.kt" ]; then
    cat feature/home/src/main/kotlin/com/example/feature/home/presentation/screen/ProfileScreen.kt
else
    echo "❌ 文件不存在!"
fi

echo ""
echo ">>> ProfileContract.kt <<<"
echo "------------------------------------------"
if [ -f "feature/home/src/main/kotlin/com/example/feature/home/presentation/fragment/profile/ProfileContract.kt" ]; then
    cat feature/home/src/main/kotlin/com/example/feature/home/presentation/fragment/profile/ProfileContract.kt
else
    echo "❌ 文件不存在!"
fi

echo ""
echo ">>> ProfileViewModel.kt <<<"
echo "------------------------------------------"
if [ -f "feature/home/src/main/kotlin/com/example/feature/home/presentation/fragment/profile/ProfileViewModel.kt" ]; then
    cat feature/home/src/main/kotlin/com/example/feature/home/presentation/fragment/profile/ProfileViewModel.kt
else
    echo "❌ 文件不存在!"
fi

echo ""
echo "[6/6] 验证: 不应该生成的文件"
echo "=========================================="
if [ -f "feature/home/src/main/kotlin/com/example/feature/home/presentation/fragment/profile/ProfileFragment.kt" ]; then
    echo "❌ 错误: Fragment 文件存在!"
else
    echo "✅ 正确: 没有生成 Fragment"
fi

if [ -f "feature/home/src/main/res/layout/fragment_profile.xml" ]; then
    echo "❌ 错误: XML Layout 文件存在!"
else
    echo "✅ 正确: 没有生成 XML Layout"
fi

echo ""
echo "=========================================="
echo "  测试项目位置: $TEST_DIR"
echo "=========================================="
