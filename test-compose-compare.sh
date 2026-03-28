#!/bin/bash
# test-compose-compare.sh - 对比 Compose 版本与旧 Fragment 版本

echo "=========================================="
echo "  Compose Migration - 新旧版本对比"
echo "=========================================="
echo ""

echo "这个脚本展示了 Compose 迁移前后代码生成的差异"
echo ""

echo "=========================================="
echo "  旧版本 (Fragment + XML) 生成:"
echo "=========================================="
cat << 'EOF'
feature/home/
├── src/main/
│   ├── kotlin/.../presentation/
│   │   ├── HomeContract.kt
│   │   ├── HomeViewModel.kt
│   │   └── HomeFragment.kt              ← ❌ 不再生成
│   └── res/
│       ├── layout/
│       │   └── fragment_home.xml        ← ❌ 不再生成
│       └── navigation/
│           └── home_nav_graph.xml       ← ❌ 不再生成
└── build.gradle.kts

生成的代码示例:

// HomeFragment.kt (旧版本)
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    private val viewModel: HomeViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Data Binding
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
    }
}

<!-- fragment_home.xml (旧版本) -->
<layout xmlns:android="...">
    <data>
        <variable name="viewModel" type="...HomeViewModel"/>
    </data>
    <ConstraintLayout ...>
        <!-- XML UI -->
    </ConstraintLayout>
</layout>

EOF

echo ""
echo "=========================================="
echo "  新版本 (Compose UI) 生成:"
echo "=========================================="
cat << 'EOF'
feature/home/
├── src/main/
│   ├── kotlin/.../presentation/
│   │   ├── HomeNavigationRoute.kt       ← ✅ 新增!
│   │   ├── PresentationModule.kt
│   │   ├── screen/
│   │   │   └── HomeScreen.kt            ← ✅ 新增!
│   │   └── fragment/home/
│   │       ├── HomeContract.kt
│   │       └── HomeViewModel.kt
│   └── res/
│       └── AndroidManifest.xml          ← ✅ 保留
└── build.gradle.kts

生成的代码示例:

// HomeScreen.kt (新版本 - Compose)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val viewModel: HomeViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeContract.Effect.ShowToast -> { ... }
            }
        }
    }

    // Compose UI
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.error != null -> ErrorView(...)
            else -> ContentView(...)
        }
    }
}

// HomeNavigationRoute.kt (新版本)
sealed interface HomeNavigationRoute {
    @Serializable
    object Home : HomeNavigationRoute
}

EOF

echo ""
echo "=========================================="
echo "  主要变化总结"
echo "=========================================="
cat << 'EOF'

✅ 新增:
   • {Name}Screen.kt - Compose UI 代码
   • {Module}NavigationRoute.kt - 类型安全导航

❌ 移除:
   • {Name}Fragment.kt - Fragment 类
   • fragment_{name}.xml - XML 布局
   • {name}_nav_graph.xml - XML 导航图

✅ 保留:
   • {Name}Contract.kt - MVI 契约
   • {Name}ViewModel.kt - 状态管理
   • Data/Domain 层代码

📝 架构变化:
   旧: Fragment + Data Binding + XML Layout
   新: @Composable + ViewModel + NavigationRoute

EOF

echo ""
echo "=========================================="
echo "  运行完整测试来验证"
echo "=========================================="
echo ""
echo "运行命令:"
echo "  ./test-compose-full.sh"
echo ""
