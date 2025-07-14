package com.example.dynamic

import android.app.Application
import android.content.Intent
import android.util.Log
import com.example.dynamic.service.OverlayService

// 앱 전체에서 사용할 전역 설정을 관리
class DynamicApplication : Application() {

    private var isOverlayServiceRunning = false // 오버레이 서비스가 실행 중인지 여부 파악
    
    companion object {
        private const val TAG = "DynamicApplication"
    }

    // 시작 로그를 띄우기 위한 코드
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DynamicApplication onCreate")
    }
    
    // 오버레이 서비스 시작
    fun startOverlayService() {
        // 실행 중이 아니면 실행
        if (!isOverlayServiceRunning) {
            Log.d(TAG, "오버레이 서비스 시작")
            // OverlayService를 실행하기 위한 인텐트 생성
            val intent = Intent(this, OverlayService::class.java)
            startService(intent) // 서비스 시작
            isOverlayServiceRunning = true // 실행 상태 플래그 업데이트
        } else {
            Log.d(TAG, "오버레이 서비스 이미 실행 중")
        }
    }
    

    // 오버레이 서비스 중지 (외부에서 호출)
    fun stopOverlayService() {
        // 실행 중이면 중지
        if (isOverlayServiceRunning) {
            Log.d(TAG, "오버레이 서비스 중지")
            // OverlayService를 실행하기 위한 인텐트 생성
            val intent = Intent(this, OverlayService::class.java)
            stopService(intent)
            isOverlayServiceRunning = false // 실행 상태 플래그 업데이터
        } else {
            Log.d(TAG, "오버레이 서비스 이미 중지됨")
        }
    }

     // 현재 사용 x - 오버레이 서비스 실행 상태 확인(현재 오버레이 서비스가 실행 중인지 Boolean 값으로 반환)
    fun isOverlayServiceRunning(): Boolean = isOverlayServiceRunning
} 