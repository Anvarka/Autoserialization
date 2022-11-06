package ru.ifmo.java.serialization.processor;

import com.squareup.javapoet.*;
import ru.ifmo.java.serialization.Letterize;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {
    public static MethodSpec.Builder createConstructor(Class<?> className, String constant) {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(className, constant)
                .addStatement("this.$N = $N", constant, constant);

    }

    public static boolean isInheritedFromLetter(Element element, Types typeUtils) {
        Element currentElement = element;
        while (!Object.class.getSimpleName().contentEquals(currentElement.getSimpleName())) {
            for (TypeMirror el : ((TypeElement) currentElement).getInterfaces()) {
                if (typeUtils.asElement(el).getSimpleName().toString().equals(Constants.LETTER))
                    return true;
            }
            TypeElement elem = (TypeElement) currentElement;
            currentElement = typeUtils.asElement(elem.getSuperclass());
        }
        return false;
    }

    public static boolean hasLetterizeAnnotation(Element element) {
        return element.getAnnotation(Letterize.class) != null;
    }

    public static String getTypeOfField(Element element) {
        if (element.asType().getKind().isPrimitive()) {
            String resType = element.asType().toString();
            return resType.substring(0, 1).toUpperCase() + resType.substring(1);
        } else {
            if (element.asType().toString().equals("java.lang.String")) {
                return "UTF";
            }
            return "";
        }
    }

    static String getDefaultValue(Types typeUtils, Element e) {
        switch (e.asType().getKind()) {
            case BOOLEAN:
                return "false";
            case INT:
                return "0";
            case FLOAT:
            case DOUBLE:
                return "0.0";
            case DECLARED:
                if (e.asType().toString().equals(String.class.getCanonicalName())) {
                    return "\"\"";
                }
                return "null";
            default:
                return "null";
        }
    }

    static FieldSpec createList(String listName, Class<?> typeOfList) {
        return FieldSpec
                .builder(ParameterizedTypeName.get(List.class, typeOfList), listName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .initializer("new $T<>()", ClassName.get("java.util",
                        ArrayList.class.getSimpleName()))
                .build();
    }

    static FieldSpec createSet(String setName, Class<?> typeOfSet) {
        return FieldSpec
                .builder(ParameterizedTypeName.get(Set.class, typeOfSet), setName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .initializer("new $T<>()", ClassName.get("java.util",
                        HashSet.class.getSimpleName()))
                .build();
    }
}
