package org.glavo.japp.packer;

import org.glavo.japp.packer.processor.LocalClassPathProcessor;
import org.glavo.japp.packer.processor.MavenClassPathProcessor;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ClassPathProcessor {
    private static ClassPathProcessor getProcessor(String type) {
        if (type == null) {
            return LocalClassPathProcessor.INSTANCE;
        }
        switch (type) {
            case "maven":
                return new MavenClassPathProcessor();
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    public static void process(JAppPacker packer, String pathList, boolean isModulePath) throws Throwable {
        if (pathList.isEmpty()) {
            return;
        }

        for (String fullPath : pathList.split(File.pathSeparator)) {
            if (fullPath.isEmpty()) {
                continue;
            }

            Map<String, String> options = new LinkedHashMap<>();

            if (fullPath.charAt(0) != '[') {
                LocalClassPathProcessor.INSTANCE.process(packer, fullPath, isModulePath, options);
                continue;
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

            ClassPathProcessor processor = getProcessor(options.remove("type"));
            processor.process(packer, path, isModulePath, options);
        }
    }

    public abstract void process(JAppPacker packer, String path, boolean isModulePath, Map<String, String> options) throws Throwable;
}
