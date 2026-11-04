package io.github.renanbacheschi.aguiabranca.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JsonSchemaContractTests {
    @Test void validatorRejectsDeliberatelyIncompatibleIdeaResponse() throws Exception {
        var schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(getClass().getResourceAsStream("/schemas/idea.json"));
        var incompatible = new ObjectMapper().readTree("{\"id\":42,\"status\":true}");
        Assertions.assertFalse(schema.validate(incompatible).isEmpty());
    }
}
