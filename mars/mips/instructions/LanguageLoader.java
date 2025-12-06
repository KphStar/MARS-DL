package mars.mips.instructions;

import mars.*;
import mars.util.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;   // 🔹 add this
import java.net.URLClassLoader;
import java.net.URL;

/**
 * Handles all user-defined instruction sets. Loads JAR files containing classes which extend CustomAssembly from the mars/mips/instructions/customlangs directory
 * and adds them to the Language Switcher.
 * @see CustomAssembly
 * @see assemblyList
 */
public class LanguageLoader{
    private static final String CUSTOM_LANG_DIRECTORY = "mars/mips/instructions/customlangs";
    private static final String JAR_EXTENSION = "jar";
    private static final String EXAMPLE_FILE = "ExampleCustomLanguage";

    private static ArrayList<BasicInstruction> finalInstructionList = new ArrayList<BasicInstruction>();
    public static ArrayList<CustomAssembly> assemblyList = new ArrayList<CustomAssembly>(){{
        MipsAssembly m = new MipsAssembly();
        add(m);

        ArrayList<String> langCandidates =
            FilenameFinder.getFilenameList(m.getClass().getClassLoader(),
                                           CUSTOM_LANG_DIRECTORY, JAR_EXTENSION);
        HashSet<String> languages = new HashSet<String>();

        for (String file : langCandidates){
            // Ensure duplicates are not loaded
            if (languages.contains(file)){
                continue;
            } else{
                languages.add(file);
            }

            try {
                String jarPath = CUSTOM_LANG_DIRECTORY + "/" + file;
                File f = new File(jarPath);

                // Create a class loader that can load classes from our JAR files
                URL[] urls = {f.toURI().toURL()};
                URLClassLoader cl = URLClassLoader.newInstance(urls);

                JarFile jarFile = new JarFile(f);
                Enumeration<JarEntry> e = jarFile.entries();

                while (e.hasMoreElements()){
                    JarEntry je = e.nextElement();
                    if (!je.getName().endsWith(".class")){
                        continue;
                    }

                    String className = je.getName()
                                         .replace(".class", "")
                                         .replace("/", ".");
                    Class<?> langClass = cl.loadClass(className);

                    // Optionally skip example class
                    /*
                    if (className.endsWith(EXAMPLE_FILE))
                        continue;
                    */

                    // Skip classes that do not implement CustomAssembly
                    if (!CustomAssembly.class.isAssignableFrom(langClass)){
                        continue;
                    }

                    // 🔹 Skip abstract classes and interfaces
                    if (Modifier.isAbstract(langClass.getModifiers()) || langClass.isInterface()) {
                        System.out.println("Skipping abstract/interface CustomAssembly: " + className);
                        continue;
                    }

                    try {
                        // 🔹 Get a no-arg constructor and create instance
                        Constructor<?> ctor = langClass.getDeclaredConstructor();
                        ctor.setAccessible(true);
                        CustomAssembly instance = (CustomAssembly) ctor.newInstance();
                        add(instance);
                        System.out.println("Loaded custom assembly: " + className +
                                           " from JAR: " + file);
                    } catch (Exception ex) {
                        System.out.println("Error instantiating CustomAssembly class " +
                                           className + " from file " + file + ": " + ex);
                    }
                }
                jarFile.close();
            } catch(Exception e){
                System.out.println("Error scanning file " + file + ": " + e);
            }
        }
    }};

    /**
    * Merges all enabled custom instruction sets into the main instruction set that the simulator reads from.
    * @param instrList The global instruction list.
    */
    public static void mergeCustomInstructions(ArrayList<BasicInstruction> instrList){
        boolean pseudo = false;
        finalInstructionList.clear();
        for (CustomAssembly c : assemblyList){
            if (c.enabled){
                c.addCustomInstructions(finalInstructionList);
                if (c instanceof MipsAssembly){
                    pseudo = true;
                }
            }
        }

        instrList.addAll(finalInstructionList);
        if (pseudo == true){
            Globals.instructionSet.addPseudoInstructions();
        }
    }
}
