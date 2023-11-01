package de.cacheoverflow.reactnativerustplugin.rust.analyer;

import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.RustAttribute;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.RustFunction;
import de.cacheoverflow.reactnativerustplugin.rust.analyer.data.RustStruct;
import de.cacheoverflow.reactnativerustplugin.rust.parser.RustParser;
import de.cacheoverflow.reactnativerustplugin.rust.parser.RustParserBaseListener;
import de.cacheoverflow.reactnativerustplugin.utils.NullableHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SourceFileAnalyzerListener extends RustParserBaseListener {

    private final List<RustAttribute> attributeCache = new ArrayList<>();
    private final List<RustFunction> functions;
    private final List<RustStruct> structures;
    private final List<String> imports;

    public SourceFileAnalyzerListener(@NotNull final List<RustFunction> functions,
                                      @NotNull final List<RustStruct> structures, @NotNull final List<String> imports) {
        this.functions = functions;
        this.structures = structures;
        this.imports = imports;
    }

    @Override
    public void enterUseDeclaration(RustParser.UseDeclarationContext context) {
        this.imports.add(context.useTree().getText());
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
        this.functions.add(new RustFunction(new ArrayList<>(this.attributeCache), context.identifier().getText(),
                parameters, returnType));
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
        this.structures.add(new RustStruct(new ArrayList<>(this.attributeCache),
                context.structStruct().identifier().getText(), parameters));
        this.attributeCache.clear();
    }

    @Override
    public void enterOuterAttribute(RustParser.OuterAttributeContext context) {
        Map<String, String> parameters = new HashMap<>();
        if (context.attr().attrInput() != null) {
            for (RustParser.TokenTreeContext tokenTreeContext : context.attr().attrInput().delimTokenTree().tokenTree()) {
                if (tokenTreeContext.tokenTreeToken().stream().noneMatch(current -> current.getText().equals("=")))
                    continue;

                if (tokenTreeContext.tokenTreeToken().size() != 3)
                    continue;

                parameters.put(tokenTreeContext.tokenTreeToken().getFirst().getText(),
                        tokenTreeContext.tokenTreeToken().getLast().getText());
            }
        }
        this.attributeCache.add(new RustAttribute(context.attr().simplePath().getText(), parameters));
    }

}
