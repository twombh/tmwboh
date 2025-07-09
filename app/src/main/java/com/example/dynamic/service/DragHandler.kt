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
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val dragThreshold = 10f // 드래그로 인식할 최소 이동 거리
    
    // 배경 상태
    private var originalBackground: GradientDrawable? = null
    
    /**
     * 터치 이벤트 처리 (드래그 기능)
     */
    fun handleTouchEvent(
        event: MotionEvent, 
        overlayView: View, 
        params: WindowManager.LayoutParams
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return handleTouchDown(event, overlayView, params)
            }
            
            MotionEvent.ACTION_MOVE -> {
                return handleTouchMove(event, overlayView, params)
            }
            
            MotionEvent.ACTION_UP -> {
                return handleTouchUp(overlayView)
            }
            
            MotionEvent.ACTION_CANCEL -> {
                return handleTouchCancel(overlayView)
            }
        }
        return false
    }
    
    /**
     * 터치 시작 처리
     */
    private fun handleTouchDown(
        event: MotionEvent, 
        overlayView: View, 
        params: WindowManager.LayoutParams
    ): Boolean {
        isDragging = false
        initialX = params.x
        initialY = params.y
        initialTouchX = event.rawX
        initialTouchY = event.rawY
        
        // 원본 배경 저장 및 터치 피드백
        saveOriginalBackground(overlayView)
        applyTouchFeedback(overlayView)
        return true
    }
    
    /**
     * 터치 이동 처리 (드래그)
     */
    private fun handleTouchMove(
        event: MotionEvent, 
        overlayView: View, 
        params: WindowManager.LayoutParams
    ): Boolean {
        val deltaX = event.rawX - initialTouchX
        val deltaY = event.rawY - initialTouchY
        
        // 드래그 임계값 확인
        if (!isDragging && (abs(deltaX) > dragThreshold || abs(deltaY) > dragThreshold)) {
            isDragging = true
        }
        
        if (isDragging) {
            // 새로운 위치 계산
            val newX = initialX + deltaX.toInt()
            val newY = initialY + deltaY.toInt()
            
            // 화면 경계 확인 및 제한
            val screenBounds = getScreenBounds()
            val clampedX = newX.coerceIn(0, screenBounds.first - params.width)
            val clampedY = newY.coerceIn(0, screenBounds.second - params.height)
            
            // 위치 업데이트
            updateOverlayPosition(overlayView, params, clampedX, clampedY)
        }
        return true
    }
    
    /**
     * 터치 종료 처리
     */
    private fun handleTouchUp(overlayView: View): Boolean {
        restoreOriginalBackground(overlayView)
        
        if (!isDragging) {
            // 단순 클릭으로 처리 (드래그가 아니었다면)
            performClickAnimation(overlayView)
            // 클릭 시 앱으로 돌아가기
            navigateToMainApp()
        }
        
        isDragging = false
        return true
    }
    
    /**
     * 터치 취소 처리
     */
    private fun handleTouchCancel(overlayView: View): Boolean {
        restoreOriginalBackground(overlayView)
        isDragging = false
        return true
    }
    
    /**
     * 오버레이 위치 업데이트
     */
    private fun updateOverlayPosition(
        overlayView: View, 
        params: WindowManager.LayoutParams, 
        x: Int, 
        y: Int
    ) {
        params.x = x
        params.y = y
        windowManager.updateViewLayout(overlayView, params)
    }
    
    /**
     * 화면 크기 가져오기
     */
    private fun getScreenBounds(): Pair<Int, Int> {
        val displayMetrics = resources.displayMetrics
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }
    
    /**
     * 원본 배경 저장
     */
    private fun saveOriginalBackground(overlayView: View) {
        originalBackground = (overlayView.background as? GradientDrawable)?.constantState?.newDrawable() as? GradientDrawable
    }
    
    /**
     * 터치 피드백 적용 (약간 어두운 배경)
     */
    private fun applyTouchFeedback(overlayView: View) {
        val touchBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#F0F0F0")) // 약간 회색빛 흰색
            cornerRadius = 30f * resources.displayMetrics.density // 30dp를 px로 변환
        }
        overlayView.background = touchBackground
    }
    
    /**
     * 원본 배경 복원
     */
    private fun restoreOriginalBackground(overlayView: View) {
        originalBackground?.let { background ->
            overlayView.background = background
        } ?: run {
            // 원본 배경이 없으면 기본 흰색 배경으로 설정
            val defaultBackground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = 30f * resources.displayMetrics.density
            }
            overlayView.background = defaultBackground
        }
    }
    
    /**
     * 클릭 애니메이션 효과
     */
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
    
    /**
     * 메인 앱으로 돌아가기
     */
    private fun navigateToMainApp() {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 현재 드래그 상태 반환
     */
    fun isDragging(): Boolean = isDragging
} 