package com.strobel.assembler.metadata;

import com.strobel.assembler.Collection;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.IFreezable;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class MutableMethodDefinition extends MethodDefinition implements IFreezable {
    final Collection<CustomAnnotation> customAnnotations;
    final Collection<GenericParameter> genericParameters;
    final Collection<TypeReference> thrownTypes;
    final ParameterDefinitionCollection parameters;

    private int _flags;
    private String _name;
    private TypeReference _returnType;
    private TypeReference _declaringType;
    private boolean _isFrozen;

    public MutableMethodDefinition() {
        customAnnotations = new Collection<>();
        genericParameters = new GenericParameterCollection(this);
        parameters = new ParameterDefinitionCollection(this);
        thrownTypes = new Collection<>();
    }

    @Override
    public TypeReference getDeclaringType() {
        return _declaringType;
    }

    @Override
    public long getFlags() {
        return _flags;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public TypeReference getReturnType() {
        return _returnType;
    }

    @Override
    public List<ParameterDefinition> getParameters() {
        return parameters;
    }

    @Override
    public boolean hasAnnotations() {
        return !customAnnotations.isEmpty();
    }

    @Override
    public List<CustomAnnotation> getAnnotations() {
        return customAnnotations;
    }

    @Override
    public List<TypeReference> getThrownTypes() {
        return thrownTypes;
    }

    public void setFlags(final int flags) {
        verifyNotFrozen();
        _flags = flags;
    }

    public void setReturnType(final TypeReference returnType) {
        verifyNotFrozen();
        _returnType = returnType;
    }

    public void setName(final String name) {
        verifyNotFrozen();
        _name = name;
    }

    public void setDeclaringType(final TypeReference declaringType) {
        verifyNotFrozen();
        _declaringType = declaringType;
    }

    @Override
    protected StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        return sb.append(_name);
    }

    // <editor-fold defaultstate="collapsed" desc="IFreezable Implementation">

    @Override
    public boolean canFreeze() {
        return !isFrozen();
    }

    @Override
    public final boolean isFrozen() {
        return _isFrozen;
    }

    @Override
    public final void freeze() throws IllegalStateException {
        if (!canFreeze()) {
            throw new IllegalStateException(
                "Field cannot be frozen.  Be sure to check canFreeze() before calling " +
                "freeze(), or use the tryFreeze() method instead."
            );
        }

        freezeCore();

        _isFrozen = true;
    }

    protected void freezeCore() {
        customAnnotations.freeze();
    }

    protected final void verifyNotFrozen() {
        if (isFrozen()) {
            throw new IllegalStateException("Frozen field definition cannot be modified.");
        }
    }

    protected final void verifyFrozen() {
        if (!isFrozen()) {
            throw new IllegalStateException(
                "Field must be frozen before performing this operation."
            );
        }
    }

    @Override
    public final boolean tryFreeze() {
        if (!canFreeze()) {
            return false;
        }

        try {
            freeze();
            return true;
        }
        catch (final Throwable t) {
            return false;
        }
    }

    @Override
    public final void freezeIfUnfrozen() throws IllegalStateException {
        if (isFrozen()) {
            return;
        }
        freeze();
    }

    // </editor-fold>
}