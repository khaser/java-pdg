package ru.itmo.khaser.java_pdg;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java HelloWorld <java-file> [method-name]");
            System.err.println("If method-name is not provided, the first method will be used.");
            System.exit(1);
        }

        String filePath = args[0];
        String methodName = args.length > 1 ? args[1] : null;

        try {
            String code = new String(Files.readAllBytes(Paths.get(filePath)));

            JavaParser parser = new JavaParser();
            CompilationUnit cu = parser.parse(code).getResult().orElseThrow(
                () -> new RuntimeException("Failed to parse the file")
            );

            Optional<MethodDeclaration> methodOpt;
            if (methodName != null) {
                methodOpt = cu.findAll(MethodDeclaration.class).stream()
                    .filter(m -> m.getNameAsString().equals(methodName))
                    .findFirst();
            } else {
                methodOpt = cu.findFirst(MethodDeclaration.class);
            }

            if (!methodOpt.isPresent()) {
                System.err.println("Method not found: " + (methodName != null ? methodName : "(any)"));
                System.exit(1);
            }

            MethodDeclaration method = methodOpt.get();

            PDGBuilder builder = new PDGBuilder(method);
            PDG pdg = builder.build();

            DotExporter exporter = new DotExporter();
            String dot = exporter.export(pdg);
            System.out.println(dot);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

