package de.upb.swt.soot.java.bytecode.inputlocation;

import de.upb.swt.soot.core.frontend.AbstractClassSource;
import de.upb.swt.soot.core.frontend.ResolveException;
import de.upb.swt.soot.core.inputlocation.AnalysisInputLocation;
import de.upb.swt.soot.core.types.ClassType;
import de.upb.swt.soot.core.views.View;
import de.upb.swt.soot.java.bytecode.frontend.apk.dexpler.DexClassLoader;
import de.upb.swt.soot.java.bytecode.frontend.apk.dexpler.DexFileProvider;
import de.upb.swt.soot.java.bytecode.frontend.apk.dexpler.Util;
import de.upb.swt.soot.java.core.JavaSootClass;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.MultiDexContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ApkAnalysisInputLocation implements AnalysisInputLocation<JavaSootClass> {

    private static final @Nonnull
    Logger logger = LoggerFactory.getLogger(ApkAnalysisInputLocation.class);

    private final static Comparator<DexFileProvider.DexContainer<? extends DexFile>> DEFAULT_PRIORITIZER
            = new Comparator<DexFileProvider.DexContainer<? extends DexFile>>() {

        @Override
        public int compare(DexFileProvider.DexContainer<? extends DexFile> o1, DexFileProvider.DexContainer<? extends DexFile> o2) {
            String s1 = o1.getDexName(), s2 = o2.getDexName();

            // "classes.dex" has highest priority
            if (s1.equals("classes.dex")) {
                return 1;
            } else if (s2.equals("classes.dex")) {
                return -1;
            }

            // if one of the strings starts with "classes", we give it the edge right here
            boolean s1StartsClasses = s1.startsWith("classes");
            boolean s2StartsClasses = s2.startsWith("classes");

            if (s1StartsClasses && !s2StartsClasses) {
                return 1;
            } else if (s2StartsClasses && !s1StartsClasses) {
                return -1;
            }

            // otherwise, use natural string ordering
            return s1.compareTo(s2);
        }
    };


    /**
     * Mapping of filesystem file (apk, dex, etc.) to mapping of dex name to dex file
     */
    private final Map<String, Map<Path, DexFileProvider.DexContainer<? extends DexFile>>> dexMap = new HashMap<>();

    @Nonnull private final Path apkPath;
    boolean process_multiple_dex = true;
    boolean search_dex_in_archives = true;
    boolean verbose = true;
    int android_api_version = 10;

    public ApkAnalysisInputLocation(@Nonnull Path apkPath, Collection<MultiDexContainer.DexEntry<? extends DexFile>> dexFiles){
        this.dexFiles = dexFiles;
        if (!Files.exists(apkPath)) {
            throw new ResolveException("No APK file found",apkPath);
        }
        this.apkPath = apkPath;
    }

    @Nonnull
    @Override
    public Optional<? extends AbstractClassSource<JavaSootClass>> getClassSource(@Nonnull ClassType type, @Nonnull View<?> view) {
        // TODO code here
        ArrayList<DexFileProvider.DexContainer<? extends DexFile>> resultList = new ArrayList<>();
        List<Path> allSources = allSourcesFromFile(apkPath);
        updateIndex(allSources);

        for (Path theSource : allSources) {
            resultList.addAll(dexMap.get(theSource).values());
        }

        if (resultList.size() > 1) {
            Collections.sort(resultList, Collections.reverseOrder(DEFAULT_PRIORITIZER));
        }

        return Optional.ofNullable(new DexFileProvider().createClassSource(this, apkPath, type));
    }

    @Nonnull
    @Override
    public Collection<? extends AbstractClassSource<JavaSootClass>> getClassSources(@Nonnull View<?> view) {
        // TODO code here

        return null;
    }

    /**
     * Returns all dex files found in dex source sorted by the default dex prioritizer
     *
     * @param dexSource
     *          Path to a jar, apk, dex, odex or a directory containing multiple dex files
     * @return List of dex files derived from source
     */
    public List<DexFileProvider.DexContainer<? extends DexFile>> getDexFromSource(Path dexSource) throws IOException {
        return getDexFromSource(dexSource, DEFAULT_PRIORITIZER);
    }

    /**
     * Returns all dex files found in dex source sorted by the default dex prioritizer
     *
     * @param dexSource
     *          Path to a jar, apk, dex, odex or a directory containing multiple dex files
     * @param prioritizer
     *          A comparator that defines the ordering of dex files in the result list
     * @return List of dex files derived from source
     */
    public List<DexFileProvider.DexContainer<? extends DexFile>> getDexFromSource(Path dexSource,
                                                                  Comparator<DexFileProvider.DexContainer<? extends DexFile>> prioritizer) throws IOException {
        ArrayList<DexFileProvider.DexContainer<? extends DexFile>> resultList = new ArrayList<>();
        List<Path> allSources = allSourcesFromFile(dexSource);
        updateIndex(allSources);

        for (Path theSource : allSources) {
            resultList.addAll(dexMap.get(theSource).values());
        }

        if (resultList.size() > 1) {
            Collections.sort(resultList, Collections.reverseOrder(prioritizer));
        }
        return resultList;
    }

    /**
     * Returns the first dex file with the given name found in the given dex source
     *
     * @param dexSource
     *          Path to a jar, apk, dex, odex or a directory containing multiple dex files
     * @return Dex file with given name in dex source
     * @throws ResolveException
     *           If no dex file with the given name exists
     */
    public DexFileProvider.DexContainer<? extends DexFile> getDexFromSource(Path dexSource, String dexName) throws IOException {
        List<Path> allSources = allSourcesFromFile(dexSource);
        updateIndex(allSources);

        // we take the first dex we find with the given name
        for (Path theSource : allSources) {
            DexFileProvider.DexContainer<? extends DexFile> dexFile = dexMap.get(theSource).get(dexName);
            if (dexFile != null) {
                return dexFile;
            }
        }

        throw new ResolveException("Dex file with name '" + dexName + "' not found in " + dexSource, dexSource);
    }

    private List<Path> allSourcesFromFile(Path dexSource) {
        if (Files.isDirectory(dexSource)) {
            List<Path> dexFiles = getAllDexFilesInDirectory(dexSource);
            if (dexFiles.size() > 1 && !process_multiple_dex) {
                Path path = dexFiles.get(0);
                logger.warn("Multiple dex files detected, only processing '" + path
                        + "'. Use '-process-multiple-dex' option to process them all.");
                return Collections.singletonList(path);
            } else {
                return dexFiles;
            }
        } else {
            String ext = com.google.common.io.Files.getFileExtension(dexSource.getFileName().toString()).toLowerCase();
            if ((ext.equals("jar") || ext.equals("zip")) && !search_dex_in_archives) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList(dexSource);
            }
        }
    }

    private void updateIndex(List<Path> dexSources) {
        for (Path theSource : dexSources) {
            Map<Path, DexFileProvider.DexContainer<? extends DexFile>> dexFiles = dexMap.get(theSource);
            if (dexFiles == null) {
                try {
                    dexFiles = mappingForFile(theSource);
                    dexMap.put(theSource.toString(), dexFiles);
                } catch (IOException e) {
                    throw new ResolveException("Error parsing dex source ", theSource, e);
                }
            }
        }
    }

    /**
     * @param dexSourceFile
     *          A file containing either one or multiple dex files (apk, zip, etc.) but no directory!
     * @return
     * @throws IOException
     */
    private Map<Path, DexFileProvider.DexContainer<? extends DexFile>> mappingForFile(Path dexSourceFile) throws IOException {
        int api = android_api_version;
        boolean multiple_dex = process_multiple_dex;

        // load dex files from apk/folder/file
        MultiDexContainer<? extends DexBackedDexFile> dexContainer
                = DexFileFactory.loadDexContainer(dexSourceFile.toFile(), Opcodes.forApi(api));

        List<String> dexEntryNameList = dexContainer.getDexEntryNames();
        int dexFileCount = dexEntryNameList.size();

        if (dexFileCount < 1) {
            if (verbose) {
                logger.debug("" + String.format("Warning: No dex file found in '%s'", dexSourceFile));
            }
            return Collections.emptyMap();
        }

        Map<Path, DexFileProvider.DexContainer<? extends DexFile>> dexMap = new HashMap<>(dexFileCount);

        // report found dex files and add to list.
        // We do this in reverse order to make sure that we add the first entry if there is no classes.dex file in single dex
        // mode
        ListIterator<String> entryNameIterator = dexEntryNameList.listIterator(dexFileCount);
        while (entryNameIterator.hasPrevious()) {
            String entryName = entryNameIterator.previous();
            Path entryPath = Paths.get(entryName);
            MultiDexContainer.DexEntry<? extends DexFile> entry = dexContainer.getEntry(entryName);
            logger.debug("" + String.format("Found dex file '%s' with %d classes in '%s'", entryName,
                    entry.getDexFile().getClasses().size(), dexSourceFile));

            if (multiple_dex) {
                dexMap.put(entryPath, new DexFileProvider.DexContainer<>(entry, entryName, dexSourceFile));
            } else if (dexMap.isEmpty() && (entryName.equals("classes.dex") || !entryNameIterator.hasPrevious())) {
                // We prefer to have classes.dex in single dex mode.
                // If we haven't found a classes.dex until the last element, take the last!
                dexMap = Collections.singletonMap(entryPath, new DexFileProvider.DexContainer<>(entry, entryName, dexSourceFile));
                if (dexFileCount > 1) {
                    logger.warn("Multiple dex files detected, only processing '" + entryName
                            + "'. Use '-process-multiple-dex' option to process them all.");
                }
            }
        }
        return Collections.unmodifiableMap(dexMap);
    }

    private List<Path> getAllDexFilesInDirectory(Path path) {
        try {
            return Files.walk(path).filter(p-> {
                return p.toString().endsWith(".dex") && Files.isDirectory(p);
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ResolveException("Error while finding .dex file",path,e);
        }
    }

    public void initialize() {
        // resolve classes in dex files
        for (MultiDexContainer.DexEntry<? extends DexFile> dexEntry : dexFiles) {
            final DexFile dexFile = dexEntry.getDexFile();
            for (ClassDef defItem : dexFile.getClasses()) {
                String forClassName = Util.dottedClassName(defItem.getType());
                classesToDefItems.put(forClassName, new ClassInformation(dexEntry, defItem));
            }
        }

    }

    private final static Set<String> systemAnnotationNames;

    static {
        Set<String> systemAnnotationNamesModifiable = new HashSet<String>();
        // names as defined in the ".dex - Dalvik Executable Format" document
        systemAnnotationNamesModifiable.add("dalvik.annotation.AnnotationDefault");
        systemAnnotationNamesModifiable.add("dalvik.annotation.EnclosingClass");
        systemAnnotationNamesModifiable.add("dalvik.annotation.EnclosingMethod");
        systemAnnotationNamesModifiable.add("dalvik.annotation.InnerClass");
        systemAnnotationNamesModifiable.add("dalvik.annotation.MemberClasses");
        systemAnnotationNamesModifiable.add("dalvik.annotation.Signature");
        systemAnnotationNamesModifiable.add("dalvik.annotation.Throws");
        systemAnnotationNames = Collections.unmodifiableSet(systemAnnotationNamesModifiable);
    }

    private final DexClassLoader dexLoader = new DexClassLoader();

    private static class ClassInformation {
        public MultiDexContainer.DexEntry<? extends DexFile> dexEntry;
        public ClassDef classDefinition;

        public ClassInformation(MultiDexContainer.DexEntry<? extends DexFile> entry, ClassDef classDef) {
            this.dexEntry = entry;
            this.classDefinition = classDef;
        }
    }

    private final Map<String, ClassInformation> classesToDefItems = new HashMap<String, ClassInformation>();
    private final Collection<MultiDexContainer.DexEntry<? extends DexFile>> dexFiles;

    /**
     * Construct a DexlibWrapper from a dex file and stores its classes referenced by their name. No further process is done
     * here.
     */
    public ApkAnalysisInputLocation(Path dexSource) {
        this.apkPath = dexSource;
        try {
            List<DexFileProvider.DexContainer<? extends DexFile>> containers = new DexFileProvider().getDexFromSource(dexSource);
            this.dexFiles = new ArrayList<>(containers.size());
            for (DexFileProvider.DexContainer<? extends DexFile> container : containers) {
                this.dexFiles.add(container.getBase());
            }
        } catch (IOException e) {
            throw new ResolveException("IOException during dex parsing", dexSource);
        }
    }

}