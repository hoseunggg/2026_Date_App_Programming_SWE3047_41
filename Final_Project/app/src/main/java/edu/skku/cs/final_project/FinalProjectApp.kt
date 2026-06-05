package edu.skku.cs.final_project

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

class FinalProjectApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
    }
}
