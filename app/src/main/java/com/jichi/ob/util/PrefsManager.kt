package com.jichi.ob.util
 
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
 
/**
 * 本地存储：三平台凭证 + Outbase
 */
class PrefsManager(context: Context) {
 
    companion object {
        private const val TAG = "PrefsManager"
    }
 
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jichi_ob", Context.MODE_PRIVATE)
 
    // iGPSPORT: Bearer token
    fun saveIgpsportToken(token: String) {
        Log.d(TAG, "saveIgpsportToken: ${token.length}")
        prefs.edit().putString("igpsport_token", token).apply()
    }
    fun getIgpsportToken(): String? = prefs.getString("igpsport_token", null)
    fun isIgpsportLoggedIn(): Boolean = !getIgpsportToken().isNullOrEmpty()
 
    // 行者: sessionid cookie
    fun saveXingzheSessionId(sid: String) {
        Log.d(TAG, "saveXingzheSessionId: ${sid.length}")
        prefs.edit().putString("xingzhe_session_id", sid).apply()
    }
    fun getXingzheSessionId(): String? = prefs.getString("xingzhe_session_id", null)
    fun isXingzheLoggedIn(): Boolean = !getXingzheSessionId().isNullOrEmpty()
 
    // 迈金: OTM token (JWT)
    fun saveMageneToken(token: String) {
        Log.d(TAG, "saveMageneToken: ${token.length}")
        prefs.edit().putString("magene_token", token).apply()
    }
    fun getMageneToken(): String? = prefs.getString("magene_token", null)
    fun saveMageneRefreshToken(token: String) {
        prefs.edit().putString("magene_refresh_token", token).apply()
    }
    fun getMageneRefreshToken(): String? = prefs.getString("magene_refresh_token", null)
    fun isMageneLoggedIn(): Boolean = !getMageneToken().isNullOrEmpty()
 
    // Outbase: sessionId + 网关cookie
    fun saveOutbaseSessionId(sid: String) {
        Log.d(TAG, "saveOutbaseSessionId: ${sid.length}")
        prefs.edit().putString("outbase_session_id", sid).apply()
    }
    fun getOutbaseSessionId(): String? = prefs.getString("outbase_session_id", null)
    fun saveGatewayCookies(cookies: String) {
        Log.d(TAG, "saveGatewayCookies: ${cookies.length}")
        prefs.edit().putString("gateway_cookies", cookies).apply()
    }
    fun getGatewayCookies(): String? = prefs.getString("gateway_cookies", null)
    fun saveCdnStrategy(s: String) {
        Log.d(TAG, "saveCdnStrategy: $s")
        prefs.edit().putString("cdn_strategy", s).apply()
    }
    fun getCdnStrategy(): String? = prefs.getString("cdn_strategy", null)
    fun isOutbaseLoggedIn(): Boolean = !getOutbaseSessionId().isNullOrEmpty()
 
    fun clearAll() = prefs.edit().clear().apply()
}
 

WebBridge.kt — WebView桥