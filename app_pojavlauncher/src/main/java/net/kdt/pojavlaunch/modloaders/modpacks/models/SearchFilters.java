package net.kdt.pojavlaunch.modloaders.modpacks.models;

import org.jetbrains.annotations.Nullable;

/**
 * Search filters, passed to APIs
 */
public class SearchFilters {
    public static final String TYPE_MOD = "mod";
    public static final String TYPE_RESOURCEPACK = "resourcepack";
    public static final String TYPE_SHADER = "shader";
    public static final String TYPE_WORLD = "world";
    public static final String TYPE_MODPACK = "modpack";

    /** When true the search targets modpacks (CurseForge class 4471). */
    public boolean isModpack;
    /** Modrinth project_type used for the facet filter (mod / resourcepack / shader / world). */
    public String projectType = TYPE_MOD;
    public String name;
    @Nullable public String mcVersion;

}
