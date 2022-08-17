package models

case class SearchResponse[T](hits: Seq[T], total: Long)
