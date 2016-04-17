import shapeless._
import shapeless.labelled.{field, FieldType}
import shapeless.Lazy
import shapeless.ops.hlist.Mapper
import shapeless.ops.record.{Fields, Keys}
import shapeless.syntax.singleton._
import scalaz.syntax.applicative._
import scalaz.syntax.nel._
import scalaz.syntax.std.string._
import scalaz.syntax.validation._
import scalaz.{\/, NonEmptyList, ValidationNel}

trait Parser[A] {
  def apply(ma: Map[String, Any]): ValidationNel[String, A]
}

trait FieldParser[A] {
  def apply(k: String, ma: Map[String, Any]): ValidationNel[String, A]
}

object toName extends Poly1 { implicit def keys[A] = at[Symbol with A](_.name) }

object Parser {
  implicit val hnilParser: Parser[HNil] = new Parser[HNil] {
    def apply(ma: Map[String, Any]): ValidationNel[String, HNil] =
      HNil.successNel[String]
  }

  implicit def hconsParser[K <: Symbol, H: FieldParser, T <: HList: Parser](implicit key: Witness.Aux[K]):
  Parser[FieldType[K, H] :: T] = new Parser[FieldType[K, H] :: T] {
    def apply(ma: Map[String, Any]): ValidationNel[String, FieldType[K, H] :: T] =
        (implicitly[FieldParser[H]].apply(key.value.name, ma).map(field[K](_))
          |@| implicitly[Parser[T]].apply(ma))(_ :: _)
  }

  implicit val stringParser: FieldParser[String] = new FieldParser[String] {
    def apply(k: String, ma: Map[String, Any]): ValidationNel[String, String] =
      \/.fromTryCatchNonFatal(ma(k).asInstanceOf[String])
        .leftMap(_ => s"$k not a string".wrapNel)
        .validation
  }

  implicit val intParser: FieldParser[Int] = new FieldParser[Int] {
    def apply(k: String, ma: Map[String, Any]): ValidationNel[String, Int] =
      \/.fromTryCatchNonFatal(ma(k).asInstanceOf[Int])
        .leftMap(_ => s"$k not an int".wrapNel)
        .validation
  }

  def apply[A](ma: Map[String, Any])(implicit parser: Parser[A]): ValidationNel[String, A] = parser(ma)

  implicit def caseClassParser[A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    reprParser: Lazy[Parser[R]]
  ): Parser[A] = new Parser[A] {
    def apply(ma: Map[String, Any]): ValidationNel[String, A] = reprParser.value.apply(ma).map(gen.from)
  }

}
