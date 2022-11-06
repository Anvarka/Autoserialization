package ru.ifmo.java.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.*;
import org.junit.jupiter.api.Test;

public class SerializationTest {
    @Test
    public void testSimple() throws IOException {
        Person person = new Person();
        person.age = 10;
        person.name = "Peter";

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new Serializer(new DataOutputStream(outputStream)).serializePerson(person);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        assertEquals(person, new Deserializer(new DataInputStream(inputStream)).deserializePerson());
    }

    @Test
    public void testLetterizeOptional() throws IOException {
        File file = new File("E.dump");

        // First stage
        E e = new E();
        e.x = 10;
        e.cat = 120;
        new Serializer(new DataOutputStream(new FileOutputStream(file))).serializeE(e);

        // Second stage
//        E e = new Deserializer(new DataInputStream(new FileInputStream(file))).deserializeE();
//        assertEquals(e.x, 10);
//        assertEquals(e.name, "");
//        assertEquals(e.age, 0);
//        assertEquals(e.male, false);
    }

    @Test
    public void testSimpleRecursion() throws IOException {
        D d = new D();
        d.name = "Peter";
        d.d = d;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new Serializer(new DataOutputStream(outputStream)).serializeD(d);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        D deserializedD = new Deserializer(new DataInputStream(inputStream)).deserializeD();
        assertEquals(d.name, deserializedD.name);
        assertEquals(deserializedD.d, deserializedD);
    }

    @Test
    public void testComplexRecursion() throws IOException {
        D d1 = new D();
        d1.name = "Peter";
        D d2 = new D();
        d2.name = "John";
        d2.d = d1;
        d1.d = d2;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new Serializer(new DataOutputStream(outputStream)).serializeD(d1);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        D deserializedD1 = new Deserializer(new DataInputStream(inputStream)).deserializeD();
        D deserializedD2 = deserializedD1.d;
        assertEquals(d1.name, deserializedD1.name);
        assertEquals(d2.name, deserializedD2.name);
        assertEquals(deserializedD1.d, deserializedD2);
        assertEquals(deserializedD2.d, deserializedD1);
    }

    @Test
    public void testNewSerializeMethod() throws IOException {
        Person person = new Person();
        person.age = 10;
        person.name = "Peter";

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new Serializer(new DataOutputStream(outputStream)).serialize(Person.class, person);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        assertEquals(person, new Deserializer(new DataInputStream(inputStream)).deserialize(Person.class));
    }

}
