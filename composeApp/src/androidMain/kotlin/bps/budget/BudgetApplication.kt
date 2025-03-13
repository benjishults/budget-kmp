package bps.budget

import android.app.Application
import bps.budget.di.initKoin
import org.koin.android.ext.koin.androidContext

class BudgetApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@BudgetApplication)
        }
    }
}
