package repositories

import javax.inject._
import javax.sql.DataSource
import java.sql.{Connection, Timestamp}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import anorm._
import anorm.SqlParser._
import models.Recipe

@Singleton
class RecipeRepository @Inject()(dataSource: DataSource) {

    private var recipes: Map[Int, Recipe] = Map()

    implicit val columnToOptTimestamp: Column[Option[Timestamp]] = Column { (value, _) =>
        try {
        value match {
            case ts: Timestamp        => Right(Some(ts))
            case ldt: LocalDateTime   => Right(Some(Timestamp.valueOf(ldt)))
            case s: String =>
            val formatted = if (s.contains("T")) s else s.replace(" ", "T")
            Right(Some(Timestamp.valueOf(LocalDateTime.parse(formatted))))
            case null => Right(None)
            case _    => Left(TypeDoesNotMatch(s"Cannot convert $value (${value.getClass}) to Timestamp"))
        }
        } catch {
        case e: Exception =>
            Left(TypeDoesNotMatch(s"Timestamp parse error for value $value: ${e.getMessage}"))
        }
    }

    val recipeParser: RowParser[Recipe] = {
        get[Option[Int]]("id") ~
        get[String]("title") ~
        get[String]("making_time") ~
        get[String]("serves") ~
        get[String]("ingredients") ~
        get[Int]("cost") ~
        get[Option[Timestamp]]("created_at") ~
        get[Option[Timestamp]]("updated_at") map {
        case id ~ title ~ making_time ~ serves ~ ingredients ~ cost ~ created_at ~ updated_at =>
            Recipe(id, title, making_time, serves, ingredients, cost, created_at, updated_at)
        }
    }

    def create(recipe: Recipe): Option[Recipe] = {
        withConnection { implicit conn =>
        SQL"""
            INSERT INTO recipes (title, making_time, serves, ingredients, cost)
            VALUES (${recipe.title}, ${recipe.making_time}, ${recipe.serves}, ${recipe.ingredients}, ${recipe.cost})
        """.executeInsert().flatMap(id => findById(id.toInt))
        }
    }

    def findById(id: Int): Option[Recipe] = {
        withConnection { implicit conn =>
        SQL"SELECT * FROM recipes WHERE id = $id".as(recipeParser.singleOpt)
        }
    }

    def findAll(): List[Recipe] = {
        withConnection { implicit conn =>
        SQL"SELECT * FROM recipes".as(recipeParser.*)
        }
    }

    def update(id: Int, updatedRecipe: Recipe): Option[Recipe] = {
        withConnection { implicit conn =>
        val rowsUpdated = SQL"""
            UPDATE recipes
            SET title = ${updatedRecipe.title},
                making_time = ${updatedRecipe.making_time},
                serves = ${updatedRecipe.serves},
                ingredients = ${updatedRecipe.ingredients},
                cost = ${updatedRecipe.cost},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = $id
        """.executeUpdate()

        if (rowsUpdated > 0) findById(id) else None
        }
    }

    def delete(id: Int): Boolean = {
        withConnection { implicit conn =>
            val rowsDeleted = SQL"DELETE FROM recipes WHERE id = $id".executeUpdate()
            rowsDeleted > 0
        }
    }

    private def withConnection[A](block: Connection => A): A = {
        val conn = dataSource.getConnection
        try {
        block(conn)
        } finally {
        conn.close()
        }
    }
}
