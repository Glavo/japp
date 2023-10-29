package org.glavo.japp.packer;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ClassPathProcessor {
    private static final Map<String, ClassPathProcessor> processors = new HashMap<>();

    public static void process(JAppPacker packer, String fullPath, boolean isModulePath) throws IOException {
        if (fullPath.isEmpty()) {
            return;
        }

        Map<String, String> options = new LinkedHashMap<>();

        if (fullPath.charAt(0) != '[') {
            LocalClassPathProcessor.INSTANCE.process(packer, fullPath, isModulePath, options);
        }

        int endIndex = fullPath.indexOf(']', 1);
        if (endIndex < 0) {
            throw new IllegalArgumentException("Unterminated option group: " + fullPath);
        }

        String path = fullPath.substring(endIndex + 1);
        String[] optionArray = fullPath.substring(1, endIndex).split(",");
        for (String option : optionArray) {
            int idx = option.indexOf('=');
            if (idx >= 0) {
                options.put(option.substring(0, idx), option.substring(idx + 1));
            } else {
                options.put(option, "");
            }
        }

        String type = options.get("type");

        ClassPathProcessor processor;
        if (type != null) {
            processor = processors.get(type);
            if (processor == null) {
                throw new IllegalArgumentException("Unknown type: " + type);
            }
        } else {
            processor = LocalClassPathProcessor.INSTANCE;
        }

        processor.process(packer, path, isModulePath, options);
    }

    public abstract void process(JAppPacker packer, String path, boolean isModulePath, Map<String, String> options) throws IOException;
}
