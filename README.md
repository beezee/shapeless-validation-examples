##Shapeless Validation Example

This demonstrates validation of case classes using scalaz and Shapeless.

The validated Poly1 must be defined at each of the field types on a given case class for it to validate.

By combining this with type tags, you can require at compile-time that a given case class is implemented
using only field types for which a validator has been defined.

Let's define two types, one that can be validated and one that cannot:

```scala
scala> case class Foo(s: String, i: Int)
defined class Foo

scala> case class Bar(s: String, is: List[Int])
defined class Bar
```

Foo can be validated because all of its field types have a definition in the validated Poly1

```scala
scala> Example.validate(Foo("foo", 1))
res0: scalaz.Validation[scalaz.NonEmptyList[String],Boolean] = Failure(NonEmpty[String too short,Number too short])

scala> Example.validate(Foo("foobar", 10))
res1: scalaz.Validation[scalaz.NonEmptyList[String],Boolean] = Success(true)
```

Because Bar contains fields which do not define a validator, validation fails to compile:

```scala
scala> Example.validate(Bar("bar", List(1)))
<console>:15: error: could not find implicit value for parameter m: shapeless.ops.hlist.Mapper.Aux[validated.type,R,M]
       Example.validate(Bar("bar", List(1)))
                       ^
```

More about using type tags will go here...
