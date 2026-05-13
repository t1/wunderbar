package com.github.t1.wunderbar.junit;

import java.util.Locale;

/// The format used to store or load consumer-driven contract files.
public enum ContractFormat {
    /// Infer the format from the file name, when possible
    AUTO {
        @Override public ContractFormat resolve(String fileName) {
            var lowerCaseFileName = fileName.toLowerCase(Locale.ROOT);
            if (lowerCaseFileName.endsWith(".json")) return OPENAPI;
            if (lowerCaseFileName.endsWith(".yaml") || lowerCaseFileName.endsWith(".yml"))
                throw new IllegalArgumentException("OpenAPI YAML files are not supported yet; use JSON instead: " + fileName);
            return BAR;
        }

        @Override public String defaultPackaging() {throw new IllegalStateException("AUTO has no default packaging");}

        @Override public String defaultClassifier() {throw new IllegalStateException("AUTO has no default classifier");}
    },

    /// WunderBar's full-fidelity native format
    BAR {
        @Override public ContractFormat resolve(String fileName) {return this;}

        @Override public String defaultPackaging() {return "bar";}

        @Override public String defaultClassifier() {return "bar";}
    },

    /// A JSON OpenAPI document.
    ///
    /// This format is intentionally lossy compared to [#BAR]. It is suitable for simple single-interaction cases
    /// and interoperability with other tools, but it does not preserve all WunderBar semantics. When exporting,
    /// WunderBar logs warnings about dropped fidelity where possible. When importing, only a simple subset of OpenAPI
    /// can be consumed reliably.
    OPENAPI {
        @Override public ContractFormat resolve(String fileName) {return this;}

        @Override public String defaultPackaging() {return "json";}

        @Override public String defaultClassifier() {return "openapi";}
    };

    public abstract ContractFormat resolve(String fileName);

    public abstract String defaultPackaging();

    public abstract String defaultClassifier();
}
