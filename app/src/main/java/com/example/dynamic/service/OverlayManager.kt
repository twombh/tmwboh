package com.example.dynamic.service

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.dynamic.R

class OverlayManager(
    private val context: Context,
    private val windowManager: WindowManager
) {
    
    private var overlayView: View? = null // 현재 표시 중인 오버레이 뷰
    private var layoutParams: WindowManager.LayoutParams? = null // 오버레이의 레이아웃 속성
    
    // 반응형 UI 크기 계산을 위한 화면 정보와 계산된 크기
    private val screenMetrics = getScreenMetrics() // 현재 디바이스 해상도 정보
    private val responsiveSizes = calculateResponsiveSizes() // 해상도 기반 동적 크기 설정
    
    companion object {
        private const val TAG = "OverlayManager"
        
        // 기준 화면 크기 - dp 단위
        private const val REFERENCE_SCREEN_WIDTH = 412f
        private const val REFERENCE_SCREEN_HEIGHT = 892f
        
        // 기준 오버레이 크기 UI 요소 크기
        private const val REFERENCE_OVERLAY_WIDTH = 276f
        private const val REFERENCE_OVERLAY_HEIGHT = 100f
        private const val REFERENCE_BORDER_RADIUS = 30f
        private const val REFERENCE_FONT_SIZE_LARGE = 22f
        private const val REFERENCE_FONT_SIZE_SMALL = 16f
    }
    
    // 화면 크기 정보 가져오기
    private fun getScreenMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        // Service Context에서는 WindowManager를 통해서만 Display 접근 가능
        @Suppress("DEPRECATION") // 컴파일 시 오류 무시 (오래된 API 허용)
        windowManager.defaultDisplay.getRealMetrics(metrics) // 실제 화면 전체 픽셀 정보 가져오기
        return metrics
    }
    
    // 해상도 기반 반응형 크기 계산
    private fun calculateResponsiveSizes(): ResponsiveSizes {
        val screenWidthPx = screenMetrics.widthPixels.toFloat() // 가로 넓이 저장
        val screenHeightPx = screenMetrics.heightPixels.toFloat() // 세로 넓이 저장
        val density = screenMetrics.density
        
        // 화면 크기를 dp로 변환 (dp = px / density(dpi/160))
        val screenWidthDp = screenWidthPx / density
        val screenHeightDp = screenHeightPx / density
        
        Log.d(TAG, "화면 크기: ${screenWidthDp}dp x ${screenHeightDp}dp (밀도: $density)")
        
        // 비율 계산 - 기준 화면 대비
        val widthRatio = screenWidthDp / REFERENCE_SCREEN_WIDTH
        val heightRatio = screenHeightDp / REFERENCE_SCREEN_HEIGHT
        
        // 가로와 세로 중 더 작인 비율을 선택하여 화면 밖으로 나가는 것 방지
        val scaleFactor = minOf(widthRatio, heightRatio)
        
        Log.d(TAG, "더 작은 비율 크기: $scaleFactor (가로비: $widthRatio, 세로비: $heightRatio)")

        // 반응형 크기 값을 계산해서 데이터 클래스에 저장
        return ResponsiveSizes(
            overlayWidth = (REFERENCE_OVERLAY_WIDTH * scaleFactor).toInt(),
            overlayHeight = (REFERENCE_OVERLAY_HEIGHT * scaleFactor).toInt(),
            borderRadius = REFERENCE_BORDER_RADIUS * scaleFactor,
            fontSizeLarge = REFERENCE_FONT_SIZE_LARGE * scaleFactor,
            fontSizeSmall = REFERENCE_FONT_SIZE_SMALL * scaleFactor,
            logoSize = (50f * scaleFactor).toInt(),
            // 사용할 gap들 비율 기반 변환
            gap16dp = (16f * scaleFactor).toInt(),
            gap12dp = (12f * scaleFactor).toInt(),
            gap4dp = (4f * scaleFactor).toInt(),
            gap2dp = (2f * scaleFactor).toInt(),
            padding24dp = (24f * scaleFactor).toInt()
        )
    }
    
    // 반응형 크기 담는 데이터 클래스
    data class ResponsiveSizes(
        val overlayWidth: Int,
        val overlayHeight: Int,
        val borderRadius: Float,
        val fontSizeLarge: Float,
        val fontSizeSmall: Float,
        val logoSize: Int,
        val gap16dp: Int,
        val gap12dp: Int,
        val gap4dp: Int,
        val gap2dp: Int,
        val padding24dp: Int
    )
    
    // 오버레이 생성 및 화면에 추가
    fun createOverlay(onTouchListener: View.OnTouchListener): Pair<View, WindowManager.LayoutParams>? {
        try {
            // XML 레이아웃을 객체화하여 오버레이 뷰 생성
            val inflater = LayoutInflater.from(context)
            Log.d(TAG, "XML 레이아웃 inflate 시작")
            val view = inflater.inflate(R.layout.overlay_layout, null) // xml을 실제 뷰로 변환
            Log.d(TAG, "XML 레이아웃 inflate 완료: ${view::class.simpleName}")
            
            // 크기와 텍스트 사이즈 적용
            applyResponsiveSizes(view)
            
            // 코드로 둥근 모서리 배경 설정
            setupBackgrounds(view)
            
            // WindowManager 레이아웃 파라미터(오버레이 속성) 생성
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
            
            Log.d(TAG, "오버레이 생성 완료 (크기: ${responsiveSizes.overlayWidth}x${responsiveSizes.overlayHeight}dp)")
            return Pair(view, params)
            
        } catch (e: Exception) {
            Log.e(TAG, "오버레이 생성 실패: ${e.message}", e)
            return null
        }
    }



    // 크기와 텍스트 사이즈 적용
    private fun applyResponsiveSizes(view: View) {
        // 컨테이너 최소 크기 및 패딩 설정
        val container = view.findViewById<LinearLayout>(R.id.overlayContainer)
        container?.let { cont ->
            // 좌우 패딩 적용 (좌우 24dp → 반응형)
            val paddingPx = dpToPx(responsiveSizes.padding24dp.toFloat()).toInt()
            // setPadding은 int 밖에 안됨
            cont.setPadding(paddingPx, 0, paddingPx, 0)
            
            // 최소 크기 설정
            cont.minimumWidth = dpToPx(responsiveSizes.overlayWidth.toFloat()).toInt()
            cont.minimumHeight = dpToPx(responsiveSizes.overlayHeight.toFloat()).toInt()
            
            Log.d(TAG, "컨테이너 최소크기: ${responsiveSizes.overlayWidth}x${responsiveSizes.overlayHeight}dp, 컨테이너 패딩: 패딩 ${responsiveSizes.padding24dp}dp")
        }
        
        // 로고 크기 조정
        val logoView = view.findViewById<View>(R.id.logoView)
        logoView?.let { logo ->
            // layout params는 px로 밖에 사용 안되어 px로 변환 - layout params로 동적 UI 수정
            val logoParams = logo.layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(
                dpToPx(responsiveSizes.logoSize.toFloat()).toInt(),
                dpToPx(responsiveSizes.logoSize.toFloat()).toInt()
            )
            logoParams.width = dpToPx(responsiveSizes.logoSize.toFloat()).toInt()
            logoParams.height = dpToPx(responsiveSizes.logoSize.toFloat()).toInt()
            logoParams.setMargins(0, 0, dpToPx(responsiveSizes.gap16dp.toFloat()).toInt(), 0)
            logo.layoutParams = logoParams
            
            Log.d(TAG, "로고 설정: ${responsiveSizes.logoSize}x${responsiveSizes.logoSize}dp")
        }
        
        // 간격 크기 조정
        adjustSpaces(view)
        
        // 텍스트 크기 조정
        adjustTextSizes(view)
        
        Log.d(TAG, "반응형 UI 적용 완료: ${responsiveSizes}")
    }
    
    // 간격 크기 조정
    private fun adjustSpaces(view: View) {
        // 상단 여백
        view.findViewById<View>(R.id.topSpace)?.let { space ->
            val params = space.layoutParams
            params.height = dpToPx(responsiveSizes.gap12dp.toFloat() / 2).toInt() // 6dp → 반응형 (gap6dp는 없기에)
            space.layoutParams = params
        }
        
        // 중간 여백 (따릉이 (이 곳) 대여중)
        view.findViewById<View>(R.id.middleSpace)?.let { space ->
            val params = space.layoutParams
            params.width = dpToPx(responsiveSizes.gap4dp.toFloat()).toInt()
            space.layoutParams = params
        }
        
        // 텍스트 간 여백
        view.findViewById<View>(R.id.bottomSpace)?.let { space ->
            val params = space.layoutParams
            params.height = dpToPx(responsiveSizes.gap2dp.toFloat()).toInt()
            space.layoutParams = params
        }

        // 하단 여백
        view.findViewById<View>(R.id.bottomSpaceMain)?.let { space ->
            val params = space.layoutParams
            params.height = dpToPx(responsiveSizes.gap12dp.toFloat() / 2).toInt() // 6dp → 반응형
            space.layoutParams = params
        }

        Log.d(TAG, "간격 조정 완료: gap12=${responsiveSizes.gap12dp}dp, gap4=${responsiveSizes.gap4dp}dp, gap2=${responsiveSizes.gap2dp}dp")
    }
    
    // 텍스트 크기 조정
    private fun adjustTextSizes(view: View) {
        // 따릉이 텍스트
        view.findViewById<TextView>(R.id.ddareungiText)?.let { textView ->
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, responsiveSizes.fontSizeLarge)
            Log.d(TAG, "따릉이 텍스트 크기: ${responsiveSizes.fontSizeLarge}sp")
        }
        
        // 대여중 텍스트  
        view.findViewById<TextView>(R.id.daeyeojungText)?.let { textView ->
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, responsiveSizes.fontSizeLarge)
            Log.d(TAG, "대여중 텍스트 크기: ${responsiveSizes.fontSizeLarge}sp")
        }
        
        // 부제목 텍스트
        view.findViewById<TextView>(R.id.subtitleText)?.let { textView ->
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, responsiveSizes.fontSizeSmall)
            Log.d(TAG, "부제목 텍스트 크기: ${responsiveSizes.fontSizeSmall}sp")
        }
    }
    


    // dp를 px로 변환
    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }
    
    // 둥근 모서리 배경 설정
    private fun setupBackgrounds(view: View) {
        // 컨테이너 배경 - 둥근 모서리 흰색
        val container = view.findViewById<View>(R.id.overlayContainer)
        if (container != null) {
            container.background = createRoundedBackground()
            Log.d(TAG, "컨테이너 배경 설정 완료")
        } else {
            Log.e(TAG, "overlayContainer 찾을 수 없음")
        }
        
        // 로고 배경 - 원형
        val logoView = view.findViewById<View>(R.id.logoView)
        if (logoView != null) {
            logoView.background = createLogoBackground()
            Log.d(TAG, "로고 배경 설정 완료")
        } else {
            Log.e(TAG, "logoView ID 찾을 수 없음")
        }
    }

    // 둥근 모서리 흰색 배경 생성 - 반응형
    private fun createRoundedBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(android.graphics.Color.WHITE)
            cornerRadius = dpToPx(responsiveSizes.borderRadius)
        }
    }
    
    // 원형 배경 생성 - 원형을 둘러줌
    private fun createLogoBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(android.graphics.Color.WHITE)
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
        
        // 초기 위치 설정
        params.gravity = Gravity.TOP or Gravity.START // 오버레이를 화면 좌측 상단 기준으로 위치
        params.x = 100
        params.y = 100
        
        return params
    }

    // Getter 함수들
    // 현재 오버레이 뷰 반환
    fun getOverlayView(): View? = overlayView
    
    // 현재 레이아웃 파라미터 반환
    fun getLayoutParams(): WindowManager.LayoutParams? = layoutParams
    
    // 오버레이가 생성되어 있는지 확인
    fun isOverlayCreated(): Boolean = overlayView != null
} 