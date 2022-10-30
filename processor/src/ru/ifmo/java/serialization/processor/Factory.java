package ru.ifmo.java.serialization.processor;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.io.DataInputStream;

public class Factory {
    public static void createConstructor(Class<?> className, String constant, TypeSpec.Builder classBuilder) {
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(className, constant)
                .addStatement("this.$N = $N", constant, constant)
                .build();
        classBuilder.addMethod(constructor);
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

    public static String getTypeOfField(Element element) {
        if (element.asType().getKind() == TypeKind.DECLARED) {
            if (element.asType().toString().equals("java.lang.String")) {
                return "UTF";
            } else {
                return "";
            }
        } else {
            String resType = element.asType().toString();
            return resType.substring(0, 1).toUpperCase() + resType.substring(1);
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
                if (typeUtils.asElement(e.asType()).getSimpleName().toString().equals("String")) {
                    return "\"\"";
                }
                return "null";
            default:
                return "null";
        }
    }
}
