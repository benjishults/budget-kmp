package bps.budget.di

import bps.budget.account.data.network.KtorRemoteAccountDataSource
import bps.budget.account.data.network.RemoteAccountDataSource
import bps.budget.account.data.repository.DefaultAccountRepository
import bps.budget.account.domain.AccountRepository
import bps.budget.account.presentation.balances.AccountBalancesViewModel
import bps.budget.core.data.HttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine

// TODO see https://youtu.be/WT9-4DXUqsM?t=10136
//      if you want to use koin
//val sharedModules = module {
//    single {
//        Ktor
//    }
//}

expect val productionEngine: HttpClientEngine

val productionHttpClient: HttpClient =
    HttpClientFactory.create(productionEngine)

val productionAccountBalancesDataSource: RemoteAccountDataSource =
    KtorRemoteAccountDataSource(productionHttpClient)

val productionAccountRepository: AccountRepository =
    DefaultAccountRepository(
        dataSource = productionAccountBalancesDataSource,
    )

// TODO maybe have a preview version as well?
val productionAccountBalancesViewModel: AccountBalancesViewModel =
    AccountBalancesViewModel(accountRepository = productionAccountRepository)

