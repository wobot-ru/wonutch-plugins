package ru.wobot.sm.core.meta;

public interface NutchDocumentMetaConstants {
    /**
     * Used to map from merged index back to segment files
     */
    String SEGMENT = "segment";
    /**
     * Used by dedup
     */
    String DIGEST = "digest";
    /**
     * Use by explain and dedup
     */
    String BOOST = "boost";
}
