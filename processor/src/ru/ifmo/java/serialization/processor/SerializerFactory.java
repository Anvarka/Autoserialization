package ru.ifmo.java.serialization.processor;

import com.squareup.javapoet.*;

import javax.lang.model.element.*;
import javax.lang.model.util.Types;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SerializerFactory extends BaseFactory {

    public SerializerFactory(Types types) {
        this.typeUtils = types;
        classBuilder = TypeSpec.classBuilder("Serializer")
                .addModifiers(Modifier.PUBLIC)
                .addField(DataOutputStream.class, Constants.OUTPUT, Modifier.PUBLIC, Modifier.FINAL);
    }

    @Override
    public void createConstructor() {
        MethodSpec.Builder constructor = Utils.createConstructor(DataOutputStream.class, Constants.OUTPUT);
        classBuilder.addMethod(constructor.build());
    }

    public void methodForPrimitiveType(MethodSpec.Builder serializeMethod,
                                       String classNameForSerializationLowerCase,
                                       Element element,
                                       String typeOfField,
                                       String statement) {
        serializeMethod.addStatement(statement,
                typeOfField,
                classNameForSerializationLowerCase,
                element.getSimpleName().toString());
    }

    public void actionsWithMethod(MethodSpec.Builder method, Element annotatedClass, Name classNameForSerialize) {
        String classNameForSerializationLowerCase = classNameForSerialize.toString().toLowerCase();
        visitFieldsOfClassToParent(method, annotatedClass, classNameForSerializationLowerCase);
    }

    @Override
    public void createCodeForReferenceType(MethodSpec.Builder serializeMethod, Element annotatedClass, Element element) {
        String annotatedClassName = annotatedClass.getSimpleName().toString().toLowerCase();
        Name fieldOfClassName = element.getSimpleName();

        serializeMethod.beginControlFlow("if($N.$N == null)", annotatedClassName, fieldOfClassName);
        serializeMethod.addStatement("output.writeBoolean(true)");
        serializeMethod.endControlFlow();
        serializeMethod.beginControlFlow("else");
        serializeMethod.addStatement("output.writeBoolean(false)");
        wrapperForRecursion(serializeMethod, annotatedClassName, fieldOfClassName, element);
        serializeMethod.endControlFlow();
    }

    public void workWithFieldsOfClass(MethodSpec.Builder serializeMethod,
                                      String typeOfField,
                                      Element annotatedClass,
                                      Element fieldOfAnnotatedClass,
                                      String classNameForSerializationLowerCase) {
        if (typeOfField.equals("")) {
            methodForReferenceType(serializeMethod, annotatedClass, fieldOfAnnotatedClass);
        } else {
            if (typeOfField.equals("UTF")) {
                serializeMethod.beginControlFlow("if($N.$N == null)",
                        classNameForSerializationLowerCase,
                        fieldOfAnnotatedClass.getSimpleName().toString());
                serializeMethod.addStatement("output.writeBoolean(true)");
                serializeMethod.endControlFlow();

                serializeMethod.beginControlFlow("else");
                serializeMethod.addStatement("output.writeBoolean(false)");
                methodForPrimitiveType(serializeMethod, classNameForSerializationLowerCase, fieldOfAnnotatedClass, typeOfField, "output.write$N($N.$N)");
                serializeMethod.endControlFlow();
            } else {
                methodForPrimitiveType(serializeMethod, classNameForSerializationLowerCase, fieldOfAnnotatedClass, typeOfField, "output.write$N($N.$N)");
            }
        }
    }

    public void workWithFieldsOfClass2(MethodSpec.Builder serializeMethod,
                                       String typeOfField,
                                       Element annotatedClass,
                                       Element fieldOfAnnotatedClass,
                                       String classNameForSerializationLowerCase) {
        serializeMethod.addStatement("output.writeUTF(\"$N.$N\")",
                classNameForSerializationLowerCase,
                fieldOfAnnotatedClass.getSimpleName().toString());
        if (typeOfField.equals("")) {
            methodForReferenceType(serializeMethod, annotatedClass, fieldOfAnnotatedClass);
        } else {
            if (typeOfField.equals("UTF")) {
                serializeMethod.beginControlFlow("if($N.$N == null)",
                        classNameForSerializationLowerCase,
                        fieldOfAnnotatedClass.getSimpleName().toString());
                serializeMethod.addStatement("output.writeBoolean(true)");
                serializeMethod.endControlFlow();

                serializeMethod.beginControlFlow("else");
                serializeMethod.addStatement("output.writeBoolean(false)");
                methodForPrimitiveType(serializeMethod, classNameForSerializationLowerCase, fieldOfAnnotatedClass, typeOfField, "output.write$N($N.$N)");
                serializeMethod.endControlFlow();
            } else {
                methodForPrimitiveType(serializeMethod, classNameForSerializationLowerCase, fieldOfAnnotatedClass, typeOfField, "output.write$N($N.$N)");
            }
        }
    }

    public MethodSpec.Builder getBaseMethod(Name classNameForSerialize, PackageElement pack) {
        return MethodSpec.methodBuilder("serialize" + classNameForSerialize)
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .addParameter(ClassName.get(pack.toString(), classNameForSerialize.toString()),
                        classNameForSerialize.toString().toLowerCase())
                .addStatement("$T<$T> listOfObject = new $T<>()", List.class, Object.class, ClassName.get("java.util",
                        ArrayList.class.getSimpleName()))
                .addStatement("listOfObject.add($N)", classNameForSerialize.toString().toLowerCase())
                .addStatement("$T<$T> numberOfObject = new $T<>()", List.class, Name.class, ClassName.get("java.util",
                        ArrayList.class.getSimpleName()))
                .addStatement("serialize$N($N, listOfObject, numberOfObject)", classNameForSerialize, classNameForSerialize.toString().toLowerCase());
    }

    public MethodSpec.Builder getOverloadMethod(Name classNameForSerialize, PackageElement pack) {
        return MethodSpec.methodBuilder("serialize" + classNameForSerialize)
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .addParameter(ClassName.get(pack.toString(),
                                classNameForSerialize.toString()),
                        classNameForSerialize.toString().toLowerCase())
                .addParameter(List.class, "listOfObject")
                .addParameter(List.class, "numberOfObject");
    }

    private void wrapperForRecursion(MethodSpec.Builder serializeMethod, String annotatedClassName, Name fieldOfClassName, Element element) {
        serializeMethod.beginControlFlow("if (!listOfObject.contains($N.$N))",
                annotatedClassName,
                fieldOfClassName
        );
        serializeMethod.addStatement("listOfObject.add($N.$N)",
                annotatedClassName,
                fieldOfClassName
        );
        serializeMethod.addStatement("output.writeInt(listOfObject.size() - 1)");
        serializeMethod.addStatement("serialize$N($N.$N, listOfObject, numberOfObject)",
                typeUtils.asElement(element.asType()).getSimpleName(),
                annotatedClassName,
                fieldOfClassName);
        serializeMethod.endControlFlow();

        serializeMethod.beginControlFlow("else");
        serializeMethod.addStatement("int number = listOfObject.indexOf($N.$N)",
                annotatedClassName,
                fieldOfClassName
        );
        serializeMethod.addStatement("output.writeInt(number)",
                annotatedClassName,
                fieldOfClassName
        );
        serializeMethod.endControlFlow();
    }

    @Override
    public void getUniversalMethod() {
        MethodSpec.Builder serializeMethod = MethodSpec.methodBuilder("serialize")
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .addParameter(Class.class, "clazz")
                .addParameter(Object.class, "object")
                .beginControlFlow("try")
                .addStatement("$T typeOfClass = Class.forName(clazz.getCanonicalName())", Class.class)
                .addStatement("String nameOfClass = typeOfClass.getSimpleName()", ClassName.class)
                .addStatement("$T method = this.getClass().getMethod(\"serialize\" + nameOfClass, typeOfClass)", Method.class)
                .addStatement("method.invoke(this, object)")
                .endControlFlow()
                .beginControlFlow("catch(Exception exception)")
                .addStatement("throw  new IllegalArgumentException(exception.getMessage())")
                .endControlFlow();

        classBuilder.addMethod(serializeMethod.build());
    }
}
