package ru.ifmo.java.serialization.processor;


import com.squareup.javapoet.JavaFile;
import ru.ifmo.java.serialization.Letterize;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedAnnotationTypes({"ru.ifmo.java.serialization.Letterize", "ru.ifmo.java.serialization.LetterizeOptional"})
public class AutoSerializationProcessor extends AbstractProcessor {
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // (de)serialize constructors init
        LetterSerializer serializerFactory = new LetterSerializer(typeUtils);
        serializerFactory.createSerializeConstructor();
        LetterDeserializer deserializerFactory = new LetterDeserializer(typeUtils, roundEnv);
        deserializerFactory.createDeserializeConstructor();

        serializerFactory.getSerializeMethod();

        for (Element annotatedClass : roundEnv.getElementsAnnotatedWith(Letterize.class)) {
            PackageElement packageOfAnnotatedClass = elementUtils.getPackageOf(annotatedClass);
            serializerFactory.createSerializeMethod(annotatedClass, packageOfAnnotatedClass);
            deserializerFactory.createDeserializeMethod(annotatedClass, packageOfAnnotatedClass);
        }

        JavaFile serializeFile = JavaFile.builder("ru.ifmo.java.serialization", serializerFactory.get()).build();
        JavaFile deserializeFile = JavaFile.builder("ru.ifmo.java.serialization", deserializerFactory.get()).build();

        try {
            serializeFile.writeTo(filer);
            deserializeFile.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}
