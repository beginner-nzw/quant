package com.quant.aiapi;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentedMavenAggregateBoundaryTests {

    private static final Path ROOT = Path.of(System.getProperty("user.dir"))
            .resolve("..")
            .resolve("..")
            .normalize();

    private static final Set<String> DOCUMENTED_MODULES = Set.of(
            "quant-common/quant-common-core",
            "quant-common/quant-common-web",
            "quant-common/quant-common-mybatis",
            "quant-common/quant-common-kafka",
            "quant-common/quant-common-redis",
            "quant-common/quant-common-security",
            "quant-common/quant-common-sentinel",
            "quant-common/quant-common-model",
            "quant-api/quant-api-user",
            "quant-api/quant-api-task",
            "quant-api/quant-api-risk",
            "quant-api/quant-api-report",
            "quant-api/quant-api-ai",
            "quant-business/user-service",
            "quant-business/market-event-service",
            "quant-business/research-task-service",
            "quant-business/ai-orchestration-service",
            "quant-business/strategy-service",
            "quant-business/risk-service",
            "quant-business/report-service",
            "quant-business/audit-service",
            "quant-business/subscription-service",
            "quant-business/data-ingest-service",
            "quant-business/config-service",
            "quant-job/dashboard-metric-job",
            "quant-job/cache-refresh-job",
            "quant-job/retry-compensation-job"
    );

    @Test
    void rootPomAggregatesEveryModuleFromCodeStructureDocument() throws Exception {
        Set<String> modules = readModules(ROOT.resolve("pom.xml"));

        assertTrue(modules.containsAll(DOCUMENTED_MODULES),
                () -> "missing documented modules: " + missing(DOCUMENTED_MODULES, modules));
    }

    @Test
    void concreteBusinessServicesDoNotCompileDependOnEachOther() throws Exception {
        Set<String> violations = new LinkedHashSet<>();
        for (Path pom : Files.list(ROOT.resolve("quant-business"))
                .filter(path -> Files.isDirectory(path))
                .map(path -> path.resolve("pom.xml"))
                .filter(Files::exists)
                .toList()) {
            String module = pom.getParent().getFileName().toString();
            for (Dependency dependency : readDependencies(pom)) {
                if ("com.quant".equals(dependency.groupId())
                        && dependency.artifactId().endsWith("-service")
                        && !"test".equals(dependency.scope())) {
                    violations.add(module + " -> " + dependency.artifactId());
                }
            }
        }

        assertEquals(Set.of(), violations,
                "concrete quant-business services must communicate through quant-api contracts");
    }

    private static Set<String> readModules(Path pom) throws Exception {
        Document document = parse(pom);
        NodeList moduleNodes = document.getElementsByTagName("module");
        Set<String> modules = new LinkedHashSet<>();
        for (int i = 0; i < moduleNodes.getLength(); i++) {
            modules.add(moduleNodes.item(i).getTextContent().trim().replace('\\', '/'));
        }
        return modules;
    }

    private static Set<Dependency> readDependencies(Path pom) throws Exception {
        Document document = parse(pom);
        NodeList dependencyNodes = document.getElementsByTagName("dependency");
        Set<Dependency> dependencies = new LinkedHashSet<>();
        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            Element dependencyNode = (Element) dependencyNodes.item(i);
            dependencies.add(new Dependency(
                    childText(dependencyNode, "groupId"),
                    childText(dependencyNode, "artifactId"),
                    childText(dependencyNode, "scope")
            ));
        }
        return dependencies;
    }

    private static Document parse(Path pom) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(pom.toFile());
    }

    private static String childText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private static Set<String> missing(Set<String> expected, Set<String> actual) {
        Set<String> missing = new LinkedHashSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }

    private record Dependency(String groupId, String artifactId, String scope) {
    }
}
