# 悬浮启动按钮实施计划

> **对于代理工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 来逐任务实施此计划。步骤使用复选框（`- [ ]`）语法进行跟踪。

**目标：** 在保留现有待命通知入口的同时，新增系统级悬浮按钮，点击后直接进入扫码程序。

**架构：** 复用 `StandbyService` 作为悬浮按钮宿主，服务启动时继续显示前台通知，并在具备悬浮窗权限时创建悬浮按钮。`MainActivity` 在用户开启待命开关时引导申请悬浮窗权限，权限缺失不影响原通知入口。

**技术栈：** Android Kotlin、Foreground Service、`WindowManager`、Robolectric 单元测试。

---

## 文件结构

- 修改 `app/src/main/AndroidManifest.xml`：声明 `SYSTEM_ALERT_WINDOW` 权限。
- 修改 `app/src/main/res/values/strings.xml`：新增悬浮按钮显示文本和权限提示文案。
- 修改 `app/src/main/java/com/debuggingonly/scanner/MainActivity.kt`：开启待命时检查悬浮窗权限，缺失时跳转系统设置页。
- 修改 `app/src/main/java/com/debuggingonly/scanner/StandbyService.kt`：管理悬浮按钮的创建、点击启动和移除。

### 任务 1：声明权限与文案

**文件：**
- 修改：`app/src/main/AndroidManifest.xml`
- 修改：`app/src/main/res/values/strings.xml`

- [ ] **步骤 1：新增 Manifest 权限**

在 `AndroidManifest.xml` 顶部权限区加入：

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

- [ ] **步骤 2：新增中文文案**

在 `strings.xml` 中加入：

```xml
<string name="floating_button_text">扫码</string>
<string name="overlay_permission_required">请开启悬浮窗权限以显示扫码悬浮按钮</string>
```

- [ ] **步骤 3：运行资源检查构建**

运行：`./gradlew.bat :app:assembleDebug`
预期：资源合并通过；如果因为本机 Android SDK 环境失败，错误应与代码语法无关。

### 任务 2：开启待命时引导悬浮窗权限

**文件：**
- 修改：`app/src/main/java/com/debuggingonly/scanner/MainActivity.kt`

- [ ] **步骤 1：添加必要 import**

添加：

```kotlin
import android.content.Intent
import android.net.Uri
import android.provider.Settings
```

- [ ] **步骤 2：在待命开关回调中触发权限引导**

将 `setOnCheckedChangeListener` 内逻辑调整为：

```kotlin
setOnCheckedChangeListener { _, checked ->
    settingsStore.setStandbyEnabled(checked)
    if (checked) {
        requestOverlayPermissionIfNeeded()
    }
    updateStandbyService()
}
```

- [ ] **步骤 3：添加权限检查方法**

在 `canPostNotifications()` 前加入：

```kotlin
private fun requestOverlayPermissionIfNeeded() {
    if (android.os.Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)) return

    showToast(getString(R.string.overlay_permission_required))
    startActivity(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        ),
    )
}
```

- [ ] **步骤 4：编译验证**

运行：`./gradlew.bat :app:compileDebugKotlin`
预期：Kotlin 编译通过。

### 任务 3：在待命服务中创建悬浮按钮

**文件：**
- 修改：`app/src/main/java/com/debuggingonly/scanner/StandbyService.kt`

- [ ] **步骤 1：阅读当前服务实现**

确认 `onStartCommand()`、`onDestroy()`、`buildNotification()` 和 `openScannerIntent()` 的现有职责，避免改动通知入口行为。

- [ ] **步骤 2：添加 WindowManager 相关字段和 import**

需要 import：

```kotlin
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
```

在类中添加字段：

```kotlin
private var floatingButton: View? = null
private var windowManager: WindowManager? = null
```

- [ ] **步骤 3：服务启动时创建悬浮按钮**

在 `onStartCommand()` 的 `startForeground(...)` 后加入：

```kotlin
showFloatingButtonIfAllowed()
```

- [ ] **步骤 4：服务销毁时移除悬浮按钮**

新增或更新 `onDestroy()`：

```kotlin
override fun onDestroy() {
    removeFloatingButton()
    super.onDestroy()
}
```

- [ ] **步骤 5：添加悬浮按钮创建和移除方法**

在 `openScannerIntent()` 后加入：

```kotlin
private fun showFloatingButtonIfAllowed() {
    if (floatingButton != null) return
    if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) return

    val manager = getSystemService(WINDOW_SERVICE) as WindowManager
    val button = Button(this).apply {
        text = getString(R.string.floating_button_text)
        setOnClickListener {
            startActivity(ScannerLaunchIntents.notificationLaunch(this@StandbyService))
        }
    }
    val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.END or Gravity.CENTER_VERTICAL
        x = 0
        y = 0
    }

    manager.addView(button, params)
    windowManager = manager
    floatingButton = button
}

private fun removeFloatingButton() {
    val button = floatingButton ?: return
    windowManager?.removeView(button)
    floatingButton = null
    windowManager = null
}
```

- [ ] **步骤 6：编译验证**

运行：`./gradlew.bat :app:compileDebugKotlin`
预期：Kotlin 编译通过。

### 任务 4：完整验证

**文件：**
- 验证：全项目

- [ ] **步骤 1：运行单元测试**

运行：`./gradlew.bat testDebugUnitTest`
预期：现有 Robolectric 测试通过。

- [ ] **步骤 2：运行 Debug 构建**

运行：`./gradlew.bat :app:assembleDebug`
预期：Debug APK 构建通过。

- [ ] **步骤 3：手动验证建议**

安装后打开 App，开启“后台待命通知”，授权悬浮窗权限，回到 App 后确认通知仍存在、悬浮按钮出现，点击悬浮按钮进入扫码界面。
