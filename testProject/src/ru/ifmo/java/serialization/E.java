package ru.ifmo.java.serialization;

// Task #4
@Letterize
public class E implements Letter {
    public int x;

//    Uncomment for second stage of test
    @LetterizeOptional
    public String name;

    public int cat;

    @LetterizeOptional
    public int age;

    @LetterizeOptional
    public boolean male;
}
