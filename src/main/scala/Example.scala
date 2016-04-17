import scalaz.syntax.applicative._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.syntax.validation._
import scalaz.{NonEmptyList, ValidationNel}
import shapeless.{HList, HNil, Poly1, Poly2, tag}
import shapeless.Generic
import shapeless.ops.hlist._
import shapeless.ops.hlist.Mapper
import shapeless.UnaryTCConstraint._
import scala.language.implicitConversions
import StringNelT._
import tag.@@
import Tags._

object StringNelT {
  type StringNel[A] = ValidationNel[String, A]
}

object Tags {
  implicit def tagA[A, B](a: A): A @@ B = tag[B](a)
  trait Age
  trait Email
}

case class Person(email: String @@ Email, age: Int @@ Age)

object validated extends Poly1 {
  implicit def age = at[Int @@ Age](i => (i > 18).option(true).toSuccess(NonEmptyList("Too young")))
  implicit def email =
    at[String @@ Email](s => (s.contains("@")).option(true).toSuccess(NonEmptyList("Invalid email")))
}

object vFolder extends Poly2 {
  implicit def apV[A, B <: HList] = at[StringNel[A], StringNel[B]]((x, y) => (x |@| y)(_ :: _))
}

object Example extends App {

  def validate[A, R <: HList, M <: HList](a: A)(
        implicit au: Generic.Aux[A, R], m: Mapper.Aux[validated.type, R, M],
                  tt: ToTraversable.Aux[M, List, ValidationNel[String, Boolean]]) =
    au.to(a).map(validated).toList.reduce(_ <* _)

  def sequence[L <: HList : *->*[StringNel]#λ, M <: HList](l: L)(implicit
    folder: RightFolder[L, StringNel[HNil], vFolder.type]
  ) = l.foldRight(HNil.successNel[String]: StringNel[HNil])(vFolder)
}
