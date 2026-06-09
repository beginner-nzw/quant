package com.quant.aiorchestrationservice;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleDependencyBoundaryTests {

    @Test
    void aiDoesNotCompileAgainstConcreteBusinessServices() throws Exception {
        Document pom = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Files.newInputStream(resolveModuleRoot().resolve("pom.xml")));

        Map<String, String> testOnlyBusinessDependencies = Map.of(
                "report-service", "report APIs must be injected through quant-api-report contracts",
                "risk-service", "risk APIs must be injected through quant-api-risk contracts",
                "strategy-service", "strategy APIs must be injected through quant-api-risk contracts",
                "market-event-service", "market event runtime should not be an AI compile dependency",
                "research-task-service", "task APIs must be injected through quant-api-task contracts",
                "data-ingest-service", "data ingest runtime should not be an AI compile dependency",
                "audit-service", "audit runtime should not be an AI compile dependency",
                "config-service", "config runtime should not be an AI compile dependency"
        );

        NodeList dependencies = pom.getElementsByTagName("dependency");
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            String artifactId = textOf(dependency, "artifactId");
            if (testOnlyBusinessDependencies.containsKey(artifactId)) {
                assertEquals("test", textOf(dependency, "scope"),
                        artifactId + " should stay test-scoped: " + testOnlyBusinessDependencies.get(artifactId));
            }
        }
    }

    private String textOf(Element dependency, String tagName) {
        NodeList values = dependency.getElementsByTagName(tagName);
        if (values.getLength() == 0) {
            return "";
        }
        return values.item(0).getTextContent().trim();
    }

    private Path resolveModuleRoot() {
        Path moduleRoot = Path.of(".");
        if (Files.exists(moduleRoot.resolve("pom.xml"))
                && Files.exists(moduleRoot.resolve("src/main/java"))) {
            return moduleRoot;
        }
        return Path.of("quant-business/ai-orchestration-service");
    }
}
