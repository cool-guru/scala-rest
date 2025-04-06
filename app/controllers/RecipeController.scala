package controllers

import play.api.mvc._
import javax.inject._
import java.sql.Timestamp
import play.api.libs.json._
import models.Recipe
import repositories.RecipeRepository

@Singleton
class RecipeController @Inject()(cc: ControllerComponents, repo: RecipeRepository) extends AbstractController(cc) {

  implicit val timestampFormat: Format[Timestamp] = new Format[Timestamp] {
    def writes(ts: Timestamp): JsValue = JsString(ts.toString)
    def reads(json: JsValue): JsResult[Timestamp] = json match {
      case JsString(s) => JsSuccess(Timestamp.valueOf(s))
      case _           => JsError("Expected string for timestamp")
    }
  }

  implicit val recipeFormat: OFormat[Recipe] = Json.format[Recipe]

  def create = Action(parse.json) { request =>
    request.body.validate[Recipe].fold(
      _ => BadRequest(Json.obj(
        "message" -> "Recipe creation failed!",
        "required" -> "title, making_time, serves, ingredients, cost"
      )),
      recipe => {
        repo.create(recipe.copy(id = None)) match {
          case Some(created) => Ok(Json.obj(
            "message" -> "Recipe successfully created!",
            "recipe" -> Json.toJson(Seq(created))
          ))
          case None => InternalServerError("Failed to insert")
        }
      }
    )
  }

  def getAll = Action {
    val recipesJson = repo.findAll().map { r =>
      Json.obj(
        "id" -> r.id.get,
        "title" -> r.title,
        "making_time" -> r.making_time,
        "serves" -> r.serves,
        "ingredients" -> r.ingredients,
        "cost" -> r.cost.toString
      )
    }
    Ok(Json.obj("recipes" -> JsArray(recipesJson)))
  }

  def getById(id: Int) = Action {
    repo.findById(id) match {
      case Some(recipe) =>
        val recipeJson = Json.obj(
          "id" -> recipe.id.get,
          "title" -> recipe.title,
          "making_time" -> recipe.making_time,
          "serves" -> recipe.serves,
          "ingredients" -> recipe.ingredients,
          "cost" -> recipe.cost.toString
        )
        Ok(Json.obj(
          "message" -> "Recipe details by id",
          "recipe" -> Json.arr(recipeJson)
        ))
      case None =>
        NotFound(Json.obj("message" -> "No Recipe found"))
    }
  }

  def update(id: Int): Action[JsValue] = Action(parse.json) { request =>
    val json = request.body

    val title        = (json \ "title").asOpt[String]
    val makingTime   = (json \ "making_time").asOpt[String]
    val serves       = (json \ "serves").asOpt[String]
    val ingredients  = (json \ "ingredients").asOpt[String]
    val cost         = (json \ "cost").asOpt[Int]

    if (Seq(title, makingTime, serves, ingredients, cost).contains(None)) {
      BadRequest(Json.obj("message" -> "Invalid parameters"))
    } else {
      val recipeToUpdate = Recipe(
        id = Some(id),
        title = title.get,
        making_time = makingTime.get,
        serves = serves.get,
        ingredients = ingredients.get,
        cost = cost.get.toInt,
        created_at = None,
        updated_at = None
      )

      repo.update(id, recipeToUpdate) match {
        case Some(updated) =>
          val responseJson = Json.obj(
            "title" -> updated.title,
            "making_time" -> updated.making_time,
            "serves" -> updated.serves,
            "ingredients" -> updated.ingredients,
            "cost" -> updated.cost.toString
          )
          Ok(Json.obj(
            "message" -> "Recipe successfully updated!",
            "recipe" -> Json.arr(responseJson)
          ))

        case None =>
          NotFound(Json.obj("message" -> "No Recipe found"))
      }
    }
  }
  def delete(id: Int) = Action {
    if (repo.delete(id)) {
      Ok(Json.obj("message" -> "Recipe successfully removed!"))
    } else {
      NotFound(Json.obj("message" -> "No recipe found"))
    }
  }
}
