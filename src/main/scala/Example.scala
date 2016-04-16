import scalaz.syntax.apply._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.syntax.validation._
import scalaz.{NonEmptyList, ValidationNel}
import shapeless.{HList, Poly1, tag}
import shapeless.Generic
import shapeless.ops.hlist._
import shapeless.ops.hlist.Mapper
import scala.language.implicitConversions
import tag.@@
import Tags._

object Tags {
  implicit def tagA[A, B](a: A): A @@ B = tag[B](a)
  trait Age
  trait Email
}

object validated extends Poly1 {
  implicit def age = at[Int @@ Age](i => (i > 18).option(true).toSuccess(NonEmptyList("Too young")))
  implicit def email =
    at[String @@ Email](s => (s.contains("@")).option(true).toSuccess(NonEmptyList("Invalid email")))
}

object Example extends App {

  def validate[A, R <: HList, M <: HList](a: A)(
        implicit au: Generic.Aux[A, R], m: Mapper.Aux[validated.type, R, M],
                  tt: ToTraversable.Aux[M, List, ValidationNel[String, Boolean]]) =
    au.to(a).map(validated).toList.reduce(_ <* _)
}
