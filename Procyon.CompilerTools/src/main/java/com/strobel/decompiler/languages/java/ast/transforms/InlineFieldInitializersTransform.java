package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.AnyNode;
import com.strobel.decompiler.patterns.Choice;
import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.MemberReferenceTypeNode;
import com.strobel.decompiler.patterns.Pattern;
import com.strobel.decompiler.patterns.TypedNode;

import java.util.HashMap;
import java.util.Map;

import static com.strobel.core.CollectionUtilities.firstOrDefault;

public class InlineFieldInitializersTransform extends ContextTrackingVisitor<Void> {
    private final Map<String, FieldDeclaration> _fieldDeclarations;
    private final Map<String, AssignmentExpression> _initializers;

    private MethodDefinition _currentStaticInitializer;

    public InlineFieldInitializersTransform(final DecompilerContext context) {
        super(context);

        _fieldDeclarations = new HashMap<>();
        _initializers = new HashMap<>();
    }

    @Override
    public void run(final AstNode compilationUnit) {
        new ContextTrackingVisitor<Void>(context) {
            @Override
            public Void visitFieldDeclaration(final FieldDeclaration node, final Void _) {
                final FieldDefinition field = node.getUserData(Keys.FIELD_DEFINITION);

                if (field != null) {
                    _fieldDeclarations.put(field.getFullName(), node);
                }

                return super.visitFieldDeclaration(node, _);
            }
        }.run(compilationUnit);

        super.run(compilationUnit);

        inlineInitializers();
    }

    private void inlineInitializers() {
        for (final String fieldName : _initializers.keySet()) {
            final FieldDeclaration declaration = _fieldDeclarations.get(fieldName);

            if (declaration != null &&
                declaration.getVariables().firstOrNullObject().getInitializer().isNull()) {

                final AssignmentExpression assignment = _initializers.get(fieldName);
                final Expression value = assignment.getRight();

                value.remove();
                declaration.getVariables().firstOrNullObject().setInitializer(value);

                final AstNode parent = assignment.getParent();

                if (parent instanceof ExpressionStatement) {
                    parent.remove();
                }
                else {
                    final Expression left = assignment.getLeft();

                    left.remove();
                    parent.replaceWith(left);
                }
            }
        }
    }

    @Override
    public Void visitMethodDeclaration(final MethodDeclaration node, final Void _) {
        final MethodDefinition oldInitializer = _currentStaticInitializer;
        final MethodDefinition method = node.getUserData(Keys.METHOD_DEFINITION);

        if (method != null &&
            method.isTypeInitializer() &&
            method.getDeclaringType().isInterface()) {

            _currentStaticInitializer = method;
        }
        else {
            _currentStaticInitializer = null;
        }

        try {
            return super.visitMethodDeclaration(node, _);
        }
        finally {
            _currentStaticInitializer = oldInitializer;
        }
    }

    private final static INode STATIC_FIELD_ASSIGNMENT;

    static {
        STATIC_FIELD_ASSIGNMENT = new Choice(
            new AssignmentExpression(
                new MemberReferenceTypeNode(
                    "target",
                    new Choice(
                        new MemberReferenceExpression(
                            new TypedNode(AstType.class).toExpression(),
                            Pattern.ANY_STRING
                        ),
                        new IdentifierExpression(Pattern.ANY_STRING)
                    ).toExpression(),
                    FieldReference.class
                ).toExpression(),
                AssignmentOperatorType.ASSIGN,
                new AnyNode("value").toExpression()
            )
        );
    }

    @Override
    public Void visitAssignmentExpression(final AssignmentExpression node, final Void data) {
        super.visitAssignmentExpression(node, data);

        if (_currentStaticInitializer == null) {
            return null;
        }

        final Match match = STATIC_FIELD_ASSIGNMENT.match(node);

        if (match.success()) {
            final Expression target = (Expression) firstOrDefault(match.get("target"));
            final MemberReference reference = target.getUserData(Keys.MEMBER_REFERENCE);

            _initializers.put(reference.getFullName(), node);
        }

        return null;
    }
}