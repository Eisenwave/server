package tech.eisen.server.content;

import org.jetbrains.annotations.Nullable;

import java.nio.file.attribute.BasicFileAttributes;

public interface FileAttributes extends BasicFileAttributes {
    
    @Nullable
    abstract String getMediaType();
    
}
