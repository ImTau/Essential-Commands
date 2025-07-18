package com.fibermc.essentialcommands.text;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fibermc.essentialcommands.types.IStyleProvider;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TagLikeParser;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

public class ECTextImpl extends ECText {
    private final ParserContext parserContext;

    public ECTextImpl(
        Map<String, String> stringMap,
        ParserContext parserContext)
    {
        super(stringMap);
        // In normal operation, `server` should always be present. For testing and other contexts,
        // that is not guaranteed. This is admittedly a bit hacky.
        this.parserContext = parserContext;
    }

    public static ECText forServer(Map<String, String> stringMap, MinecraftServer server) {
        return new ECTextImpl(
            stringMap,
            ParserContext.of(PlaceholderContext.KEY, PlaceholderContext.of(server))
        );
    }

    public String getString(String key) {
        return super.stringMap.getOrDefault(key, key);
    }

    // Literals
    public MutableText getTextLiteral(String key, TextFormatType textFormatType) {
        return getTextLiteral(key, textFormatType, null);
    }

    public MutableText getTextLiteral(
        String key,
        TextFormatType textFormatType,
        @Nullable IStyleProvider styleProvider)
    {
        return Text.literal(getString(key))
            .setStyle(styleProvider == null
                ? textFormatType.getStyle()
                : styleProvider.getStyle(textFormatType));
    }

    // Interpolated
    public MutableText getText(String key, Text... args) {
        return getTextInternal(key, TextFormatType.Default, null, args);
    }

    public MutableText getText(String key, TextFormatType textFormatType, Text... args) {
        return getTextInternal(key, textFormatType, null, args);
    }

    public MutableText getText(String key, TextFormatType textFormatType, IStyleProvider styleProvider, Text... args) {
        return getTextInternal(key, textFormatType, styleProvider, args);
    }

    private NodeParser parserForContext(
        TextFormatType textFormatType,
        @Nullable IStyleProvider styleProvider,
        List<MutableText> args)
    {
        return TagLikeParser.of(
            TagLikeParser.PLACEHOLDER_USER,
            TagLikeParser.Provider.placeholderText(placeholderId -> {
                if (placeholderId.startsWith("l:")) {
                    // handling the ${l:lang.key.here} case for interpolating value from elsewhere in language files
                    var idxAndFormattingCode = placeholderId.split(":");
                    if (idxAndFormattingCode.length < 2) {
                        throw new IllegalArgumentException(
                            "Specified lang interpolation prefix ('l'), but no lang key was provided. Expected the form: 'l:lang.key.here'. Received: "
                                + placeholderId);
                    }
                    if (idxAndFormattingCode.length > 3) {
                        throw new IllegalArgumentException("lang string placeholder had an unexpected second ':'. Received: " + placeholderId);
                    }

                    return getTextInternal(idxAndFormattingCode[1], textFormatType, styleProvider);
                }

                if (placeholderId.matches("\\d+")) {
                    // handling the ${1} case for argument interpolation
                    int targetIndex = Integer.parseInt(placeholderId);
                    if (targetIndex > args.size()) {
                        throw new IllegalArgumentException("Invalid 'Argument' placeholder: targeted argument with (0-based) index '" + targetIndex + "' but only " + args.size() + " were present");
                    }
                    return args.get(targetIndex);
                }

                return null;
            })
        );
    }

    private static int hashText(Text text) {
        return Objects.hash(text.getContent(), text.getStyle());
    }

    public MutableText getTextInternal(
        String key,
        TextFormatType textFormatType,
        @Nullable IStyleProvider styleProvider,
        Text... args)
    {
        var argsList = Arrays.stream(args).map(Text::copy).toList();
        var argsHashes = argsList.stream()
            .map(ECTextImpl::hashText)
            .collect(Collectors.toCollection(HashSet::new));

        var parser = parserForContext(textFormatType, styleProvider, argsList);
        var parsedText = parser.parseText(
            getString(key),
            this.parserContext
        );

        var specifiedStyle = styleProvider == null
            ? textFormatType.getStyle()
            : styleProvider.getStyle(textFormatType);

        return visitText(
            parsedText,
            defaultStylesVisitor(
                // currently using reference equality -- if internals of TextPlaceholderAPI change, this might not be ok
                (node) -> argsHashes.contains(hashText(node))
                    ? DEFAULT_ARGUMENT_STYLE
                    : specifiedStyle
            ),
            // we should stop traversal downward in the tree when we hit one of the args
            (node) -> argsHashes.contains(hashText(node))
        );
    }

    interface TextVisitor {
        MutableText accept(Text text);
    }

    private static final Style DEFAULT_ARGUMENT_STYLE = Style.EMPTY.withColor(Formatting.WHITE);

    TextVisitor defaultStylesVisitor(Function<Text, @Nullable Style> defaultStyleProvider) {
        return text -> {
            MutableText txt = text.copy();
            var defaultStyleForText = defaultStyleProvider.apply(txt);
            if (defaultStyleForText != null) {
                txt.setStyle(txt.getStyle().withParent(defaultStyleForText));
            }
            return txt;
        };
    }

    MutableText visitText(Text root, TextVisitor textVisitor, Predicate<Text> shouldStopAfter) {
        var txt = textVisitor.accept(root);
        if (shouldStopAfter.test(root)) {
            return txt;
        }
        var siblings = txt.getSiblings();
        for (int i = 0; i < siblings.size(); i++) {
            // DO NOT USE replaceAll here it may lead to exceptions (may be unsupported)
            siblings.set(i, visitText(siblings.get(i), textVisitor, shouldStopAfter));
        }
        return txt;
    }

    public boolean hasTranslation(String key) {
        return super.stringMap.containsKey(key);
    }

    public boolean isRightToLeft() {
        return false;
    }

    public OrderedText reorder(StringVisitable text) {
        return (visitor) ->
            text.visit((style, string) ->
                TextVisitFactory.visitFormatted(string, style, visitor)
                    ? Optional.empty()
                    : StringVisitable.TERMINATE_VISIT, Style.EMPTY).isPresent();
    }

}
