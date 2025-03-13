package bps.budget.di

import bps.budget.account.data.network.KtorRemoteAccountDataSource
import bps.budget.account.data.network.RemoteAccountDataSource
import bps.budget.account.data.repository.DefaultAccountRepository
import bps.budget.account.domain.AccountRepository
import bps.budget.account.presentation.SelectedAccountViewModel
import bps.budget.account.presentation.account_detail.AccountDetailViewModel
import bps.budget.account.presentation.balances.AccountBalancesViewModel
import bps.budget.core.data.HttpClientFactory
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformModule: Module

val sharedModules: Module = module {
    single { HttpClientFactory.create(get()) }
    single {
        KtorRemoteAccountDataSource(get())
    }
        .bind(RemoteAccountDataSource::class)
    singleOf(::DefaultAccountRepository)
        .bind(AccountRepository::class)
    viewModelOf(::AccountBalancesViewModel)
    viewModelOf(::SelectedAccountViewModel)
    viewModelOf(::AccountDetailViewModel)
}
