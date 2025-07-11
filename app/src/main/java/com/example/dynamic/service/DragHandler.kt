package com.example.dynamic.service

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.example.dynamic.MainActivity
import kotlin.math.abs

class DragHandler(
    private val windowManager: WindowManager,
    private val resources: Resources,
    private val context: Context
) {
    
    // 드래그 관련 변수들
    private var initialX = 0 // 후에 x, y 사용
    private var initialY = 0
    private var initialTouchX = 0f // 후에 rawX, rawY 사용
    private var initialTouchY = 0f
    private var isDragging = false // 드래그 중인지 판단
    private val dragThreshold = 10f // 드래그로 인식할 최소 이동 거리 - 10px
    
    // 터치 전 배경 상태 저장용
    private var originalBackground: GradientDrawable? = null // 원래 배경 저장
    
    // 터치 이벤트 처리 - 드래그 기능
    fun handleTouchEvent(
        event: MotionEvent, // 어떤 이벤트인지
        overlayView: View, // 드래그 대상
        params: WindowManager.LayoutParams
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return handleTouchDown(event, overlayView, params) // 터치 시작
            }
            
            MotionEvent.ACTION_MOVE -> {
                return handleTouchMove(event, overlayView, params) // 드래그 이동
            }
            
            MotionEvent.ACTION_UP -> {
                return handleTouchUp(overlayView) // 터치 끝
            }
            
            MotionEvent.ACTION_CANCEL -> {
                return handleTouchCancel(overlayView) // 이벤트 취소
            }
        }
        return false
    }
    
    // 터치 시작 동작
    private fun handleTouchDown(
        event: MotionEvent,
        overlayView: View,
        params: WindowManager.LayoutParams
    ): Boolean {
        isDragging = false
        initialX = params.x // 뷰 기준 위치 저장
        initialY = params.y
        initialTouchX = event.rawX // 터치 시작 좌표 저장
        initialTouchY = event.rawY

        saveOriginalBackground(overlayView) // 원래 배경 저장
        applyTouchFeedback(overlayView) // 터치 피드백 배경
        return true
    }
    
    // 터치 이동 처리 - 드래그
    private fun handleTouchMove(
        event: MotionEvent, 
        overlayView: View, 
        params: WindowManager.LayoutParams
    ): Boolean {
        val deltaX = event.rawX - initialTouchX // 움직인 거리 계산
        val deltaY = event.rawY - initialTouchY
        
        // 드래그 임계값 확인 - 드래그인지 판단
        if (!isDragging && (abs(deltaX) > dragThreshold || abs(deltaY) > dragThreshold)) {
            isDragging = true
        }
        
        if (isDragging) {
            val newX = initialX + deltaX.toInt() // 새로운 좌표 계속 계산
            val newY = initialY + deltaY.toInt()

            val screenBounds = getScreenBounds() // 해상도 불러오기
            val clampedX = newX.coerceIn(0, screenBounds.first - params.width) // 화면 경계 제한 - 왼쪽 위가 기준이기에
            val clampedY = newY.coerceIn(0, screenBounds.second - params.height)
            
            // 위치 업데이트
            updateOverlayPosition(overlayView, params, clampedX, clampedY)
        }
        return true
    }
    
   // 터치 종료 동작
    private fun handleTouchUp(overlayView: View): Boolean {
        restoreOriginalBackground(overlayView) // 원래 배경으로 돌리기
        
        if (!isDragging) {
            // 단순 클릭으로 처리 - 클릭 애니메이션만 실행
            performClickAnimation(overlayView)
            // 클릭 시 앱으로 돌아가기 - 현재 MainActivity
            navigateToMainApp()
        }
        
        isDragging = false
        return true
    }
    
    // 터치 취소 동작
    private fun handleTouchCancel(overlayView: View): Boolean {
        restoreOriginalBackground(overlayView) // 원래 배경으로 돌리기
        isDragging = false // 초기화
        return true
    }
    
    // 오버레이 위치 업데이트
    private fun updateOverlayPosition(
        overlayView: View, 
        params: WindowManager.LayoutParams, 
        x: Int, 
        y: Int
    ) {
        params.x = x // 새로운 좌표로 변경
        params.y = y
        windowManager.updateViewLayout(overlayView, params) // 실제 위치 변경
    }
    
    // 화면 해상도 가져오기
    private fun getScreenBounds(): Pair<Int, Int> {
        val displayMetrics = resources.displayMetrics // 가로, 세로 화소 수 호출
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }
    
    // 원본 배경 복사하여 저장
    private fun saveOriginalBackground(overlayView: View) {
        originalBackground = (overlayView.background as? GradientDrawable)?.constantState?.newDrawable() as? GradientDrawable
    }
    

    // 터치 피드백 적용 - 회색 배경
    private fun applyTouchFeedback(overlayView: View) {
        val touchBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE // 사격형 배경
            setColor(Color.parseColor("#F0F0F0")) // 약간 회색
            cornerRadius = 30f * resources.displayMetrics.density // 30dp를 px로 변환 - cornerRadius는 px로만 받음
        }
        overlayView.background = touchBackground // 배경 적용
    }
    
    // 원본 배경으로 돌아가기
    private fun restoreOriginalBackground(overlayView: View) {
        originalBackground?.let { background ->
            overlayView.background = background // 원본 배경이 있는 경우
        } ?: run {
            // 원본 배경이 없으면 기본 흰색 배경으로 설정
            val defaultBackground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE) // 흰 배경
                cornerRadius = 30f * resources.displayMetrics.density
            }
            overlayView.background = defaultBackground // 없으면 기본 배경
        }
    }
    
    // 클릭 애니메이션 효과 - 크기 아주 살짝 키움
    private fun performClickAnimation(overlayView: View) {
        overlayView.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(100)
            .withEndAction {
                overlayView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
    
    // MainActivity로 돌아가기
    private fun navigateToMainApp() {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                // 안정적으로 처리
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or // 새로 시작
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or // 기존 액티비티 제거
                        Intent.FLAG_ACTIVITY_SINGLE_TOP // 이미 실행 중이면 사용
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    //현재 드래그 상태 반환 - 혹시 몰라 제작, 실 사용 x
    fun isDragging(): Boolean = isDragging
} 