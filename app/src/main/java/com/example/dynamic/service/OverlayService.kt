package com.example.dynamic.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager

class OverlayService : Service() {
    
    private var overlayManager: OverlayManager? = null
    private var dragHandler: DragHandler? = null
    private var isOverlayVisible = false
    
    // LocalBinder for ViewModel binding
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): OverlayService = this@OverlayService
    }
    
    companion object {
        private const val TAG = "OverlayService"
        
        // SharedPreferences 키
        private const val PREF_NAME = "overlay_prefs"
        private const val KEY_OVERLAY_FLAG = "overlay_flag"
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OverlayService onCreate")
        setupOverlay()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 플래그 기반 오버레이 제어만 사용
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "OverlayService onDestroy")
        cleanupOverlay()
    }
    

    
    /**
     * 현재 플래그 상태 확인
     */
    private fun getOverlayFlag(): Int {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_OVERLAY_FLAG, 0)
    }
    
    /**
     * 플래그 상태 저장
     */
    fun setOverlayFlag(flag: Int) {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_OVERLAY_FLAG, flag).apply()
        
        Log.d(TAG, "플래그 변경됨: $flag")
        
        // 플래그 변경 시 오버레이 완전히 재생성 (새로운 XML 적용)
        if (flag == 1) {
            recreateOverlay()
        } else {
            cleanupOverlay()
        }
    }
    
    /**
     * 실제 View의 visibility 상태 확인
     */
    private fun getActualViewVisibility(): String {
        val view = overlayManager?.getOverlayView()
        return when (view?.visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE" 
            View.GONE -> "GONE"
            else -> "NULL"
        }
    }
    
    /**
     * 오버레이 visibility 업데이트 (핵심 로직)
     * 플래그 상태만으로 오버레이 표시/숨김 제어 (앱 상태 무관)
     */
    private fun updateOverlayVisibility() {
        val currentFlag = getOverlayFlag()
        val shouldShow = currentFlag == 1  // 앱 상태 체크 제거 - 오직 플래그만으로 제어
        val actualVisibility = getActualViewVisibility()
        
        Log.d(TAG, "상태 체크 - 플래그: $currentFlag, 추적된오버레이: $isOverlayVisible")
        Log.d(TAG, "실제 View visibility: $actualVisibility")
        Log.d(TAG, "표시해야함: $shouldShow (플래그만으로 결정)")
        
        // 실제 View 상태와 추적 상태가 다르면 경고
        val actuallyVisible = (actualVisibility == "VISIBLE")
        if (actuallyVisible != isOverlayVisible) {
            Log.e(TAG, "상태 불일치! 추적: $isOverlayVisible, 실제: $actuallyVisible")
            // 실제 상태로 동기화
            isOverlayVisible = actuallyVisible
        }
        
        if (shouldShow && !isOverlayVisible) {
            // 표시해야 하는데 안 보이면 → 표시
            Log.d(TAG, "오버레이 표시 (플래그=1)")
            showOverlay()
        } else if (!shouldShow && isOverlayVisible) {
            // 숨겨야 하는데 보이면 → 숨김
            Log.d(TAG, "오버레이 숨김 (플래그=0)")
            hideOverlay()
        } else {
            Log.d(TAG, "상태 변화 없음")
        }
        
        // 최종 상태 확인
        Log.d(TAG, "최종 상태 - 추적: $isOverlayVisible, 실제: ${getActualViewVisibility()}")
    }
    
    /**
     * 오버레이 설정 및 생성
     */
    private fun setupOverlay() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // OverlayManager 초기화
        overlayManager = OverlayManager(this, windowManager)
        
        // DragHandler 초기화
        dragHandler = DragHandler(windowManager, resources, this)
        
        // 오버레이 생성 (처음에는 숨김 상태)
        createOverlay()
    }
    
    /**
     * 오버레이 완전히 재생성 (새로운 XML 적용)
     */
    private fun recreateOverlay() {
        Log.d(TAG, "오버레이 재생성 시작")
        
        // 기존 오버레이 완전히 제거
        cleanupOverlay()
        
        // 새로운 오버레이 생성
        setupOverlay()
        
        // 현재 상태에 맞춰 visibility 설정
        updateOverlayVisibility()
        
        Log.d(TAG, "오버레이 재생성 완료")
    }
    
    /**
     * 오버레이 생성
     */
    private fun createOverlay() {
        val touchListener = createTouchListener()
        val result = overlayManager?.createOverlay(touchListener)
        
        if (result == null) {
            Log.e(TAG, "오버레이 생성 실패")
            // 오버레이 생성 실패 시 서비스 종료
            stopSelf()
        } else {
            val (view, params) = result
            Log.d(TAG, "오버레이 생성 성공 (XML inflate)")
            Log.d(TAG, "생성 직후 View visibility: ${getActualViewVisibility()}")
            
            // 서비스 시작 시 강제로 숨김 상태로 설정
            forceHideOverlay()
        }
    }
    
    /**
     * 터치 리스너 생성 (DragHandler와 연결)
     */
    private fun createTouchListener(): View.OnTouchListener {
        return View.OnTouchListener { _, event ->
            val overlayView = overlayManager?.getOverlayView()
            val layoutParams = overlayManager?.getLayoutParams()
            
            if (overlayView != null && layoutParams != null) {
                dragHandler?.handleTouchEvent(event, overlayView, layoutParams) ?: false
            } else {
                false
            }
        }
    }
    
    /**
     * 오버레이 표시 (내부 사용)
     */
    private fun showOverlay() {
        val view = overlayManager?.getOverlayView()
        if (view != null && !isOverlayVisible) {
            view.visibility = View.VISIBLE
            isOverlayVisible = true
            Log.d(TAG, "오버레이 표시됨 (isOverlayVisible = true)")
            Log.d(TAG, "표시 후 실제 visibility: ${getActualViewVisibility()}")
        }
    }
    
    /**
     * 오버레이 숨기기 (내부 사용)
     */
    private fun hideOverlay() {
        val view = overlayManager?.getOverlayView()
        if (view != null && isOverlayVisible) {
            view.visibility = View.GONE
            isOverlayVisible = false
            Log.d(TAG, "오버레이 숨겨짐 (isOverlayVisible = false)")
            Log.d(TAG, "숨김 후 실제 visibility: ${getActualViewVisibility()}")
        }
    }
    
    /**
     * 강제로 오버레이 숨기기 (초기화용)
     */
    private fun forceHideOverlay() {
        val view = overlayManager?.getOverlayView()
        if (view != null) {
            view.visibility = View.GONE
            isOverlayVisible = false
            Log.d(TAG, "강제 숨김 완료 (isOverlayVisible = false)")
            Log.d(TAG, "강제 숨김 후 실제 visibility: ${getActualViewVisibility()}")
        }
    }
    
    /**
     * 오버레이 정리 및 제거
     */
    private fun cleanupOverlay() {
        overlayManager?.removeOverlay()
        overlayManager = null
        dragHandler = null
        isOverlayVisible = false
        Log.d(TAG, "오버레이 정리 완료")
    }
    
    /**
     * 오버레이 생성 상태 확인
     */
    fun isOverlayActive(): Boolean {
        return overlayManager?.isOverlayCreated() == true && getOverlayFlag() == 1
    }
    
    /**
     * 오버레이 표시 상태 확인 (플래그 상태만으로 결정)
     */
    fun isOverlayVisible(): Boolean = isOverlayVisible && getOverlayFlag() == 1
} 