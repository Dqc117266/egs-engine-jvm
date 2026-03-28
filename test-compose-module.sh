#!/bin/bash
# test-compose-module.sh - 测试 create module 命令并展示结果

set -e

echo "=========================================="
echo "  Compose Migration - Module Test"
echo "=========================================="
echo ""

# 创建临时测试目录
TEST_DIR="/tmp/egs-compose-test-module-$(date +%s)"
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"

echo "[1/5] 创建测试项目..."
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

echo "[2/5] 运行: egs create module testModule..."
echo ""

# 运行egs命令
java -jar /Volumes/Expend/JavaProject/egs-group/kango/app-all.jar create module testModule --project "$TEST_DIR" || true

echo ""
echo "[3/5] 生成的文件结构:"
echo "=========================================="
if command -v tree &> /dev/null; then
    tree -L 6 feature/testModule/src/main/
else
    find feature/testModule/src/main/ -type f | head -20
fi

echo ""
echo "[4/5] 关键文件内容:"
echo "=========================================="

echo ""
echo ">>> TestModuleNavigationRoute.kt <<"
echo "------------------------------------------"
if [ -f "feature/testModule/src/main/kotlin/com/example/feature/testModule/presentation/TestModuleNavigationRoute.kt" ]; then
    cat feature/testModule/src/main/kotlin/com/example/feature/testModule/presentation/TestModuleNavigationRoute.kt
else
    echo "文件不存在!"
fi

echo ""
echo ">>> HomeViewModel.kt (模块默认ViewModel) <<<"
echo "------------------------------------------"
if [ -f "feature/testModule/src/main/kotlin/com/example/feature/testModule/presentation/fragment/testmodule/TestModuleViewModel.kt" ]; then
    cat feature/testModule/src/main/kotlin/com/example/feature/testModule/presentation/fragment/testmodule/TestModuleViewModel.kt
else
    echo "文件不存在!"
fi

echo ""
echo "[5/5] 验证: 不应该生成XML文件"
echo "=========================================="
if [ -f "feature/testModule/src/main/res/layout/fragment_test_module.xml" ]; then
    echo "❌ 错误: XML Layout 文件存在!"
else
    echo "✅ 正确: 没有生成 XML Layout"
fi

if [ -f "feature/testModule/src/main/res/navigation/test_module_nav_graph.xml" ]; then
    echo "❌ 错误: NavGraph XML 文件存在!"
else
    echo "✅ 正确: 没有生成 NavGraph XML"
fi

echo ""
echo "=========================================="
echo "  测试项目位置: $TEST_DIR"
echo "=========================================="
