package dev.Astra.mod.modules.impl.client;

import dev.Astra.core.impl.FontManager;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import dev.Astra.mod.modules.settings.impl.StringSetting;

public class Fonts extends Module {
    public static Fonts INSTANCE;
    public final StringSetting font = this.add(new StringSetting("Font", "default").injectTask(this::refresh));
    public final StringSetting alternate = this.add(new StringSetting("Alternate", "msyh").injectTask(this::refresh));
    public final EnumSetting<Style> style = this.add(new EnumSetting<Style>("Style", Style.PLAIN).injectTask(this::refresh));
    public final SliderSetting size = this.add(new SliderSetting("Size", 8.0, 1.0, 15.0, 1.0).injectTask(this::refresh));
    public final SliderSetting shift = this.add(new SliderSetting("Shift", 0.0, -10.0, 10.0, 1.0).injectTask(this::refresh));
    public final SliderSetting translate = this.add(new SliderSetting("Translate", 0.0, -10.0, 10.0, 1.0).injectTask(this::refresh));
    public final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));

    public Fonts() {
        super("Fonts", Module.Category.Client);
        this.setChinese("字体");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        this.refresh();
    }

    @Override
    public void onDisable() {
        FontManager.init();
    }

    public void refresh() {
        if (!this.isOn()) {
            return;
        }

        try {
            if (FontManager.ui != null) { try { FontManager.ui.close(); } catch (Exception ignored) {} }
            if (FontManager.small != null) { try { FontManager.small.close(); } catch (Exception ignored) {} }
            if (this.font.getValue().equals("default")) {
                if (this.alternate.getValue().equals("null")) {
                    FontManager.ui = FontManager.assets(this.size.getValueInt(), this.font.getValue(), this.style.getValue().get());
                    FontManager.small = FontManager.assets(6.0f, this.font.getValue(), this.style.getValue().get());
                } else {
                    FontManager.ui = FontManager.assets(this.size.getValueInt(), this.font.getValue(), this.style.getValue().get(), this.alternate.getValue());
                    FontManager.small = FontManager.assets(6.0f, this.font.getValue(), this.style.getValue().get(), this.alternate.getValue());
                }
            } else if (this.alternate.getValue().equals("null")) {
                FontManager.ui = FontManager.create(this.size.getValueInt(), this.font.getValue(), this.style.getValue().get());
                FontManager.small = FontManager.create(6, this.font.getValue(), this.style.getValue().get());
            } else {
                FontManager.ui = FontManager.create(this.size.getValueInt(), this.font.getValue(), this.style.getValue().get(), this.alternate.getValue());
                FontManager.small = FontManager.create(6, this.font.getValue(), this.style.getValue().get(), this.alternate.getValue());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static enum Style {
        PLAIN{

            @Override
            public int get() {
                return 0;
            }
        }
        ,
        BOLD{

            @Override
            public int get() {
                return 1;
            }
        }
        ,
        ITALIC{

            @Override
            public int get() {
                return 2;
            }
        };


        public abstract int get();
    }
}