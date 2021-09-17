package studio.wormhole.quark.command.mobius;

import studio.wormhole.quark.command.mobius.model.Config;
import studio.wormhole.quark.service.ChainService;

public class RiskService {
    private ChainService chainService;
    private Config config;


    public RiskService(Config config, ChainService chainService) {
        this.config = config;
        this.chainService = chainService;
    }


}
