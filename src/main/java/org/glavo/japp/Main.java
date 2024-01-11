/*
 * Copyright (C) 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp;

import org.glavo.japp.launcher.Launcher;
import org.glavo.japp.packer.JAppPacker;
import org.glavo.japp.platform.JavaRuntime;

import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

public final class Main {
    private static void printHelpMessage(PrintStream out) {
        out.println("Usage: japp <mode> [options]");
        out.println("Supported mode:");
        out.println("  japp create");
        out.println("  japp run");
        out.println("  japp list-java");
    }

    public static void main(String[] args) throws Throwable {
        if (Main.class.getProtectionDomain().getCodeSource().getLocation().toString().endsWith(".japp")) {
            Launcher.run(Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()), Collections.emptyList(), Arrays.asList(args));
            return;
        }

        if (args.length == 0) {
            printHelpMessage(System.out);
            return;
        }

        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (args[0]) {
            case "help":
            case "-help":
            case "--help":
                printHelpMessage(System.out);
                return;
            case "create":
                JAppPacker.main(commandArgs);
                break;
            case "run":
                Launcher.main(commandArgs);
                break;
            case "list-java":
                JavaRuntime.main(commandArgs);
                break;
            default:
                System.err.println("Unsupported mode: " + args[0]);
                printHelpMessage(System.err);
                System.exit(1);
        }
    }
}
