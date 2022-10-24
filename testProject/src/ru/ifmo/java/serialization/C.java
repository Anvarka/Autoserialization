package ru.ifmo.java.serialization;

import java.util.Objects;

// Task #2
@Letterize
public class C implements Letter {
    public int x;
    public A a;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        C c = (C) o;
        return x == c.x &&
                Objects.equals(a, c.a);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, a);
    }
}
