package io.sapl.analyzer;


import io.sapl.api.interpreter.PolicyEvaluationException;
import io.sapl.api.interpreter.SAPLInterpreter;
import io.sapl.benchmark.PolicyGeneratorConfiguration;
import io.sapl.grammar.sapl.Expression;
import io.sapl.grammar.sapl.SAPL;
import io.sapl.interpreter.DefaultSAPLInterpreter;
import io.sapl.interpreter.functions.AnnotationFunctionContext;
import io.sapl.interpreter.functions.FunctionContext;
import io.sapl.prp.inmemory.indexed.Bool;
import io.sapl.prp.inmemory.indexed.ConjunctiveClause;
import io.sapl.prp.inmemory.indexed.DisjunctiveFormula;
import io.sapl.prp.inmemory.indexed.Literal;
import io.sapl.prp.inmemory.indexed.TreeWalker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@Slf4j
@RequiredArgsConstructor
public class PolicyAnalyzer {

    private final SAPLInterpreter interpreter = new DefaultSAPLInterpreter();
    private static final String POLICY_FILE_GLOB_PATTERN = "*.sapl";

    private final FunctionContext functionCtx = new AnnotationFunctionContext();

    private final String policyPath;

    private final Map<String, SAPL> parsedDocuments = new HashMap<>();

    private final Map<String, SAPL> publishedDocuments = new HashMap<>();

    private final Map<String, DisjunctiveFormula> publishedTargets = new HashMap<>();

    private final Map<String, SAPL> unusableDocuments = new HashMap<>();

    public PolicyGeneratorConfiguration analyzeSaplDocuments() {
        LOGGER.info("analyzing policies in directory {}", policyPath);
        try {
            try (DirectoryStream<Path> stream = Files
                    .newDirectoryStream(Paths.get(policyPath), POLICY_FILE_GLOB_PATTERN)) {
                for (Path filePath : stream) {
                    LOGGER.trace("load: {}", filePath);
                    final SAPL saplDocument = interpreter.parse(Files.newInputStream(filePath));
                    parsedDocuments.put(filePath.toString(), saplDocument);
                }
            }
        } catch (IOException | PolicyEvaluationException e) {
            LOGGER.error("Error while initializing the document index.", e);
        }

        for (Entry<String, SAPL> entry : parsedDocuments.entrySet()) {
            if (entry.getValue() != null) {
                retainDocument(entry.getKey(), entry.getValue());
                retainTarget(entry.getKey(), entry.getValue());
            } else {
                discard(entry.getKey());
            }
        }

        return PolicyGeneratorConfiguration.builder()
                .name("HOSPITAL p" + publishedDocuments.size() + " v" + (int) countVariables())
                .policyCount(publishedDocuments.size())
                .variablePoolCount((int) countVariables())
                .path(policyPath)
                .build();

    }

    private long countVariables() {
        return publishedTargets.values().stream()
                .flatMap(formula -> formula.getClauses().stream())
                .flatMap(clause -> clause.getLiterals().stream())
                .map(Literal::getBool).count();
    }

    private void retainDocument(String documentKey, SAPL sapl) {
        publishedDocuments.put(documentKey, sapl);
    }

    private void retainTarget(String documentKey, SAPL sapl) {
        try {
            Expression targetExpression = sapl.getPolicyElement().getTargetExpression();
            DisjunctiveFormula targetFormula;
            if (targetExpression == null) {
                targetFormula = new DisjunctiveFormula(new ConjunctiveClause(new Literal(new Bool(true))));
            } else {
                Map<String, String> imports = sapl.fetchFunctionImports(functionCtx);
                targetFormula = TreeWalker.walk(targetExpression, imports);
            }
            publishedTargets.put(documentKey, targetFormula);
        } catch (PolicyEvaluationException e) {
            unusableDocuments.put(documentKey, sapl);
        }
    }

    private void discard(String documentKey) {
        publishedDocuments.remove(documentKey);
        publishedTargets.remove(documentKey);
        unusableDocuments.remove(documentKey);
    }
}
