package com.example.dynamic

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.dynamic.viewmodel.OverlayViewModel

// 메인 화면
class MainActivity : AppCompatActivity() {
    
    private val viewModel: OverlayViewModel by viewModels() // viewmodel을 activity에 바인딩 - ViewModel을 자동으로 생성하고 이후에는 기존 인스턴스를 재사용합
    
    // View 참조 변수 선언 (나중에 초기화)
    private lateinit var statusText: TextView
    private lateinit var permissionStatusText: TextView
    private lateinit var toggleButton: Button
    
    // 토스트 중복 방지
    private var hasShownPermissionToast = false

    // 오버레이 권한 설정 후 결과를 처리하는 런처 등록
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 권한 설정 후 UI 상태 재확인
        updateUIBasedOnPermission()
    }

    // 액티비티 생성 시 호출되는 함수
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ActionBar 숨기기 (중복 제목 방지)
        supportActionBar?.hide()
        
        setContentView(R.layout.activity_main)
        
        initViews() // 뷰 참조 초기화
        setupViews() // 이벤트 리스너 설정
        observeViewModel() // ViewModel LiveData 관찰 시작
        
        // 초기 상태 설정 - 권한 및 플래그 상태 확인
        updateUIBasedOnPermission()
    }
    
    // 뷰를 참조해 변수에 저장
    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        permissionStatusText = findViewById(R.id.permissionStatusText)
        toggleButton = findViewById(R.id.toggleButton)
    }

    // 버튼에 클릭 리스너 부착
    private fun setupViews() {
        // 오버레이 토글 버튼 클릭 리스너
        toggleButton.setOnClickListener {
            handleToggleButtonClick() // 버튼 클릭 시 실행할 동작 정의
        }
    }

    // 토글 버튼 클릭 시 처리 로직
    private fun handleToggleButtonClick() {
        if (hasOverlayPermission()) {
            // 권한이 있으면 플래그 토글
            viewModel.toggleOverlayFlag()
        } else {
            // 권한이 없으면 권한 요청
            requestOverlayPermission()
        }
    }
    
    /**
     * ViewModel 관찰
     */
    private fun observeViewModel() {
        // 플래그 상태 관찰
        viewModel.overlayFlag.observe(this, Observer { flag ->
            // 권한이 있을 때만 플래그 상태로 UI 업데이트
            if (hasOverlayPermission()) {
                updateUIWithFlag(flag)
            }
        })
    }
    
    /**
     * 권한 상태에 따른 UI 업데이트
     */
    private fun updateUIBasedOnPermission() {
        if (hasOverlayPermission()) {
            // 권한이 있으면 현재 플래그 상태로 UI 업데이트
            updatePermissionStatusUI(true)
            val currentFlag = viewModel.overlayFlag.value ?: 0
            updateUIWithFlag(currentFlag)
        } else {
            // 권한이 없으면 권한 요청 UI 표시
            updatePermissionStatusUI(false)
            updateUIForPermissionRequired()
        }
    }
    
    /**
     * 권한 상태 UI 업데이트
     */
    private fun updatePermissionStatusUI(hasPermission: Boolean) {
        if (hasPermission) {
            permissionStatusText.text = "권한 허용됨"
            permissionStatusText.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            permissionStatusText.text = "권한 필요"
            permissionStatusText.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }
    
    /**
     * 플래그 상태에 따른 UI 업데이트 (권한이 있을 때)
     */
    private fun updateUIWithFlag(flag: Int) {
        val statusMessage = viewModel.getOverlayStatusText()
        val isActive = flag == 1
        
        // 상태 텍스트 업데이트
        statusText.text = statusMessage
        
        // 버튼 텍스트 및 색상 업데이트
        if (isActive) {
            toggleButton.text = "오버레이 끄기 (현재: $flag)"
            toggleButton.setBackgroundColor(getColor(android.R.color.holo_red_light))
        } else {
            toggleButton.text = "오버레이 켜기 (현재: $flag)"
            toggleButton.setBackgroundColor(getColor(android.R.color.holo_green_light))
        }
        
        // 버튼 활성화
        toggleButton.isEnabled = true
    }
    
    /**
     * 권한 필요 상태 UI 업데이트
     */
    private fun updateUIForPermissionRequired() {
        statusText.text = "오버레이 권한이 필요합니다"
        toggleButton.text = "권한 요청"
        toggleButton.setBackgroundColor(getColor(android.R.color.holo_orange_light))
        toggleButton.isEnabled = true
        
        // 권한 요청 안내 토스트 (처음 한 번만 표시)
        if (!hasShownPermissionToast) {
            Toast.makeText(this, "오버레이 권한을 허용해주세요", Toast.LENGTH_LONG).show()
            hasShownPermissionToast = true
        }
    }
    
    /**
     * 오버레이 권한 확인
     */
    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    /**
     * 오버레이 권한 요청
     */
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            permissionLauncher.launch(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 액티비티가 다시 포그라운드로 올라올 때 권한 상태 재확인
        updateUIBasedOnPermission()
    }
}