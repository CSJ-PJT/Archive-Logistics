package com.csj.archive.logistics.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "archive.runtime")
public class RuntimeWorkLoopProperties {
    private Autorun autorun = new Autorun();
    private Duration tickInterval = Duration.ofSeconds(30);
    private int maxEventsPerTick = 10;
    private int maxBacklogPerTick = 50;
    private int maxHop = 5;

    public Autorun getAutorun() {
        return autorun;
    }

    public void setAutorun(Autorun autorun) {
        this.autorun = autorun;
    }

    public Duration getTickInterval() {
        return tickInterval;
    }

    public void setTickInterval(Duration tickInterval) {
        this.tickInterval = tickInterval;
    }

    public int getMaxEventsPerTick() {
        return maxEventsPerTick;
    }

    public void setMaxEventsPerTick(int maxEventsPerTick) {
        this.maxEventsPerTick = maxEventsPerTick;
    }

    public int getMaxBacklogPerTick() {
        return maxBacklogPerTick;
    }

    public void setMaxBacklogPerTick(int maxBacklogPerTick) {
        this.maxBacklogPerTick = maxBacklogPerTick;
    }

    public int getMaxHop() {
        return maxHop;
    }

    public void setMaxHop(int maxHop) {
        this.maxHop = maxHop;
    }

    public static class Autorun {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
