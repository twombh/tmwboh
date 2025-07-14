package com.example.dynamic.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager

class OverlayService : Service() {
    
    private var overlayManager: OverlayManager? = null // OverlayManager 인스턴스를 저장할 변수 - 오버레이 생성과 제거, 뷰 접근 관리
    private var dragHandler: DragHandler? = null // DragHandler 인스턴스를 저장할 변수 - 오버레이를 사용자가 드래그할 수 있게 함
    private var isOverlayVisible = false // 오버레이가 현재 보이는 상태인지 확인하는 변수

    // 외부에서 서비스에 바인딩할 수 있게 하는 LocalBinder 객체
    private val binder = LocalBinder()

    // 내부 바인더 클래스 - 외부에서 서비스 인스턴스를 얻기 위해 사용
    inner class LocalBinder : Binder() {
        fun getService(): OverlayService = this@OverlayService
    }
    
    companion object {
        private const val TAG = "OverlayService"
        
        // SharedPreferences 파일명과 키값 정의
        private const val PREF_NAME = "overlay_prefs"
        private const val KEY_OVERLAY_FLAG = "overlay_flag"
    }

    // 클라이언트가 서비스에 바인딩할 때 호출 - 바인더 객체 반환
    override fun onBind(intent: Intent?): IBinder = binder

    // 서비스가 처음 생성될 때 호출 - 오버레이 초기화 수행
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OverlayService onCreate")
        setupOverlay() // 오버레이 초기 설정
    }

    // 서비스가 Intent에 의해 시작될 때 호출
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 시스템이 서비스를 죽였을 때 자동 재시작 요청
        return START_STICKY
    }

    // 서비스가 종료될 떄 호출 - 오버레이 제거
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "OverlayService onDestroy")
        cleanupOverlay() // 생성한 오버레이 뷰 제거
    }
    

    
    // 현재 플래그 상태 확인
    private fun getOverlayFlag(): Int {
        // sharedpreference 열기
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // 오버레이 flag값 가져오고 값이 없으면 0
        return prefs.getInt(KEY_OVERLAY_FLAG, 0)
    }
    
    // 플래그 상태 저장
    fun setOverlayFlag(flag: Int) {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // 오버레이 상태 값을 KEY_OVERLAY_FLAG에 저장하고 비동기로 업데이트
        prefs.edit().putInt(KEY_OVERLAY_FLAG, flag).apply()
        
        Log.d(TAG, "플래그 변경됨: $flag")

        // 플래그가 1이면 오버레이를 새롭게 생성하고 0이면 기존 오버레이를 제거
        if (flag == 1) {
            recreateOverlay()
        } else {
            cleanupOverlay()
        }
    }
    
    // 실제 뷰의 visibility 상태 확인
    private fun getActualViewVisibility(): String {
        // overlayManager로부터 현재 오버레이 뷰 객체를 가져옴
        val view = overlayManager?.getOverlayView()
        return when (view?.visibility) {
            View.VISIBLE -> "VISIBLE" // 사용자에게 보임
            View.INVISIBLE -> "INVISIBLE" // 보이지 않지만 layout은 유지
            View.GONE -> "GONE" // 보이지도 않고 layout에서도 제외
            else -> "NULL" // 객체가 없을때
        }
    }
    
    // 플래그 상태로 오버레이 표시와 숨김 제어
    private fun updateOverlayVisibility() {
        val currentFlag = getOverlayFlag()
        val shouldShow = currentFlag == 1  // flag 상태 저장
        val actualVisibility = getActualViewVisibility() // 실제 View의 visibility 값을 가져옴
        
        Log.d(TAG, "상태 체크 - 플래그: $currentFlag, 오버레이보이는지: $isOverlayVisible")
        Log.d(TAG, "실제 View visibility: $actualVisibility")
        Log.d(TAG, "표시해야함: $shouldShow (플래그만으로 결정)")
        
        // 실제 View 상태와 '오버레이보이는지' 상태가 다르면 경고 - 에러 처리
        val actuallyVisible = (actualVisibility == "VISIBLE")
        if (actuallyVisible != isOverlayVisible) {
            Log.e(TAG, "상태 불일치 - 오버레이보이는지: $isOverlayVisible, 실제: $actuallyVisible")
            // 실제 상태로 동기화
            isOverlayVisible = actuallyVisible
        }

        // 플래그에 따라 오버레이를 표시해야 하지만 현재 안 보이는 경우엔 보여주기
        if (shouldShow && !isOverlayVisible) {
            // 표시해야 하는데 안 보이면 표시
            Log.d(TAG, "오버레이 표시 (플래그=1)")
            showOverlay()
        } else if (!shouldShow && isOverlayVisible) {
            // 숨겨야 하는데 보이면 숨김
            Log.d(TAG, "오버레이 숨김 (플래그=0)")
            hideOverlay()
        } else {
            Log.d(TAG, "상태 변화 없음")
        }
        
        // 최종 상태 확인
        Log.d(TAG, "최종 상태 - 오버레이보이는지: $isOverlayVisible, 실제: ${getActualViewVisibility()}")
    }
    
    // 오버레이 설정 및 생성
    private fun setupOverlay() {
        // 오버레이 화면을 띄우기 위해 시스템 서비스에서 windowManager 가져옴
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // OverlayManager 인스턴스를 생성 (오버레이 뷰의 생성과 제거를 책임하는 클래스)
        overlayManager = OverlayManager(this, windowManager)
        
        // DragHandler 인스턴스를 생성 (드래그 기능을 책임지는 클래스)
        dragHandler = DragHandler(windowManager, resources, this)
        
        // 오버레이 생성 (처음에는 숨김 상태)
        createOverlay()
    }
    
    // 오버레이 재생성
    private fun recreateOverlay() {
        Log.d(TAG, "오버레이 재생성 시도")
        
        // 기존 오버레이 제거
        cleanupOverlay()
        
        // 새로운 오버레이 생성
        setupOverlay()
        
        // 현재 플래그에 맞춰 오버레이 보일지 말지 설정
        updateOverlayVisibility()
        
        Log.d(TAG, "오버레이 재생성 완료")
    }
    
    // 오버레이 생성
    private fun createOverlay() {
        // DragHandler에서 사용할 터치 리스너 생성
        val touchListener = createTouchListener()
        // 오버레이를 생성하고 뷰와 LayoutParams를 받아옴
        val result = overlayManager?.createOverlay(touchListener)

        // 에러 처리 - result가 null일 경우 (생성 실패)
        if (result == null) {
            Log.e(TAG, "오버레이 생성 실패")
            // 오버레이 생성 실패 시 서비스 강제 종료
            stopSelf()
        } else {
            // 정상적으로 생성되었을 경우 뷰와 레이아웃 파라미터를 구조를 분해하여 할당
            val (view, params) = result
            Log.d(TAG, "오버레이 생성 성공")
            Log.d(TAG, "생성 직후 뷰 보이는지 여부: ${getActualViewVisibility()}")

            // 서비스 시작 시 오버레이는 기본적으로 숨겨진 상태여야 하므로 강제 숨김 처리
            forceHideOverlay()
        }
    }
    
    // 터치 리스너 생성 - DragHandler와 연결
    @SuppressLint("ClickableViewAccessibility")
    private fun createTouchListener(): View.OnTouchListener {
        return View.OnTouchListener { _, event ->
            val overlayView = overlayManager?.getOverlayView() // 현재 오버레이 뷰 가져오기
            val layoutParams = overlayManager?.getLayoutParams() // 해당 뷰의 layout params 가져오기

            // 두 값이 null이 아니면 DragHandler로 터치 이벤트 넘김
            if (overlayView != null && layoutParams != null) {
                dragHandler?.handleTouchEvent(event, overlayView, layoutParams) ?: false
            } else {
                false
            }
        }
    }
    
    // 오버레이 표시
    private fun showOverlay() {
        val view = overlayManager?.getOverlayView() // 오버레이 뷰 가져오기
        if (view != null && !isOverlayVisible) {
            view.visibility = View.VISIBLE // 뷰가 안 보이면 화면에 표시
            isOverlayVisible = true // 플래그도 변경
            Log.d(TAG, "오버레이 표시됨 (isOverlayVisible = true)")
            Log.d(TAG, "표시 후 실제 visibility: ${getActualViewVisibility()}")
        }
    }
    
    // 오버레이 숨기기
    private fun hideOverlay() {
        val view = overlayManager?.getOverlayView()
        if (view != null && isOverlayVisible) {
            view.visibility = View.GONE // 뷰가 보이면 화면에서 제거
            isOverlayVisible = false // 플래그도 변경
            Log.d(TAG, "오버레이 숨겨짐 (isOverlayVisible = false)")
            Log.d(TAG, "숨김 후 실제 visibility: ${getActualViewVisibility()}")
        }
    }
    
    // 강제로 오버레이 숨기기 (초기화용)
    private fun forceHideOverlay() {
        val view = overlayManager?.getOverlayView()
        if (view != null) {
            view.visibility = View.GONE // 무조건 화면에서 제거
            isOverlayVisible = false // 플래그도 변경
            Log.d(TAG, "강제 숨김 완료 (isOverlayVisible = false)")
            Log.d(TAG, "강제 숨김 후 실제 visibility: ${getActualViewVisibility()}")
        }
    }
    
    // 오버레이 제거
    private fun cleanupOverlay() {
        overlayManager?.removeOverlay() // windowManager에서 오버레이 제거
        // 각 객체 제거
        overlayManager = null
        dragHandler = null
        isOverlayVisible = false
        Log.d(TAG, "오버레이 정리 완료")
    }

    // 현재 미사용 - 오버레이가 생성되었고 플래그가 true 상태인지 확인
    fun isOverlayActive(): Boolean {
        return overlayManager?.isOverlayCreated() == true && getOverlayFlag() == 1
    // 뷰가 생성되었고 SharedPreferences 플래그가 1이면 true
    }

    // 현재 미사용 - 현재 오버레이가 화면에 표시 중인지 확인하는 함수
    fun isOverlayVisible(): Boolean = isOverlayVisible && getOverlayFlag() == 1
    // 함수 내 플래그와 SharedPreferences 플래그가 모두 true면 true
} 