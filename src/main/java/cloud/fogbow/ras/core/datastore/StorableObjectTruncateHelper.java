package cloud.fogbow.ras.core.datastore;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
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

    public StorableObjectTruncateHelper(Class<?> type) {
        this.type = type;
    }

    // TODO ARNETT SEE IF "<S extends T>" can be put here
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
        for (Class<?> currentType = type; currentType.equals(Object.class); currentType = currentType.getSuperclass()) {
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
        int endIndex = Math.min(originalValue.length(), newSize);
        String truncatedValue = originalValue.substring(0, endIndex);

        field.set(target, truncatedValue);
    }

    // TODO ARNETT REMOVE THIS
    public static void main(String[] args) throws UnexpectedException {
        ComputeOrder order = new ComputeOrder(new SystemUser("userId1", "userName2", "prov3"),
                "r4", "p5", "c6", "aloha7", 0, 0, 0, "i8", new ArrayList<>(), "p9", new ArrayList<>());

        Class<?> aClass = order.getClass();
        Object truncated = new StorableObjectTruncateHelper<>(aClass).truncate(order);
        System.out.println(truncated);
    }
}
