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

import java.util.Arrays;

public final class Main {
    public static void main(String[] args) throws Throwable {
        if (args.length == 0) {
            System.out.println("Supported mode:");
            System.out.println("  japp create");
            System.out.println("  japp run");
            System.out.println("  japp list-java");
            return;
        }

        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (args[0]) {
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
                throw new TODO("Command: " + args[0]);
        }
    }
}
