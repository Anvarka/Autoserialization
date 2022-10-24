# Autoserialization

## Идея

В данной работе вам предстоит реализовать небольшой фреймфорк для автоматической (де)сериализации объектов, наподобие `@Serializable` в Java.

С точки зрения пользователя это выглядит следующим образом. Есть следующие классы и интерфейсы:

```java
public interface Letter {}

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Letterize {
}
```

Пользователь помечает класс, объекты которого будет необходимо сериализовывать/десериализовывать аннотацией `@Letterize` и наследует от интерфейса `Letter`:

```java
@Letterize
class Person implements Letter {
    public int age;
    public String name;
}
```

После этого, становится возможно (де)сериализовывать объекты этого класса следующим образом:

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

## Реализация

Для этого вам необходимо будет реализовать annotation processor, который будет находить классы, помеченные аннотацией `@Letterize` и генерировать два класса `LetterSerializer` и `LetterDeserializer`, в которых будут методы для сериализации и десериализации всех классов, помеченных аннотацией `@Letterize`.

Например, для примера выше после работы anotation processor'а, будут сгенерированны следующие классы:

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

    public Person desrializePerson() throws IOException {
        Person person = new Person();
        person.age = input.readInt();
        person.name = input.readUTF();
        return person;    
    }
}
```

В данном задании *запрещается* пользоваться:
- `Object*Stream`
- Сторонними библиотеками для кодогенерации, за исключением `javapoet` (уже добавлена в зависимости проекта)
- Сторонними библиотеками для (де)сериализации


При этом, разрешается просить у пользователя подключить какие-то дополнительные утилитные классы из вашей библиотеки (это может быть полезно, чтобы упростить генерируемый код).


**Общие ограничения для всех заданий:**
- Все поля публичные, не финальные
- В случае, если нарушен формат сообщения, бросается `IllegalLetterFormatException`
- Помните, что классы могут наследоваться!

## Задание №1 (3 балла)

Реализовать (де)серилазиацию для простейших случаев:
- Классы состоят только из полей примитивных типов + `String` (т.е. вложенные сообщения поддерживать в данном задании не требуется)
- Наличие данных для всех полей обязательно

## Задание №2 (3 балла)

Поддержать вложенные сообщения:
- Класс может содержать поля reference-типов.
- Все такие поля должны быть помечены `@Letterize` и отнаследованы от интерфейса `Letter`, в противном случае компиляция должна завершиться с ошибкой, в которой описывается, для какого класса это требование было нарушено.
- Циклические структуры данных не поддерживаются, в противном случае компиляция должна завершиться с ошибкой.
- Помните, что значением reference-поля может быть `null`!

## Задание №3 (3 балла)

Поддержать циклические структуры данных

## Задание №4 (3 балла)

Поддержать опциональные поля. Опциональное поле помечено аннотацией `@LetterizeOptional` и *не обязано* присутствовать в десериализуемом сообщении.
Если при десериализации сообщения такое поле не было обнаружено во входном потоке, то это не приводит к `IllegalLetterFormatException`, а вместо этого оно заполняется дефолтным значением:
- `0` для целочисленных типов
- `0.0` для вещественных типов
- `""` (пустая строка) для `String`
- `null` для reference-типов

Пояснение: такая ситуация может случиться в следующем случае:
1. однажды был создан и сериализован (на диск, например) некоторый класс
2. в класс было добавлено поле с аннотацией `@LetterizeOptional`, после чего он был заного скомпилированн
3. этот обновлённый класс десериализуется из дампа, созданного в п.1

## Задание №5 (2 балла)

Добавить в `Serializer` и `Deserializer` методы `void serialize(Class<?> clazz, Object object)` и `Object deserialize(Class<?> clazz)` соответсвенно, которые внутри себя будут вызывать соответствующий `serialize*` и `deserialize*` в зависимости от переданного `Class<?>` или бросать `IllegalArgumentException`, если для переданный класс не имеет импементации (де)сериализации

# Баллы и сроки

За выполнение всех частей задания можно получить 14 баллов из 10-ти

Мягкий дедлайн: 30.10.22, 23:59
Жёсткий дедлайн: 06.11.22, 23:59
