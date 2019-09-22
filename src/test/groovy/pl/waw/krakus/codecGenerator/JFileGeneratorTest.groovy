package pl.waw.krakus.codecGenerator


import spock.lang.Specification
import spock.lang.Unroll


class JFileGeneratorTest extends Specification {

    def "should return proper encode result for input class"() {
        given:
        JFileGenerator gen = new JFileGenerator()

        when:
        def res = gen.generateCodec(Example.class)
        print res

        then:
        res == '''package pl.waw.krakus.codecGenerator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.Override;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;

public class ExampleCodec extends JsonCodec<Example> {
  @Override
  public ObjectNode encode(Example toJson, CodecContext context) {
    ObjectNode resultJson = context.mapper().createObjectNode()
        .put("name", toJson.getName())
        .put("id", toJson.getId())
        .put("isActive", toJson.getIsActive())
    ;
    return resultJson;
  }

  @Override
  public Example decode(ObjectNode fromJson, CodecContext context) {
    if (fromJson == null || !fromJson.isObject()) {
                return null;
            }
    return Example.builder()
        .name(fromJson.get("name").asText())
        .id(fromJson.get("id").asLong())
        .isActive(fromJson.get("isActive").asBoolean())
        .build();
  }
}
'''
    }

    @Unroll
    def "should map #type to ObjectNode.get method name: #methodName"() {
        given:
        JFileGenerator gen = new JFileGenerator()

        when:
        def res = gen.mapFieldTypeToJsonMethod(type)

        then:
        res == methodName

        where:
        type      || methodName
        "String"  || "asText()"
        "int"     || "asInt()"
        "Integer" || "asInt()"
        "boolean" || "asBoolean()"
        "Boolean" || "asBoolean()"
        "long"    || "asLong()"
        "Long"    || "asLong()"
        "unknown" || ""
    }
}
