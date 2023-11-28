package tech.relaycorp.gateway.test

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import tech.relaycorp.gateway.data.database.AppDatabase
import tech.relaycorp.gateway.test.AppTestProvider.component
import javax.inject.Inject

class ClearTestDatabaseRule : TestRule {

    @Inject
    lateinit var database: AppDatabase

    override fun apply(base: Statement, description: Description?) = object : Statement() {
        override fun evaluate() {
            component.inject(this@ClearTestDatabaseRule)
            database.clearAllTables()
            base.evaluate()
            database.clearAllTables()
        }
    }
}
