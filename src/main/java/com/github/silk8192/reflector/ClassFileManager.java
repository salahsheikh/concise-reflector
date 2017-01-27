package com.github.silk8192.reflector;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Handles the loading of classes from either a local .jar file and/or the root directory of many classes.
 * @author silk8192, acrois
 */
public class ClassFileManager {
    private final Map<String, Class<?>> classes = new HashMap<>();
    private final Map<String, Class<?>> classesReadOnly = Collections.unmodifiableMap(classes);

    // you probably wouldn't want an empty constructor, however we can slim down the code a little so it's easier to read.

    /**
     * Initializes with the supplied starting file.
     * @param start The root file or JAR file.
     * @throws IOException
     */
    public ClassFileManager(File start) throws IOException {
        this(start.toPath(), true);
    }
    
    /**
     * Initializes with the supplied starting file.
     * @param start The root file or JAR file.
     * @throws IOException
     */
    public ClassFileManager(Path start) throws IOException {
        this(start, true);
    }
    
    /**
     * Initializes with the supplied starting file.
     * @param start The root file or JAR file.
     * @param loadJar True to load .jar files, false otherwise.
     * @throws IOException
     */
    public ClassFileManager(File start, boolean loadJar) throws IOException {
        this(start.toPath(), loadJar);
    }

    /**
     * Initializes with the supplied starting file.
     * @param start The root file or JAR file.
     * @param loadJar True to load .jar files, false otherwise.
     * @throws IOException
     */
    public ClassFileManager(Path start, boolean loadJar) throws IOException {
        addAll(start, loadJar);
    }

    /**
     * A read-only mapping of the classes.
     * @return The mapping of class names to class types.
     */
    public Map<String, Class<?>> getClasses() {
        return classesReadOnly;
    }

    /**
     * Gets a class type 
     * @param canonicalClassName
     * @return The type, or null if does not exist.
     * @see {@link java.util.Map#get(Object)}
     */
    public Class<?> getClass(String canonicalClassName) {
        return classesReadOnly.get(canonicalClassName);
    }
    
    /**
     * Loads a single class file or classes from .jar file.
     * @param path
     * @throws IOException
     */
    public void add(Path path) throws IOException {
        String name = path.toFile().getName();

        if (name.toLowerCase().endsWith(".jar")) {
            try (
                URLClassLoader loader = new URLClassLoader(new URL[] { path.toUri().toURL() });
                ZipFile zf = new ZipFile(path.toFile())
            ) {
                Enumeration<? extends ZipEntry> entries = zf.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    String entryName = entry.getName();

                    if (entryName.toLowerCase().endsWith(".class") && !entryName.contains("$")) {
                        String className = entry.getName().replace(".class", "").replace("\\", ".").replace("/", ".");

                        try {
                            classes.put(className, loader.loadClass(className));
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace(); // hmm
                        }
                    }
                }
            }
        }
        else if (name.toLowerCase().endsWith(".class") && !name.contains("$")) {
            String className = path.toString().substring(0, path.toString().lastIndexOf('.'));
            className = className.replace("\\", "."); // replace forward slashes in path to "."
            URL i = path.toUri().toURL();
            
            try (URLClassLoader loader = new URLClassLoader(new URL[] { i })) {
                classes.put(className, loader.loadClass(className));
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace(); // hmm
            }
        }
    }

    /**
     * Walks through this {@link Path}'s file tree and visits each {@link Path}, including all .class files in folders starting at the specified path. By default, .jar files will be included, and so will the classes that reside in the folders inside.
     * @param path The path to start at.
     * @throws IOException
     * @see {@link com.github.silk8192.reflector.ClassFileManager#addAll(Path, boolean)}
     */
    public void addAll(Path path) throws IOException {
        addAll(path, true);
    }
    
    /**
     * Walks through the file tree and visits each {@link Path}, including all .class files in folders starting at the specified path. If allowed, .jar files will be included, and so will the classes that reside in the folders inside.
     * @param path The path to start at.
     * @param loadJar True to load .jar files, false to skip them.
     * @throws IOException
     * @see {@link com.github.silk8192.reflector.ClassFileManager#add(Path)}
     */
    public void addAll(Path path, boolean loadJar) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!loadJar && file.toFile().getName().toLowerCase().endsWith(".jar")) {
                    return FileVisitResult.CONTINUE;
                }
                
                add(path.relativize(file));
                return FileVisitResult.CONTINUE;
            }
        });
    }
}