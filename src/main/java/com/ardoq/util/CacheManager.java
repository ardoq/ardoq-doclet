package com.ardoq.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Wrapper for handling our component cache and writing it to directory
 */
public class CacheManager {
    private final Properties cacheList;
    private final String fileName;

    public CacheManager(String cacheDirectory, boolean clearCache) throws IOException {
        this.fileName = cacheDirectory + "/ardoqCache.properties";
        System.out.println("Cache file used: "+fileName);
        this.cacheList = new Properties();
        if (clearCache)
        {
            System.out.println("Clearing cache");
        }
        else if (new File(fileName).exists())
        {
            this.cacheList.load(new FileInputStream(fileName));
        }
        else
        {
            System.out.println("Cache does not exists, creating new.");
        }
    }

    public void add(String uniqueId, String id, String workspaceId){
        this.cacheList.put(uniqueId, id+","+workspaceId);
    }

    public String getComponentId(String uniqueId){
        String id = null;
        if (this.cacheList.containsKey(uniqueId))
        {
            id = this.cacheList.getProperty(uniqueId).split(",")[0];

        }
        return  id;
    }

    public String getWorkspaceId(String uniqueId){
        String id = null;
        if (this.cacheList.containsKey(uniqueId))
        {
            id = this.cacheList.getProperty(uniqueId).split(",")[1];
        }
        return  id;
    }

    public void saveCache() throws IOException {
        System.out.println("Saving component cache to: "+this.fileName);
        this.cacheList.store(new FileWriter(this.fileName), "Ardoq JavaDoc Client properties");
    }
}
