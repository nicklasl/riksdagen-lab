package nu.nldv.riksdag.model

case class Result(year: String, vertices: Set[Intressent], edges: Map[IntressentPair, Int])
