package studio.wormhole.quark.command.mobius.model;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import org.starcoin.utils.Hex;

import java.lang.reflect.Type;

public class HexDeserializer implements ObjectDeserializer {
    @Override
    public <T> T deserialze(DefaultJSONParser defaultJSONParser, Type type, Object o) {

        String value = defaultJSONParser.getLexer().stringVal();

        return (T) new String(Hex.decode(value));
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }
}
