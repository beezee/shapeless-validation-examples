##Shapeless Validation Example

This demonstrates validation of case classes using scalaz and Shapeless.

The validated Poly1 must be defined at each of the field types on a given case class for it to validate.

By combining this with type tags, you can require at compile-time that a given case class is implemented
using only field types for which a validator has been defined.

Let's define two types, one that can be validated and one that cannot:

```tut
import shapeless.tag.@@
import Tags._

case class User(email: String @@ Email, age: Int @@ Age)
case class Superuser(email: String @@ Email, age: Int @@ Age, accessLevel: Int)
```

User can be validated because all of its field types have a definition in the validated Poly1

```tut
Example.validate(User("yungun", 17))
Example.validate(User("brian@gmail", 32))
```

Because Superuser contains fields which do not define a validator, validation fails to compile:

```tut:fail
Example.validate(Superuser("admin", 24, 7))
```
