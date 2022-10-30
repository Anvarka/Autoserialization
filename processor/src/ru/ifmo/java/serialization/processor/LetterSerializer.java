package ru.ifmo.java.serialization.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.*;
import javax.lang.model.util.Types;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class LetterSerializer {
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder("Serializer")
            .addModifiers(Modifier.PUBLIC)
            .addField(DataOutputStream.class, Constants.OUTPUT, Modifier.PUBLIC, Modifier.FINAL);
    private Types typeUtils;

    public LetterSerializer(Types types) {
        this.typeUtils = types;
    }

    public void createSerializeConstructor() {
        Factory.createConstructor(DataOutputStream.class, Constants.OUTPUT, classBuilder);
    }

    public void createSerializeMethod(Element annotatedElement, PackageElement pack) {
        if (!Factory.isInheritedFromLetter(annotatedElement, typeUtils)) {
            throw new RuntimeException("class " + annotatedElement.getSimpleName() + " does not inherit from Letter");
        }

        Name classNameForSerialize = annotatedElement.getSimpleName();

        MethodSpec.Builder wrapperForMethod = MethodSpec.methodBuilder("serialize" + classNameForSerialize)
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .addParameter(ClassName.get(pack.toString(), annotatedElement.getSimpleName().toString()),
                        annotatedElement.getSimpleName().toString().toLowerCase())
                .addStatement("$T<$T> recSet = new $T<>()", Set.class, Name.class, ClassName.get("java.util",
                        HashSet.class.getSimpleName()))
                .addStatement("serialize$N($N, recSet)", annotatedElement.getSimpleName(), annotatedElement.getSimpleName().toString().toLowerCase());

        classBuilder.addMethod(wrapperForMethod.build());
        // overload for serialize method
        MethodSpec.Builder method = MethodSpec.methodBuilder("serialize" + classNameForSerialize)
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .addParameter(ClassName.get(pack.toString(),
                                classNameForSerialize.toString()),
                        classNameForSerialize.toString().toLowerCase())
                .addParameter(Set.class, "recSet");

//        MethodSpec res = getFieldsFromSerializeClass(annotatedElement, method);
        String classNameForSerializationLowerCase = annotatedElement.getSimpleName().toString().toLowerCase();
        recursiveMethod(method, annotatedElement, classNameForSerializationLowerCase);
        classBuilder.addMethod(method.build());
    }

    public void getSerializeMethod() {
        MethodSpec.Builder serializeMethod = MethodSpec.methodBuilder("serialize")
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .addParameter(Class.class, "clazz")
                .addParameter(Object.class, "object")
                .beginControlFlow("try")
                .addStatement("$T typeOfClass = Class.forName(clazz.getSimpleName())", Class.class)
                .addStatement("String nameOfClass = typeOfClass.getSimpleName()", ClassName.class)
                .addStatement("clazz.getMethod(\"serialize\" + nameOfClass, typeOfClass).invoke(clazz, object)")
        .endControlFlow()
        .beginControlFlow("catch(Exception exception)")
                .endControlFlow();

        classBuilder.addMethod(serializeMethod.build());
    }

    public TypeSpec get() {
        return classBuilder.build();
    }

//    public MethodSpec getFieldsFromSerializeClass(Element annotatedClass,
//                                                  MethodSpec.Builder serializeMethod) {
//
//        for (Element element : annotatedClass.getEnclosedElements()) {
//            if (element.getKind() != ElementKind.FIELD) {
//                continue;
//            }
//            String typeOfField = Factory.getTypeOfField(element);
//            if (typeOfField.equals("")) {
//                methodForReferenceObject(serializeMethod, annotatedClass, element);
//                continue;
//            }
//            methodForPrimitiveObject(serializeMethod, annotatedClass, element, typeOfField, classNameForSerializationLowerCase);
//        }
//        return serializeMethod.build();
//    }

    public void recursiveMethod(MethodSpec.Builder serializeMethod, Element currentAnnotatedClass, String classNameForSerializationLowerCase) {
        while (!Object.class.getSimpleName().contentEquals(typeUtils.asElement(currentAnnotatedClass.asType()).getSimpleName())) {
            for (Element element : currentAnnotatedClass.getEnclosedElements()) {
                if (element.getKind() != ElementKind.FIELD) {
                    continue;
                }
                String typeOfField = Factory.getTypeOfField(element);
                if (typeOfField.equals("")) {
                    methodForReferenceObject(serializeMethod, currentAnnotatedClass, element);
                    continue;
                }
                methodForPrimitiveObject(serializeMethod, currentAnnotatedClass, element, typeOfField, classNameForSerializationLowerCase);
            }
            TypeElement elem = (TypeElement) currentAnnotatedClass;
            currentAnnotatedClass = typeUtils.asElement(elem.getSuperclass());
        }
    }

    private void methodForPrimitiveObject(MethodSpec.Builder serializeMethod, Element annotatedClass, Element element, String typeOfField, String classNameForSerializationLowerCase) {
        serializeMethod.addStatement("output.write$N($N.$N)",
                typeOfField,
                classNameForSerializationLowerCase,
                element.getSimpleName().toString());
    }

    private void methodForReferenceObject(MethodSpec.Builder serializeMethod, Element annotatedClass, Element element) {
        serializeMethod.beginControlFlow("if($N.$N == null)",
                annotatedClass.getSimpleName().toString().toLowerCase(),
                element.getSimpleName());
        serializeMethod.addStatement("output.writeBoolean(true)");
        serializeMethod.endControlFlow();

        serializeMethod.beginControlFlow("if (recSet.contains($S))",
                typeUtils.asElement(element.asType()).getSimpleName()
        );
        serializeMethod.addStatement("throw new $T($S)",
                RuntimeException.class,
                "Error: cycle element in field  " + element.getSimpleName());
        serializeMethod.endControlFlow();


        serializeMethod.beginControlFlow("else");
        serializeMethod.addStatement("output.writeBoolean(false)");
        serializeMethod.addStatement("recSet.add($S)",
                typeUtils.asElement(element.asType()).getSimpleName());

        serializeMethod.addStatement("serialize$N($N.$N, recSet)",
                typeUtils.asElement(element.asType()).getSimpleName(),
                annotatedClass.getSimpleName().toString().toLowerCase(),
                element.getSimpleName());
        serializeMethod.endControlFlow();
    }
}
