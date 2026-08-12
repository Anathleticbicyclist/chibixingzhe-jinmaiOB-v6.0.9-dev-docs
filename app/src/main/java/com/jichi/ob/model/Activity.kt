package com.jichi.ob.model
 
/**
 * 活动记录数据模型（三平台通用）
 */
data class ActivityRecord(
    val id: String,
    val title: String,
    val startTime: String,
    val distance: Double,   // km
    val duration: Int,      // seconds
    val source: DataSource,
    var extra: String? = null  // 平台附加信息（iGP的downloadUrl / 迈金的rid等）
)
 
enum class DataSource(val displayName: String) {
    IGPSPORT("iGPSPORT"),
    XINGZHE("行者"),
    MAGENE("迈金")
}
 
enum class FileKind(val ext: String, val displayName: String) {
    FIT("fit", "FIT"),
    GPX("gpx", "GPX"),
    UNKNOWN("", "未知")
}
 

LoginWebActivity.kt — 四平台WebView登录