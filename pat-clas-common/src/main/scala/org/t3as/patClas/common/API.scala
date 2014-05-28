package org.t3as.patClas.common;

object API {
  
  trait HitBase {
    def score: Float
    def symbol: String
  }
  
  trait SearchService[H <: HitBase] {
    def search(q: String): List[H]
  }
  
  trait LookupService[D] {
    def ancestorsAndSelf(symbol: String, format: String): List[D]
    def children(parentId: Int, format: String): List[D]
  }

}
