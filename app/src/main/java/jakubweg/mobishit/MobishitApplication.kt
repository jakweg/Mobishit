package jakubweg.mobishit

import android.app.Application
import com.squareup.leakcanary.LeakCanary

class MobishitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        LeakCanary.install(this)
    }
}
