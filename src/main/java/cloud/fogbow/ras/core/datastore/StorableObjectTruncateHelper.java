package cloud.fogbow.ras.core.datastore;

import cloud.fogbow.common.exceptions.UnexpectedException;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Column;
import javax.validation.constraints.Size;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StorableObjectTruncateHelper<T> {

    private final Class<?> type;

    /**
     * Constructs a Helper for creating a clone of an object using the default constructor of <tt>type</tt>.
     * @param type
     */
    public StorableObjectTruncateHelper(Class<?> type) {
        this.type = type;
    }

    public T truncate(T object) throws UnexpectedException {
        try {
            Constructor<?> constructor = this.type.getConstructor();
            T created = (T) constructor.newInstance();
            List<Field> declaredFields = getAllFields(this.type);
            for (Field field : declaredFields) {
                boolean accessible = field.isAccessible();
                field.setAccessible(true);

                if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                    if (field.isAnnotationPresent(Column.class) && field.getType().equals(String.class)) {
                        String originalValue = (String) field.get(object);
                        if (field.isAnnotationPresent(Size.class)) {
                            Size sizeAnnotation = field.getAnnotation(Size.class);
                            setTruncatedValue(originalValue, created, field, sizeAnnotation.max());
                        } else if (field.isAnnotationPresent(Length.class)) {
                            Length lengthAnnotation = field.getAnnotation(Length.class);
                            setTruncatedValue(originalValue, created, field, lengthAnnotation.max());
                        } else {
                            setTruncatedValue(originalValue, created, field, originalValue.length());
                        }
                    } else {
                        field.set(created, field.get(object));
                    }
                }

                field.setAccessible(accessible);
            }
            return created;
        } catch (Throwable e) {
            throw new UnexpectedException("", e);
        }
    }

    protected List<Field> getAllFields(Class<?> type) {
        List<Field> allFields = new ArrayList<>();
        for (Class<?> currentType = type; currentType != null && !currentType.equals(Object.class); currentType = currentType.getSuperclass()) {
            Field[] declaredFields = currentType.getDeclaredFields();
            if (declaredFields.length == 0) {
                break;
            } else {
                allFields.addAll(Arrays.asList(declaredFields));
            }
        }
        return allFields;
    }

    private void setTruncatedValue(String originalValue, Object target, Field field, int newSize) throws IllegalAccessException {
        String truncatedValue;
        if (originalValue != null) {
            int endIndex = Math.min(originalValue.length(), newSize);
            truncatedValue = originalValue.substring(0, endIndex);
        } else {
            truncatedValue = null;
        }

        field.set(target, truncatedValue);
    }
}
