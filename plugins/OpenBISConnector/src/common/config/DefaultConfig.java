/*
 * Copyright 2015 ETH Zurich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package common.config;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * @author pkupczyk
 */
public class DefaultConfig
{

    private static final String PREF_NAME_SERVER = "PREF_NAME_SERVER";

    private static final String PREF_NAME_USER = "PREF_NAME_USER";

    private static final String PREF_NAME_WORKSPACE = "PREF_NAME_WORKSPACE";

    private static final String PREF_NAME_TIMEOUT = "PREF_NAME_TIMEOUT";

    private DefaultConfig()
    {
    }

    public static synchronized Config load(Class<?> clazz)
    {
        Config config = new Config(clazz);
        Preferences prefs = Preferences.userNodeForPackage(config.getClassToConfigure());

        String urlValue = prefs.get(PREF_NAME_SERVER, "");
        String userValue = prefs.get(PREF_NAME_USER, "");
        String workspaceValue = prefs.get(PREF_NAME_WORKSPACE, System.getProperty("user.home"));
        int timeoutValue = prefs.getInt(PREF_NAME_TIMEOUT, 10000);

        if (urlValue != null && userValue != null)
        {
            config.setUrl(urlValue);
            config.setUser(userValue);
        }

        if (workspaceValue != null)
        {
            config.setWorkspace(workspaceValue);
        }

        config.setTimeout(timeoutValue);

        return config;
    }

    public static synchronized void save(Config config)
    {
        try
        {
            Preferences prefs = Preferences.userNodeForPackage(config.getClassToConfigure());

            prefs.put(PREF_NAME_SERVER, config.getUrl());
            prefs.put(PREF_NAME_USER, config.getUser());
            prefs.put(PREF_NAME_WORKSPACE, config.getWorkspace());
            prefs.put(PREF_NAME_TIMEOUT, String.valueOf(config.getTimeout()));

            prefs.flush();
        } catch (BackingStoreException ex)
        {
            ex.printStackTrace();
        }
    }

}