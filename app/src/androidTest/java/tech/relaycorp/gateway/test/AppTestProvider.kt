package tech.relaycorp.gateway.test

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.TestCoroutineDispatcher
import tech.relaycorp.gateway.data.database.AppDatabase

object AppTestProvider {
    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    val app
        get() = context.applicationContext as TestApp

    val component
        get() = app.component

    val database
        get() =
            Room
                .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .setTransactionExecutor(testDispatcher.asExecutor())
                .setQueryExecutor(testDispatcher.asExecutor())
                .build()

    private val testDispatcher by lazy { TestCoroutineDispatcher() }
}
