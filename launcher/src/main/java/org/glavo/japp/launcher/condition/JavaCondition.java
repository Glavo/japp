package org.glavo.japp.launcher.condition;

import java.util.Map;

public final class JavaCondition implements Condition {

    public static JavaCondition fromMap(Map<String, String> options) {
        String version = options.remove("version");
        String os = options.remove("os");
        String arch = options.remove("arch");

        if (!options.isEmpty()) {
            throw new IllegalArgumentException("Unknown options: " + options.keySet());
        }

        return new JavaCondition(
                version == null ? null : Integer.parseInt(version),
                MatchList.of(os), MatchList.of(arch)
        );
    }

    private final Integer version;
    private final MatchList os;
    private final MatchList arch;

    private JavaCondition(Integer version, MatchList os, MatchList arch) {
        this.version = version;
        this.os = os;
        this.arch = arch;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean test(JAppRuntimeContext context) {
        if (version != null && context.getJava().getVersion().major() < version) {
            return false;
        }

        if (os != null && !os.test(context.getJava().getOperatingSystem().getCheckedName())) {
            return false;
        }

        if (arch != null && !arch.test(context.getJava().getArchitecture().getCheckedName())) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "java(" +
               "version=" + version +
               ", os=" + os +
               ", arch=" + arch +
               ')';
    }
}
