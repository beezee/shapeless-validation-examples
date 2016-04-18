import shapeless.{HList, HNil, LabelledGeneric, Lazy, Witness}
import shapeless.labelled.{field, FieldType}
import shapeless.ops.hlist.RemoveAll
import shapeless.record._
import shapeless.syntax.DynamicRecordOps
import shapeless.syntax.singleton._
import shapeless.tag.@@
import scalaz.syntax.applicative._
import scalaz.syntax.nel._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.syntax.validation._
import scalaz.{\/, ValidationNel}
import Tags._

trait Parser[L, A] {
  def apply(ma: Map[String, Any]): ValidationNel[L, A]
}

trait FieldParser[L, A] {
  def apply(k: String, ma: Map[String, Any]): ValidationNel[L, A]
}

trait BasicParser[L, A] {
  def apply(k: String, ma: Map[String, Any]): ValidationNel[L, A]
}

object parsers {

  implicit val stringParser: BasicParser[String, String] = new BasicParser[String, String] {
    def apply(k: String, ma: Map[String, Any]): ValidationNel[String, String] =
      \/.fromTryCatchNonFatal(ma(k).asInstanceOf[String])
        .leftMap(_ => s"$k not a string".wrapNel)
        .validation
  }

  implicit val intParser: BasicParser[String, Int] = new BasicParser[String, Int] {
    def apply(k: String, ma: Map[String, Any]): ValidationNel[String, Int] =
      \/.fromTryCatchNonFatal(ma(k).asInstanceOf[Int])
        .leftMap(_ => s"$k not an int".wrapNel)
        .validation
  }

  def validate[A, B](v: A => ValidationNel[String, B])(implicit fp: BasicParser[String, A]):
  FieldParser[String, B] = new FieldParser[String, B] {
    def apply(k: String, ma: Map[String, Any]) =
      fp.apply(k, ma).disjunction.flatMap(v.andThen(_.disjunction)).validation
  }

  implicit val emailParser =
    validate[String, String @@ Email](x =>
      x.contains("@").option(x: String @@ Email).toSuccess("invalid email".wrapNel))

  implicit val ageParser =
    validate[Int, Int @@ Age](x =>
      (x > 18).option(x: Int @@ Age).toSuccess("too young".wrapNel))
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

  def apply[L, A](ma: Map[String, Any])(implicit parser: Parser[L, A]): ValidationNel[L, A] = parser(ma)

  def record[L, A, R <: HList](ma: Map[String, Any])(implicit gen: LabelledGeneric.Aux[A, R],
                                          parser: Parser[L, R]): ValidationNel[L, DynamicRecordOps[R]] =
    parser(ma).map(_.record)

  def subtract[A <: HList, B <: HList](a: A, b: B)(implicit ra: RemoveAll[A, B]) =
    ra(a)

  implicit def caseClassParser[L, A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    reprParser: Lazy[Parser[L, R]]
  ): Parser[L, A] = new Parser[L, A] {
    def apply(ma: Map[String, Any]): ValidationNel[L, A] = reprParser.value.apply(ma).map(gen.from)
  }

}
