/* VMClass.java -- VM Specific Class methods
   Copyright (C) 2003, 2004, 2005 Free Software Foundation

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

package java.lang;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/*
 * This class is a reference version, mainly for compiling a class library
 * jar.  It is likely that VM implementers replace this with their own
 * version that can communicate effectively with the VM.
 */

/**
 *
 * @author Etienne Gagnon (etienne.gagnon@uqam.ca)
 * @author Archie Cobbs (archie@dellroad.org)
 * @author C. Brian Jones (cbj@gnu.org)
 * @author Tom Tromey (tromey@cygnus.com)
 * @author Andrew John Hughes (gnu_andrew@member.fsf.org)
 */
final class VMClass 
{

  // Only static methods. Cannot be instantiated.
  private VMClass()
  {
  }

  /**
   * Discover whether an Object is an instance of this Class.  Think of it
   * as almost like <code>o instanceof (this class)</code>.
   *
   * @param klass the Class object that's calling us
   * @param o the Object to check
   * @return whether o is an instance of this class
   * @since 1.1
   */
  static native boolean isInstance(Class klass, Object o);

  /**
   * Discover whether an instance of the Class parameter would be an
   * instance of this Class as well.  Think of doing
   * <code>isInstance(c.newInstance())</code> or even
   * <code>c.newInstance() instanceof (this class)</code>. While this
   * checks widening conversions for objects, it must be exact for primitive
   * types.
   *
   * @param klass the Class object that's calling us
   * @param c the class to check
   * @return whether an instance of c would be an instance of this class
   *         as well
   * @throws NullPointerException if c is null
   * @since 1.1
   */
  static native boolean isAssignableFrom(Class klass, Class c);

  /**
   * Check whether this class is an interface or not.  Array types are not
   * interfaces.
   *
   * @param klass the Class object that's calling us
   * @return whether this class is an interface or not
   */
  static native boolean isInterface(Class klass);

  /**
   * Return whether this class is a primitive type.  A primitive type class
   * is a class representing a kind of "placeholder" for the various
   * primitive types, or void.  You can access the various primitive type
   * classes through java.lang.Boolean.TYPE, java.lang.Integer.TYPE, etc.,
   * or through boolean.class, int.class, etc.
   *
   * @param klass the Class object that's calling us
   * @return whether this class is a primitive type
   * @see Boolean#TYPE
   * @see Byte#TYPE
   * @see Character#TYPE
   * @see Short#TYPE
   * @see Integer#TYPE
   * @see Long#TYPE
   * @see Float#TYPE
   * @see Double#TYPE
   * @see Void#TYPE
   * @since 1.1
   */
  static native boolean isPrimitive(Class klass);

  /**
   * Get the name of this class, separated by dots for package separators.
   * Primitive types and arrays are encoded as:
   * <pre>
   * boolean             Z
   * byte                B
   * char                C
   * short               S
   * int                 I
   * long                J
   * float               F
   * double              D
   * void                V
   * array type          [<em>element type</em>
   * class or interface, alone: &lt;dotted name&gt;
   * class or interface, as element type: L&lt;dotted name&gt;;
   *
   * @param klass the Class object that's calling us
   * @return the name of this class
   */
  static native String getName(Class klass);

  /**
   * Get the direct superclass of this class.  If this is an interface,
   * Object, a primitive type, or void, it will return null. If this is an
   * array type, it will return Object.
   *
   * @param klass the Class object that's calling us
   * @return the direct superclass of this class
   */
  static native <T> Class<? super T> getSuperclass(Class<T> klass);

  /**
   * Get the interfaces this class <EM>directly</EM> implements, in the
   * order that they were declared. This returns an empty array, not null,
   * for Object, primitives, void, and classes or interfaces with no direct
   * superinterface. Array types return Cloneable and Serializable.
   *
   * @param klass the Class object that's calling us
   * @return the interfaces this class directly implements
   */
  static native Class[] getInterfaces(Class klass);

  /**
   * If this is an array, get the Class representing the type of array.
   * Examples: "[[Ljava.lang.String;" would return "[Ljava.lang.String;", and
   * calling getComponentType on that would give "java.lang.String".  If
   * this is not an array, returns null.
   *
   * @param klass the Class object that's calling us
   * @return the array type of this class, or null
   * @see Array
   * @since 1.1
   */
  static native Class getComponentType(Class klass);

  /**
   * Get the modifiers of this class.  These can be decoded using Modifier,
   * and is limited to one of public, protected, or private, and any of
   * final, static, abstract, or interface. An array class has the same
   * public, protected, or private modifier as its component type, and is
   * marked final but not an interface. Primitive types and void are marked
   * public and final, but not an interface.
   *
   * @param klass the Class object that's calling us
   * @param ignoreInnerClassesAttrib if set, return the real modifiers, not
   * the ones specified in the InnerClasses attribute.
   * @return the modifiers of this class
   * @see Modifer
   * @since 1.1
   */
  static native int getModifiers(Class klass, boolean ignoreInnerClassesAttrib);

  /**
   * If this is a nested or inner class, return the class that declared it.
   * If not, return null.
   *
   * @param klass the Class object that's calling us
   * @return the declaring class of this class
   * @since 1.1
   */
  static native Class getDeclaringClass(Class klass);

  /**
   * Like <code>getDeclaredClasses()</code> but without the security checks.
   *
   * @param klass the Class object that's calling us
   * @param pulicOnly Only public classes should be returned
   */
  static native Class[] getDeclaredClasses(Class klass, boolean publicOnly);

  /**
   * Like <code>getDeclaredFields()</code> but without the security checks.
   *
   * @param klass the Class object that's calling us
   * @param pulicOnly Only public fields should be returned
   */
  static native Field[] getDeclaredFields(Class klass, boolean publicOnly);

  /**
   * Like <code>getDeclaredMethods()</code> but without the security checks.
   *
   * @param klass the Class object that's calling us
   * @param pulicOnly Only public methods should be returned
   */
  static native Method[] getDeclaredMethods(Class klass, boolean publicOnly);

  /**
   * Like <code>getDeclaredConstructors()</code> but without
   * the security checks.
   *
   * @param klass the Class object that's calling us
   * @param pulicOnly Only public constructors should be returned
   */
  static native Constructor[] getDeclaredConstructors(Class klass, boolean publicOnly);

  /**
   * Return the class loader of this class.
   *
   * @param klass the Class object that's calling us
   * @return the class loader
   */
  static native ClassLoader getClassLoader(Class klass);

  /**
   * VM implementors are free to make this method a noop if 
   * the default implementation is acceptable.
   *
   * @param name the name of the class to find
   * @return the Class object representing the class or null for noop
   * @throws ClassNotFoundException if the class was not found by the
   *         classloader
   * @throws LinkageError if linking the class fails
   * @throws ExceptionInInitializerError if the class loads, but an exception
   *         occurs during initialization
   */
  static native Class forName(String name) throws ClassNotFoundException;

  /**
   * Return whether this class is an array type.
   *
   * @param klass the Class object that's calling us
   * @return true if this class is an array type
   * operation
   */
  static native boolean isArray(Class klass);

  /**
   * This method should trigger class initialization (if the
   * class hasn't already been initialized)
   * 
   * @param klass the Class object that's calling us
   * @throws ExceptionInInitializerError if an exception
   *         occurs during initialization
   */
  static native void initialize(Class klass);

  /**
   * Load an array class.
   *
   * @return the Class object representing the class
   * @throws ClassNotFoundException if the class was not found by the
   *         classloader
   */
  static native Class loadArrayClass(String name, ClassLoader classloader)
	throws ClassNotFoundException;

  /**
   * Throw a checked exception without declaring it.
   */
  static native void throwException(Throwable t);

  /**
   * Downcast object to the class' type.
   */
  static native <K> K cast(Object obj, Class<K> k);

  /**
   * Returns true if this class is a synthetic class, generated by the
   * compiler.
   *
   * @param klass the Class object that's calling us
   * @return whether this class is synthetic or not
   */
  static native boolean isSynthetic(Class klass);

  /**
   * Returns true if this class represents an annotation.
   *
   * @param klass the Class object that's calling us
   * @return whether this class is an annotation or not
   */
  static native boolean isAnnotation(Class klass);

  /**
   * Returns true if this class was declared as an enum.
   *
   * @param klass the Class object that's calling us
   * @return whether this class is an enumeration or not
   */
  static native boolean isEnum(Class klass);

  /**
   * Returns the simple name for the specified class, as used in the source
   * code.  For normal classes, this is the content returned by
   * <code>getName()</code> which follows the last ".".  Anonymous
   * classes have no name, and so the result of calling this method is
   * "".  The simple name of an array consists of the simple name of
   * its component type, followed by "[]".  Thus, an array with the 
   * component type of an anonymous class has a simple name of simply
   * "[]".
   *
   * @param klass the class whose simple name should be returned. 
   * @return the simple name for this class.
   */
  static String getSimpleName(Class<?> klass)
  {
    if (isArray(klass))
      {
	return getComponentType(klass).getSimpleName() + "[]";
      }
    String fullName = getName(klass);
    return fullName.substring(fullName.lastIndexOf(".") + 1);
  }

  /**
   * Returns the enumeration constants of this class, or
   * null if this class is not an <code>Enum</code>.
   *
   * @param klass the class whose enumeration constants should be returned.
   * @return an array of <code>Enum</code> constants
   *         associated with this class, or null if this
   *         class is not an <code>enum</code>.
   * @since 1.5
   */
  static <T> T[] getEnumConstants(Class<T> klass)
  {
    if (isEnum(klass))
      {
	try
	  {
	    return (T[])
	      klass.getMethod("values").invoke(null);
	  }
	catch (NoSuchMethodException exception)
	  {
	    throw new Error("Enum lacks values() method");
	  }
	catch (IllegalAccessException exception)
	  {
	    throw new Error("Unable to access Enum class");
	  }
	catch (InvocationTargetException exception)
	  {
	    throw new
	      RuntimeException("The values method threw an exception",
			       exception);
	  }
      }
    else
      {
	return null;
      }
  }

  /**
   * Returns all annotations directly defined by the specified class.  If
   * there are no annotations associated with this class, then a zero-length
   * array will be returned.  The returned array may be modified by the client
   * code, but this will have no effect on the annotation content of this
   * class, and hence no effect on the return value of this method for
   * future callers.
   *
   * @param klass the class whose annotations should be returned.
   * @return the annotations directly defined by the specified class.
   * @since 1.5
   */
  static native Annotation[] getDeclaredAnnotations(Class<?> klass);

  /**
   * <p>
   * Returns the canonical name of the specified class, as defined by section
   * 6.7 of the Java language specification.  Each package, top-level class,
   * top-level interface and primitive type has a canonical name.  A member
   * class has a canonical name, if its parent class has one.  Likewise,
   * an array type has a canonical name, if its component type does.
   * Local or anonymous classes do not have canonical names.
   * </p>
   * <p>
   * The canonical name for top-level classes, top-level interfaces and
   * primitive types is always the same as the fully-qualified name.
   * For array types, the canonical name is the canonical name of its
   * component type with `[]' appended.  
   * </p>
   * <p>
   * The canonical name of a member class always refers to the place where
   * the class was defined, and is composed of the canonical name of the
   * defining class and the simple name of the member class, joined by `.'.
   *  For example, if a <code>Person</code> class has an inner class,
   * <code>M</code>, then both its fully-qualified name and canonical name
   * is <code>Person.M</code>.  A subclass, <code>Staff</code>, of
   * <code>Person</code> refers to the same inner class by the fully-qualified
   * name of <code>Staff.M</code>, but its canonical name is still
   * <code>Person.M</code>.
   * </p>
   * <p>
   * Where no canonical name is present, <code>null</code> is returned.
   * </p>
   *
   * @param klass the class whose canonical name should be retrieved.
   * @return the canonical name of the class, or <code>null</code> if the
   *         class doesn't have a canonical name.
   * @since 1.5
   */
  static String getCanonicalName(Class<?> klass)
  {
    if (isArray(klass))
      {
	String componentName = getComponentType(klass).getCanonicalName();
	if (componentName != null)
	  return componentName + "[]";
      }
    if (isMemberClass(klass))
      {
	String memberName = getDeclaringClass(klass).getCanonicalName();
	if (memberName != null)
	  return memberName + "." + getSimpleName(klass);
      }
    if (isLocalClass(klass) || isAnonymousClass(klass))
      return null;
    return getName(klass);
  }

  /**
   * Returns the class which immediately encloses the specified class.  If
   * the class is a top-level class, this method returns <code>null</code>.
   *
   * @param klass the class whose enclosing class should be returned.
   * @return the immediate enclosing class, or <code>null</code> if this is
   *         a top-level class.
   * @since 1.5
   */
  static native Class<?> getEnclosingClass(Class<?> klass);

  /**
   * Returns the constructor which immediately encloses the specified class.
   * If the class is a top-level class, or a local or anonymous class 
   * immediately enclosed by a type definition, instance initializer
   * or static initializer, then <code>null</code> is returned.
   *
   * @param klass the class whose enclosing constructor should be returned.
   * @return the immediate enclosing constructor if the specified class is
   *         declared within a constructor.  Otherwise, <code>null</code>
   *         is returned.
   * @since 1.5
   */
  static native Constructor<?> getEnclosingConstructor(Class<?> klass);

  /**
   * Returns the method which immediately encloses the specified class.  If
   * the class is a top-level class, or a local or anonymous class 
   * immediately enclosed by a type definition, instance initializer
   * or static initializer, then <code>null</code> is returned.
   *
   * @param klass the class whose enclosing method should be returned.
   * @return the immediate enclosing method if the specified class is
   *         declared within a method.  Otherwise, <code>null</code>
   *         is returned.
   * @since 1.5
   */
  static native Method getEnclosingMethod(Class<?> klass);

  /**
   * <p>
   * Returns an array of <code>Type</code> objects which represent the
   * interfaces directly implemented by the specified class or extended by the
   * specified interface.
   * </p>
   * <p>
   * If one of the superinterfaces is a parameterized type, then the
   * object returned for this interface reflects the actual type
   * parameters used in the source code.  Type parameters are created
   * using the semantics specified by the <code>ParameterizedType</code>
   * interface, and only if an instance has not already been created.
   * </p>
   * <p>
   * The order of the interfaces in the array matches the order in which
   * the interfaces are declared.  For classes which represent an array,
   * an array of two interfaces, <code>Cloneable</code> and
   * <code>Serializable</code>, is always returned, with the objects in
   * that order.  A class representing a primitive type or void always
   * returns an array of zero size.
   * </p>
   *
   * @param klass the class whose generic interfaces should be retrieved.
   * @return an array of interfaces implemented or extended by the specified
   *         class.
   * @throws GenericSignatureFormatError if the generic signature of one
   *         of the interfaces does not comply with that specified by the Java
   *         Virtual Machine specification, 3rd edition.
   * @throws TypeNotPresentException if any of the superinterfaces refers
   *         to a non-existant type.
   * @throws MalformedParameterizedTypeException if any of the interfaces
   *         refer to a parameterized type that can not be instantiated for
   *         some reason.
   * @since 1.5
   * @see java.lang.reflect.ParameterizedType
   */
  static native Type[] getGenericInterfaces(Class<?> klass);

  /**
   * <p>
   * Returns a <code>Type</code> object representing the direct superclass,
   * whether class, interface, primitive type or void, of the specified class.
   * If the class is an array class, then a class instance representing
   * the <code>Object</code> class is returned.  If the class is primitive,
   * an interface, or a representation of either the <code>Object</code>
   * class or void, then <code>null</code> is returned.
   * </p>
   * <p>
   * If the superclass is a parameterized type, then the
   * object returned for this interface reflects the actual type
   * parameters used in the source code.  Type parameters are created
   * using the semantics specified by the <code>ParameterizedType</code>
   * interface, and only if an instance has not already been created.
   * </p>
   *
   * @param klass the class whose generic superclass should be obtained.
   * @return the superclass of the specified class.
   * @throws GenericSignatureFormatError if the generic signature of the
   *         class does not comply with that specified by the Java
   *         Virtual Machine specification, 3rd edition.
   * @throws TypeNotPresentException if the superclass refers
   *         to a non-existant type.
   * @throws MalformedParameterizedTypeException if the superclass
   *         refers to a parameterized type that can not be instantiated for
   *         some reason.
   * @since 1.5
   * @see java.lang.reflect.ParameterizedType
   */
  static native Type getGenericSuperclass(Class<?> klass);

  /**
   * Returns an array of <code>TypeVariable</code> objects that represents
   * the type variables declared by the specified class, in declaration order.
   * An array of size zero is returned if the specified class has no type
   * variables.
   *
   * @param klass the class whose type variables should be returned.
   * @return the type variables associated with this class. 
   * @throws GenericSignatureFormatError if the generic signature does
   *         not conform to the format specified in the Virtual Machine
   *         specification, version 3.
   * @since 1.5
   */
  static native <T> TypeVariable<Class<T>>[] getTypeParameters(Class<T> klass);

  /**
   * Returns true if the specified class represents an anonymous class.
   *
   * @param klass the klass to test.
   * @return true if the specified class represents an anonymous class.
   * @since 1.5
   */
  static native boolean isAnonymousClass(Class<?> klass);

  /**
   * Returns true if the specified class represents an local class.
   *
   * @param klass the klass to test.
   * @return true if the specified class represents an local class.
   * @since 1.5
   */
  static native boolean isLocalClass(Class<?> klass);

  /**
   * Returns true if the specified class represents an member class.
   *
   * @param klass the klass to test. 
   * @return true if the specified class represents an member class.
   * @since 1.5
   */
  static native boolean isMemberClass(Class<?> klass);

} // class VMClass
