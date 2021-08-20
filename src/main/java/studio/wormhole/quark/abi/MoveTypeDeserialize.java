package studio.wormhole.quark.abi;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;

import java.lang.reflect.Type;

public class MoveTypeDeserialize implements ObjectDeserializer {

  @Override
  public ArgType deserialze(DefaultJSONParser defaultJSONParser, Type type, Object o) {
    JSONLexer jsonLexer = defaultJSONParser.getLexer();
    return parse(jsonLexer);
  }


  private ArgType parse(JSONLexer jsonLexer) {
    int token = jsonLexer.token();
    if (token == JSONToken.LITERAL_STRING) {
      return parseBaseType(jsonLexer);
    } else if (token == JSONToken.LBRACE) {

      return parseArgType(jsonLexer);
    }
    return null;
  }

  private ArgType parseArgType(JSONLexer jsonLexer) {
    jsonLexer.nextToken(JSONToken.COMMA);
    String value = jsonLexer.stringVal();
    jsonLexer.nextToken(JSONToken.COLON);
    jsonLexer.nextToken();
    int token = jsonLexer.token();
    if (token != JSONToken.LBRACE) {
      String vecType = jsonLexer.stringVal();
      return ArgType.builder()
          .moveType(MoveType.fromString(value))
          .argType(ArgType.builder().moveType(MoveType.fromString(vecType)).build())
          .build();
    }
    return ArgType.builder()
        .moveType(MoveType.fromString(value))
        .argType( parseArgType(jsonLexer) )
        .build();
  }

  private ArgType parseBaseType(JSONLexer jsonLexer) {
    String value = jsonLexer.stringVal();
    return ArgType.builder().moveType(MoveType.fromString(value)).build();
  }

  @Override
  public int getFastMatchToken() {
    return 0;
  }
}