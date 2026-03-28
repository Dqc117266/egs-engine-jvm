#!/bin/bash
# test-compose-full.sh - 完整测试 create module 和 create page

set -e

echo "=========================================="
echo "  Compose Migration - Full Test"
echo "=========================================="
echo ""

# 创建临时测试目录
TEST_DIR="/tmp/egs-compose-test-full-$(date +%s)"
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"

echo "[1/8] 创建测试项目..."
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

echo ""
echo "=========================================="
echo "  PART 1: Create Module"
echo "=========================================="
echo ""

echo "[2/8] 运行: egs create module dashboard..."
java -jar /Volumes/Expend/JavaProject/egs-group/kango/app-all.jar create module dashboard --project "$TEST_DIR" || true

echo ""
echo "=========================================="
echo "  PART 2: Create Pages"
echo "=========================================="
echo ""

echo "[3/8] 创建页面: DashboardHome..."
java -jar /Volumes/Expend/JavaProject/egs-group/kango/app-all.jar create page --module dashboard --name DashboardHome --project "$TEST_DIR" || true

echo ""
echo "[4/8] 创建页面: DashboardSettings..."
java -jar /Volumes/Expend/JavaProject/egs-group/kango/app-all.jar create page --module dashboard --name DashboardSettings --project "$TEST_DIR" || true

echo ""
echo "=========================================="
echo "  PART 3: Generated File Structure"
echo "=========================================="
echo ""

echo "[5/8] 文件结构:"
echo "------------------------------------------"
if command -v tree &> /dev/null; then
    tree feature/dashboard/src/main/
else
    find feature/dashboard/src/main/ -type f | sort | while read f; do
        echo "  $f"
    done
fi

echo ""
echo "=========================================="
echo "  PART 4: Generated Code Samples"
echo "=========================================="
echo ""

echo "[6/8] NavigationRoute:"
echo "------------------------------------------"
cat feature/dashboard/src/main/kotlin/com/example/feature/dashboard/presentation/DashboardNavigationRoute.kt 2>/dev/null || echo "❌ 文件不存在"

echo ""
echo "[7/8] Compose Screen (DashboardHomeScreen):"
echo "------------------------------------------"
cat feature/dashboard/src/main/kotlin/com/example/feature/dashboard/presentation/screen/DashboardHomeScreen.kt 2>/dev/null || echo "❌ 文件不存在"

echo ""
echo "[8/8] Compose Screen (DashboardSettingsScreen):"
echo "------------------------------------------"
cat feature/dashboard/src/main/kotlin/com/example/feature/dashboard/presentation/screen/DashboardSettingsScreen.kt 2>/dev/null || echo "❌ 文件不存在"

echo ""
echo "=========================================="
echo "  PART 5: Verification"
echo "=========================================="
echo ""

ERRORS=0

echo "检查应该生成的文件:"
echo "------------------------------------------"
if [ -f "feature/dashboard/src/main/kotlin/com/example/feature/dashboard/presentation/DashboardNavigationRoute.kt" ]; then
    echo "✅ DashboardNavigationRoute.kt 存在"
else
    echo "❌ DashboardNavigationRoute.kt 不存在"
    ERRORS=$((ERRORS+1))
fi

if [ -f "feature/dashboard/src/main/kotlin/com/example/feature/dashboard/presentation/screen/DashboardHomeScreen.kt" ]; then
    echo "✅ DashboardHomeScreen.kt 存在"
else
    echo "❌ DashboardHomeScreen.kt 不存在"
    ERRORS=$((ERRORS+1))
fi

if [ -f "feature/dashboard/src/main/kotlin/com/example/feature/dashboard/presentation/screen/DashboardSettingsScreen.kt" ]; then
    echo "✅ DashboardSettingsScreen.kt 存在"
else
    echo "❌ DashboardSettingsScreen.kt 不存在"
    ERRORS=$((ERRORS+1))
fi

echo ""
echo "检查不应该生成的文件:"
echo "------------------------------------------"
if [ -f "feature/dashboard/src/main/res/layout/fragment_dashboard.xml" ]; then
    echo "❌ fragment_dashboard.xml 不应该存在"
    ERRORS=$((ERRORS+1))
else
    echo "✅ fragment_dashboard.xml 不存在 (正确)"
fi

if [ -f "feature/dashboard/src/main/res/navigation/dashboard_nav_graph.xml" ]; then
    echo "❌ dashboard_nav_graph.xml 不应该存在"
    ERRORS=$((ERRORS+1))
else
    echo "✅ dashboard_nav_graph.xml 不存在 (正确)"
fi

if [ -f "feature/dashboard/src/main/kotlin/com/example/feature/dashboard/presentation/fragment/dashboard/DashboardFragment.kt" ]; then
    echo "❌ DashboardFragment.kt 不应该存在"
    ERRORS=$((ERRORS+1))
else
    echo "✅ DashboardFragment.kt 不存在 (正确)"
fi

echo ""
echo "=========================================="
if [ $ERRORS -eq 0 ]; then
    echo "  ✅ 所有检查通过!"
else
    echo "  ❌ 发现 $ERRORS 个错误"
fi
echo "=========================================="
echo "  测试项目位置: $TEST_DIR"
echo "=========================================="
