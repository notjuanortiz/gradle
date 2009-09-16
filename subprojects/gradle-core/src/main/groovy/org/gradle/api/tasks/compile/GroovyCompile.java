/*
 * Copyright 2007 the original author or authors.
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

package org.gradle.api.tasks.compile;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.util.GUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Hans Dockter
 */
public class GroovyCompile extends Compile {
    private List groovySourceDirs = null;

    private AntGroovyc antGroovyCompile = new AntGroovyc();

    private FileCollection groovyClasspath;

    private List groovyIncludes = null;

    private List groovyExcludes = null;

    private GroovyCompileOptions groovyOptions = new GroovyCompileOptions();

    protected void compile() {
        if (getAntGroovyCompile() == null) throw new InvalidUserDataException("The ant groovy compile command must be set!");
        if (getSourceCompatibility() == null || getTargetCompatibility() == null) {
            throw new InvalidUserDataException("The sourceCompatibility and targetCompatibility must be set!");
        }

        List<File> classpath = null;
        List existingGroovySourceDirs = existentDirsFilter.findExistingDirs(getGroovySourceDirs());
        if (existingGroovySourceDirs.size() > 0) {
            if (classpath == null) {
                classpath = GUtil.addLists(getClasspath());
            }
            // todo We need to understand why it is not good enough to put groovy and ant in the task classpath but also Junit. As we don't understand we put the whole testCompile in it right now. It doesn't hurt, but understanding is better :)
            List<File> taskClasspath = new ArrayList<File>(getGroovyClasspath().getFiles());
            throwExceptionIfTaskClasspathIsEmpty(taskClasspath);
            antGroovyCompile.execute(getProject().getAnt(), existingGroovySourceDirs, getGroovyIncludes(),
                    getGroovyExcludes(), getDestinationDir(), classpath, getSourceCompatibility(),
                    getTargetCompatibility(), getGroovyOptions(), getOptions(), taskClasspath);
            setDidWork(antGroovyCompile.getNumFilesCompiled() > 0);            
        }
    }

    private void throwExceptionIfTaskClasspathIsEmpty(Collection<File> taskClasspath) {
        if (taskClasspath.size() == 0) {
            throw new InvalidUserDataException("You must assign a Groovy library to the groovy configuration!");
        }
    }

    /**
     * Gets the options for the groovyc compilation. To set specific options for the nested javac compilation,
     * use {@link #getOptions()}.
     */
    public GroovyCompileOptions getGroovyOptions() {
        return groovyOptions;
    }

    /**
     * Sets the options for the groovyc compilation. To set specific options for the nested javac compilation,
     * use {@link #getOptions()}. Usually you don't set the options, but you modify the existing instance
     * provided by {@link #getGroovyOptions()}  
     * 
     * @param groovyOptions
     */
    public void setGroovyOptions(GroovyCompileOptions groovyOptions) {
        this.groovyOptions = groovyOptions;
    }

    /**
     * Adds include pattern for which groovy files should be compiled (e.g. '**&#2F;org/gradle/package1/')).
     * This pattern is added as an nested include the groovyc task.
     * @param groovyIncludes The include patterns 
     * @return this
     */
    public GroovyCompile groovyInclude(String... groovyIncludes) {
        if (getGroovyIncludes() == null) {
            this.groovyIncludes = new ArrayList();
        }
        this.groovyIncludes.addAll(Arrays.asList(groovyIncludes));
        return this;
    }

    /**
     * Adds exclude patterns for which groovy files should be compiled (e.g. '**&#2F;org/gradle/package2/A*.groovy').
     * This pattern is added as an nested exclude the groovyc task.
     * @param groovyExcludes The exclude patterns
     * @return this
     */
    public GroovyCompile groovyExclude(String... groovyExcludes) {
        if (getGroovyExcludes() == null) {
            this.groovyExcludes = new ArrayList();
        }
        this.groovyExcludes.addAll(Arrays.asList(groovyExcludes));
        return this;
    }

    @InputFiles
    public FileCollection getGroovyClasspath() {
        return groovyClasspath;
    }

    public void setGroovyClasspath(FileCollection groovyClasspath) {
        this.groovyClasspath = groovyClasspath;
    }

    public AntGroovyc getAntGroovyCompile() {
        return antGroovyCompile;
    }

    public void setAntGroovyCompile(AntGroovyc antGroovyCompile) {
        this.antGroovyCompile = antGroovyCompile;
    }

    @InputFiles
    public List getGroovySourceDirs() {
        return groovySourceDirs;
    }

    public void setGroovySourceDirs(List groovySourceDirs) {
        this.groovySourceDirs = groovySourceDirs;
    }

    /**
     * Return the include patterns for which groovy files should be compiled.
     */
    public List getGroovyIncludes() {
        return groovyIncludes;
    }

    /**
     * Sets the include patterns for which groovy files should be compiled.
     *
     * @param groovyIncludes The patterns to include
     * @see #groovyInclude(String...)
     */
    public void setGroovyIncludes(List groovyIncludes) {
        this.groovyIncludes = groovyIncludes;
    }

    /**
     * Returns the exclude patterns for which groovy files should be compiled.
     */
    public List getGroovyExcludes() {
        return groovyExcludes;
    }

    /**
     * Sets the exclude patterns for which groovy files should be compiled.
     *
     * @param groovyExcludes The patterns to exclude
     * @see #groovyExclude(String...)
     */
    public void setGroovyExcludes(List groovyExcludes) {
        this.groovyExcludes = groovyExcludes;
    }
}
