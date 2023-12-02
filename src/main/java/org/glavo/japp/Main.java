package org.glavo.japp;

import org.glavo.japp.launcher.Launcher;
import org.glavo.japp.packer.JAppPacker;

import java.util.Arrays;

public final class Main {
    public static void main(String[] args) throws Throwable {
        if (args.length == 0) {
            System.out.println("Supported mode:");
            System.out.println("  japp create");
            System.out.println("  japp run");
            return;
        }

        String command = args[0];
        args = Arrays.copyOfRange(args, 1, args.length);
        switch (command) {
            case "create":
                JAppPacker.main(args);
                break;
            case "run":
                Launcher.main(args);
                break;
            default:
                throw new TODO("Command: " + command);
        }
    }
}
