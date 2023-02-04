# Autoserialization

## Idea

In this work, you will implement a small framework for automatic (de)serialization of objects, like `@Serializable` in Java.

From the user's point of view, it looks like this. There are the following classes and interfaces:

```java
public interface Letter {}

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Letterize {
}
```

The user marks the class whose objects will need to be serialized/deserialized with the `@Letterize` annotation and inherits from the `Letter` interface:

```java
@letterize
class Person implements Letter {
     public int age;
     public Stringname;
}
```

After that, it becomes possible to (de)serialize objects of this class like this:

```java
private void showcaseSend() throws IOException {
     DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
     LetterSerializer serializer = new LetterSerializer(dataOutputStream);

     Person person = new Person();
     person.age = 24;
     person.name = "John Doe";

   
     serializer.serializePerson(person);
}


private void showcaseReceive() throws IOException {
     DataInputStream inputStream = new DataInputStream(socket.getInputStream());
     LetterDeserializer deserializer = new LetterDeserializer(dataOutputStream);

     Person person = deserializer.deserializePerson();

     assertEquals(24, person.age);
     assertEquals("John Doe", person.name);
}
```

## Implementation

To do this, you will need to implement an annotation processor that will find classes marked with the `@Letterize` annotation and generate two classes `LetterSerializer` and `LetterDeserializer`, which will have methods for serializing and deserializing all classes marked with the `@Letterize` annotation.

For example, for the example above, after running the anotation processor, the following classes will be generated:

```java
public class Serializer {
     private final DataOutputStream output;

     public Serializer(DataOutputStream output) {
         this.output = output;
     }

     public void serializePerson(Person person) throws IOException {
         output.writeInt(person.age);
         output.writeUTF(person.name);
     }
}

public class Deserializer {
     private final DataInputStream input;

     public Deserializer(DataInputStream input) {
         this.input = input;
     }

     public Person deserializePerson() throws IOException {
         Person person = new Person();
         person.age = input.readInt();
         person.name = input.readUTF();
         return person;
     }
}
```

In this task it is *prohibited* to use:
- `Object*Stream`
- Third-party libraries for code generation, except for `javapoet` (already added to project dependencies)
- Third-party libraries for (de)serialization


At the same time, it is allowed to ask the user to include some additional utility classes from your library (this can be useful to simplify the generated code).


**Common limits for all jobs:**
- All fields are public, not final
- In case the message format is violated, an `IllegalLetterFormatException` is thrown
- Remember that classes can be inherited!

## Task

Implement (de)serialization for the simplest cases:
- Classes consist only of fields of primitive types + `String` (i.e. nested messages are not required to be supported in this task)
- The presence of data for all fields is required

## Task

Support nested messages:
- The class can contain fields of reference-types.
- All such fields must be marked `@Letterize` and inherited from the `Letter` interface, otherwise the compilation must fail with an error describing which class this requirement was violated for.
- Cyclic data structures are not supported, otherwise compilation should fail.
- Remember that the value of a reference field can be `null`!

## Task

Support cyclic data structures

## Task

Support optional fields. The optional field is annotated with the `@LetterizeOptional` annotation and is *not required* to be present in the message being deserialized.
If such a field was not found in the input stream when the message was deserialized, then this does not result in an `IllegalLetterFormatException`, but instead it is filled with a default value:
- `0` for integer types
- `0.0` for real types
- `""` (empty string) for `String`
- `null` for reference types

Explanation: this situation can happen in the following case:
1. once a class was created and serialized (to disk, for example)
2. a field with the `@LetterizeOptional` annotation was added to the class, after which it was recompiled
3. this updated class is deserialized from the dump created in step 1

## Task

Add to `Serializer` and `Deserializer` the methods `void serialize(Class<?> clazz, Object object)` and `Object deserialize(Class<?> clazz)` respectively, which will internally call the corresponding matching `serialize*` and `deserialize*` depending on the passed `Class<?>` or throwing `IllegalArgumentException` if the passed class has no implementation (de)serialization

