package studio.fantasyit.quick_take.integration;

import net.neoforged.fml.ModList;

public class Integration {
    private static final String JECH_ID = "jecharacters";

    public static boolean isJechLoaded() {
        return ModList.get().isLoaded(JECH_ID);
    }
}