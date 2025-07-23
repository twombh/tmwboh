package com.example.dynamic.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.example.dynamic.R

class OverlayManager(
    private val context: Context,
    private val windowManager: WindowManager
) {

    private var overlayView: View? = null // 현재 표시 중인 오버레이 뷰
    private var layoutParams: WindowManager.LayoutParams? = null // 오버레이의 레이아웃 속성
    

    companion object {
        private const val TAG = "OverlayManager"

    }

    // 오버레이 생성 및 화면에 추가
    fun createOverlay(onTouchListener: View.OnTouchListener): Pair<View, WindowManager.LayoutParams>? {
        try {
            // XML 레이아웃을 객체화하여 오버레이 뷰 생성
            val inflater = LayoutInflater.from(context)
            Log.d(TAG, "XML 레이아웃 inflate 시작")
            val view = inflater.inflate(R.layout.overlay_layout, null) // xml을 실제 뷰로 변환
            Log.d(TAG, "XML 레이아웃 inflate 완료: ${view::class.simpleName}")

            // WindowManager 레이아웃 파라미터(오버레이 속성 관련) 생성
            val params = createLayoutParams()

            // 터치 리스너 연결
            view.setOnTouchListener(onTouchListener)

            // 윈도우 매니저에 뷰 추가 - 화면에 표시
            windowManager.addView(view, params)

            // 내부 상태 저장
            overlayView = view
            layoutParams = params

            // 실제 픽셀 크기 확인을 위한 post 로그
            view.post {
                val actualWidth = view.width
                val actualHeight = view.height
                Log.d(TAG, "화면에 표시된 오버레이 실제 크기: ${actualWidth}px x ${actualHeight}px")
            }
            return Pair(view, params)

        } catch (e: Exception) {
            Log.e(TAG, "오버레이 생성 실패: ${e.message}", e)
            return null
        }
    }


    // 오버레이 제거
    fun removeOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "오버레이 제거 완료")
            } catch (e: Exception) {
                Log.e(TAG, "오버레이 제거 실패: ${e.message}", e)
            }
        }
        
        overlayView = null
        layoutParams = null
    }
    
    // WindowManager 레이아웃 파라미터 생성
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            // 오버레이의 가로, 세로 크기를 뷰 크기로 설정
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE // 안드로이드 8.0 (API 26) 미만 - 8.0부턴 TYPE_APPLICATION_OVERLAY만 허용
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or // 포커스 X - 클릭해도 키보드 X
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or // 외부 터치 이벤트 O - 다른 앱 위에 그리기 위해
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, // 외부 터치 감지 - 다른 앱 위에 그리기 위해
            PixelFormat.TRANSLUCENT // 배경 반투명 처리
        )
        
        // 위치 설정 - 초기 100, 100
        params.gravity = Gravity.TOP or Gravity.START // 오버레이를 화면 좌측 상단 기준으로 위치
        val (savedX, savedY) = getSavedPosition()
        params.x = savedX  // 저장된 X 위치 적용
        params.y = savedY  // 저장된 Y 위치 적용
        
        return params
    }

    // 저장된 위치 불러오기 - 다시 끄고 켰을 때를 대비
    private fun getSavedPosition(): Pair<Int, Int> {
        val prefs = context.getSharedPreferences("overlay_prefs", Context.MODE_PRIVATE)
        val x = prefs.getInt("last_x", 100) // 초기값
        val y = prefs.getInt("last_y", 100)
        return Pair(x, y)
    }

    // Getter 함수들
    // 현재 오버레이 뷰 반환
    fun getOverlayView(): View? = overlayView
    
    // 현재 레이아웃 파라미터 반환
    fun getLayoutParams(): WindowManager.LayoutParams? = layoutParams
    
    // 오버레이가 생성되어 있는지 확인
    fun isOverlayCreated(): Boolean = overlayView != null
} 