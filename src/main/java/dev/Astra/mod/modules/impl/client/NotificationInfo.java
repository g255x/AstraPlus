package dev.Astra.mod.modules.impl.client;

public class NotificationInfo {
    private final int hashCode;
    private String title;
    private String subTitle;
    private NotificationMode mode;
    private long createTime;
    private int displayDuration;
    private final boolean isModule;
    private boolean skipIntroAnimation;
    private float currentY;
    private float targetY;
    private long yAnimationStartTime;
    private float yAnimationStartValue;

    public NotificationInfo(int hashCode, String title, String subTitle, NotificationMode mode, int displayTime, float initialY, boolean isModule) {
        this.skipIntroAnimation = false;
        this.hashCode = hashCode;
        this.title = title;
        this.subTitle = subTitle;
        this.mode = mode;
        this.createTime = System.currentTimeMillis();
        this.displayDuration = displayTime;
        this.isModule = isModule;
        this.currentY = initialY;
        this.targetY = initialY;
        this.yAnimationStartTime = System.currentTimeMillis();
        this.yAnimationStartValue = initialY;
    }

    public NotificationInfo(String title, String subTitle, NotificationMode mode, int displayTime, float initialY, boolean isModule) {
        this(0, title, subTitle, mode, displayTime, initialY, isModule);
    }

    public void update() {
        long elapsed = System.currentTimeMillis() - this.yAnimationStartTime;
        float progress = Math.min(1.0F, (float) elapsed / 300.0F);
        float eased = this.easeOutExpo(progress);
        this.currentY = this.yAnimationStartValue + (this.targetY - this.yAnimationStartValue) * eased;
    }

    private float easeOutExpo(float t) {
        return t == 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0D, -10.0D * (double) t);
    }

    public void updateModuleState(String newTitle, String newSubTitle, NotificationMode newMode, int newDisplayDuration) {
        this.title = newTitle;
        this.subTitle = newSubTitle;
        this.mode = newMode;
        this.displayDuration = newDisplayDuration;
        this.createTime = System.currentTimeMillis();
        this.skipIntroAnimation = true;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - this.createTime > (long) this.displayDuration + 500L;
    }

    public boolean isExiting() {
        return System.currentTimeMillis() - this.createTime > (long) this.displayDuration;
    }

    public int getHashCode() {
        return this.hashCode;
    }

    public String getTitle() {
        return this.title;
    }

    public String getSubTitle() {
        return this.subTitle;
    }

    public NotificationMode getMode() {
        return this.mode;
    }

    public int getDisplayDuration() {
        return this.displayDuration;
    }

    public float getCurrentY() {
        return this.currentY;
    }

    public boolean isModule() {
        return this.isModule;
    }

    public long getCreateTime() {
        return this.createTime;
    }

    public void setTargetY(float targetY) {
        if (this.targetY != targetY) {
            this.targetY = targetY;
            this.yAnimationStartTime = System.currentTimeMillis();
            this.yAnimationStartValue = this.currentY;
        }
    }

    public boolean shouldSkipIntroAnimation() {
        return this.skipIntroAnimation;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            NotificationInfo that = (NotificationInfo) obj;
            return this.hashCode == that.hashCode;
        } else {
            return false;
        }
    }
}