package org.lonevox.tectonicDiscWorld;

/**
 * Platform-agnostic config holder. Fields are populated by each platform's
 * config implementation at startup before the generated pack is written.
 */
public final class TectonicDiscWorldConfig {
    /** Radius in blocks where Tectonic terrain is left untouched. */
    public static int unaffectedRadius = 5000;
    /** Width in blocks of the transition band from land to outer ocean. */
    public static int falloffRadius = 2500;
    /** Shifts the center of the circular region on the X axis. */
    public static int offsetX = 0;
    /** Shifts the center of the circular region on the Z axis. */
    public static int offsetZ = 0;
    /** If true, registers the generated data pack as always enabled and top priority. */
    public static boolean forceEnablePack = true;

    private TectonicDiscWorldConfig() {}
}
