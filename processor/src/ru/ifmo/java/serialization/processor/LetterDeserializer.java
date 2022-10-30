package ru.ifmo.java.serialization.processor;

import com.squareup.javapoet.*;
import ru.ifmo.java.serialization.IllegalLetterFormatException;
import ru.ifmo.java.serialization.LetterizeOptional;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.util.Types;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class LetterDeserializer {
    private Types typeUtils;
    private RoundEnvironment roundEnv;
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder("Deserializer")
            .addModifiers(Modifier.PUBLIC)
            .addField(DataInputStream.class, Constants.INPUT, Modifier.PUBLIC, Modifier.FINAL);

    public static Set<String> elementsWithOptionalAnnotation = new HashSet<>();

    public LetterDeserializer(Types types, RoundEnvironment roundEnv) {
        this.typeUtils = types;
        this.roundEnv = roundEnv;
        FieldSpec optionalListField = FieldSpec
                .builder(ParameterizedTypeName.get(Set.class, String.class), "optionalElements")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .initializer("new $T<>()", ClassName.get("java.util",
                        HashSet.class.getSimpleName()))
                .build();
        classBuilder.addField(optionalListField);
    }

    public void createDeserializeConstructor() {
//        Factory.createConstructor(DataInputStream.class, Constants.INPUT, classBuilder);
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(DataInputStream.class, Constants.INPUT)
                .addStatement("this.$N = $N", Constants.INPUT, Constants.INPUT);
        getDeserializeMethod();
        getOptionalFields();

        for (String s : elementsWithOptionalAnnotation) {
            constructor.addStatement("optionalElements.add($S)", s);
        }
        classBuilder.addMethod(constructor.build());


    }

    public TypeSpec get() {
        return classBuilder.build();
    }

    public void createDeserializeMethod(Element annotatedElement, PackageElement pack) {
        if (!Factory.isInheritedFromLetter(annotatedElement, typeUtils)) {
            throw new RuntimeException("class " + annotatedElement.getSimpleName() + " does not inherit from Letter");
        }

        Name classNameForDeserialize = annotatedElement.getSimpleName();

        MethodSpec.Builder deserializeMethod = MethodSpec.methodBuilder("deserialize" + classNameForDeserialize)
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .returns(ClassName.get("ru.ifmo.java.serialization", classNameForDeserialize.toString()));

        deserializeMethod.addStatement("$N $N = new $N()", classNameForDeserialize.toString(), annotatedElement.getSimpleName().toString().toLowerCase(),
                classNameForDeserialize.toString());

        getFieldsFromClass(deserializeMethod, annotatedElement);
        deserializeMethod.addStatement("return $N", annotatedElement.getSimpleName().toString().toLowerCase());

        classBuilder.addMethod(deserializeMethod.build());
    }

    public void getDeserializeMethod() {
        MethodSpec.Builder deserializeMethod = MethodSpec.methodBuilder("deserialize")
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .returns(Object.class)
                .addParameter(Class.class, "clazz")
                .beginControlFlow("try")
                .addStatement("$T typeOfClass = Class.forName(clazz.getSimpleName())", Class.class)
                .addStatement("String nameOfClass = typeOfClass.getSimpleName()", ClassName.class)
                .addStatement("Object obj = clazz.getMethod(\"deserialize\" + nameOfClass).invoke(clazz)")
                .addStatement("return obj")
                .endControlFlow()
                .beginControlFlow("catch(Exception exception)")
                .addStatement("throw new $T()", IllegalLetterFormatException.class)
                .endControlFlow();

        classBuilder.addMethod(deserializeMethod.build());
    }

    public void getFieldsFromClass(MethodSpec.Builder deserializeMethod,
                                   Element annotatedClass
    ) {
        for (Element fieldOfAnnotatedClass : annotatedClass.getEnclosedElements()) {
            if (fieldOfAnnotatedClass.getKind() != ElementKind.FIELD) {
                continue;
            }
            String typeOfField = Factory.getTypeOfField(fieldOfAnnotatedClass);
            optionalTryCatchWrapper(deserializeMethod, typeOfField, annotatedClass, fieldOfAnnotatedClass);
        }
    }

    public void optionalTryCatchWrapper(MethodSpec.Builder deserializeMethod, String typeOfField, Element annotatedClass, Element fieldOfAnnotatedClass) {
        String classNameForDeserializeLowerCase = annotatedClass.getSimpleName().toString().toLowerCase();
        deserializeMethod.beginControlFlow("try");

        if (typeOfField.equals("")) {
            methodOfReferenceType(deserializeMethod, annotatedClass, fieldOfAnnotatedClass);
        } else{
            methodOfPrimitiveType(deserializeMethod, annotatedClass, fieldOfAnnotatedClass, typeOfField);
        }

        deserializeMethod.endControlFlow();
        deserializeMethod.beginControlFlow("catch(Exception exception)");
        deserializeMethod.beginControlFlow("if(optionalElements.contains($S))",
                annotatedClass.getSimpleName() + "." + fieldOfAnnotatedClass.getSimpleName());
        deserializeMethod.addStatement("$N.$N = $N", classNameForDeserializeLowerCase,
                fieldOfAnnotatedClass.getSimpleName(), Factory.getDefaultValue(typeUtils, fieldOfAnnotatedClass));
        deserializeMethod.endControlFlow();

        deserializeMethod.beginControlFlow("else");
        deserializeMethod.addStatement("throw new $T()", IllegalLetterFormatException.class);
        deserializeMethod.endControlFlow();
        deserializeMethod.endControlFlow();
    }

    public void methodOfPrimitiveType(MethodSpec.Builder deserializeMethod, Element annotatedElement, Element e, String typeForRead){

        String classNameForDeserializeLowerCase = annotatedElement.getSimpleName().toString().toLowerCase();

        deserializeMethod.addStatement("$N.$N = input.read$N()", classNameForDeserializeLowerCase,
                e.getSimpleName(), typeForRead);
    }

    public void methodOfReferenceType(MethodSpec.Builder deserializeMethod, Element annotatedElement, Element e) {
        String classNameForDeserializeLowerCase = annotatedElement.getSimpleName().toString().toLowerCase();

        deserializeMethod.addStatement("boolean isNull = input.readBoolean()");
        deserializeMethod.beginControlFlow("if(isNull == true)");
        deserializeMethod.addStatement("$N.$N = null",
                classNameForDeserializeLowerCase,
                e.getSimpleName());
        deserializeMethod.endControlFlow();
        deserializeMethod.beginControlFlow("else");
        deserializeMethod.addStatement("$N.$N = deserialize$N()",
                classNameForDeserializeLowerCase,
                e.getSimpleName(),
                typeUtils.asElement(e.asType()).getSimpleName());
        deserializeMethod.endControlFlow();
    }

    public void getOptionalFields() {
        for (Element e : roundEnv.getElementsAnnotatedWith(LetterizeOptional.class)) {
            String parentClass = e.getEnclosingElement().getSimpleName().toString();
            elementsWithOptionalAnnotation.add(parentClass + "." + e.getSimpleName());
        }
    }
}
