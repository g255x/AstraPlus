package dev.Astra.mod.modules.impl.player;

import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.SliderSetting;

public class IQBoost extends Module {
    public final SliderSetting iq = this.add(new SliderSetting("IQ", 100, 0, 1000));

    public IQBoost() {
        super("IQBoost", Module.Category.Player);
        this.setChinese("IQ提升");
    }

    @Override
    public String getInfo() {
        return "IQ: " + (int) this.iq.getValue();
    }
}