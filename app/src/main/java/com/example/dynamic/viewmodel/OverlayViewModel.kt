package com.example.dynamic.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.dynamic.DynamicApplication
import com.example.dynamic.service.OverlayService

class OverlayViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val app = application as DynamicApplication
    
    // SharedPreferences 관련
    private val prefs = context.getSharedPreferences("overlay_prefs", Context.MODE_PRIVATE)
    
    // 플래그 상태 (0: 비활성화, 1: 활성화)
    private val _overlayFlag = MutableLiveData<Int>()
    val overlayFlag: LiveData<Int> = _overlayFlag
    
    // OverlayService 연결
    private var overlayService: OverlayService? = null
    private var isServiceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? OverlayService.LocalBinder
            overlayService = binder?.getService()
            isServiceBound = true
            
            // 서비스 연결 시 현재 플래그 상태 동기화
            val currentFlag = _overlayFlag.value ?: 0
            overlayService?.setOverlayFlag(currentFlag)
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            overlayService = null
            isServiceBound = false
        }
    }
    
    init {
        // SharedPreferences에서 저장된 플래그 상태 로드 (즉시 UI 업데이트)
        val savedFlag = prefs.getInt("overlay_flag", 0)
        _overlayFlag.value = savedFlag
        
        // 저장된 플래그가 1이면 서비스 시작 (앱 재시작 시 복원)
        if (savedFlag == 1) {
            startOverlayService()
        }
    }
    
    /**
     * 오버레이 플래그 토글 (0 ↔ 1)
     */
    fun toggleOverlayFlag() {
        val currentFlag = _overlayFlag.value ?: 0
        val newFlag = if (currentFlag == 0) 1 else 0
        setOverlayFlag(newFlag)
    }
    
    /**
     * 오버레이 플래그 설정
     */
    private fun setOverlayFlag(flag: Int) {
        // LiveData 즉시 업데이트 (UI 반응성 향상)
        _overlayFlag.value = flag
        
        // SharedPreferences에 저장
        prefs.edit().putInt("overlay_flag", flag).apply()
        
        // 플래그에 따라 서비스 시작/중지
        if (flag == 1) {
            startOverlayService()
        } else {
            stopOverlayService()
        }
    }
    
    /**
     * 오버레이 서비스 시작
     */
    private fun startOverlayService() {
        // DynamicApplication을 통해 서비스 시작
        app.startOverlayService()
        
        // 서비스와 바인딩
        if (!isServiceBound) {
            val intent = Intent(context, OverlayService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    
    /**
     * 오버레이 서비스 중지
     */
    private fun stopOverlayService() {
        // 서비스 바인딩 해제
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
            overlayService = null
        }
        
        // DynamicApplication을 통해 서비스 중지
        app.stopOverlayService()
    }
    
    /**
     * 오버레이가 활성화 상태인지 확인
     */
    fun isOverlayActive(): Boolean {
        return (_overlayFlag.value ?: 0) == 1
    }
    
    /**
     * 오버레이 상태 텍스트 반환
     */
    fun getOverlayStatusText(): String {
        val flag = _overlayFlag.value ?: 0
        return if (flag == 1) {
            "오버레이가 활성화되었습니다 (플래그: $flag)"
        } else {
            "오버레이가 비활성화되었습니다 (플래그: $flag)"
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // 서비스 바인딩 해제
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }
    }
} 