package ru.ifmo.java.serialization;

import java.util.Objects;

// Task #1
@Letterize
public class B extends A {
    public double d;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B b = (B) o;
        return Double.compare(b.d, d) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(d);
    }
}
