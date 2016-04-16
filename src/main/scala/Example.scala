import scalaz.syntax.apply._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.syntax.validation._
import scalaz.{NonEmptyList, ValidationNel}
import shapeless.{HList, Poly1}
import shapeless.Generic
import shapeless.ops.hlist._
import shapeless.ops.hlist.Mapper

object validated extends Poly1 {
  implicit def intCase = at[Int](i => (i > 5).option(true).toSuccess(NonEmptyList("Number too short")))
  implicit def stringCase = at[String](s => (s.length > 5).option(true).toSuccess(NonEmptyList("String too short")))
}

object Example extends App {

  def validate[A, R <: HList, M <: HList](a: A)(
        implicit au: Generic.Aux[A, R], m: Mapper.Aux[validated.type, R, M],
                  tt: ToTraversable.Aux[M, List, ValidationNel[String, Boolean]]) =
    au.to(a).map(validated).toList.reduce(_ <* _)
}
