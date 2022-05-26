package com.we1u.filesync.configs;

import java.util.List;

public class FileComparatorConfig {

    public boolean checkSize;
    public boolean checkContent;
    public boolean checkModTime;
    public List<String> extensions;
    public boolean excludeExtension;

    public FileComparatorConfig(boolean _checkSize,
                                boolean _checkContent,
                                boolean _checkModTime,
                                List<String> _extensions,
                                boolean _removeExtensions){
        this.checkSize = _checkSize;
        this.checkContent = _checkContent;
        this.checkModTime = _checkModTime;
        this.extensions = _extensions;
        this.excludeExtension = _removeExtensions;
    }
}
