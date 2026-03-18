/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.tag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modbus 标签管理器
 */
public class TagManager {
    
    private final Map<String, Tag> tags = new ConcurrentHashMap<>();
    
    public void register(Tag tag) {
        tags.put(tag.getName(), tag);
    }
    
    public Tag getTag(String name) {
        return tags.get(name);
    }
    
    public void unregister(String name) {
        tags.remove(name);
    }
    
    public Map<String, Tag> getAllTags() {
        return Map.copyOf(tags);
    }
}
