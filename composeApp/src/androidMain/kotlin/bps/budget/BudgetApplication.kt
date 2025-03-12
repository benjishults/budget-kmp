package bps.budget

import android.app.Application

class BudgetApplication : Application() {

    override fun onCreate() {
        super.onCreate()
//        initKoin {
//            androidContext(this@BudgetApplication)
//        }
    }
}
