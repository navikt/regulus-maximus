package no.nav.tsm.mottak.example

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@Serializable data class ExposedExample(val text: String, val someNumber: Int)

object ExampleService {
  private object Example : Table() {
    val id = integer("id").autoIncrement()
    val text = text("text")
    val someNumber = integer("some_number")

    override val primaryKey = PrimaryKey(id)
  }

  suspend fun <T> dbQuery(block: suspend () -> T): T =
      newSuspendedTransaction(Dispatchers.IO) { block() }

  suspend fun getAll(): List<ExposedExample> = dbQuery {
    Example.selectAll().orderBy(Example.id, SortOrder.DESC).limit(10).map {
      ExposedExample(it[Example.text], it[Example.someNumber])
    }
  }

  suspend fun create(example: ExposedExample): Int = dbQuery {
    Example.insert {
          it[text] = example.text
          it[someNumber] = example.someNumber
        }[Example.id]
  }

  suspend fun delete(id: Int) {
    dbQuery { Example.deleteWhere { Example.id.eq(id) } }
  }
}
