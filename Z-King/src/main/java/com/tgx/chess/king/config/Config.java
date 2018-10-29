/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.king.config;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;

/**
 * @author william.d.zk
 *         <p>
 *         {group}.{parent}.{owner}.{extension}.{key}.{reversion}
 *         group 可为 a.b.c 用于约束配置项的领域
 *         parent 可为 空或 a.b.c 多用于区分不同的业务模型
 *         owner 用于指定配置指向哪个组件，必须为组件名
 *         extension 可为 空或a.b.c 用于区分相同的 key 的不同自实现
 *         key 用于指定配置指向哪个组件的具体参数名
 *         reversion 用于指定当前配置的子版本，可以为空将覆盖当前值
 */
public class Config
        implements
        Cloneable,
        Serializable
{

    private final static long                        serialVersionUID       = 7432284958748032464L;
    private final static Logger                      LOG                    = Logger.getLogger(Config.class.getName());
    private final static String                      PACKAGE_PREFIX_NAME    = "com.tgx.chess.config";
    private final static String                      ROOT_OWNER             = "Environment";
    private final static String                      PACKAGE_PREFIX_PATTERN = PACKAGE_PREFIX_NAME.replaceAll("\\.", "\\\\.");
    private final static Set<String>                 KEY_SET                = new ConcurrentSkipListSet<>();
    private final static List<Pair<Pattern, String>> KEY_PATTERNS           = new ArrayList<>();
    private final Map<String, ? super Object>        _ValueStoreMap         = new TreeMap<>();
    private final String                             _ConfigParent;
    private final String                             _ConfigOwner;
    private final Map<String, String>                _ConfigExternal;

    public static Pattern getKeyPatternExactly(String group, String parent, String owner, String key)
    {
        return getKeyPattern(group, parent, owner, null, key, -1);
    }

    public static Pattern getKeyPatternAnyKey(String group, String parent, String owner)
    {
        return getKeyPattern(group, parent, owner, null, null, -1);
    }

    public static Pattern getKeyPatternAnyOwner(String group, String parent, String key)
    {
        return getKeyPattern(group, parent, null, null, key, -1);
    }

    public static Pattern getKeyPatternGroupAndParent(String group, String parent)
    {
        return getKeyPattern(group, parent, null, null, null, -1);
    }

    public static Pattern getKeyPatternGroup(String group)
    {
        return getKeyPattern(group, null, null, null, null, -1);
    }

    private static Pattern getKeyPattern(String group, String parent, String owner, String extension, String key, int revision)
    {
        group     = Objects.isNull(group) ? "((\\w+\\.)+)"
                                          : "(" + (group.matches(".*\\.$") ? group.replaceAll("\\.", "\\\\.") : group.replaceAll("\\.", "\\\\.") + "\\.") + ")";
        parent    = Objects.isNull(parent) ? "((\\w+\\.)*)"
                                           : "(" + (parent.matches(".*\\.$") ? parent.replaceAll("\\.", "\\\\.") : parent.replaceAll("\\.", "\\\\.") + "\\.") + ")";

        owner     = Objects.isNull(owner) ? "((\\w+)\\.)" : "(" + (owner.matches("\\w+\\.$") ? owner.replace(".", "\\.") : owner + "\\.") + ")";
        extension = Objects.isNull(extension) ? "((\\w+\\.)*)"
                                              : "("
                                                + (extension.matches(".*\\.$") ? extension.replaceAll("\\.", "\\\\.") : extension.replaceAll("\\.", "\\\\.") + "\\.")
                                                + ")";
        key       = Objects.isNull(key) ? "(\\w+)" : "(" + key + ")";
        String revisionStr = revision < 0 ? "(\\.\\d+)?" : "\\." + Integer.toString(revision);
        String pattern     = "^(" + PACKAGE_PREFIX_PATTERN + "\\.)" + group + parent + owner + extension + key + revisionStr;
        return Pattern.compile(pattern);
    }

    private Config(String owner, String parent, Map<String, String> external)
    {
        _ConfigOwner    = owner;
        _ConfigParent   = Objects.isNull(parent) || "".equals(parent) ? null : parent;
        _ConfigExternal = external;
        load();
    }

    public Config(String parent, Map<String, String> external)
    {
        this(ROOT_OWNER, parent, external);
    }

    public Config(String parent)
    {
        this(parent, null);
    }

    public Config()
    {
        this(null, null);
    }

    public void load()
    {
        load(_ConfigOwner, _ConfigParent);
    }

    public Config load(String owner)
    {
        load(owner, _ConfigParent);
        return this;
    }

    private void load(ResourceBundle resourceBundle, String parent)
    {
        Objects.requireNonNull(resourceBundle);
        LOG.info("service: " + parent + " Config name " + resourceBundle.getBaseBundleName());
        for (Enumeration<String> e = resourceBundle.getKeys(); e.hasMoreElements();) {
            String element = e.nextElement();
            Object value   = ConfigReader.readObject(resourceBundle, element);
            KEY_PATTERNS.stream()
                        .filter(pair ->
                        {
                            Matcher matcher = pair.first()
                                                  .matcher(element);
                            return Objects.isNull(parent) ? matcher.matches()
                                                          : matcher.find()
                                                            && matcher.group(3)
                                                                      .matches("(" + parent.replaceAll("\\.", "\\\\.") + "\\.)");
                        })
                        .map(Pair::second)
                        .forEach(str -> _ValueStoreMap.put(str, value));
            _ValueStoreMap.put(element, value);
            LOG.debug(element + " = " + value);
        }
    }

    public void load(String owner, String parent)
    {
        Objects.requireNonNull(owner);
        ResourceBundle resourceBundle = null;
        try {
            resourceBundle = ResourceBundle.getBundle(Objects.isNull(parent) ? owner : parent.replaceAll("\\.", "_") + "/" + owner);
            LOG.debug("load resource " + owner + " parent: " + parent);
        }
        catch (MissingResourceException e) {
            LOG.debug("biz path: \"" + parent + "\" is not exist,load from loader");
            if (Objects.nonNull(_ConfigExternal)) {
                String dbKey = owner.toLowerCase();
                if (!_ConfigExternal.isEmpty() && Objects.nonNull(_ConfigExternal.get(dbKey))) {
                    try {
                        String resource = _ConfigExternal.get(dbKey);
                        resourceBundle = new TgxResourceBundle(owner, new StringReader(resource));
                    }
                    catch (IOException ie) {
                        e.printStackTrace();
                    }
                }
            }
        }
        LOG.info(String.format("load default %s", owner));
        load(ResourceBundle.getBundle(owner), parent);
        if (Objects.nonNull(resourceBundle)) {
            LOG.info(String.format("load parent %s %s", parent, owner));
            load(resourceBundle, parent);
        }
    }

    private String getConfigParent()
    {
        return _ConfigParent;
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key)
    {
        Objects.requireNonNull(key);
        Object result = _ValueStoreMap.get(key);
        if (Objects.isNull(result)) {
            LOG.warning("unknown config key!: " + key);
            throw new NullPointerException(String.format("not contains key:%s", key));
        }
        return (T) result;
    }

    public boolean contains(String key)
    {
        Objects.requireNonNull(key);
        return _ValueStoreMap.containsKey(key) && Objects.nonNull(_ValueStoreMap.get(key));
    }

    public boolean contains(String group, String owner, String key)
    {
        String configKey = getKeySet().stream()
                                      .filter(getKeyPattern(group, getConfigParent(), owner, null, key, -1).asPredicate())
                                      .findFirst()
                                      .orElse(getPackagePrefix() + "." + group + "." + owner + "." + key);
        return contains(configKey);
    }

    public <T> void setConfigValue(String group, String owner, String key, String extentsion, int revision, T value)
    {
        String configKey = String.format("%s.%s", getPackagePrefix(), group);
        if (Objects.nonNull(getConfigParent())) {
            configKey += String.format(".%s", getConfigParent());
        }
        configKey += String.format(".%s", owner);
        if (Objects.nonNull(extentsion) && !"".equals(extentsion)) {
            configKey += String.format(".%s", extentsion);
        }
        configKey += String.format(".%s", key);
        if (revision >= 0) {
            configKey += String.format(".%d", revision);
        }
        LOG.debug("set config : " + configKey + " = " + value);
        _ValueStoreMap.put(configKey, value);
    }

    <T> void setConfigValue(String group, String owner, String key, T value)
    {
        String configKey = getKeySet().stream()
                                      .filter(getKeyPattern(group, getConfigParent(), owner, null, key, -1).asPredicate())
                                      .findFirst()
                                      .orElse(getPackagePrefix() + "." + group + "." + owner + "." + key);
        LOG.debug("set config : " + configKey + " = " + value);
        _ValueStoreMap.put(configKey, value);
    }

    public <T> T getConfigValue(String group, String owner, String extension, String key)
    {
        return getConfigValue(getKeySet().stream()
                                         .filter(getKeyPattern(group, getConfigParent(), owner, extension, key, -1).asPredicate())
                                         .findFirst()
                                         .orElse(getPackagePrefix() + "." + group + "." + owner + "." + key));
    }

    public <T> T getConfigValue(String group, String owner, String extension, String key, int revision)
    {
        return getConfigValue(getKeySet().stream()
                                         .filter(getKeyPattern(group, getConfigParent(), owner, extension, key, revision).asPredicate())
                                         .findFirst()
                                         .orElse(getPackagePrefix() + "." + group + "." + owner + "." + key));
    }

    public <T> T getConfigValue(String group, String parent, String owner, String extension, String key, int revision)
    {
        return getConfigValue(getKeySet().stream()
                                         .filter(getKeyPattern(group, parent, owner, extension, key, revision).asPredicate())
                                         .findFirst()
                                         .orElse(getPackagePrefix() + "." + group + "." + owner + "." + key));
    }

    public <T> T getConfigValue(String group, String owner, String key)
    {
        return getConfigValue(getKeySet().stream()
                                         .filter(getKeyPattern(group, getConfigParent(), owner, null, key, -1).asPredicate())
                                         .findFirst()
                                         .orElse(getPackagePrefix() + "." + group + "." + owner + "." + key));
    }

    public Set<String> getKeySet()
    {
        Set<String> keySet = new TreeSet<>();
        keySet.addAll(KEY_SET);
        keySet.addAll(_ValueStoreMap.keySet());
        return keySet;
    }

    @Override
    public String toString()
    {
        return String.format("Config: Owner:%s Parent:%s \n%s",
                             _ConfigOwner,
                             _ConfigParent,
                             Logger.arrayToString(_ValueStoreMap.entrySet()
                                                                .toArray()));
    }

    public static String getPackagePrefix()
    {
        return PACKAGE_PREFIX_NAME;
    }

    public static String getParent(String... args)
    {
        Objects.requireNonNull(args);
        return Stream.of(args)
                     .reduce((l, r) -> l + "." + r)
                     .orElse(null);
    }
}
