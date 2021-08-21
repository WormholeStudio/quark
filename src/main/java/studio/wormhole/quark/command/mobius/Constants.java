package studio.wormhole.quark.command.mobius;

import org.apache.commons.lang3.StringUtils;

public class Constants {

    public static final String STORE = "/tmp/mobius/account.json";



    public static String checkStore(String store) {
        if (StringUtils.isEmpty(store)) {
            return Constants.STORE;
        }
        return store;
    }
}