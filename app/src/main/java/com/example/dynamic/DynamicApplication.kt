package com.example.dynamic

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.example.dynamic.service.OverlayService

class DynamicApplication : Application() {
    
    private var activityCount = 0
    private var isAppInForeground = false
    private var isOverlayServiceRunning = false
    
    companion object {
        private const val TAG = "DynamicApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "🚀 DynamicApplication onCreate")
        
        // 앱 생명주기 관찰 시작
        setupActivityLifecycleCallbacks()
    }
    
    /**
     * Activity 생명주기 콜백 설정
     */
    private fun setupActivityLifecycleCallbacks() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                activityCount++
                Log.d(TAG, "📊 Activity Started - activityCount: $activityCount")
                
                if (!isAppInForeground) {
                    // 앱이 백그라운드에서 포그라운드로 전환
                    isAppInForeground = true
                    Log.d(TAG, "🌅 앱 포그라운드 전환!")
                    notifyAppState(OverlayService.ACTION_APP_FOREGROUND)
                }
            }

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                activityCount--
                Log.d(TAG, "📊 Activity Stopped - activityCount: $activityCount")
                
                if (activityCount == 0 && isAppInForeground) {
                    // 앱이 포그라운드에서 백그라운드로 전환
                    isAppInForeground = false
                    Log.d(TAG, "🌙 앱 백그라운드 전환!")
                    notifyAppState(OverlayService.ACTION_APP_BACKGROUND)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
    
    /**
     * 오버레이 서비스 시작 (외부에서 호출)
     */
    fun startOverlayService() {
        if (!isOverlayServiceRunning) {
            Log.d(TAG, "🔧 오버레이 서비스 시작")
            val intent = Intent(this, OverlayService::class.java)
            startService(intent)
            isOverlayServiceRunning = true
        } else {
            Log.d(TAG, "⚠️ 오버레이 서비스 이미 실행 중")
        }
    }
    
    /**
     * 오버레이 서비스 중지 (외부에서 호출)
     */
    fun stopOverlayService() {
        if (isOverlayServiceRunning) {
            Log.d(TAG, "🛑 오버레이 서비스 중지")
            val intent = Intent(this, OverlayService::class.java)
            stopService(intent)
            isOverlayServiceRunning = false
        } else {
            Log.d(TAG, "⚠️ 오버레이 서비스 이미 중지됨")
        }
    }
    
    /**
     * OverlayService에 앱 상태 알림
     */
    private fun notifyAppState(action: String) {
        if (isOverlayServiceRunning) {
            Log.d(TAG, "📡 서비스에 상태 알림: $action")
            val intent = Intent(this, OverlayService::class.java).apply {
                this.action = action
            }
            startService(intent)
        } else {
            Log.d(TAG, "⚠️ 서비스가 실행 중이 아님 - 상태 알림 생략")
        }
    }
    
    /**
     * 오버레이 서비스 실행 상태 확인
     */
    fun isOverlayServiceRunning(): Boolean = isOverlayServiceRunning
} 