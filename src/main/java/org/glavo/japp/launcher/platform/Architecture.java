package org.glavo.japp.launcher.platform;

import java.util.Locale;

public enum Architecture {
    X86(false, "x86"),
    X86_64(true, "x86-64"),
    IA64(true, "IA-64"),
    SPARC(false, "SPARC"),
    SPARCV9(true, "SPARC V9"),
    ARM(false, "ARM"),
    AARCH64(true, "AArch64"),
    MIPS(false, "MIPS (Big-Endian)"),
    MIPS64(true, "MIPS64 (Big-Endian)"),
    MIPSEL(false, "MIPS (Little-Endian)"),
    MIPS64EL(true, "MIPS64 (Little-Endian)"),
    PPC(false, "PowerPC (Big-Endian)"),
    PPC64(true, "PowerPC-64 (Big-Endian)"),
    PPCLE(false, "PowerPC (Little-Endian)"),
    PPC64LE(true, "PowerPC-64 (Little-Endian)"),
    S390(false, "S390"),
    S390X(true, "S390x"),
    RISCV32(false, "RISC-V 32"),
    RISCV64(true, "RISC-V 64"),
    LOONGARCH32(false, "LoongArch32"),
    LOONGARCH64(true, "LoongArch64");

    private final String checkedName;
    private final String displayName;
    private final boolean is64Bit;

    Architecture(boolean is64Bit, String displayName) {
        this.checkedName = this.name().toLowerCase(Locale.ROOT).replace("_", "-");
        this.displayName = displayName;
        this.is64Bit = is64Bit;
    }

    public static Architecture parseArchitecture(String value) {
        value = value.trim().toLowerCase(Locale.ROOT);

        switch (value) {
            case "x8664":
            case "x86-64":
            case "x86_64":
            case "amd64":
            case "ia32e":
            case "em64t":
            case "x64":
                return X86_64;
            case "x8632":
            case "x86-32":
            case "x86_32":
            case "x86":
            case "i86pc":
            case "i386":
            case "i486":
            case "i586":
            case "i686":
            case "ia32":
            case "x32":
                return X86;
            case "arm64":
            case "aarch64":
                return AARCH64;
            case "arm":
            case "arm32":
                return ARM;
            case "mips64":
                return MIPS64;
            case "mips64el":
                return MIPS64EL;
            case "mips":
            case "mips32":
                return MIPS;
            case "mipsel":
            case "mips32el":
                return MIPSEL;
            case "riscv32":
                return RISCV32;
            case "riscv64":
                return RISCV64;
            case "ia64":
            case "ia64w":
            case "itanium64":
                return IA64;
            case "sparcv9":
            case "sparc64":
                return SPARCV9;
            case "sparc":
            case "sparc32":
                return SPARC;
            case "ppc64":
            case "powerpc64":
                return PPC64;
            case "ppc64le":
            case "powerpc64le":
                return PPC64LE;
            case "ppc":
            case "ppc32":
            case "powerpc":
            case "powerpc32":
                return PPC;
            case "ppcle":
            case "ppc32le":
            case "powerpcle":
            case "powerpc32le":
                return PPCLE;
            case "s390":
                return S390;
            case "s390x":
                return S390X;
            case "loongarch32":
                return LOONGARCH32;
            case "loongarch64":
                return LOONGARCH64;
            default:
                if (value.startsWith("armv7")) {
                    return ARM;
                }
                if (value.startsWith("armv8") || value.startsWith("armv9")) {
                    return AARCH64;
                }
        }

        throw new IllegalArgumentException();
    }

    public boolean is64Bit() {
        return is64Bit;
    }

    public String getCheckedName() {
        return checkedName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
