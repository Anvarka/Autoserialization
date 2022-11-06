package ru.ifmo.java.serialization.processor;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import ru.ifmo.java.serialization.IllegalLetterFormatException;
import ru.ifmo.java.serialization.LetterizeOptional;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.util.Types;
import java.util.*;

public abstract class BaseFactory {

    public Types typeUtils;
    public RoundEnvironment roundEnv;
    public List<Map.Entry<Element,Element>> optionalElements1 = new ArrayList<>();
    public TypeSpec.Builder classBuilder;

    public abstract void createConstructor();

    public abstract MethodSpec.Builder getOverloadMethod(Name classNameForSerialize, PackageElement pack);

    public abstract MethodSpec.Builder getBaseMethod(Name classNameForSerialize, PackageElement pack);

    // task 5
    public abstract void getUniversalMethod();

    public abstract void actionsWithMethod(MethodSpec.Builder method, Element annotatedClass, Name classNameForSerialize);

    public void createMethod(Element annotatedClass, PackageElement pack) {
        Name classNameForSerialize = annotatedClass.getSimpleName();

        MethodSpec.Builder baseMethod = getBaseMethod(classNameForSerialize, pack);
        classBuilder.addMethod(baseMethod.build());
        // overload for (de)serialize method
        MethodSpec.Builder overloadMethod = getOverloadMethod(classNameForSerialize, pack);

        actionsWithMethod(overloadMethod, annotatedClass, classNameForSerialize);
        classBuilder.addMethod(overloadMethod.build());
    }

    public abstract void createCodeForReferenceType(MethodSpec.Builder deserializeMethod, Element annotatedClass, Element fieldOfClass);

    public abstract void workWithFieldsOfClass(MethodSpec.Builder deserializeMethod,
                                               String typeOfField,
                                               Element annotatedClass,
                                               Element fieldOfAnnotatedClass,
                                               String classNameForDeserializationLowerCase);

    public void methodForReferenceType(MethodSpec.Builder serializeMethod, Element annotatedClass, Element element) {
        Element fieldType = typeUtils.asElement(element.asType());
        if (!Utils.hasLetterizeAnnotation(fieldType) && !Utils.isInheritedFromLetter(fieldType, typeUtils)) {
            throw new IllegalLetterFormatException("Field has not Letterize annotation");
        }
        createCodeForReferenceType(serializeMethod, annotatedClass, element);
    }

    public void visitFieldsOfClassToParent(MethodSpec.Builder serializeMethod, Element currentAnnotatedClass, String classNameForSerializationLowerCase) {
        while (!Object.class.getSimpleName().contentEquals(typeUtils.asElement(currentAnnotatedClass.asType()).getSimpleName())) {
            for (Element fieldOfAnnotatedClass : currentAnnotatedClass.getEnclosedElements()) {
                if (fieldOfAnnotatedClass.getKind() != ElementKind.FIELD) {
                    continue;
                }
                if (fieldOfAnnotatedClass.getAnnotation(LetterizeOptional.class) != null) {
                    optionalElements1.add(Map.entry(currentAnnotatedClass, fieldOfAnnotatedClass));
                    continue;
                }
                String typeOfField = Utils.getTypeOfField(fieldOfAnnotatedClass);
                workWithFieldsOfClass(serializeMethod, typeOfField, currentAnnotatedClass, fieldOfAnnotatedClass, classNameForSerializationLowerCase);
            }
            TypeElement elem = (TypeElement) currentAnnotatedClass;
            currentAnnotatedClass = typeUtils.asElement(elem.getSuperclass());
        }

        for (Map.Entry<Element, Element> fieldOfAnnotatedClass : optionalElements1) {
            String typeOfField = Utils.getTypeOfField(fieldOfAnnotatedClass.getValue());
            currentAnnotatedClass = fieldOfAnnotatedClass.getKey();
            String classNameForSerializationLowerCase2 = currentAnnotatedClass.getSimpleName().toString().toLowerCase();
            workWithFieldsOfClass(serializeMethod, typeOfField, currentAnnotatedClass, fieldOfAnnotatedClass.getValue(), classNameForSerializationLowerCase2);
        }
        optionalElements1.clear();
    }

    public TypeSpec get() {
        return classBuilder.build();
    }

}
