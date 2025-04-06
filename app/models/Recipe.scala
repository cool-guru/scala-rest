package models

import java.sql.Timestamp

case class Recipe(
  id: Option[Int],
  title: String,
  making_time: String,
  serves: String,
  ingredients: String,
  cost: Int,
  created_at: Option[Timestamp] = None,  // Automatically set when the record is created
  updated_at: Option[Timestamp] = None   // Will be updated when the recipe is modified
)
