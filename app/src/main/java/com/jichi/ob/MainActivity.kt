package com.jichi.ob
 
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.slider.Slider
import com.jichi.ob.api.IgpsportApi
import com.jichi.ob.api.MageneApi
import com.jichi.ob.api.OutbaseApi
import com.jichi.ob.api.XingzheApi
import com.jichi.ob.model.ActivityRecord
import com.jichi.ob.model.DataSource
import com.jichi.ob.model.FileKind
import com.jichi.ob.ui.LoginWebActivity
import com.jichi.ob.util.PrefsManager
import com.jichi.ob.util.WebBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
 
/**
 * 鸡翅幸哲迈进OB v6.0.7
 * iGPSPORT / 行者 / 迈金 → Outbase 三平台聚合同步
 * 行者GPX直传（Outbase网页内置gpx转换）；CDN多策略鉴权探测+网页内回退
 */
class MainActivity : AppCompatActivity() {
 
    companion object {
        private const val TAG = "JichiOB"
        private const val APP_VERSION = "v6.0.9"
    }
 
    private lateinit var prefs: PrefsManager
    private lateinit var igpsportApi: IgpsportApi
    private lateinit var xingzheApi: XingzheApi
    private lateinit var mageneApi: MageneApi
    private lateinit var outbaseApi: OutbaseApi
    private var webBridge: WebBridge? = null
 
    private lateinit var tvIgpStatus: TextView
    private lateinit var tvXingzheStatus: TextView
    private lateinit var tvMageneStatus: TextView
    private lateinit var tvOutbaseStatus: TextView
    private lateinit var btnIgpLogin: MaterialButton
    private lateinit var btnXingzheLogin: MaterialButton
    private lateinit var btnMageneLogin: MaterialButton
    private lateinit var btnOutbaseLogin: MaterialButton
    private lateinit var btnSync: MaterialButton
    private lateinit var btnTest: MaterialButton
    private lateinit var btnCopyLog: MaterialButton
    private lateinit var sliderCount: Slider
    private lateinit var tvCount: TextView
    private lateinit var sliderSkip: Slider
    private lateinit var tvSkip: TextView
    private lateinit var tvLog: TextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipIgp: Chip
    private lateinit var chipXingzhe: Chip
    private lateinit var chipMagene: Chip
    private lateinit var chipAll: Chip
    private lateinit var logScrollView: ScrollView
 
    @Suppress("DEPRECATION")
    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                val type = data.getStringExtra(LoginWebActivity.RESULT_LOGIN_TYPE) ?: ""
                val token = data.getStringExtra(LoginWebActivity.RESULT_TOKEN) ?: ""
                val sid = data.getStringExtra(LoginWebActivity.RESULT_SESSION_ID) ?: ""
                val extra = data.getStringExtra(LoginWebActivity.RESULT_EXTRA) ?: ""
                when (type) {
                    LoginWebActivity.TYPE_IGPSPORT -> if (token.length > 20) {
                        prefs.saveIgpsportToken(token)
                        appendLog("✅ iGPSPORT登录成功! token长度=${token.length}")
                    }
                    LoginWebActivity.TYPE_XINGZHE -> if (sid.length > 10) {
                        prefs.saveXingzheSessionId(sid)
                        appendLog("✅ 行者登录成功! session长度=${sid.length}")
                    }
                    LoginWebActivity.TYPE_MAGENE -> if (token.length > 20) {
                        prefs.saveMageneToken(token)
                        if (extra.isNotEmpty()) prefs.saveMageneRefreshToken(extra)
                        appendLog("✅ 迈金登录成功! token长度=${token.length}")
                    }
                    LoginWebActivity.TYPE_OUTBASE -> if (sid.length > 10) {
                        prefs.saveOutbaseSessionId(sid)
                        prefs.saveGatewayCookies(extra)
                        appendLog("✅ Outbase登录成功! sessionId长度=${sid.length}" +
                                if (extra.isNotEmpty()) " (已捕获网关cookie)" else "")
                    }
                }
                updateStatusUI()
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                appendLog("ℹ️ 登录取消")
            }
        } catch (e: Exception) {
            Log.e(TAG, "login result error", e)
            appendLog("❌ 登录回调异常: ${e.message}")
        }
    }
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            prefs = PrefsManager(this)
            igpsportApi = IgpsportApi()
            xingzheApi = XingzheApi()
            mageneApi = MageneApi()
            outbaseApi = OutbaseApi()
            initViews()
            setupListeners()
            updateStatusUI()
            appendLog("🚴 鸡翅幸哲迈进OB $APP_VERSION 启动")
            appendLog("📱 Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLog("ℹ️ 登录: iGP=${flag(prefs.isIgpsportLoggedIn())} 行者=${flag(prefs.isXingzheLoggedIn())} 迈金=${flag(prefs.isMageneLoggedIn())} OB=${flag(prefs.isOutbaseLoggedIn())}")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate failed", e)
        }
    }
 
    private fun flag(ok: Boolean) = if (ok) "✅" else "❌"
 
    private fun initViews() {
        tvIgpStatus = findViewById(R.id.tvIgpStatus)
        tvXingzheStatus = findViewById(R.id.tvXingzheStatus)
        tvMageneStatus = findViewById(R.id.tvMageneStatus)
        tvOutbaseStatus = findViewById(R.id.tvOutbaseStatus)
        btnIgpLogin = findViewById(R.id.btnIgpLogin)
        btnXingzheLogin = findViewById(R.id.btnXingzheLogin)
        btnMageneLogin = findViewById(R.id.btnMageneLogin)
        btnOutbaseLogin = findViewById(R.id.btnOutbaseLogin)
        btnSync = findViewById(R.id.btnSync)
        btnTest = findViewById(R.id.btnTest)
        btnCopyLog = findViewById(R.id.btnCopyLog)
        sliderCount = findViewById(R.id.sliderCount)
        tvCount = findViewById(R.id.tvCount)
        sliderSkip = findViewById(R.id.sliderSkip)
        tvSkip = findViewById(R.id.tvSkip)
        tvLog = findViewById(R.id.tvLog)
        progressBar = findViewById(R.id.progressBar)
        chipGroup = findViewById(R.id.chipGroupSource)
        chipIgp = findViewById(R.id.chipIgp)
        chipXingzhe = findViewById(R.id.chipXingzhe)
        chipMagene = findViewById(R.id.chipMagene)
        chipAll = findViewById(R.id.chipAll)
        logScrollView = findViewById(R.id.svLog)
    }
 
    private fun setupListeners() {
        btnIgpLogin.setOnClickListener { openLogin(LoginWebActivity.TYPE_IGPSPORT, IgpsportApi.LOGIN_URL, "iGPSPORT") }
        btnXingzheLogin.setOnClickListener { openLogin(LoginWebActivity.TYPE_XINGZHE, XingzheApi.LOGIN_URL, "行者") }
        btnMageneLogin.setOnClickListener { openLogin(LoginWebActivity.TYPE_MAGENE, MageneApi.LOGIN_URL, "迈金") }
        btnOutbaseLogin.setOnClickListener { openLogin(LoginWebActivity.TYPE_OUTBASE, OutbaseApi.LOGIN_URL, "Outbase") }
 
        sliderCount.addOnChangeListener { _, v, _ -> tvCount.text = v.toInt().toString() }
        sliderSkip.addOnChangeListener { _, v, _ -> tvSkip.text = v.toInt().toString() }
 
        btnSync.setOnClickListener { startSync() }
        btnTest.setOnClickListener { testDownload() }
        btnCopyLog.setOnClickListener {
            val log = tvLog.text?.toString() ?: ""
            if (log.isNotBlank()) {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("运行日志", log))
                Toast.makeText(this, "日志已复制", Toast.LENGTH_SHORT).show()
            }
        }
    }
 
    private fun openLogin(type: String, url: String, name: String) {
        appendLog("🔐 打开 $name 登录页...")
        val intent = Intent(this, LoginWebActivity::class.java)
        intent.putExtra(LoginWebActivity.EXTRA_LOGIN_TYPE, type)
        intent.putExtra(LoginWebActivity.EXTRA_URL, url)
        loginLauncher.launch(intent)
    }
 
    private fun updateStatusUI() {
        try {
            fun set(tv: TextView, btn: MaterialButton, ok: Boolean, loginText: String) {
                tv.text = if (ok) "✅ 已登录" else "❌ 未登录"
                tv.setTextColor(getColor(if (ok) R.color.green else R.color.red))
                btn.text = if (ok) "重新登录" else loginText
            }
            set(tvIgpStatus, btnIgpLogin, prefs.isIgpsportLoggedIn(), "登录 iGPSPORT")
            set(tvXingzheStatus, btnXingzheLogin, prefs.isXingzheLoggedIn(), "登录行者")
            set(tvMageneStatus, btnMageneLogin, prefs.isMageneLoggedIn(), "登录迈金")
            set(tvOutbaseStatus, btnOutbaseLogin, prefs.isOutbaseLoggedIn(), "登录 Outbase")
        } catch (e: Exception) {
            Log.e(TAG, "updateStatusUI error", e)
        }
    }
 
    private fun appendLog(message: String) {
        val ts = try { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()) } catch (_: Exception) { "??:??:??" }
        Log.i(TAG, message)
        try {
            runOnUiThread {
                val cur = tvLog.text?.toString() ?: ""
                tvLog.text = if (cur.isBlank() || cur == "等待操作...") "[$ts] $message" else "$cur\n[$ts] $message"
                logScrollView.post { try { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) } catch (_: Exception) {} }
            }
        } catch (e: Exception) {
            Log.e(TAG, "appendLog error", e)
        }
    }
 
    private fun setSyncing(syncing: Boolean) {
        runOnUiThread {
            btnSync.isEnabled = !syncing
            btnTest.isEnabled = !syncing
            btnCopyLog.isEnabled = !syncing
            btnSync.text = if (syncing) "⏳ 同步中..." else "🚴 开始同步到 Outbase"
            progressBar.visibility = if (syncing) View.VISIBLE else View.GONE
            if (syncing) progressBar.isIndeterminate = true
        }
    }
 
    private fun selectedSources(): List<DataSource> {
        return when {
            chipAll.isChecked -> listOf(DataSource.IGPSPORT, DataSource.XINGZHE, DataSource.MAGENE)
            chipIgp.isChecked -> listOf(DataSource.IGPSPORT)
            chipXingzhe.isChecked -> listOf(DataSource.XINGZHE)
            chipMagene.isChecked -> listOf(DataSource.MAGENE)
            else -> listOf(DataSource.IGPSPORT)
        }
    }
 
    private suspend fun bridge(): WebBridge {
        if (webBridge == null) webBridge = WebBridge(this)
        return webBridge!!
    }
 
    private suspend fun fetchActivities(source: DataSource, skip: Int, count: Int): List<ActivityRecord> {
        return when (source) {
            DataSource.IGPSPORT -> igpsportApi.getActivities(prefs.getIgpsportToken()!!, skip, count)
            DataSource.XINGZHE -> xingzheApi.getActivities(prefs.getXingzheSessionId()!!, skip, count)
            DataSource.MAGENE -> mageneApi.getActivities(prefs.getMageneToken()!!, skip, count)
        }
    }
 
    /** 下载文件（字节 + 扩展名 + 方式描述） */
    private suspend fun downloadFile(act: ActivityRecord): Triple<ByteArray, String, String> {
        return when (act.source) {
            DataSource.IGPSPORT -> {
                val bytes = igpsportApi.downloadFitFile(prefs.getIgpsportToken()!!, act.id, act.extra)
                Triple(bytes, "fit", "FIT原生")
            }
            DataSource.XINGZHE -> {
                val (bytes, kind) = xingzheApi.downloadGpxOrFit(prefs.getXingzheSessionId()!!, act.id)
                if (kind == FileKind.FIT) {
                    Triple(bytes, "fit", "FIT原生")
                } else {
                    // Outbase只收FIT：GPX经内置官方转换器转FIT
                    val fit = bridge().convertGpxToFit(bytes)
                    Triple(fit, "fit", "GPX→FIT")
                }
            }
            DataSource.MAGENE -> {
                val bytes = mageneApi.downloadFit(prefs.getMageneToken()!!, act.id)
                Triple(bytes, "fit", "FIT直链")
            }
        }
    }
 
    private fun testDownload() {
        val sources = selectedSources()
        appendLog("━━━━━━━━━━━━━━━━━━━━━━")
        appendLog("🧪 测试下载模式（${sources.joinToString("+") { it.displayName }}，不上传）")
        for (s in sources) {
            val logged = when (s) {
                DataSource.IGPSPORT -> prefs.isIgpsportLoggedIn()
                DataSource.XINGZHE -> prefs.isXingzheLoggedIn()
                DataSource.MAGENE -> prefs.isMageneLoggedIn()
            }
            if (!logged) { appendLog("❌ 请先登录 ${s.displayName}"); return }
        }
        setSyncing(true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                for (s in sources) {
                    appendLog("📥 [${s.displayName}] 获取活动列表...")
                    try {
                        val acts = fetchActivities(s, 0, 5)
                        appendLog("📋 [${s.displayName}] ${acts.size} 条:")
                        for ((i, a) in acts.withIndex()) {
                            appendLog("   [${i + 1}] ${a.id.take(12)} | ${a.title.ifEmpty { "(无标题)" }} | ${a.startTime.take(19)} | ${"%.1f".format(a.distance)}km")
                        }
                        if (acts.isNotEmpty()) {
                            val first = acts[0]
                            appendLog("⬇️ [${s.displayName}] 测试下载: ${first.title}")
                            val (bytes, ext, how) = downloadFile(first)
                            appendLog("✅ [${s.displayName}] $how 成功! ${bytes.size} bytes (.$ext) 头:${bytes.take(12).joinToString("") { "%02x".format(it) }}")
                        }
                    } catch (e: Exception) {
                        appendLog("❌ [${s.displayName}] ${e.javaClass.simpleName}: ${e.message}")
                    }
                }
            } finally {
                setSyncing(false)
                appendLog("🧪 测试完成")
                appendLog("━━━━━━━━━━━━━━━━━━━━━━")
            }
        }
    }
 
    private fun startSync() {
        val count = sliderCount.value.toInt()
        val skip = sliderSkip.value.toInt()
        val sources = selectedSources()
        appendLog("━━━━━━━━━━━━━━━━━━━━━━")
        appendLog("🚀 开始同步 (${sources.joinToString("+") { it.displayName }} 跳过$skip 同步$count)")
 
        if (!prefs.isOutbaseLoggedIn()) { appendLog("❌ 请先登录 Outbase"); return }
        for (s in sources) {
            val logged = when (s) {
                DataSource.IGPSPORT -> prefs.isIgpsportLoggedIn()
                DataSource.XINGZHE -> prefs.isXingzheLoggedIn()
                DataSource.MAGENE -> prefs.isMageneLoggedIn()
            }
            if (!logged) { appendLog("❌ 请先登录 ${s.displayName}"); return }
        }
 
        setSyncing(true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val all = mutableListOf<ActivityRecord>()
                for (s in sources) {
                    appendLog("📥 [${s.displayName}] 获取活动 (skip=$skip, count=$count)...")
                    try {
                        val acts = fetchActivities(s, skip, count)
                        appendLog("📋 [${s.displayName}] 获取到 ${acts.size} 条")
                        all.addAll(acts)
                    } catch (e: Exception) {
                        appendLog("⚠️ [${s.displayName}] 获取失败: ${e.message}")
                    }
                }
                if (all.isEmpty()) {
                    appendLog("❌ 未获取到任何活动")
                    setSyncing(false)
                    return@launch
                }
 
                val obSid = prefs.getOutbaseSessionId()!!
 
                appendLog("🔍 校验Outbase会话...")
                if (!outbaseApi.warmUp(obSid)) {
                    appendLog("⚠️ Outbase会话校验未通过（仍尝试上传）")
                } else {
                    appendLog("✅ Outbase会话有效")
                }
 
                val br = bridge()
 
                withContext(Dispatchers.Main) {
                    progressBar.isIndeterminate = false
                    progressBar.max = all.size
                    progressBar.progress = 0
                }
 
                var success = 0; var skipped = 0; var failed = 0; var noFile = 0
                var sessionExpired = false
                for ((i, act) in all.withIndex()) {
                    if (sessionExpired) { failed += all.size - i; break }
                    try {
                        appendLog("⬇️ [${i + 1}/${all.size}] (${act.source.displayName}) ${act.title.ifEmpty { act.id }}")
                        val (bytes, ext, how) = downloadFile(act)
                        appendLog("   📦 $how ${bytes.size} bytes")
 
                        val safeTime = act.startTime.replace(":", "-").replace(" ", "_").take(19)
                        val fileName = "${act.source.displayName}_${act.id.take(16)}_$safeTime.$ext"
                        appendLog("⬆️ 上传 $fileName")
                        val (msg, dup, _) = outbaseApi.upload(obSid, br, bytes, fileName)
                        if (dup) { skipped++; appendLog("⏭️ $msg") } else { success++; appendLog("✅ $msg") }
                        withContext(Dispatchers.Main) { progressBar.progress = i + 1 }
                    } catch (e: MageneApi.NoFileException) {
                        noFile++
                        appendLog("⏭️ 无文件跳过: ${e.message}")
                    } catch (e: OutbaseApi.UploadException) {
                        failed++
                        appendLog("❌ ${e.message}")
                        if (e.sessionExpired) {
                            sessionExpired = true
                            appendLog("⚠️ Outbase登录态失效，请重新登录后再试")
                        }
                    } catch (e: Exception) {
                        failed++
                        appendLog("❌ ${e.javaClass.simpleName}: ${e.message}")
                        if (e.message?.contains("重新登录") == true) {
                            appendLog("⚠️ ${act.source.displayName} 登录已过期，后续记录跳过")
                            failed += all.size - i - 1
                            break
                        }
                    }
                    if (i < all.size - 1) kotlinx.coroutines.delay(300)
                }
 
                appendLog("═══════════════════════")
                appendLog("📊 完成! ✅$success ⏭️重复$skipped 📭无文件$noFile ❌$failed / ${all.size}")
                appendLog("═══════════════════════")
            } catch (e: Exception) {
                appendLog("💥 ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                setSyncing(false)
            }
        }
    }
 
    override fun onDestroy() {
        try { webBridge?.destroy() } catch (_: Exception) {}
        super.onDestroy()
    }
}
 

IgpsportApi.kt — iGPSPORT接口