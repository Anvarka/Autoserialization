package ru.ifmo.java.serialization.processor;

import com.squareup.javapoet.*;
import ru.ifmo.java.serialization.IllegalLetterFormatException;
import ru.ifmo.java.serialization.LetterizeOptional;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.util.Types;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeserializerFactory extends BaseFactory {
    public static Set<String> elementsWithOptionalAnnotation = new HashSet<>();

    public DeserializerFactory(Types types, RoundEnvironment roundEnv) {
        this.typeUtils = types;
        this.roundEnv = roundEnv;
        classBuilder = TypeSpec.classBuilder("Deserializer")
                .addModifiers(Modifier.PUBLIC)
                .addField(DataInputStream.class, Constants.INPUT, Modifier.PUBLIC, Modifier.FINAL);

        FieldSpec optionalListField = Utils.createSet("optionalElements", String.class);
        FieldSpec listOfObjects = Utils.createList("listOfObjects", Object.class);
        FieldSpec seenNumbers = Utils.createList("seenNumbers", Integer.class);
        FieldSpec booleanOpt = FieldSpec.builder(Boolean.class, "boolOptional")
                .addModifiers(Modifier.PUBLIC)
                .initializer("true")
                .build();

        FieldSpec nameObject = FieldSpec.builder(String.class, "nameObject")
                .addModifiers(Modifier.PUBLIC)
                .build();

        classBuilder.addField(optionalListField);
        classBuilder.addField(listOfObjects);
        classBuilder.addField(seenNumbers);
        classBuilder.addField(booleanOpt);
        classBuilder.addField(nameObject);
    }

    @Override
    public void createConstructor() {
        MethodSpec.Builder constructor = Utils.createConstructor(DataInputStream.class, Constants.INPUT);
        getOptionalFields();

        for (String s : elementsWithOptionalAnnotation) {
            constructor.addStatement("optionalElements.add($S)", s);
        }
        classBuilder.addMethod(constructor.build());
    }

    public void getOptionalFields() {
        for (Element e : roundEnv.getElementsAnnotatedWith(LetterizeOptional.class)) {
            String parentClass = e.getEnclosingElement().getSimpleName().toString();
            elementsWithOptionalAnnotation.add(parentClass + "." + e.getSimpleName());
        }
    }

    public void methodForPrimitiveType(MethodSpec.Builder deserializeMethod,
                                       Element annotatedElement,
                                       Element e,
                                       String typeForRead,
                                       String classNameForSerializationLowerCase) {
        deserializeMethod.addStatement("$N.$N = input.read$N()", classNameForSerializationLowerCase,
                e.getSimpleName(), typeForRead);
    }

    public void actionsWithMethod(MethodSpec.Builder deserializeMethod, Element annotatedClass, Name classNameForDeserialize) {
        deserializeMethod.addStatement("$N $N = new $N()", classNameForDeserialize.toString(), annotatedClass.getSimpleName().toString().toLowerCase(),
                        classNameForDeserialize.toString())
                .addStatement("seenNumbers.add(0)")
                .addStatement("listOfObjects.add($N)", annotatedClass.getSimpleName().toString().toLowerCase());

        String classNameForDeserializationLowerCase = classNameForDeserialize.toString().toLowerCase();
        visitFieldsOfClassToParent(deserializeMethod, annotatedClass, classNameForDeserializationLowerCase);

        deserializeMethod.addStatement("return $N", annotatedClass.getSimpleName().toString().toLowerCase());
    }

    @Override
    public void createCodeForReferenceType(MethodSpec.Builder deserializeMethod, Element annotatedClass, Element fieldOfClass) {
        String classNameForDeserializeLowerCase = annotatedClass.getSimpleName().toString().toLowerCase();
        Name fieldOfClassName = fieldOfClass.getSimpleName();

        deserializeMethod.addStatement("boolean isNull = input.readBoolean()");

        deserializeMethod.beginControlFlow("if(isNull == true)");
        deserializeMethod.addStatement("$N.$N = null",
                classNameForDeserializeLowerCase,
                fieldOfClassName);
        deserializeMethod.endControlFlow();
        deserializeMethod.beginControlFlow("else");
        wrapperForRecursion(deserializeMethod, classNameForDeserializeLowerCase, fieldOfClass);
        deserializeMethod.endControlFlow();
    }

    public void workWithFieldsOfClass(MethodSpec.Builder deserializeMethod,
                                      String typeOfField,
                                      Element annotatedClass,
                                      Element fieldOfAnnotatedClass,
                                      String classNameForDeserializationLowerCase) {
        // optional try catch wrapper
        deserializeMethod.beginControlFlow("try");

        if (typeOfField.equals("")) {
            methodForReferenceType(deserializeMethod, annotatedClass, fieldOfAnnotatedClass);
        } else {
            if (typeOfField.equals("UTF")) {
                deserializeMethod.addStatement("boolean isNull = input.readBoolean()");
                deserializeMethod.beginControlFlow("if(isNull == true)");
                deserializeMethod.addStatement("$N.$N = null", classNameForDeserializationLowerCase, fieldOfAnnotatedClass.getSimpleName().toString());
                deserializeMethod.endControlFlow();

                deserializeMethod.beginControlFlow("else");
                methodForPrimitiveType(deserializeMethod, annotatedClass, fieldOfAnnotatedClass, typeOfField, classNameForDeserializationLowerCase);
                deserializeMethod.endControlFlow();
            } else {
                methodForPrimitiveType(deserializeMethod, annotatedClass, fieldOfAnnotatedClass, typeOfField, classNameForDeserializationLowerCase);
            }
        }

        deserializeMethod.endControlFlow();
        deserializeMethod.beginControlFlow("catch(Exception exception)");
        deserializeMethod.beginControlFlow("if(optionalElements.contains($S))",
                annotatedClass.getSimpleName() + "." + fieldOfAnnotatedClass.getSimpleName());
        deserializeMethod.addStatement("$N.$N = $N", classNameForDeserializationLowerCase,
                fieldOfAnnotatedClass.getSimpleName(), Utils.getDefaultValue(typeUtils, fieldOfAnnotatedClass));
        deserializeMethod.endControlFlow();

        deserializeMethod.beginControlFlow("else");
        deserializeMethod.addStatement("throw new $T()", IllegalLetterFormatException.class);
        deserializeMethod.endControlFlow();
        deserializeMethod.endControlFlow();
    }

    public void workWithFieldsOfClass2(MethodSpec.Builder deserializeMethod,
                                       String typeOfField,
                                       Element annotatedClass,
                                       Element fieldOfAnnotatedClass,
                                       String classNameForDeserializationLowerCase) {
        deserializeMethod.beginControlFlow("if(boolOptional)");
        deserializeMethod.addStatement("nameObject = input.readUTF()");
        deserializeMethod.endControlFlow();


        deserializeMethod.beginControlFlow("if(!nameObject.equals(\"$N.$N\"))",
                classNameForDeserializationLowerCase,
                fieldOfAnnotatedClass.getSimpleName().toString());
        deserializeMethod.addStatement("boolOptional = false");
        deserializeMethod.beginControlFlow("if(!optionalElements.contains($S))",
                annotatedClass.getSimpleName() + "." + fieldOfAnnotatedClass.getSimpleName());
        deserializeMethod.addStatement("throw new $T()", IllegalLetterFormatException.class);
        deserializeMethod.endControlFlow();
        deserializeMethod.addStatement("$N.$N = $N", classNameForDeserializationLowerCase,
                fieldOfAnnotatedClass.getSimpleName(), Utils.getDefaultValue(typeUtils, fieldOfAnnotatedClass));
        deserializeMethod.endControlFlow();
        deserializeMethod.beginControlFlow("else");
        deserializeMethod.addStatement("boolOptional = true");

        // optional try catch wrapper
        deserializeMethod.beginControlFlow("try");

        if (typeOfField.equals("")) {
            methodForReferenceType(deserializeMethod, annotatedClass, fieldOfAnnotatedClass);
        } else {
            if (typeOfField.equals("UTF")) {
                deserializeMethod.addStatement("boolean isNull = input.readBoolean()");
                deserializeMethod.beginControlFlow("if(isNull == true)");
                deserializeMethod.addStatement("$N.$N = null", classNameForDeserializationLowerCase, fieldOfAnnotatedClass.getSimpleName().toString());
                deserializeMethod.endControlFlow();

                deserializeMethod.beginControlFlow("else");
                methodForPrimitiveType(deserializeMethod, annotatedClass, fieldOfAnnotatedClass, typeOfField, classNameForDeserializationLowerCase);
                deserializeMethod.endControlFlow();
            } else {
                methodForPrimitiveType(deserializeMethod, annotatedClass, fieldOfAnnotatedClass, typeOfField, classNameForDeserializationLowerCase);
            }
        }

        deserializeMethod.endControlFlow();
        deserializeMethod.beginControlFlow("catch(Exception exception)");
        deserializeMethod.beginControlFlow("if(optionalElements.contains($S))",
                annotatedClass.getSimpleName() + "." + fieldOfAnnotatedClass.getSimpleName());
        deserializeMethod.addStatement("$N.$N = $N", classNameForDeserializationLowerCase,
                fieldOfAnnotatedClass.getSimpleName(), Utils.getDefaultValue(typeUtils, fieldOfAnnotatedClass));
        deserializeMethod.endControlFlow();

        deserializeMethod.beginControlFlow("else");
        deserializeMethod.addStatement("throw new $T()", IllegalLetterFormatException.class);
        deserializeMethod.endControlFlow();
        deserializeMethod.endControlFlow();
        deserializeMethod.endControlFlow();
    }

    public MethodSpec.Builder getBaseMethod(Name classNameForDeserialize, PackageElement pack) {
        return MethodSpec.methodBuilder("deserialize" + classNameForDeserialize)
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .returns(ClassName.get(pack.toString(), classNameForDeserialize.toString()))
                .addStatement("$T<$T> listOfObject = new $T<>()", List.class, Object.class, ClassName.get("java.util",
                        ArrayList.class.getSimpleName()))
                .addStatement("$T<$T> seenNumbers = new $T<>()", List.class, Integer.class, ClassName.get("java.util",
                        ArrayList.class.getSimpleName()))
                .addStatement("return deserialize$N(listOfObject, seenNumbers)", classNameForDeserialize);
    }

    public MethodSpec.Builder getOverloadMethod(Name classNameForDeserialize, PackageElement pack) {
        return MethodSpec.methodBuilder("deserialize" + classNameForDeserialize)
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .returns(ClassName.get(pack.toString(), classNameForDeserialize.toString()))
                .addParameter(List.class, "listOfObject")
                .addParameter(List.class, "seenNumbers");
    }

    void wrapperForRecursion(MethodSpec.Builder deserializeMethod, String classNameForDeserializeLowerCase, Element e) {
        deserializeMethod.addStatement("int number = input.readInt()");
        deserializeMethod.beginControlFlow("if(!seenNumbers.contains(number))");
        deserializeMethod.addStatement("seenNumbers.add(number)");
        deserializeMethod.addStatement("$N.$N = deserialize$N(listOfObjects, seenNumbers)",
                classNameForDeserializeLowerCase,
                e.getSimpleName(),
                typeUtils.asElement(e.asType()).getSimpleName());

        deserializeMethod.addStatement("listOfObjects.add($N.$N)",
                classNameForDeserializeLowerCase,
                e.getSimpleName());
        deserializeMethod.endControlFlow();
        deserializeMethod.beginControlFlow("else");
        deserializeMethod.addStatement("$N.$N = ($T) listOfObjects.get(number)",
                classNameForDeserializeLowerCase,
                e.getSimpleName(),
                typeUtils.asElement(e.asType()));
        deserializeMethod.endControlFlow();
    }

    @Override
    public void getUniversalMethod() {
        MethodSpec.Builder deserializeMethod = MethodSpec.methodBuilder("deserialize")
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .returns(Object.class)
                .addParameter(Class.class, "clazz")
                .beginControlFlow("try")
                .addStatement("$T typeOfClass = Class.forName(clazz.getCanonicalName())", Class.class)
                .addStatement("String nameOfClass = typeOfClass.getSimpleName()", ClassName.class)
                .addStatement("Object obj = this.getClass().getMethod(\"deserialize\" + nameOfClass).invoke(this)")
                .addStatement("return obj")
                .endControlFlow()
                .beginControlFlow("catch(Exception exception)")
                .addStatement("throw new $T(exception.getMessage())", IllegalArgumentException.class)
                .endControlFlow();

        classBuilder.addMethod(deserializeMethod.build());
    }
}
