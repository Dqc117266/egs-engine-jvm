#!/bin/bash
# test-create-screen.sh - 演示新的 create screen 命令

echo "=========================================="
echo "  Create Screen 命令演示"
echo "=========================================="
echo ""

# 创建临时测试目录
TEST_DIR="/tmp/egs-screen-test-$(date +%s)"
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"

echo "[1/3] 创建测试项目..."
mkdir -p .egs
mkdir -p feature/base
mkdir -p feature/common

cat > .egs/config.json << EOF
{
  "projectName": "test-screen",
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
rootProject.name = "test-screen"
include(
    ":feature:base",
    ":feature:common"
)
EOF

echo "[2/3] 先创建 user 模块..."
java -jar /Volumes/Expend/JavaProject/egs-group/kango/app-all.jar create module user --project "$TEST_DIR" > /dev/null 2>&1

echo ""
echo "=========================================="
echo "  示例 1: 基础用法"
echo "=========================================="
echo ""
echo "命令: egs create screen Login --module user"
echo ""
java -jar /Volumes/Expend/JavaProject/egs-group/kango/app-all.jar create screen Login --module user --project "$TEST_DIR" --dry-run 2>&1 | head -20

echo ""
echo "=========================================="
echo "  示例 2: 完整参数"
echo "=========================================="
echo ""
echo "命令: egs create screen Profile \\"
echo "  --module user \\"
echo "  --usecase GetUserUseCase,UpdateUserUseCase \\"
echo "  --route \"user/profile\" \\"
echo "  --params \"userId:Long,isEditMode:Boolean\""
echo ""

# 创建 UseCase 用于演示
mkdir -p feature/user/src/main/kotlin/com/example/feature/user/domain/usecase
cat > feature/user/src/main/kotlin/com/example/feature/user/domain/usecase/GetUserUseCase.kt << 'EOF'
package com.example.feature.user.domain.usecase
import com.example.feature.base.domain.result.Result

class GetUserUseCase {
    suspend operator fun invoke(userId: Long): Result<Boolean> = Result.Success(true)
}
EOF

cat > feature/user/src/main/kotlin/com/example/feature/user/domain/usecase/UpdateUserUseCase.kt << 'EOF'
package com.example.feature.user.domain.usecase
import com.example.feature.base.domain.result.Result

class UpdateUserUseCase {
    suspend operator fun invoke(userId: Long, name: String): Result<Boolean> = Result.Success(true)
}
EOF

java -jar /Volumes/Expend/JavaProject/egs-group/kango/app-all.jar create screen Profile \
  --module user \
  --usecase GetUserUseCase,UpdateUserUseCase \
  --route "user/profile" \
  --params "userId:Long,isEditMode:Boolean" \
  --project "$TEST_DIR" --dry-run 2>&1 | head -20

echo ""
echo "=========================================="
echo "  示例 3: 实际生成"
echo "=========================================="
echo ""
echo "命令: egs create screen Settings --module user"
echo ""
java -jar /Volumes/Expend/JavaProject/egs-group/kango/app-all.jar create screen Settings --module user --project "$TEST_DIR" 2>&1

echo ""
echo "=========================================="
echo "  生成的文件结构"
echo "=========================================="
echo ""
find "$TEST_DIR/feature/user/src/main/kotlin" -type f -name "*.kt" | sort | while read f; do
    echo "  $f"
done

echo ""
echo "=========================================="
echo "  测试项目位置: $TEST_DIR"
echo "=========================================="
