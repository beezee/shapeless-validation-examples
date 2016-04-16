##Shapeless Validation Example

This demonstrates validation of case classes using scalaz and Shapeless.

The validated Poly1 must be defined at each of the field types on a given case class for it to validate.

By combining this with type tags, you can require at compile-time that a given case class is implemented
using only field types for which a validator has been defined.

Let's define two types, one that can be validated and one that cannot:

```scala
scala> import shapeless.tag.@@
import shapeless.tag.$at$at

scala> import Tags._
import Tags._

scala> case class User(email: String @@ Email, age: Int @@ Age)
defined class User

scala> case class Superuser(email: String @@ Email, age: Int @@ Age, accessLevel: Int)
defined class Superuser
```

User can be validated because all of its field types have a definition in the validated Poly1

```scala
scala> Example.validate(User("yungun", 17))
res0: scalaz.Validation[scalaz.NonEmptyList[String],Boolean] = Failure(NonEmpty[Invalid email,Too young])

scala> Example.validate(User("brian@gmail", 32))
res1: scalaz.Validation[scalaz.NonEmptyList[String],Boolean] = Success(true)
```

Because Superuser contains fields which do not define a validator, validation fails to compile:

```scala
scala> Example.validate(Superuser("admin", 24, 7))
<console>:19: error: could not find implicit value for parameter m: shapeless.ops.hlist.Mapper.Aux[validated.type,R,M]
       Example.validate(Superuser("admin", 24, 7))
                       ^
```
