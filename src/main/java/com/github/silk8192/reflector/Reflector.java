package com.github.silk8192.reflector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public class Reflector {

    private static Map<Class<?>, Class<?>> primitiveMap;

    static {
        primitiveMap = new HashMap<>();
        primitiveMap.put(int.class, Integer.class);
        primitiveMap.put(boolean.class, Boolean.class);
        primitiveMap.put(void.class, Void.class);
        primitiveMap.put(byte.class, Byte.class);
        primitiveMap.put(char.class, Character.class);
        primitiveMap.put(short.class, Short.class);
        primitiveMap.put(float.class, Float.class);
        primitiveMap.put(long.class, Long.class);
        primitiveMap.put(double.class, Double.class);
    }

    private boolean isClass;
    private Object object;
    private Logger logger = LoggerFactory.getLogger(Reflector.class);

    public Reflector(Class<?> clazz) {
        this.object = clazz;
        this.isClass = true;
    }

    public Reflector(Object object) {
        this.object = object;
        this.isClass = false;
    }

    public static Reflector forClass(Class<?> clazz) {
        return new Reflector(clazz);
    }

    /**
     * Gets the object representation of a primitive.
     *
     * @param type a primitive reference
     * @return the object representation of a primitive
     */
    private static Class<?> handlePrimitiveCasting(Class<?> type) {
        if (type == null)
            return null;
        else if (type.isPrimitive())
            return primitiveMap.get(type);
        return type;
    }

    /**
     * Instantiates an object with its arguments.
     *
     * @param args The arguments of the constructor
     * @return the Reflector object for chaining
     */
    public Reflector create(Object... args) {
        Class<?>[] types = getParameterTypes(args);
        try {
            Constructor<?> constructor = getType().getConstructor(types);
            Object obj = constructor.newInstance(args);
            return new Reflector(obj);
        } catch (NoSuchMethodException e) {
            for (Constructor<?> constructor : getType().getDeclaredConstructors()) {
                if (checkArgumentTypes(constructor.getParameterTypes(), types)) {
                    try {
                        return new Reflector(constructor.newInstance(args));
                    } catch (Exception e1) {
                        logger.error("Could not create object", e1);
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error("Could not access or create from constructor", e);
        }
        return this;
    }

    /**
     * Calls a method from the class.
     *
     * @param name The name of the method
     * @param args The method's arguments
     * @return the Reflector object for chaining
     */
    public Reflector invoke(String name, Object... args) {
        Class<?>[] types = getParameterTypes(args);
        Optional<Reflector> reflector = Optional.empty();
        try {
            Method method = getType().getMethod(name, types);
            if (checkArgumentTypes(method.getParameterTypes(), types)) {
                logger.info("Method does not match argument types!");
                throw new NoSuchMethodException();
            } else if (method.getReturnType() == void.class) {
                method.invoke(object, args);
                return new Reflector(object);
            } else {
                return new Reflector(method.invoke(object, args));
            }
        } catch (NoSuchMethodException e) {
            for (Method method : getType().getMethods()) {
                if (method.getName().equals(name) && checkArgumentTypes(method.getParameterTypes(), types)) {
                    try {
                        if (method.getReturnType() == void.class) {
                            method.invoke(object, args);
                            return new Reflector(object);
                        } else {
                            return new Reflector(method.invoke(object, args));
                        }
                    } catch (IllegalAccessException | InvocationTargetException e1) {
                        logger.error("Could not access method or find similar method", e1);
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("Could not access method", e);
        }
        return reflector.get();
    }

    /**
     * Sets the value of a field.
     *
     * @param name     The name of the field
     * @param newValue The new value of the field
     * @return the Reflector object for chaining
     */
    public Reflector set(String name, Object newValue) {
        try {
            getType().getField(name).set(object, newValue);
            return this;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            logger.error("Could not access or find field!", e);
        }
        return null;
    }

    /**
     * Gets the value of a field.
     *
     * @param name The name of the field
     * @param <T>  The type of the field
     * @return The value of the field
     */
    public <T> T get(String name) {
        try {
            Field field = getType().getField(name);
            return (T) field.get(object);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            logger.error("Could not access or find field!", e);
        }
        return (T) object;
    }

    /**
     * Returns the object.
     *
     * @param <T> The generic type of the object
     * @return The object
     */
    public <T> T get() {
        return (T) object;
    }

    private Class<?>[] getParameterTypes(Object... args) {
        if (args == null)
            return new Class<?>[0];
        Class<?>[] types = new Class<?>[args.length];
        IntStream.range(0, args.length).forEach(index -> types[index] = args[index].getClass());
        return types;
    }

    /**
     * Checks if given arguments can be fed to a method with slightly different argument requirements, such as {@code Integer} vs {@code int}.
     *
     * @param declaredTypes The arguments of the original method
     * @param givenTypes    The given argument types
     * @return whether the types are compatible
     */
    private boolean checkArgumentTypes(Class<?>[] declaredTypes, Class<?>[] givenTypes) {
        if (declaredTypes.length == givenTypes.length) {
            for (int i = 0; i < givenTypes.length; i++) {
                if (handlePrimitiveCasting(declaredTypes[i]).isAssignableFrom(handlePrimitiveCasting(givenTypes[i])))
                    continue;
                return false;
            }
            return true;
        } else
            return false;
    }

    /**
     * A utility method for differentiation between a {@code Class} object or its instantiated form as an object.
     *
     * @return a {@code Class} representation
     */
    private Class<?> getType() {
        if (isClass)
            return (Class<?>) object;
        else
            return object.getClass();
    }

}
