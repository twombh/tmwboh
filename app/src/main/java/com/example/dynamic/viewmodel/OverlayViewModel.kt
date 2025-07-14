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

// AndroidViewModel을 상송받은 뷰모델 클래스
class OverlayViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext // 앱 전체에서 사용 가능한 Context 가져옴
    private val app = application as DynamicApplication // DynamicApplication 클래스 참조 (Start와 Stop 서비스용)
    
    // SharedPreferences 객체 - 오버레이 상태를 계속 저장하고 불러오기 위해 사용
    private val prefs = context.getSharedPreferences("overlay_prefs", Context.MODE_PRIVATE)
    
    // 플래그 상태 (0: 비활성화, 1: 활성화) - view가 플래그 상태를 자동 감지
    private val _overlayFlag = MutableLiveData<Int>()
    val overlayFlag: LiveData<Int> = _overlayFlag
    
    // 연결된 OverlayService를 저장할 변수
    private var overlayService: OverlayService? = null
    private var isServiceBound = false // 서비스가 바인딩 중인지 확인하는 변수

    private val serviceConnection = object : ServiceConnection {
        // 서비스와 성공적으로 연결되었을 때 호출
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? OverlayService.LocalBinder
            overlayService = binder?.getService()
            isServiceBound = true
            
            // 서비스 연결 시 현재 플래그 상태 동기화
            val currentFlag = _overlayFlag.value ?: 0
            overlayService?.setOverlayFlag(currentFlag)
        }

        // 연결 끊겼을 때 호출
        override fun onServiceDisconnected(name: ComponentName?) {
            overlayService = null
            isServiceBound = false
        }
    }
    
    init {
        // 초기화 시 SharedPreferences에서 저장된 플래그 상태 가져옴
        val savedFlag = prefs.getInt("overlay_flag", 0)
        _overlayFlag.value = savedFlag
        
        // 저장된 플래그가 1이면 앱이 꺼졌어도 오버레이 시작
        if (savedFlag == 1) {
            startOverlayService()
        }
    }
    
    // 오버레이 플래그 토글 (0이면 1, 1이면 0)
    fun toggleOverlayFlag() {
        val currentFlag = _overlayFlag.value ?: 0
        val newFlag = if (currentFlag == 0) 1 else 0
        setOverlayFlag(newFlag)
    }
    
    // 오버레이 플래그 설정
    private fun setOverlayFlag(flag: Int) {
        // LiveData 즉시 업데이트 - UI 반영
        _overlayFlag.value = flag
        
        // SharedPreferences에 저장
        prefs.edit().putInt("overlay_flag", flag).apply()
        
        // 플래그에 따라 서비스 시작과 중지
        if (flag == 1) {
            startOverlayService() // 1일때 서비스 시작
        } else {
            stopOverlayService() // 0일때 서비스 종료
        }
    }
    
    // 오버레이 서비스 시작
    private fun startOverlayService() {
        // DynamicApplication을 통해 서비스 시작
        app.startOverlayService()
        
        // 오버레이 서비스 시작 및 바인딩
        if (!isServiceBound) {
            val intent = Intent(context, OverlayService::class.java) // 바인딩 할 intent 생성
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            // Context.BIND_AUTO_CREATE: 서비스가 바인딩된 컴포넌트(액티비티 등)가 활성 상태인 경우에 서비스가 아직 실행 중이지 않으면 시스템이 서비스를 자동으로 시작 - 서비스 명시적 실행 x
        }
    }
    
    // 오버레이 서비스 중지 및 바인딩 해제
    private fun stopOverlayService() {
        // 서비스 바인딩 해제
        if (isServiceBound) {
            context.unbindService(serviceConnection) // 바인딩 해제
            isServiceBound = false
            overlayService = null
        }
        
        // DynamicApplication을 통해 서비스 중지
        app.stopOverlayService()
    }
    
    // 오버레이가 활성화 상태인지 확인
    fun isOverlayActive(): Boolean {
        return (_overlayFlag.value ?: 0) == 1
    }
    
    // 오버레이 상태 텍스트 반환
    fun getOverlayStatusText(): String {
        val flag = _overlayFlag.value ?: 0
        return if (flag == 1) {
            "오버레이가 활성화 (플래그: $flag)"
        } else {
            "오버레이가 비활성화 (플래그: $flag)"
        }
    }

    // 뷰모델이 파괴될 때 호출 - 서비스 정리
    override fun onCleared() {
        super.onCleared()
        // 서비스 바인딩 해제 - 메모리 누수 방지
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }
    }
} 