package de.cacheoverflow.rustnativerustplugin.rust.analyer;

import de.cacheoverflow.rustnativerustplugin.rust.parser.RustParser;
import de.cacheoverflow.rustnativerustplugin.rust.parser.RustParserBaseListener;
import de.cacheoverflow.rustnativerustplugin.utils.NullableHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SourceFileAnalyzerListener extends RustParserBaseListener {

    private final List<String> attributeCache = new ArrayList<>();
    private final List<RustFunction> functions;
    private final List<RustStruct> structures;

    public SourceFileAnalyzerListener(@NotNull final List<RustFunction> functions, @NotNull final List<RustStruct> structures) {
        this.functions = functions;
        this.structures = structures;
    }

    @Override
    public void enterFunction_(RustParser.Function_Context context) {
        // Get return type
        Optional<String> returnType = NullableHelper.successOrElse(() -> context.functionReturnType().type_()
                .getText(), NullPointerException.class, null);

        // Parse Parameters
        Map<String, String> parameters = new HashMap<>();
        if (context.functionParameters() != null) {
            for (RustParser.FunctionParamContext parameterContext : context.functionParameters().functionParam()) {
                parameters.put(parameterContext.functionParamPattern().getText(), parameterContext.type_().getText());
            }
        }

        // Add function to list and clear attribute cache
        this.functions.add(new RustFunction(this.attributeCache, context.identifier().getText(), parameters, returnType));
        this.attributeCache.clear();
    }

    @Override
    public void enterStruct_(RustParser.Struct_Context context) {
        // Parse Parameters
        Map<String, String> parameters = new HashMap<>();
        if (context.structStruct().structFields() != null) {
            for (RustParser.StructFieldContext fieldContext : context.structStruct().structFields().structField()) {
                parameters.put(fieldContext.identifier().getText(), fieldContext.type_().getText());
            }
        }

        // Add struct to list and clear attribute cache
        this.structures.add(new RustStruct(this.attributeCache, context.structStruct().identifier().getText(), parameters));
        this.attributeCache.clear();
    }

    @Override
    public void enterOuterAttribute(RustParser.OuterAttributeContext attributeContext) {
        this.attributeCache.add(attributeContext.attr().simplePath().getText());
    }

}
