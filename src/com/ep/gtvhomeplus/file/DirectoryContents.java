package com.ep.gtvhomeplus.file;

import java.util.List;

public class DirectoryContents {
    public List<IconifiedText> listDir;
    public List<IconifiedText> listFile;
    public List<IconifiedText> listSdCard;
    
    // If true, there's a ".nomedia" file in this directory.
    boolean noMedia;
}
