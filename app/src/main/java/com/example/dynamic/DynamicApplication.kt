package com.example.dynamic

import android.app.Application
import android.content.Intent
import android.util.Log
import com.example.dynamic.service.OverlayService

class DynamicApplication : Application() {
    
    private var isOverlayServiceRunning = false
    
    companion object {
        private const val TAG = "DynamicApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "DynamicApplication onCreate")
    }
    
    /**
     * 오버레이 서비스 시작 (외부에서 호출)
     */
    fun startOverlayService() {
        if (!isOverlayServiceRunning) {
            Log.d(TAG, "오버레이 서비스 시작")
            val intent = Intent(this, OverlayService::class.java)
            startService(intent)
            isOverlayServiceRunning = true
        } else {
            Log.d(TAG, "⚠오버레이 서비스 이미 실행 중")
        }
    }
    
    /**
     * 오버레이 서비스 중지 (외부에서 호출)
     */
    fun stopOverlayService() {
        if (isOverlayServiceRunning) {
            Log.d(TAG, "오버레이 서비스 중지")
            val intent = Intent(this, OverlayService::class.java)
            stopService(intent)
            isOverlayServiceRunning = false
        } else {
            Log.d(TAG, "오버레이 서비스 이미 중지됨")
        }
    }
    
    /**
     * 오버레이 서비스 실행 상태 확인
     */
    fun isOverlayServiceRunning(): Boolean = isOverlayServiceRunning
} 