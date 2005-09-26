/* Class.java -- Representation of a Java class.
   Copyright (C) 2005
   Free Software Foundation

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

package gnu.java.lang.reflect;

import java.lang.reflect.*;
import java.util.ArrayList;

class TypeImpl implements Type
{
    Type resolve()
    {
        return this;
    }
}

class TypeVariableImpl extends TypeImpl implements TypeVariable
{
    private GenericDeclaration decl;
    private Type[] bounds;
    private String name;

    TypeVariableImpl(GenericDeclaration decl, Type[] bounds, String name)
    {
        this.decl = decl;
        this.bounds = bounds;
        this.name = name;
    }

    public Type[] getBounds()
    {
        return bounds.clone();
    }

    public GenericDeclaration getGenericDeclaration()
    {
        return decl;
    }

    public String getName()
    {
        return name;
    }

    // TODO implement equals/hashCode
}

class ParameterizedTypeImpl extends TypeImpl implements ParameterizedType
{
    private Type rawType;
    private Type owner;
    private Type[] typeArgs;

    ParameterizedTypeImpl(Type rawType, Type owner, Type[] typeArgs)
    {
        this.rawType = rawType;
        this.owner = owner;
        this.typeArgs = typeArgs;
    }

    public Type[] getActualTypeArguments()
    {
        for (int i = 0; i < typeArgs.length; i++)
        {
            if (typeArgs[i] instanceof TypeImpl)
            {
                typeArgs[i] = ((TypeImpl)typeArgs[i]).resolve();
            }
        }
        return typeArgs.clone();
    }

    public Type getRawType()
    {
        return rawType;
    }

    public Type getOwnerType()
    {
        return owner;
    }

    // TODO implement equals/hashCode
}

class GenericArrayTypeImpl extends TypeImpl  implements GenericArrayType
{
    private Type componentType;

    GenericArrayTypeImpl(Type componentType)
    {
        this.componentType = componentType;
    }

    public Type getGenericComponentType()
    {
        return componentType;
    }

    // TODO implement equals/hashCode
}

class UnresolvedTypeVariable extends TypeImpl  implements Type
{
    private GenericDeclaration decl;
    private String name;

    UnresolvedTypeVariable(GenericDeclaration decl, String name)
    {
        this.decl = decl;
        this.name = name;
    }

    Type resolve()
    {
        GenericDeclaration d = decl;
        while (d != null)
        {
            for (TypeVariable t : d.getTypeParameters())
            {
                if (t.getName().equals(name))
                {
                    return t;
                }
            }
            d = getParent(d);
        }
        throw new MalformedParameterizedTypeException();
    }

    private static GenericDeclaration getParent(GenericDeclaration d)
    {
        if (d instanceof Class)
        {
            Method m = ((Class)d).getEnclosingMethod();
            if (m != null)
            {
                return m;
            }
            Constructor c = ((Class)d).getEnclosingConstructor();
            if (c != null)
            {
                return c;
            }
            return ((Class)d).getEnclosingClass();
        }
        else if (d instanceof Method)
        {
            return ((Method)d).getDeclaringClass();
        }
        else if (d instanceof Constructor)
        {
            return ((Constructor)d).getDeclaringClass();
        }
        else
        {
            // TODO figure out what this represents
            throw new Error();
        }
    }
}

class GenericSignatureParser
{
    private ClassLoader loader;
    private GenericDeclaration container;
    private String signature;
    private int pos;

    GenericSignatureParser(GenericDeclaration container, ClassLoader loader, String signature)
    {
        this.container = container;
        this.loader = loader;
        this.signature = signature;
    }

    TypeVariable[] readFormalTypeParameters()
    {
        consume('<');
        ArrayList<TypeVariable> params = new ArrayList<TypeVariable>();
        do
        {
            // TODO should we handle name clashes?
            params.add(readFormalTypeParameter());
        } while (peekChar() != '>');
        consume('>');
        TypeVariable[] list = new TypeVariable[params.size()];
        params.toArray(list);
        return list;
    }

    private TypeVariable readFormalTypeParameter()
    {
        String identifier = readIdentifier();
        consume(':');
        ArrayList<Type> bounds = new ArrayList<Type>();
        if (peekChar() != ':')
        {
            bounds.add(readFieldTypeSignature());
        }
        else
        {
            bounds.add(java.lang.Object.class);
        }
        while (peekChar() == ':')
        {
            consume(':');
            bounds.add(readFieldTypeSignature());
        }
        return new TypeVariableImpl(container, bounds.toArray(new Type[bounds.size()]), identifier);
    }

    Type readFieldTypeSignature()
    {
        switch (peekChar())
        {
            case 'L':
                return readClassTypeSignature();
            case '[':
                return readArrayTypeSignature();
            case 'T':
                return readTypeVariableSignature();
            default:
                throw new GenericSignatureFormatError();
        }
    }

    private Class resolveClass(String className)
    {
        try
        {
            return Class.forName(className, false, loader);
        }
        catch (ClassNotFoundException x)
        {
            throw new TypeNotPresentException(className, x);
        }
    }

    Type readClassTypeSignature()
    {
        consume('L');
        String className = "";
        for (;;)
        {
            String part = readIdentifier();
            if (peekChar() != '/')
            {
                className += part;
                break;
            }
            consume('/');
            className += part + ".";
        }
        Type[] typeArguments = null;
        if (peekChar() == '<')
        {
            typeArguments = readTypeArguments();
        }
        while (peekChar() == '.')
        {
            consume('.');
            // TODO
            readIdentifier();
            if (peekChar() == '<')
            {
                // TODO
                readTypeArguments();
            }
        }
        consume(';');
        if (typeArguments == null || typeArguments.length == 0)
        {
            return resolveClass(className);
        }
        // TODO get the owner
        return new ParameterizedTypeImpl(resolveClass(className), null, typeArguments);
    }

    private Type[] readTypeArguments()
    {
        consume('<');
        ArrayList<Type> list = new ArrayList<Type>();
        do
        {
            list.add(readTypeArgument());
        } while ((peekChar() != '>'));
        consume('>');
        Type[] arr = new Type[list.size()];
        list.toArray(arr);
        return arr;
    }

    private Type readTypeArgument()
    {
        char c = peekChar();
        if (c == '+' || c == '-')
        {
            readChar();
            // FIXME add wildcard indicator
            return readFieldTypeSignature();
        }
        else if (c == '*')
        {
            // FIXME what does this mean?
            consume('*');
            return java.lang.Object.class;
        }
        else
        {
            return readFieldTypeSignature();
        }
    }

    Type readArrayTypeSignature()
    {
        consume('[');
        switch (peekChar())
        {
            case 'L':
            case '[':
            case 'T':
                return new GenericArrayTypeImpl(readFieldTypeSignature());
            case 'Z':
                return boolean[].class;
            case 'B':
                return byte[].class;
            case 'S':
                return short[].class;
            case 'C':
                return char[].class;
            case 'I':
                return int[].class;
            case 'F':
                return float[].class;
            case 'J':
                return long[].class;
            case 'D':
                return double[].class;
            default:
                throw new GenericSignatureFormatError();
        }
    }

    Type readTypeVariableSignature()
    {
        consume('T');
        String identifier = readIdentifier();
        consume(';');
        return new UnresolvedTypeVariable(container, identifier);
    }

    private String readIdentifier()
    {
        int start = pos;
        char c;
        do
        {
            readChar();
            c = peekChar();
        } while (";:./<>-+*".indexOf(c) == -1);
        return signature.substring(start, pos);
    }

    final char peekChar()
    {
        if (pos == signature.length())
            return '\u0000';
        else
            return signature.charAt(pos);
    }

    final char readChar()
    {
        return signature.charAt(pos++);
    }

    final void consume(char c)
    {
        if (readChar() != c)
            throw new GenericSignatureFormatError();
    }
}
