import shapeless.{HList, HNil, LabelledGeneric, Lazy, Witness}
import shapeless.labelled.{field, FieldType}
import shapeless.syntax.singleton._
import scalaz.syntax.applicative._
import scalaz.syntax.nel._
import scalaz.syntax.validation._
import scalaz.{\/, ValidationNel}

trait Parser[L, A] {
  def apply(ma: Map[String, Any]): ValidationNel[L, A]
}

trait FieldParser[L, A] {
  def apply(k: String, ma: Map[String, Any]): ValidationNel[L, A]
}

object Parser {
  implicit def hnilParser[L]: Parser[L, HNil] = new Parser[L, HNil] {
    def apply(ma: Map[String, Any]): ValidationNel[L, HNil] =
      HNil.successNel[L]
  }

  implicit def hconsParser[L, K <: Symbol, H, T <: HList](
    implicit key: Witness.Aux[K], fp: FieldParser[L, H], tp: Parser[L, T]):
  Parser[L, shapeless.::[FieldType[K, H], T]] = new Parser[L, shapeless.::[FieldType[K, H], T]] {
    def apply(ma: Map[String, Any]): ValidationNel[L, shapeless.::[FieldType[K, H], T]] =
        (fp.apply(key.value.name, ma).map(field[K](_))
          |@| implicitly[Parser[L, T]].apply(ma))(_ :: _)
  }

  implicit val stringParser: FieldParser[String, String] = new FieldParser[String, String] {
    def apply(k: String, ma: Map[String, Any]): ValidationNel[String, String] =
      \/.fromTryCatchNonFatal(ma(k).asInstanceOf[String])
        .leftMap(_ => s"$k not a string".wrapNel)
        .validation
  }

  implicit val intParser: FieldParser[String, Int] = new FieldParser[String, Int] {
    def apply(k: String, ma: Map[String, Any]): ValidationNel[String, Int] =
      \/.fromTryCatchNonFatal(ma(k).asInstanceOf[Int])
        .leftMap(_ => s"$k not an int".wrapNel)
        .validation
  }

  def apply[L, A](ma: Map[String, Any])(implicit parser: Parser[L, A]): ValidationNel[L, A] = parser(ma)

  implicit def caseClassParser[L, A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    reprParser: Lazy[Parser[L, R]]
  ): Parser[L, A] = new Parser[L, A] {
    def apply(ma: Map[String, Any]): ValidationNel[L, A] = reprParser.value.apply(ma).map(gen.from)
  }

}
