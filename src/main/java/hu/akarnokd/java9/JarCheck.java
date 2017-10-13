package hu.akarnokd.java9;

import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class JarCheck {
    public static void main(String[] args) throws Exception {
        try (JarFile jar = new JarFile("c:\\Users\\akarnokd\\Downloads\\reactor-core-3.1.0.RELEASE-sources.jar")) {
            jar.stream()
                    .collect(Collectors.groupingBy(e -> e.getName()))
            .entrySet()
            .stream()
            .filter(e -> e.getValue().size() > 1)
            .forEach(System.out::println);
        }
    }
}
