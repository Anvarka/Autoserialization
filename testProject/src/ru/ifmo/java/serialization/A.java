package ru.ifmo.java.serialization;

import java.util.Objects;

// Task #1
@Letterize
public class A implements Letter {
    public int x;
    public boolean b;
    public String s;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        A a = (A) o;
        return x == a.x &&
                b == a.b &&
                Objects.equals(s, a.s);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, b, s);
    }
}
