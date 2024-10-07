package xyz.lesnik.zerobs;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class TestAnnotationProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        processingEnv.getMessager().printNote("Match: @Executable");
        return Set.of(Executable.class.getCanonicalName());
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        processingEnv.getMessager().printNote("Init annotation processing");
//        System.out.println(processingEnv.);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printNote("TestAnnotationProcessor: Annotation processing started");
        processingEnv.getMessager().printNote("Supported compile versions: ");
//        ToolProvider.getSystemJavaCompiler().getSourceVersions().forEach(sv -> {
//            processingEnv.getMessager().printNote(sv.name());
//        });
        if (!roundEnv.getRootElements().isEmpty()) {
            return true;
        }
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final StandardJavaFileManager manager = jc.getStandardFileManager(diagnostics, null, null);
        final File main = new File("src/xyz/lesnik/zerobs/Main.java");
        final File executable = new File("src/xyz/lesnik/zerobs/Executable.java");
        processingEnv.getMessager().printNote("Main.java exists: " + main.exists());
        final Iterable<? extends JavaFileObject> sources =
                manager.getJavaFileObjectsFromFiles(Arrays.asList(main, executable));
        try (Writer cw = new FileWriter("compile.out") ){
            final JavaCompiler.CompilationTask task = jc.getTask(null, manager, diagnostics,
                    null, null, sources);
            Boolean call = task.call();
            processingEnv.getMessager().printNote("Main.java compilation success: " + call);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String pathname = "src/xyz/lesnik/zerobs/Main.class";
        File compiledFile = new File(pathname);
        boolean exists = compiledFile.exists();
        processingEnv.getMessager().printNote(String.format("File exists by path: %s : %B", pathname, exists));
        if (exists) processingEnv.getMessager().printNote(String.format("File size: %d bytes", compiledFile.length()));
        try {
            FileObject resource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "xyz.lesnik.zerobs", "out.jar");
            try (JarOutputStream jos = new JarOutputStream(resource.openOutputStream())) {
                jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "xyz.lesnik.zerobs.Main");
                manifest.write(jos);
                jos.closeEntry();
                jos.putNextEntry(new JarEntry("xyz/lesnik/zerobs/Main.class"));
                Files.copy(Path.of(pathname), jos);
                jos.closeEntry();
                jos.putNextEntry(new JarEntry("xyz/lesnik/zerobs/Executable.class"));
                Files.copy(Path.of("src/xyz/lesnik/zerobs/Executable.class"), jos);
                jos.closeEntry();
                processingEnv.getMessager().printNote("Jar main created");
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        processingEnv.getMessager().printNote("TestAnnotationProcessor: Annotation processing ended");
        return true;
    }
}
