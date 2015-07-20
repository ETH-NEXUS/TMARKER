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

/**
 * @author pkupczyk
 */
public class Config {

    private Class<?> classToConfigure;

    private String url;

    private String user;

    private String workspace;

    private String password;

    private int timeout;

    public Config(Class<?> classToConfigure) {
        this.classToConfigure = classToConfigure;
    }

    public Class<?> getClassToConfigure() {
        return classToConfigure;
    }

    public void setClassToConfigure(Class<?> classToConfigure) {
        this.classToConfigure = classToConfigure;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public synchronized String getUrl() {
        return url;
    }

    public synchronized void setUrl(String url) {
        this.url = url;
    }

    public synchronized String getUser() {
        return user;
    }

    public synchronized void setUser(String user) {
        this.user = user;
    }

    public synchronized String getPassword() {
        return password;
    }

    public synchronized void setPassword(String password) {
        this.password = password;
    }

    public synchronized int getTimeout() {
        return timeout;
    }

    public synchronized void setTimeout(int timeout) {
        this.timeout = timeout;
    }

}