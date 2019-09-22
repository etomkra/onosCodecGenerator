package pl.waw.krakus.codecGenerator;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;

public class JFileGenerator {
    public static final String FROM_JSON = "fromJson";
    public static final String TO_JSON = "toJson";
    public static final String CONTEXT = "context";
    public static final String CODEC_SUFFIX = "Codec";
    public static final Map<String, String> TYPE_TO_METHOD_MAPPING = new HashMap<String, String>() {{
        put("String", "asText()");
        put("int", "asInt()");
        put("Integer", "asInt()");
        put("long", "asLong()");
        put("Long", "asLong()");
        put("boolean", "asBoolean()");
        put("Boolean", "asBoolean()");
    }};
    public static final String ENCODE_METHOD_NAME = "encode";
    public static final String DECODE_METHOD_NAME = "decode";
    public static final Class<JsonCodec> JSON_CODEC_CLASS = JsonCodec.class;
    public static final Class<ObjectNode> OBJECT_NODE_CLASS = ObjectNode.class;
    public static final Class<CodecContext> CODEC_CONTEXT_CLASS = CodecContext.class;
    public static final Class<Override> OVERRIDE_CLASS = Override.class;
    public static final String INDENTATION = "    ";
    private String packageName;

    public JFileGenerator(String packageName) {
        this.packageName = packageName;
    }

    public JFileGenerator() {
        this.packageName = this.getClass().getPackageName();
    }

    public <T> String generateCodec(Class<T> inputClass) {
        Map<String, String> inputClassFields = getFieldNamesAndTypes(inputClass);
        MethodSpec encodeMethodSpec = getEncodeMethodSpec(inputClass, inputClassFields);
        MethodSpec decodeMethodSpec = getDecodeMethodSpec(inputClass, inputClassFields);

        TypeSpec codecClass = generateOutputClassSpec(inputClass, encodeMethodSpec, decodeMethodSpec);
        return generateResultCode(inputClass, codecClass);
    }

    protected <T> Map<String, String> getFieldNamesAndTypes(Class<T> inputClass) {
        Map<String, String> result = new HashMap<>();
        Field[] fields = inputClass.getDeclaredFields();

        Arrays.stream(fields)
                .filter(f -> !f.isSynthetic())
                .forEach(
                        f -> result.put(f.getName(), f.getType().getSimpleName())
                );
        return result;
    }

    private <T> MethodSpec getEncodeMethodSpec(Class<T> inputClass, Map<String, String> classFields) {
        String encodeBody = getEncodeBody(classFields);
        return MethodSpec.methodBuilder(ENCODE_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .returns(OBJECT_NODE_CLASS)
                .addParameter(inputClass, TO_JSON)
                .addParameter(CODEC_CONTEXT_CLASS, CONTEXT)
                .addAnnotation(OVERRIDE_CLASS)
                .addCode(encodeBody)
                .addStatement("return resultJson", OBJECT_NODE_CLASS)
                .build();
    }

    private <T> MethodSpec getDecodeMethodSpec(Class<T> inputClass, Map<String, String> classFields) {
        String decodeBody = getDecodeBody(inputClass.getSimpleName(), classFields);
        return MethodSpec.methodBuilder(DECODE_METHOD_NAME)
                .addModifiers(Modifier.PUBLIC)
                .returns(inputClass)
                .addParameter(ObjectNode.class, FROM_JSON)
                .addParameter(CodecContext.class, CONTEXT)
                .addAnnotation(Override.class)
                .addCode(decodeBody)
                .build();
    }

    private <T> TypeSpec generateOutputClassSpec(Class<T> inputClass, MethodSpec encodeMethodSpec, MethodSpec decodeMethodSpec) {
        return TypeSpec.classBuilder(inputClass.getSimpleName() + CODEC_SUFFIX)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ParameterizedTypeName.get(ClassName.get(JSON_CODEC_CLASS),ClassName.get(inputClass)))
                .addMethod(encodeMethodSpec)
                .addMethod(decodeMethodSpec)
                .build();
    }

    private <T> String generateResultCode(Class<T> inputClass, TypeSpec codecClass) {
        String result;
        JavaFile javaFile = JavaFile.builder(packageName, codecClass).build();
        result = javaFile.toString();
        return result;
    }

    private String getEncodeBody(Map<String, String> inputClassFields) {
        StringBuffer code = new StringBuffer("ObjectNode resultJson = context.mapper().createObjectNode()\n");
        inputClassFields.entrySet().forEach(
                e -> code
                        .append(INDENTATION + ".put(\"")
                        .append(e.getKey())
                        .append("\", ")
                        .append(TO_JSON)
                        .append(fieldToGetter(e.getKey()))
                        .append(")\n")
        );
        code.append(";\n");
        return code.toString();
    }

    private String getDecodeBody(String className, Map<String, String> classFields) {
        StringBuffer code = new StringBuffer("if (fromJson == null || !fromJson.isObject()) {\n" +
                "            return null;\n" +
                "        }\n" +
                "return " + className + ".builder()\n");
        classFields.entrySet().forEach(
                e -> code
                        .append(INDENTATION)
                        .append(".")
                        .append(e.getKey())
                        .append("(")
                        .append(FROM_JSON)
                        .append(".get(\"")
                        .append(e.getKey())
                        .append("\").")
                        .append(mapFieldTypeToJsonMethod(e.getValue()))
                        .append(")\n")
        );
        code.append(INDENTATION + ".build();\n");
        return code.toString();
    }

    protected String mapFieldTypeToJsonMethod(String inType) {
        return Optional.ofNullable(TYPE_TO_METHOD_MAPPING.get(inType)).orElse("");
    }

    private String fieldToGetter(String name) {
        return ".get" + name.substring(0, 1).toUpperCase() + name.substring(1) + "()";
    }
}
